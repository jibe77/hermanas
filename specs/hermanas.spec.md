# Hermanas — System Specification

> **Spec Kit format.** This document is the source of truth for re-generating the Hermanas application. It describes the **what** and **why**, not the **how**. Implementation choices that are non-negotiable (e.g., Java 11 due to hardware) are flagged as **Constraints**. Anything else is a default the implementer may revisit.

**Version:** 1.0
**Status:** Active
**Last revised:** 2026-06-21
**Maintainer:** jibe77

---

## 1. Vision

Hermanas is a **chicken coop automation system** for a backyard flock. It opens and closes the coop door automatically around sunrise and sunset, manages light/fan/music inside the coop, monitors temperature and humidity, and provides a live camera with image analysis. A single keeper interacts with it through a web interface and (optionally) push notifications.

**Primary user:** the coop owner (one person, one coop, one Raspberry Pi).
**Live instance:** http://www.hermanas.fr

---

## 2. Constraints (non-negotiable)

These constraints flow from the **hardware target** and must shape every implementation choice.

| Constraint | Reason |
|---|---|
| **Target hardware: Raspberry Pi Zero** | The deployed device. Single ARMv6 core, 512 MB RAM. |
| **Java 11 (no higher)** | Pi Zero cannot run Java 17+. Blocks Spring Boot 3, Jakarta namespace, Pi4j 4.x, Spring Security 7, etc. |
| **Spring Boot 2.7.x** | Last 2.x line, compatible with Java 11. |
| **Pi4j 2.4.0 (pinned)** | Versions >2.4.0 are known broken on Pi Zero + Java 11. |
| **picam JNI library pinned to 2.0.1** | Versions >2.4.0 incompatible with Pi Zero + Java 11. Native `.so` deployed separately. |
| **Single user installation** | No multi-tenancy. One household, ~one keeper plus optional guest accounts. |
| **No external auth provider** | Authentication must be self-contained (local file-backed users). Previous AWS Amplify dependency was removed. |
| **Frontend SPA bundled in the same JAR** | One artifact deployment. Frontend built at Maven `package` phase only — not during `test`/`compile`. |
| **GPIO is real on Pi, faked elsewhere** | Tests and dev machines must work without hardware via a `gpio-fake` Spring profile. |

---

## 3. Functional scope

The system manages **eight functional domains**. Each domain has REST endpoints (`/api/v1/<domain>`), may have a scheduler, may publish WebSocket events, and may have a frontend module.

### 3.1 Door

The coop door is driven by a **servo motor** with two limit-switch buttons (top = fully open, bottom = fully closed).

**Behavior:**
- Opens automatically a configurable offset after sunrise.
- Closes automatically a configurable offset after sunset.
- Manual open/close via REST (rate-limited and audited).
- After each scheduled open/close, a **verification job** runs 30 minutes later. If the limit-switch reading is ambiguous, the system captures a photo and queries a local vision LLM to confirm. If the door is wrongly positioned, the keeper is notified.
- Servo also exposes direct-control endpoints (turn clockwise / counter-clockwise / arbitrary duty cycle) for calibration.
- Every operation emits an **Event** (event sourcing) with an `EventType` for audit history.

**Critical implementation notes:**
- The servo's **stop sequence** must explicitly send a zero-duty-cycle pulse train before turning the pin off, because Pi4j 2.4 software-PWM leaves the pin at the last duty cycle otherwise. Skipping this causes the servo to keep twitching.
- Open/close durations and servo positions are configurable. Defaults: open 10 000 ms, close 2 350 ms, open position 16 (CCW), close position 5 (CW), range 100.

### 3.2 Light, Fan, Music

Three appliances driven by **GPIO relays** (light, fan) and a **system command** (music via VLC). All three share the same pattern:

- `POST /switch` toggles the appliance.
- `GET /status` returns its state.
- Each appliance has a **security timer** that auto-switches it off after a delay. The delay depends on the current **consumption mode** (ECO / REGULAR / SUNNY) — see §3.7.
- State changes publish a `CoopStatus` message on the `/topic/progress` WebSocket destination so any open UI updates in real time.

**Light scheduling:** the light is switched on a configurable offset before sunset (default 0 minutes), then auto-off by the security timer.

**Music:**
- Plays a "cocorico" sound at sunrise (toggleable).
- Plays a music mix at sunset (toggleable).
- Volume controllable via `amixer`.

### 3.3 Camera

