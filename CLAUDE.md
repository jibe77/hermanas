# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Hermanas is a chicken coop automation system built with Spring Boot, designed to run on **Raspberry Pi Zero**. It automates door control based on sunrise/sunset, manages lighting, camera monitoring, temperature/humidity sensors, and fan control.

**Hardware Constraint:** This application runs on Raspberry Pi Zero, which limits the runtime environment to **Java 11** and **Spring Boot 2.x**. Upgrading to Spring Boot 3+ and Java 17+ is not possible until the hardware is upgraded.

- **Frontend repository:** https://github.com/jibe77/hermanasclient
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

# automate deployment on the chicken-coop
./deploy.sh
```

## Architecture

### Layer Structure

The application follows a **REST Controller-Service-Repository** pattern with an IoT twist:

- **REST Controllers** (`service/` package, `*RestController` classes) - REST endpoints marked with `@RestController`
- **Services** (`controller/` package, `*Service` classes) - Hardware abstraction and business logic (door, light, fan, camera, sensor)
- **Repositories** (`data/repository/`) - Spring Data JPA interfaces
- **Schedulers** (`scheduler/`) - Quartz-based jobs for sun-related automation

**Note:** The package naming is being gradually refactored. Currently:
- Files in `src/main/java/org/jibe77/hermanas/service/` contain REST controllers (package `org.jibe77.hermanas.service.*`)
- Files in `src/main/java/org/jibe77/hermanas/controller/` contain services (package `org.jibe77.hermanas.controller.*`)

### Key Packages

| Package | Purpose |
|---------|---------|
| `service/` | REST controllers (`*RestController`) exposing HTTP API endpoints |
| `controller/door/` | Door services: servo motor control, button management |
| `controller/gpio/` | GPIO abstraction with real (Rpi) and fake implementations |
| `controller/fan/`, `controller/light/`, etc. | Hardware control services |
| `scheduler/job/` | Periodic jobs: SunRelatedJob (60s), CameraJob (3h), DiskSpaceJob |
| `scheduler/event/` | Event handlers for door opening/closing based on sunrise/sunset |
| `scheduler/sun/` | Sun time calculations with caching |
| `websocket/` | STOMP/WebSocket for real-time status notifications |
| `health/` | Custom Spring Boot health indicators |

### GPIO Strategy Pattern

```
GpioHermanasService (interface)
├── GpioHermanasRpiService  - Real Pi4j hardware implementation
└── GpioHermanasFakeService - Mock for testing (@Profile("gpio-fake"))
```

## Configuration

Main configuration in `application.properties`. Key externalized parameters:

- **GPIO pins:** door servo (25), buttons (15, 18), light (14), fan (23), sensor (4)
- **Door timing:** opening 10000ms, closing 2350ms, servo positions 5-16
- **Sun scheduling:** location coordinates, door delays after sunrise/sunset
- **Camera:** resolution, quality, rotation, streaming port 8081

Test profile `application-gpio-fake.properties` uses H2 in-memory database.

## Database

- **Production:** MariaDB
- **Testing:** H2 in-memory
- **Entities:** Parameter, Sensor, Event, Picture

## API Documentation

Swagger UI available at: `/swagger-ui/index.html?configUrl=/v3/api-docs/swagger-config`

## Testing Notes

### Backend (JUnit 5 + Spring Boot Test)
- Image processing tests use `@Tag("image_processing")` and are excluded by default
- Use `gpio-fake` profile to run tests without Raspberry Pi hardware
- Native library `picam-2.0.1.so` required for camera on Pi (versions >2.4.0 incompatible with Pi Zero + Java 11)

### Frontend (Vitest + @analogjs/vitest-angular)
- Runner: **Vitest 3** (not Karma — migrated). Karma was deprecated in Angular 20.
- Environment: **jsdom + zone.js**, no headless browser. `vitest.config.ts` at the
  frontend root, setup file `src/test-setup.ts`.
- Commands (from `frontend/`):
  - `npm test` — single pass, exits non-zero on failure (CI-friendly).
  - `npm run test:watch` — re-runs on save.
  - `npm run test:coverage` — adds the v8 coverage report under `coverage/`.
- Assertion style is Jest-compatible (`expect`, `vi.fn()`, `vi.spyOn()`).
  **No Jasmine globals** (`jasmine.SpyObj`, `jasmine.createSpyObj`, `spyOn`,
  `.and.returnValue`) — they were translated during the port.
- `done()` callbacks are forbidden by Vitest 3. Use `firstValueFrom` /
  `lastValueFrom` for Observables, or return a Promise from the test body.
- TestBed setup: most specs use plain `provideHttpClient()` +
  `provideHttpClientTesting()`. For services that depend on Angular signals or
  Router, mock those services directly (see `side-nav.component.spec.ts` for
  an example mocking NavigationService).

---

## TODO - Technical Debt & Improvements

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
  - 🔒 **Decision:** CSRF remains disabled (appropriate for this architecture)

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

### Phase 7 : Feature Backlog

- [ ] Improve test coverage (currently ~21%: 18 test files / 84 source files)
  - Add `@SpringBootTest` integration tests
  - Add `@WebMvcTest` for REST layer
  - Add `@DataJpaTest` for repositories

- [ ] **Refresh the Playwright e2e suite** (`frontend/tests/e2e/app.spec.ts`)
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

- [ ] **Frontend major upgrades worth doing in their own commits**
  - **Context:** Routine `npm update` only picks up patches/minors that
    fit the existing semver ranges. The bumps below cross a major and
    were deliberately deferred from the 2026-06 dependency pass — each
    deserves its own branch + visual QA pass + e2e run before merging.
  - **Angular 20 → 21** — Angular 21 shipped 2025-11-19 and is now on
    21.2.x (mature). The Pi Zero deployment is unaffected since this
    only touches the frontend. Run `ng update @angular/core@21
    @angular/cli@21` and let the Angular migrations do their work; the
    control-flow / signal API surface keeps evolving, expect template
    deprecations. Hold off on Angular 22 (shipped 2026-06-03, brand new)
    until ~early 2027 — same posture took us through 20 → 21 cleanly.
  - **`@angular/cdk` + `@angular/material` 20 → 22** — Material 3 design
    tokens land in v22; expect overrides in `styles/` to need rewiring,
    especially anything touching color/typography roles. Best done
    *after* Angular core 21 lands so there is exactly one moving target
    at a time.
  - **`@angular-eslint/*` 20 → 22** — Must follow Angular core. The
    lint rules drift with each Angular release, so chain it with the
    Angular 21 upgrade.
  - **`@fortawesome/angular-fontawesome` 2 → 5 + `fontawesome-svg-core`
    6 → 7** — Triple major. FA7 finishes the FA5 → FA6 alias deprecation
    (`info-circle` → `circle-info`, `arrow-up-from-bracket`, etc.). Grep
    every `<fa-icon [icon]="['fas', 'X']">` in the templates and update
    each name; the missing-icon warnings will tell you what slipped.
  - **`@ng-bootstrap/ng-bootstrap` 19 → 20** — Drives the top-nav user/
    lang dropdowns and the login modal. Smoke-test the dropdowns, the
    NgbActiveModal close paths and the keyboard shortcuts before merge.
  - **`typescript` 5.8 → 6.0** — Stricter type checking, decorators
    stage 3. Likely surfaces latent issues — easier when Angular has
    already moved (Angular 21 needs TS ≥ 5.8 and is fine with 5.x).
  - **`vitest` 3.2 → 4.1** — Mock/hook API refactor. The 192 specs
    might need adjustments around `vi.fn()` and lifecycle hooks.
    `@vitest/coverage-v8` follows the same major.
  - **`eslint` 9 → 10** — Pure linting, no runtime impact. Already on
    flat config so the migration should be cosmetic. Lowest-risk of the
    bunch — could be done alone if you want a small win.
  - **`uuid` 9 → 14** — Five majors. ESM-only since v10; grep for any
    `require('uuid')` in scripts/* and convert to `import`.
  - **`zone.js` 0.15 → 0.16** — Tied to the Angular version; bump it in
    the same commit as Angular core 21 so the versions stay matched.
  - **`cross-env` 7 → 10 + `shelljs` 0.8 → 0.10** — Used by the npm
    scripts (`scripts/index.js`, `scripts/version.js`); skim those two
    files when upgrading so the env-var syntax still resolves.
  - **Held back as long as the Pi Zero stays:** Spring Boot 3, Spring
    Security 7, Pi4j 4.x, h2 2.4, spring-retry 2.x, resilience4j 2.x —
    all require Java 17. Move with the hardware upgrade (Phase 9).
  - **Held back for visual breakage risk:** jQuery 3 → 4 (touches every
    legacy template script), webjars:webjars-locator-core breaking
    change between 0.x and 1.x.

- [ ] **Bundle Angular frontend into the same JAR** (currently deployed separately on another server)
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

### Phase 8 : add new features (blocked for the moment, need analysis)

- [ ] **Add debug panel to verify in real time the status of all the buttons (pressed / not pressed) or use actuator to get the status ... not sure yet !**

- [ ] **Implement missing screens ... see on the front-end what is necessary first**

- [ ] **Improve image processing to count chicken, eggs, and amount of durt in the coop** 

### Phase 9 - Major Upgrade (BLOCKED - Hardware Constraint)

> **NOT POSSIBLE:** The Raspberry Pi Zero hardware does not support Java 17+, which is required for Spring Boot 3.x. These upgrades are blocked until the hardware is upgraded to a more capable Raspberry Pi model. Move to a recent Raspberry Pi hardware !

- [ ] ~~**Upgrade to Spring Boot 3.2+**~~
  - Requires Java 17+ (not supported on Pi Zero)
  - Would require migrating `javax.*` → `jakarta.*` (persistence, annotation)
  - Would require Pi4j update if compatible version exists

- [ ] ~~**Upgrade Java 11 → Java 17 or 21 LTS**~~
  - Not supported on Raspberry Pi Zero hardware

### Deprecated Components (EOL) - Cannot Be Updated

> **Note:** Due to Raspberry Pi Zero hardware limitations (Java 11 max), these deprecated components cannot be updated. The deprecation warnings must be accepted as technical debt until hardware is upgraded.

| Component | Status | Action |
|-----------|--------|--------|
| Spring Boot 2.7.18 | EOL Nov 2023 | **Blocked** - Pi Zero cannot run Java 17+ |
| `WebSecurityConfigurerAdapter` | Deprecated | ✅ **FIXED** - Replaced with `SecurityFilterChain` bean (Phase 4) |
| `@EnableGlobalMethodSecurity` | Deprecated | **Blocked** - Replacement requires Spring Boot 3.x |
| `javax.persistence.*` | Legacy | **Blocked** - Jakarta namespace requires Spring Boot 3.x |
| `javax.annotation.*` | Legacy | **Blocked** - Jakarta namespace requires Spring Boot 3.x |
