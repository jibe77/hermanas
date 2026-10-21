# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Hermanas is a chicken coop automation system built with Spring Boot, running on a **Raspberry Pi Zero 2 W** (hostname `pru`). It automates door control based on sunrise/sunset, manages lighting, camera monitoring, temperature/humidity sensors, and fan control.

**Runtime:** **Java 25** + **Spring Boot 4.1.x**, on Raspberry Pi OS Trixie arm64. The old
Java 11 / Spring Boot 2.x constraint is gone — it came from the original Pi Zero
(`poupou`), decommissioned during the 2026 hardware migration. See *Deployment & hardware*
below.

- **Frontend:** bundled in this repo under `frontend/` and packaged into the same JAR.
  (The standalone https://github.com/jibe77/hermanasclient repo is the historical source.)
- **Live instance:** http://www.hermanas.fr

## Build & Development Commands

```bash
# Build the project
mvn clean package

# Run the application
mvn spring-boot:run

# Run on non-Pi hardware (fake GPIO)
mvn spring-boot:run -Dspring.profiles.active=gpio-fake

# Run all tests
mvn test

# Run tests excluding image processing tests
mvn test -DexcludedGroups=image_processing

# Check dependency updates
mvn versions:display-dependency-updates

# Check maven plugins updates
mvn versions:display-plugin-updates

# Frontend (from frontend/)
npm test            # Vitest, single pass
npm run e2e         # Playwright

# automate deployment on the chicken-coop (default target: pru)
./deploy.sh
./deploy.sh poupou        # old Pi Zero, rollback target
./deploy.sh --skip-build  # reuse the current JAR
./deploy.sh --internet    # public hostname + box SSH port instead of pru.local (mDNS)
```

`mvn package` also builds the Angular SPA (`frontend-maven-plugin`) and bundles it into the
JAR — hence the ~105 MB artifact and the slow package step. `mvn test` / `mvn compile` skip
the npm build and stay fast.

## Deployment & hardware

Current target: **`pru`**, a Raspberry Pi Zero 2 W on Raspberry Pi OS Trixie arm64 — migrated
in 2026 from `poupou` (original Pi Zero, kept as rollback). Key facts that shape the code:

- **Java 25** installed via apt, **Spring Boot 4.1**, `jakarta.*` throughout (no `javax.*` left).
- **GPIO via `pi4j-plugin-ffm`** (Foreign Function & Memory, needs Java 22+), talking to
  `/dev/gpiochip0`. Replaced the pigpio/`/dev/mem` stack, which Debian Trixie dropped.
  FFM needs `--enable-native-access=ALL-UNNAMED` to avoid restricted-method warnings.
- **Runs as the non-root `hermanas` user** from `/var/lib/hermanas/` (see `deploy.sh`) — that
  is where `application.properties`, `users.properties`, `photos/`, `music/` and the keystore
  live. Not `/home/pi/`. Needs supplementary groups `gpio` (chardev) and `audio` (cvlc).
- **Reverse proxy:** Traefik (TLS) → nginx (`deploy/nginx/hermanas-cache.conf`, caching) →
  Spring on `:8080`. `/api/**` and WebSockets are never cached; hashed assets cache 1 year;
  HTML 60 s. nginx forwards `X-Forwarded-*`.
  ⚠️ nginx gotcha documented in that file: redefining a single `proxy_set_header` inside a
  `location` block drops all inherited ones, so each block re-declares them.
- **512 MB RAM**, CPU deliberately underclocked to 600 MHz. Keep dependencies lean and cap the
  heap; npm builds never run on the device.

The launch wrapper (`Hermanas.sh`) and the systemd unit live **on the device**, not in this
repo — check there for the actual JVM flags.
- **Monitoring:** `prometheus-node-exporter` on `pru`; Prometheus + Grafana on `shannen`.

## Architecture

### Layer Structure

The application follows a **REST Controller-Service-Repository** pattern with an IoT twist:

- **REST Controllers** (`web/` package, `*RestController` classes) - REST endpoints marked with `@RestController`
- **Services** (`service/` package, `*Service` classes) - Hardware abstraction and business logic (door, light, fan, camera, sensor)
- **Repositories** (`data/repository/`) - Spring Data JPA interfaces
- **Schedulers** (`scheduler/`) - Quartz-based jobs for sun-related automation

The package-naming refactor described in earlier phases is **done**: REST controllers live in
`web/`, services in `service/`. The `controller/` directory still exists on disk but is empty —
safe to delete.

### Key Packages

| Package | Purpose |
|---------|---------|
| `web/` | REST controllers (`*RestController`), `WebConfig` (SPA routing), `SpaErrorController` |
| `service/door/` | Door services: servo motor control, button management |
| `service/gpio/` | GPIO abstraction with real (Rpi) and fake implementations |
| `service/fan/`, `service/light/`, `service/camera/`, etc. | Hardware control services |
| `security/` | `SecurityConfig`, DB-backed users, remember-me, rate limiting, audit, Siri token |
| `scheduler/job/` | Periodic jobs: SunRelatedJob (60s), CameraJob (3h), DiskSpaceJob |
| `scheduler/event/` | Event handlers for door opening/closing based on sunrise/sunset |
| `scheduler/sun/` | Sun time calculations with caching |
| `websocket/` | STOMP/WebSocket for real-time status notifications |
| `health/` | Custom Spring Boot health indicators |
| `metrics/` | `HermanasMetrics` — Micrometer counters/gauges/timers |
| `image/` | Image processing (chicken/egg/dirt counting) |

### GPIO Strategy Pattern

```
GpioHermanasService (interface)
├── GpioHermanasRpiService  - Real Pi4j hardware implementation
└── GpioHermanasFakeService - Mock for testing (@Profile("gpio-fake"))
```

## Configuration

Main configuration in `application.properties`. Key externalized parameters:

- **GPIO pins:** door servo (**12** — hardware PWM; moved off 25 during the Pi4j FFM
  migration), door buttons (15 bottom, 18 up), birdhouse button (24), light relay (14),
  fan relay (23)
- **Door timing:** opening 10000 ms, closing 2350 ms, servo positions 5–16
- **Sun scheduling:** location coordinates, door delays after sunrise/sunset
- **Camera:** resolution, quality, rotation (180), streaming port
- **Auth/session:** `hermanas.security.users-file`, `hermanas.security.remember-me-key`
  (⚠️ still `change-me-in-production-please` in the committed default — override in prod;
  rotating it invalidates every remember-me cookie), `spring.session.timeout`

In production these are overridden by `/var/lib/hermanas/application.properties`
(not in git — it holds the DB credentials).

Test profile `application-gpio-fake.properties` uses H2 in-memory database.

## Database

- **Production:** MariaDB
- **Testing:** H2 in-memory
- **Entities:** Parameter, Sensor, Event, Picture, HermanasUser, Resident, PushSubscription
- **Schema:** Hibernate `ddl-auto=update` for entities, plus `schema.sql`
  (`spring.sql.init.mode=always`, run after Hibernate via
  `spring.jpa.defer-datasource-initialization=true`) for non-JPA tables such as
  `persistent_logins`. `spring-session-jdbc` creates its own tables.
  ⚠️ `ParameterColumnMigration` issues MariaDB-specific `ALTER TABLE … MODIFY`, which fails
  harmlessly on H2 — expected noise in dev logs.

## Authentication & Session

Form login + session cookie, no Amplify, no HTTP Basic, no CORS (the SPA is same-origin,
served from the same JAR).

- **Login:** `POST /api/v1/auth/login` (form-urlencoded) → 200 / 401. `GET /api/v1/auth/me`
  reports the current session.
- **Users:** stored in the DB (`HermanasUser`), served by `DbUserDetailsService`. On an empty
  table it bootstraps from `users.properties` (path via `hermanas.security.users-file`);
  `marguerite` is forced to `ADMIN`. Self-registration creates `NOT_VALIDATED_YET` accounts
  that cannot log in until an admin promotes them (`PendingValidationUserDetailsChecker`).
  ⚠️ **Known bug:** `java -jar hermanas.jar --hash <pw>` prints a `{bcrypt}`-prefixed hash,
  but `passwordEncoder()` is a bare `BCryptPasswordEncoder` that doesn't strip that prefix —
  pasting the output verbatim into `users.properties` yields a permanent 401. Strip the
  `{bcrypt}` prefix until this is fixed (Phase 10).
- **CSRF:** enabled, `XSRF-TOKEN` cookie echoed by Angular in `X-XSRF-TOKEN`. Requires the
  non-XOR `CsrfTokenRequestAttributeHandler` with `setCsrfRequestAttributeName(null)` —
  otherwise Spring Security 6+ defers token creation, the cookie is never written, and every
  POST 403s. Requests carrying `X-Siri-Token` are exempt.
- **Sessions:** persisted in the DB via `spring-session-jdbc` (survives redeploys),
  `spring.session.timeout = 30m`.
- **Remember-me:** persistent-token strategy, `hermanas-remember-me` cookie, 31 days,
  `persistent_logins` table (created by `schema.sql`).
  ⚠️ Rotation is wrapped by **`GraceWindowRememberMeServices`**: the token rotates on every
  use, so the SPA's parallel boot requests would otherwise look like cookie theft — and the
  error response's cookie-deletion header would wipe the freshly rotated cookie, silently
  killing remember-me. The grace window (30 s) accepts the just-superseded token without
  rotating or rewriting the cookie. Genuine replays after the window still raise
  `CookieTheftException`. Don't "fix" a theft exception by silencing its logger.

**Authorization model is inverted:** everything is `permitAll` by default; only mutating verbs
on `/api/v1/**` (plus a handful of legacy state-changing GETs) require a role. The dashboard is
a read-only public showcase. Admins do **not** inherit `USER` (no role hierarchy), so use
`hasAnyRole(USER, ADMIN)`.

## API Documentation

Swagger UI available at: `/swagger-ui/index.html?configUrl=/v3/api-docs/swagger-config`

## Testing Notes

### Backend (JUnit 5 + Spring Boot Test)
- Image processing tests use `@Tag("image_processing")` and are excluded by default
- Use `gpio-fake` profile to run tests without Raspberry Pi hardware
- ⚠️ **`src/test/resources/application.properties` fully shadows the one in `src/main/resources`**
  (test classpath wins — it is not merged). It omits many keys, so a broad `@SpringBootTest`
  loading the whole context fails on unresolved placeholders (`cors.origins.allowed`,
  `camera.streaming.command`, …) and on the missing `JavaMailSender`. Existing tests dodge this
  by using narrow slices (`@SpringBootTest(classes = {OneService.class})`). Widening a test means
  either reconciling that file or passing the keys via `@SpringBootTest(properties = {...})`.
- ⚠️ **Spring Boot 4 moved the test annotations**: `@AutoConfigureMockMvc` is now
  `org.springframework.boot.webmvc.test.autoconfigure`, *not*
  `org.springframework.boot.test.autoconfigure.web.servlet`.
- To exercise auth/session behaviour end-to-end, running the real JAR is often faster than
  fighting the context: `java -jar target/hermanas-*.jar --spring.profiles.active=gpio-fake
  --server.port=8099 --spring.mail.host=localhost --hermanas.security.users-file=/tmp/users.properties`
  then drive it with curl. On H2 the `ALTER TABLE parameter MODIFY` migration and the
  `rfkill`/photo-directory errors are expected noise, not startup failures.

### Frontend (Vitest + @analogjs/vitest-angular)
- Runner: **Vitest 4** (not Karma — migrated). Karma was deprecated in Angular 20.
- Environment: **jsdom + zone.js**, no headless browser. `vitest.config.ts` at the
  frontend root, setup file `src/test-setup.ts`.
- Commands (from `frontend/`):
  - `npm test` — single pass, exits non-zero on failure (CI-friendly).
  - `npm run test:watch` — re-runs on save.
  - `npm run test:coverage` — adds the v8 coverage report under `coverage/`.
- Assertion style is Jest-compatible (`expect`, `vi.fn()`, `vi.spyOn()`).
  **No Jasmine globals** (`jasmine.SpyObj`, `jasmine.createSpyObj`, `spyOn`,
  `.and.returnValue`) — they were translated during the port.
- `done()` callbacks are forbidden by Vitest 4. Use `firstValueFrom` /
  `lastValueFrom` for Observables, or return a Promise from the test body.
- TestBed setup: most specs use plain `provideHttpClient()` +
  `provideHttpClientTesting()`. For services that depend on Angular signals or
  Router, mock those services directly (see `side-nav.component.spec.ts` for
  an example mocking NavigationService).

---

## TODO - Technical Debt & Improvements

> **Phases 1–9 are a historical changelog, kept for context.** They record what was true when
> each phase shipped, and some details were later superseded (file/class names, the CSRF
> decision, the Pi Zero constraints). When a phase entry disagrees with the sections above,
> **the sections above win.** The live backlog is **Phase 10**.

### Phase 1 - Critical Bugs & Quick Wins ✅ COMPLETED

- [x] **BUG: Fix `Parameter.equals()`** (`data/entity/Parameter.java:42`)
  - ✅ Fixed: Now correctly compares `entryKey` with `parameter.entryKey`

- [x] **Fix logger declarations** - Change to `private static final Logger`
  - ✅ Fixed: Updated all logger declarations to be `private static final`

- [x] **Replace `e.printStackTrace()` with proper logging** (`DoorService.java:124`)
  - ✅ Fixed: Using `logger.error("message", e)` and restoring interrupt flag

- [x] **Remove `NullPointerException` catch** (`DoorService.java:83`)
  - ✅ Fixed: Added proper null checks

- [x] **Use `Integer.parseInt()` instead of `Integer.valueOf()`** for String parsing
  - ✅ Fixed: Updated all String to int conversions

### Phase 2 - HTTP/REST Best Practices ✅ COMPLETED

- [x] **Change GET to POST for state-changing endpoints**
  - ✅ `POST /system/shutdown`
  - ✅ `POST /system/reboot`
  - ✅ `POST /door/open`
  - ✅ `POST /door/close`
  - ✅ `POST /light/switch`

- [x] **Add input validation** - Use `@Valid`, `@NotNull`, `@Min`, `@Max` on request parameters
  - ✅ Added to `DoorRestController` endpoints (`turnClockwise`, `turnCounterClockwise`, `turnServo`)

- [x] **Add global exception handler** - Create `@ControllerAdvice` class
  - ✅ Enhanced existing `GlobalExceptionHandler` with domain-specific exception handlers
  - ✅ Added handlers for `DoorNotClosedCorrectlyException` (422 Unprocessable Entity)
  - ✅ Added handlers for `PredictionException` (500 Internal Server Error with door status)
  - ✅ Existing handlers: validation errors, constraint violations, type mismatches, illegal arguments, number format errors
  - ✅ All handlers return consistent JSON error responses with timestamp, status, error, and message
  - ✅ Comprehensive test coverage with 5 test cases

- [x] **Add API versioning** - Prefix endpoints with `/api/v1/`
  - ✅ All REST endpoints now use `/api/v1/*` paths exclusively (old paths removed)
  - ✅ Updated all 10 REST controllers with `@RequestMapping("/api/v1/...")`:
    - `DoorRestController`: `/api/v1/door` (6 endpoints: open, close, turnClockwise, turnCounterClockwise, turnServo, status)
    - `CameraRestController`: `/api/v1/camera` (4 endpoints: takePicture, stream, stopStream, closingRate)
    - `LightRestController`: `/api/v1/light` (2 endpoints: switch, status)
    - `FanRestController`: `/api/v1/fan` (2 endpoints: switch, status)
    - `MusicRestController`: `/api/v1/music` (3 endpoints: switch, status, cocorico)
    - `SensorRestController`: `/api/v1/sensor` (10 endpoints: info, history/*)
    - `EnergyRestController`: `/api/v1/energy` (7 endpoints: wifi/*, currentMode, dateRange, currentConfigMode, configMode, updateMode)
    - `SchedulerRestController`: `/api/v1/scheduler` (4 endpoints: doorClosingTime, doorOpeningTime, lightOnTime, nextEvents)
    - `SystemRestController`: `/api/v1/system` (2 endpoints: shutdown, reboot)
    - `InfoRestController`: `/api/v1/info` (1 endpoint: version info)
  - ✅ WebSocket endpoint updated: `/api/v1/stomp`
  - ✅ Compilation verified and all 61 tests passing
  - ⚠️ **BREAKING CHANGE:** Frontend must be updated to use `/api/v1/*` paths for all API calls

### Phase 3 - Architecture Refactoring ✅ COMPLETED

- [x] **Rename "Controller" classes to "Service" in controller package**
  - ✅ Renamed 17 classes: `DoorController` → `DoorService`, `LightController` → `LightService`, etc.
  - ✅ Updated all 277 references across 50+ files
  - ✅ All tests updated and passing

- [x] **Rename "Service" classes to "RestController" in service package**
  - ✅ Renamed 10 classes: `DoorService` → `DoorRestController`, etc.
  - ✅ REST controllers now have consistent naming with `*RestController` suffix

- [x] **Separate REST controllers from services**
  - ✅ Moved all 10 REST controllers to `web/` package
  - ✅ Updated package declarations: `org.jibe77.hermanas.service` → `org.jibe77.hermanas.web`
  - ✅ Updated imports in health indicators
  - ✅ All 61 tests passing
  - 📦 **Architecture:** Clear separation between web layer (`web/`) and service layer (`controller/`)

- [x] **Add DTOs** - Don't expose JPA entities directly in API responses
  - ✅ Created `SensorDTO` class (`dto/SensorDTO.java`) with OpenAPI schema annotations
  - ✅ Created `SensorMapper` Spring component (`dto/mapper/SensorMapper.java`) for entity-DTO conversion
  - ✅ Updated `SensorRestController` - all 10 endpoints now return DTOs instead of JPA entities
  - ✅ Updated `SensorIndicator` health check to use `SensorDTO`
  - ✅ All OpenAPI `@Schema` annotations updated to reference `SensorDTO.class`
  - ✅ Compilation verified and all 61 tests passing
  - 🔒 **Security improvement:** Internal entity structure no longer exposed to API consumers

- [x] **Add OpenAPI annotations** - Document API with `@Operation`, `@ApiResponse`
  - ✅ Added comprehensive annotations to all 10 REST controllers (46 total endpoints documented)
  - ✅ Each controller tagged with `@Tag(name, description)` for logical grouping
  - ✅ All endpoints annotated with `@Operation(summary, description)`
  - ✅ Response codes documented with `@ApiResponses` (200, 400, 422, 500 where applicable)
  - ✅ Request parameters documented with `@Parameter(description, example)`
  - ✅ Response schemas specified with `@Schema(implementation)` using DTOs
  - 📚 **Documentation:** Enhanced Swagger UI at `/swagger-ui/index.html?configUrl=/v3/api-docs/swagger-config`
  - Controllers documented:
    - `DoorRestController` (6 endpoints), `CameraRestController` (4), `LightRestController` (2)
    - `FanRestController` (2), `MusicRestController` (3), `SensorRestController` (10)
    - `EnergyRestController` (7), `SchedulerRestController` (4), `SystemRestController` (2), `InfoRestController` (1)

### Phase 4 - Security Hardening ✅ COMPLETED

- [x] **Modernize `SecurityConfig`**
  - ✅ Replaced deprecated `WebSecurityConfigurerAdapter` with `SecurityFilterChain` bean
  - ✅ Updated to use `InMemoryUserDetailsManager` bean pattern
  - ✅ Updated all security rules to use `/api/v1/*` endpoint paths
  - ⚠️ **Cannot replace** `@EnableGlobalMethodSecurity` with `@EnableMethodSecurity` (requires Spring Boot 3.x)
  - Updated file: `security/SecurityConfig.java`

- [x] **Document CSRF protection decision**
  - ✅ Added comprehensive JavaDoc explaining CSRF is disabled for stateless REST API
  - ✅ Reasoning documented: HTTP Basic auth + separate SPA frontend + proper CORS configuration
  - ⚠️ **Superseded:** CSRF was later **re-enabled** when auth moved to form login + session
    cookies, and CORS was removed (same-origin SPA). See *Authentication & Session* above —
    that section is authoritative.

- [x] **Add rate limiting** for sensitive endpoints
  - ✅ Created lightweight custom rate limiter (no new dependencies for Pi Zero)
  - ✅ `@RateLimited` annotation with per-IP tracking
  - ✅ Applied to `shutdown` and `reboot`: 2 requests per 5 minutes
  - ✅ Returns HTTP 429 (Too Many Requests) when exceeded
  - ✅ Added exception handler in `GlobalExceptionHandler`
  - New files: `security/ratelimit/` package (3 classes)
  - Updated: `SystemRestController.java`, `GlobalExceptionHandler.java`

- [x] **Add audit logging** for security-sensitive operations
  - ✅ Created `@AuditLog` annotation for marking auditable operations
  - ✅ Logs: timestamp, username, IP address, category, operation, result
  - ✅ Separate "AUDIT" logger for easy log file configuration
  - ✅ Applied to: system shutdown/reboot, door open/close
  - New files: `security/audit/` package (2 classes)
  - Updated: `SystemRestController.java`, `DoorRestController.java`

- [x] **Testing**
  - ✅ All 61 tests passing
  - ✅ No regressions introduced

### Phase 5 - New features for the configuration

- [x] **Refactore config service and expose the features to a rest endpoint**

### Phase 6 - Observability & Event Sourcing ✅ COMPLETED

- [x] **Improved health check for door physical position**
  - Enhanced `DoorIndicator` to check position without moving door
  - Returns `Health.UP` for definite positions (OPENED/CLOSED)
  - Returns `Health.DOWN` for uncertain positions (UNDEFINED/SEEMS_OPENED/SEEMS_CLOSED)
  - Provides status details and last action timestamp

- [x] **Added Micrometer metrics for observability**
  - Created `HermanasMetrics` service with comprehensive metrics tracking
  - Door metrics: open/close counters, failure counter, operation duration timer, position gauge
  - Sensor metrics: current temperature and humidity gauges
  - Appliance metrics: light/fan/music switch counters
  - Camera metrics: picture capture and streaming session counters
  - Configuration metrics: config change counters
  - All metrics exposed at `/actuator/metrics/hermanas.*`
  - Metrics optional (graceful degradation in tests)

- [x] **Added configuration hot-reload without restart**
  - All config setters use `@CacheEvict` for automatic cache invalidation
  - Created `POST /api/v1/config/refresh` endpoint to manually clear all caches
  - Enables runtime configuration changes without application restart
  - Useful for seasonal adjustments and remote tuning on Pi Zero

- [x] **Added event sourcing for door state history**
  - Extended `EventType` enum with door events: `DOOR_OPENED`, `DOOR_CLOSED`, `DOOR_OPEN_FAILED`, `DOOR_CLOSE_FAILED`, `DOOR_POSITION_UNKNOWN`
  - Enhanced `EventRepository` with time-range query methods
  - Created `DoorEventService` for recording and querying door events
  - Automatic event recording in `DoorRestController` on all operations
  - Created `GET /api/v1/door/events` endpoint for event history queries
  - Provides complete audit trail for door operations

### Phase 7 : Feature Backlog ✅ COMPLETED

- [x] Improve test coverage (was ~21%: 18 test files / 84 source files;
  now 28 test files / 152 source files, **136 backend tests** green)
  - Add `@SpringBootTest` integration tests
  - Add `@WebMvcTest` for REST layer
  - Add `@DataJpaTest` for repositories

- [x] **Refresh the Playwright e2e suite** (`frontend/tests/e2e/app.spec.ts`)
  - **Context:** a single spec file with 22 tests across 7 `test.describe` blocks
    (Hermanas Application, Login Page, Application Navigation, Performance,
    Accessibility, SEO and Meta Information, Error Handling). Chromium-only
    via `playwright.config.ts`, baseURL `http://localhost:4200`.
    Scripts: `npm run e2e` / `e2e:headed` / `e2e:ui` / `e2e:debug` / `e2e:report`.
    Not run by `npm test` (Vitest only) nor by any pre-commit hook.
  - **A. Fix the stale Amplify selectors** (4 tests broken since Phase 6 swapped
    AWS Amplify for the local users-file login):
    - line 49 `should display login form elements` — queries `amplify-authenticator`
    - line 75 `should be responsive on mobile`
    - line 86 `should be responsive on tablet`
    - line 236 the "AWS Amplify token-related logs" exception in
      `should not expose sensitive information in errors`
    Replace with the new Angular login form selector.
  - **B. Cover demo mode end-to-end** (high value: most fragile surface area
    we just touched). Suggested tests:
    - Enabling demo mode from the top-nav user menu opens the welcome modal
      (`sb-demo-welcome-modal`, title "Demo mode enabled") exactly once.
    - With demo mode on, the side-nav exposes the admin entries (Users,
      Electronics, Logs, System).
    - Navigating to each protected page renders content (no
      "Information not available", no red error toast — the
      ToastService demo-suppression should keep the toast container empty).
    - Triggering a mutation (e.g. Save in any config form) opens the
      `sb-demo-confirm-modal`; Cancel and Confirm both close cleanly with
      no follow-up toast.
  - **C. Cover the new visual layer** added in 2026-06:
    - `?theme=advent` → `.sb-topnav.advent` class present, `.snowfall` and
      `.xmas-tree` rendered, brand-hen carries `.brand-santa-hat`.
    - `?theme=easter` → `.sb-topnav.easter`, `.easter-field` rendered,
      pastel border visible.
    - `?theme=halloween` → `.sb-topnav.halloween`, `.halloween-field`
      and `.spider-web` rendered, brand-hen carries `.brand-witch-hat`.
    - `?theme=april` → `.chickens-strip.april-fools` class present.
    - `sb-chickens-strip` mounts 4 hens; clicking a hen toggles `.jumping`
      for ~600 ms.
    - Konami sequence on `document` flips `.chickens-strip.dancing` on for
      ~6 s.
    - 5 clicks on `.brand-hen` within 3 s renders `<sb-disco-overlay>`
      visible for ~8 s.
  - **D. Wire e2e into the build loop** so the suite stops drifting:
    - `npm test:all` running Vitest then Playwright sequentially
    - GitHub Actions job (or whatever CI takes over) on PRs, with the dev
      server started before the suite via `webServer` config block of
      `playwright.config.ts`.
    - Also tee the e2e report to `coverage/e2e/` so the link from the
      docs survives between runs.

- [x] **Frontend major upgrades worth doing in their own commits**
  - **Context:** Routine `npm update` only picks up patches/minors that
    fit the existing semver ranges. The bumps below cross a major —
    each deserves its own branch + visual QA pass + e2e run before
    merging.

  - **Already shipped (state at 2026-06-20):**
    - ✅ **Angular 20 → 21** — on `~21.2.17` (core, common, compiler,
      forms, router, platform-browser, service-worker, animations,
      compiler-cli, language-service, localize).
    - ✅ **`@angular/cli` + `@angular-devkit/build-angular` → 21.2.15**.
    - ✅ **`@angular-eslint/*` → 21.4.0**.
    - ✅ **`zone.js` 0.15 → 0.16.2** (bumped with Angular core).
    - ✅ **`typescript` 5.8 → 5.9.3**.
    - ✅ **`@fortawesome/angular-fontawesome` 2 → 4** +
      **`fontawesome-svg-core` / icon packs 6 → 7** (commit `0edfa00`).
    - ✅ **`@ng-bootstrap/ng-bootstrap` 19 → 20**.
    - ✅ **`vitest` 3.2 → 4.1.8** + `@vitest/coverage-v8` 4.1.8 (commit
      `51ab85a`).
    - ✅ **`eslint` 9 → 10.5.0**.
    - ✅ **`cross-env` 7 → 10.1.0** + **`shelljs` 0.8 → 0.10.0** (commit
      `3b3ca67`).

  - **Also shipped since (state at 2026-10-21):**
    - ✅ **Angular 21 → 22** — now on `~22.1.0`.
    - ✅ **`@angular/cdk` + `@angular/material` 21 → 22** — on `^22.1.0`.
    - ✅ **`typescript` 5.9 → 6.0** — on `~6.0.3`.
    - ✅ **`vitest` → 4.1.10**.
    - ✅ **Backend Java 17+ blockers cleared** — Spring Boot 4, Spring Security 7,
      Pi4j 4.0.2 all landed with the Phase 9 hardware migration.

  - **Held back for visual breakage risk:** jQuery 3 → 4 (touches every
    legacy template script), webjars:webjars-locator-core breaking
    change between 0.x and 1.x.

- [x] **Bundle Angular frontend into the same JAR** (currently deployed separately on another server)
  - Frontend source: https://github.com/jibe77/hermanasclient
  - Goal: single JAR deployment containing both backend REST API and Angular SPA
  - **Approach:**
    - Move/copy Angular code into a `frontend/` subdirectory of this project
    - Add `frontend-maven-plugin` (eirslett) to `pom.xml` to download Node/npm locally and run `npm install` + `npm run build`
    - ⚠️ **IMPORTANT — bind the npm build to the Maven `package` phase** (not `compile` or `test`), so:
      - `mvn test` / `mvn compile` stay fast and don't trigger the npm build
      - `mvn package` produces a self-contained JAR with the SPA bundled inside
      - Concretely: set the `frontend-maven-plugin` executions (`install-node-and-npm`, `npm install`, `npm run build`) to `<phase>generate-resources</phase>` or `<phase>prepare-package</phase>` — both run as part of `mvn package` but not during `mvn test`
    - Configure `maven-resources-plugin` to copy `frontend/dist/...` into `target/classes/static/` so Spring Boot serves it automatically (also bound to `prepare-package`)
    - Add a `WebMvcConfigurer` to forward unknown routes to `index.html` (SPA routing — avoids 404 on F5)
    - Update `SecurityConfig` to permit unauthenticated access to `/`, `/index.html`, static assets (`*.js`, `*.css`, `/assets/**`)
  - **Pi Zero constraint:** npm build is too heavy for the Pi — must run on dev machine, not on the device. Optionally wrap in a Maven profile (e.g. `-Pwith-frontend`) for extra control, but the `package`-phase binding already keeps `mvn test` fast.
  - **Tradeoff:** front/back release cycles become coupled (acceptable for single-maintainer personal project), but deployment simplifies to one artifact.
  - Update `deploy.sh` accordingly once integrated.

- [x] **Decouple authentication/user management from AWS Amplify** — replaced with a self-hosted login form backed by a local user file
  - ✅ **Backend:**
    - Created `FileBasedUserDetailsService` loading users from `users.properties` (multi-user, bcrypt). Path configurable via `hermanas.security.users-file` (default `./users.properties`).
      ⚠️ **Since replaced by `DbUserDetailsService`** — users live in the DB, and
      `users.properties` is now only a first-startup bootstrap source.
    - `SecurityConfig` refactored: form login on `POST /api/v1/auth/login` returning 200/401 (no HTML redirects), `BCryptPasswordEncoder` bean, `httpBasic()` removed, old `security.user.*` / `security.guest.*` properties removed.
    - New `AuthRestController`: `GET /api/v1/auth/me` (session check).
    - **Session model:** form login + session cookie (`JSESSIONID`).
    - **CSRF re-enabled** via `CookieCsrfTokenRepository.withHttpOnlyFalse()` — token in `XSRF-TOKEN` cookie, echoed by Angular in `X-XSRF-TOKEN` header.
    - **CORS removed entirely** (bean, `.cors()` call, all `spring-web` CORS imports) — SPA is now same-origin.
    - CLI added to `HermanasApplication`: `java -jar hermanas.jar --hash [password]` outputs a `{bcrypt}...` line ready for `users.properties`.
    - `users.properties` added to `.gitignore`.
    - README updated with full "User management" section (file format, hash generation, full add-user workflow, auth flow).
  - ✅ **Frontend:**
    - Removed npm deps: `aws-amplify`, `@aws-amplify/core`, `@aws-amplify/ui-angular`, `zen-observable-ts` → **310 packages uninstalled**.
    - Deleted `src/aws-exports.js`, `src/app/API.service.ts`, `src/graphql/`.
    - New `LoginService` (POST `/api/v1/auth/login` & `/logout`); `UserService` rewritten to call `/api/v1/auth/me`.
    - Custom Angular login form replacing `<amplify-authenticator>` (`login.component.pug` + `.ts`).
    - `top-nav-user` rewritten: logout button replacing `<amplify-sign-out>`, `Hub.listen` removed.
    - `auth.interceptor.ts` simplified to `withCredentials: true` (session cookie sent automatically).
    - `AmplifyAuthenticatorModule` removed from `auth/`, `navigation/`, `dashboard/` modules.
    - `app-common.module.ts`: `Amplify.configure()` removed.
    - `dashboard.guard.ts` cleaned of `backEndUser`/`backEndPassword` checks.
    - Test stubs updated.
  - ✅ **Validation:** backend tests 71/71 ✅, frontend `ng build` ✅, full `mvn package` produces a 64 MB self-contained JAR.
  - 🗂 **Leftover cleanup (not in this round):** `frontend/amplify/`, `frontend/amplify.yml`, `frontend/.graphqlconfig.yml` are no longer referenced by the code but kept for now — to be removed in a separate PR.
  - 🧪 **To validate in runtime:** create `users.properties` next to the JAR (use `--hash`), launch, navigate to `/`, test login → dashboard → logout.

### Phase 8 : add new features ✅ COMPLETED

- [x] **Add debug panel to verify in real time the status of all the buttons (pressed / not pressed) or use actuator to get the status ... not sure yet !**

- [x] **Implement missing screens ... see on the front-end what is necessary first**

- [x] **Improve image processing to count chicken, eggs, and amount of durt in the coop** 

### Phase 9 - Hardware & Runtime Migration ✅ COMPLETED (2026)

> Previously blocked by the Pi Zero's Java 11 ceiling. Unblocked by migrating the hardware to a
> **Pi Zero 2 W** (`pru`), then doing the runtime/framework jump in one cutover.

- [x] **Raspberry Pi Zero → Pi Zero 2 W** — Raspberry Pi OS Trixie arm64, non-root `hermanas`
  user under `/var/lib/hermanas/`, MariaDB 10.3 → 10.11, old SD kept as rollback.
- [x] **Java 11 → Java 25** (apt-installed, not SDKMAN).
- [x] **Spring Boot 2.7 → 4.1.x**, including Spring Security 6/7 migration.
- [x] **`javax.*` → `jakarta.*`** — zero `javax.persistence` / `javax.annotation` left.
- [x] **`@EnableGlobalMethodSecurity` → `@EnableMethodSecurity`**.
- [x] **Pi4j 2.4 → 4.0.2**, single `pi4j-plugin-ffm` plugin (FFM / chardev), replacing the
  pigpio stack that Debian Trixie removed.
- [x] **Maven + npm dependency catch-up** — see the frontend list below.

### Deprecated Components — resolved

| Component | Status |
|-----------|--------|
| Spring Boot 2.7.18 (EOL) | ✅ on **4.1.1** |
| Java 11 | ✅ on **Java 25** |
| `WebSecurityConfigurerAdapter` | ✅ `SecurityFilterChain` bean (Phase 4) |
| `@EnableGlobalMethodSecurity` | ✅ `@EnableMethodSecurity` |
| `javax.persistence.*` / `javax.annotation.*` | ✅ migrated to `jakarta.*` |
| pigpio / `/dev/mem` GPIO | ✅ `pi4j-plugin-ffm` on `/dev/gpiochip0` |

**Still held back for visual-breakage risk (not hardware-related):** jQuery 3 → 4 (touches
every legacy template script), `webjars-locator-core` 0.x → 1.x.

### Phase 10 - Current backlog

- [ ] **`--hash` CLI emits an unusable hash.** `HermanasApplication` prints
  `{bcrypt}$2a$...`, but `SecurityConfig.passwordEncoder()` is a bare `BCryptPasswordEncoder`
  that does not understand the `{bcrypt}` prefix — it treats it as part of the hash, so login
  returns 401. Fix by either using a `DelegatingPasswordEncoder` or dropping the prefix from
  the CLI output. Affects only the `users.properties` bootstrap path, not `/register`.
- [ ] **Reconcile `src/test/resources/application.properties`** so broad `@SpringBootTest`
  context loads are possible (see Testing Notes).
- [ ] **Delete the empty `src/main/java/org/jibe77/hermanas/controller/` directory** — the
  rename refactor left it behind.
- [ ] **Leftover Amplify files**: `frontend/amplify/`, `frontend/amplify.yml`,
  `frontend/.graphqlconfig.yml` are unreferenced and can be removed.
- [ ] **Consider `micrometer-registry-prometheus`** — `management.endpoints.web.exposure.include`
  lists `prometheus`, but the dependency is not declared explicitly in `pom.xml`. Verify the
  endpoint actually serves content.