- `GET /takePicture` captures a still JPEG. Two quality presets (`high`, `regular`).
- `GET /stream` starts an MJPEG stream via `mjpg_streamer` on port 8081.
- `GET /closingRate` returns a number reflecting how confidently the door appears closed in the latest photo.
- `GET /analyze` sends the latest photo to a **local vision LLM** (configurable URL, default model `focus` / qwen2.5-vl) with a chicken-coop-specific prompt and returns the description. Rate-limited; response cached per language for 120 s.
- `CameraJob` captures a photo every 3 hours during daylight (configurable). Photos are archived to disk.
- `GET /photos` lists archived captures; `GET /photos/file` retrieves one.

### 3.4 Sensor

- A **DHT22** sensor (temperature + humidity) is read by an external Python script (`AdafruitDHT.py`), invoked from Java.
- Latest reading is cached (default 30 s).
- Sensor readings are persisted every 2 hours (configurable) for history.
- `GET /sensor` returns the latest reading; `GET /sensor/history` returns time-range queries.
- Sensor also tracks **external** temperature/humidity (from a separate source — typically the weather API).

### 3.5 Scheduler

Internally coordinates all time-driven behavior. Implementation uses Quartz + Spring `@Scheduled`.

| Job | Interval | Purpose |
|---|---|---|
| SunRelatedJob | 60 s | Master tick. Computes today's sunrise/sunset and fires four sub-events (open door, close door, switch light on, verify door). |
| CameraJob | 3 h | Capture archive photo (skips night unless overridden). |
| SensorJob | 2 h | Persist sensor reading. |
| DiskSpaceJob | 3 h | Monitor disk and memory; alert if low. |
| FanJob | 1 h | Safety timer cleanup. |
| EcoModeJob | periodic | Re-evaluate consumption mode based on current month. |

Sunrise/sunset is computed from configurable **latitude/longitude** (default: Eiffel Tower) via the `lib-sunrise-sunset` library. Result is cached for the day.

`GET /scheduler/quartz-jobs` and `GET /scheduler/quartz-status` expose the live schedule.

### 3.6 Energy / Consumption mode

The system has three **consumption modes** that affect appliance security-timer durations:

| Mode | Light timer | Fan timer | Music timer |
|---|---|---|---|
| ECO | 60 s | 10 s | 60 s |
| REGULAR | 15 min | 60 s | 20 min |
| SUNNY | 30 min | 20 min | 60 min |

A **monthly mapping** (Jan→ECO, Feb→ECO, …, Jun→SUNNY, …) drives automatic mode selection. The mapping is editable at runtime via `POST /energy/mode` and `PUT /config/...`.

Optional: switch the Pi's wifi on/off depending on mode (`wifi.disabled.<mode>` config; default off).

### 3.7 Residents (chickens)

A small CRUD for chicken profiles. Each resident has a name, breed, birth date, arrival date, optional death date, comments, and a photo. Used for record-keeping; does not affect automation.

### 3.8 System administration

- `POST /system/reboot`, `POST /system/shutdown` — physical Pi commands, **rate-limited** (max 2 per 5 min) and **audit-logged**.
- `GET /system/disk-space`, `GET /system/uptime`, `GET /system/health`.
- `GET /logs/app` — paginated app log access (so the keeper doesn't need to SSH).
- `POST /config/refresh` and `PUT /config/<key>` — runtime configuration changes with cache invalidation (no restart needed for most knobs).
- `POST /email/test` — send a test email.
- `POST /push/subscribe`, `DELETE /push/subscribe`, `GET /push/public-key` — Web Push (VAPID) for browser notifications.

---

## 4. Non-functional requirements

### 4.1 Performance & resource budget

- The Pi Zero has 512 MB RAM and one slow ARMv6 core. Implementations must avoid:
  - Heavy frameworks loaded eagerly.
  - Polling loops with sub-second granularity.
  - Synchronous calls to external services without timeouts.
- Sensor reads, weather calls, and AI calls **must** be cached.
- Logs **must** stay at INFO by default; DEBUG floods the SD card.

### 4.2 Reliability

- All external calls (weather API, vision LLM) use **timeouts and a circuit breaker** (resilience4j 1.7.1, last Java 11 line).
- A failed appliance command must not crash the scheduler tick.
- Door operation failures emit a specific `EventType` so they appear in the audit journal.
- Health indicators expose actual hardware state, not just "Spring is up."

### 4.3 Observability

- **Micrometer metrics** under the `hermanas.*` namespace: door open/close counters, failure counter, operation duration, sensor gauges, appliance switch counters.
- **Spring Boot Actuator** for health and metrics, mounted under the same security as the API.
- **Audit log** (separate logger `AUDIT`) for security-sensitive operations (login, system reboot/shutdown, door manual override).
- **Event sourcing**: every state change persists an `Event` row, queryable via `GET /events/journal` and `GET /door/events`.

### 4.4 Security

- **Authentication:** session-based, form login at `POST /api/v1/auth/login`. Returns 200/401 (no HTML redirects). Session cookie `JSESSIONID`. Optional remember-me cookie.
- **User store:** local `users.properties` file (bcrypt hashes). Path configurable. Default location next to the JAR.
- **CLI tool:** `java -jar hermanas.jar --hash <password>` outputs a `{bcrypt}…` line ready to paste into `users.properties`.
- **CSRF enabled** via `CookieCsrfTokenRepository.withHttpOnlyFalse()`. The SPA reads `XSRF-TOKEN` and echoes `X-XSRF-TOKEN`.
- **No CORS** — SPA is same-origin (bundled in the JAR).
- **Rate limiting** on `/system/reboot`, `/system/shutdown`, `/auth/login`. Custom lightweight implementation (no extra dependency).
- **Roles:** at minimum `USER` and `ADMIN`. Admin-only endpoints: system control, user management, electronics debug, config writes.

### 4.5 Internationalization

- Frontend supports at least French and English.
- AI vision prompts can be selected per language.
- User entity carries a `language` field (default `fr`).

### 4.6 Deployment

- Single fat JAR. `mvn package` produces it, including the Angular SPA at `target/classes/static/`.
- Frontend build runs only at `package` phase (bound to `frontend-maven-plugin`), so `mvn test` stays fast.
- `mvn -P!with-frontend` skips the npm build for backend-only iterations.
- A `deploy.sh` script automates Pi deployment.
- `users.properties` is **not** committed (`.gitignore`).

---

## 5. Architecture (binding)

### 5.1 Layers

```
web/             ← REST controllers (*RestController), @RestController, /api/v1/* paths
controller/      ← Services (*Service), business logic and hardware orchestration
data/repository/ ← Spring Data JPA interfaces
data/entity/     ← JPA entities
dto/             ← API DTOs (entities are never returned directly)
scheduler/       ← Quartz jobs and event listeners
health/          ← Spring Boot health indicators
security/        ← Security config, rate limiting, audit logging
websocket/       ← STOMP configuration
```

**Naming rule:** anything ending in `RestController` is a REST entry point. Anything ending in `Service` is a service. The old "Controller" suffix for services is forbidden — it confuses readers.

### 5.2 GPIO strategy

```
GpioHermanasService (interface)
├── GpioHermanasRpiService    (Pi4j 2.4.0)            — default profile
└── GpioHermanasFakeService   (in-memory mock)        — @Profile("gpio-fake")
```

All hardware-touching services depend on `GpioHermanasService`, never on Pi4j classes directly.

### 5.3 GPIO pin map (defaults, configurable)

| GPIO | Component |
|---|---|
| 14 | Light relay |
| 15 | Door bottom button (closed limit switch) |
| 18 | Door top button (open limit switch) |
| 23 | Fan relay |
| 24 | Birdhouse button |
| 25 | Door servo (PWM) |

### 5.4 Persistence

- **Production:** MariaDB.
- **Tests / dev with `gpio-fake`:** H2 in-memory.
- **Entities (binding):** `Event`, `HermanasUser`, `Parameter`, `Picture`, `PushSubscription`, `Resident`, `Sensor`.

**Constraints on `Event.eventType`:**
- Stored as **ordinal** (integer) for compactness.
- New values **must be appended** to the enum, never reordered or inserted, or historical events decode incorrectly.

**Constraint on `Parameter.entryValue`:**
- Must be a `TEXT`/`@Lob` column, not `VARCHAR(255)`. Some config values (AI prompts) exceed 1.5 kB and broke the column historically.

### 5.5 WebSocket

- Endpoint: `/api/v1/stomp` (SockJS fallback enabled).
- Broker prefix: `/topic`. Application prefix: `/app`.
- One active topic: `/topic/progress`, payload `{ appliance: string, state: string }`. Published on any light/fan/door/music state change.

### 5.6 Configuration management

- All knobs live in `application.properties`.
- A `Parameter` table mirrors runtime overrides — `GET/PUT /api/v1/config/<key>` reads and writes this table.
- Config setters use Spring `@CacheEvict` so changes take effect without restart.
- `POST /api/v1/config/refresh` evicts all caches manually.

---

## 6. REST API surface

All endpoints under `/api/v1/`. State-changing operations use `POST` / `PUT` / `DELETE`. State queries use `GET`.

**Resource groups (each = one `*RestController`):**

`auth`, `buttons`, `camera`, `captures`, `config`, `door`, `electronics`, `email`, `energy`, `events`, `fan`, `info`, `light`, `logs`, `music`, `push`, `residents`, `scheduler`, `sensor`, `system`, `users`, `weather`.

**Every controller must:**
- Use `@Tag` for Swagger grouping.
- Use `@Operation` and `@ApiResponses` on each method.
- Document parameters with `@Parameter`.
- Return DTOs, **not** JPA entities.
- Validate inputs with `@Valid`, `@NotNull`, `@Min`, `@Max`.

**Global exception handler** (`@ControllerAdvice`) returns a consistent JSON shape `{ timestamp, status, error, message }` for validation errors, constraint violations, illegal arguments, domain exceptions (`DoorNotClosedCorrectlyException` → 422, `PredictionException` → 500), and rate-limit denials (429).

**Swagger UI:** `/swagger-ui/index.html?configUrl=/v3/api-docs/swagger-config`.

---

## 7. Frontend

- **Framework:** Angular 21.x, standalone components.
- **Test runner:** Vitest (not Karma).
- **Build:** integrated into the Maven `package` phase via `frontend-maven-plugin`. Output copied to `target/classes/static/`.
- **Routing:** lazy-loaded feature modules. SPA fallback: unknown routes serve `index.html`.

**Feature modules (one per functional domain):**
`auth`, `camera`, `dashboard`, `electronics`, `energy`, `logs`, `music`, `notification`, `residents`, `system`, `utility`, `weather`.

**Cross-cutting features:**
- **Demo mode**: toggleable from the top-nav user menu. Suppresses error toasts, exposes admin links, opens a welcome modal once per session, and intercepts mutations with a confirmation modal.
- **Visual themes** (purely cosmetic, URL-driven `?theme=…`): `advent` (snowfall, xmas tree, Santa hat on brand hen), `easter` (pastel field), `halloween` (spider web, witch hat), `april` (April Fool's strip).
- **Easter eggs:** clicking a hen in the strip makes it jump; the Konami code makes the strip dance for ~6 s; 5 clicks on the brand hen within 3 s opens an 8-s disco overlay.

---

## 8. Out of scope (explicitly)

- Multi-user / multi-coop SaaS deployment.
- Spring Boot 3 / Java 17 migration (blocked by Pi Zero hardware).
- Replacing the Python DHT22 reader with a Java-native driver (the Python script is reliable and cheap).
- Cloud-based image inference (the local LLM endpoint is configurable and may be on a separate machine on the LAN, but is not a SaaS dependency).
- Mobile apps (the SPA is responsive and PWA-installable via service-worker).

---

## 9. Acceptance criteria (smoke level)

A re-generated Hermanas is acceptable when:

1. `mvn package` produces a single runnable JAR including the SPA.
2. `mvn test` runs the backend test suite without invoking npm.
3. `java -jar hermanas.jar --hash secret` prints a usable bcrypt line.
4. `java -jar hermanas.jar` with `gpio-fake` profile starts on a laptop, serves the SPA at `/`, accepts login from `users.properties`, and exposes `/api/v1/door/status` returning a fake-but-consistent payload.
5. On a real Pi Zero with a wired servo and limit switches, `POST /api/v1/door/open` opens the door and `POST /api/v1/door/close` closes it, with audit log entries and `Event` rows persisted.
6. `SunRelatedJob` correctly computes sunrise/sunset for the configured location and triggers the four sub-events at the right times.
7. `/actuator/health` reports door, light, sensor, weather, mail+camera, and repository indicators.
8. `/api/v1/config/refresh` clears caches; subsequent `GET /api/v1/sensor` re-reads the sensor.
9. Frontend `?theme=advent` renders the snowfall layer; demo mode opens the welcome modal exactly once.
10. Playwright e2e suite passes against `localhost:4200`.

---

## 10. Open questions for re-generation

If the re-generator hits any of these, **ask the maintainer** before deciding:

- Should the AI vision endpoint be made mandatory or optional? (Currently optional; empty URL = 501.)
- Should consumption-mode monthly mapping be northern-hemisphere-biased or fully configurable?
- Should `users.properties` allow runtime add/remove via REST, or stay file-only? (Current: both, REST under `/api/v1/users` for ADMIN.)
- Should remember-me tokens be invalidated on password change? (Currently no — rotation only via key change.)
