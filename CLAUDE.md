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

- Image processing tests use `@Tag("image_processing")` and are excluded by default
- Use `gpio-fake` profile to run tests without Raspberry Pi hardware
- Native library `picam-2.0.1.so` required for camera on Pi (versions >2.4.0 incompatible with Pi Zero + Java 11)

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

### Phase 5 - New features

- [ ] **Add configuration panel**

- [ ] **Implement missing screens**

### Phase 6 - Major Upgrade (BLOCKED - Hardware Constraint)

> **NOT POSSIBLE:** The Raspberry Pi Zero hardware does not support Java 17+, which is required for Spring Boot 3.x. These upgrades are blocked until the hardware is upgraded to a more capable Raspberry Pi model.

- [ ] ~~**Upgrade to Spring Boot 3.2+**~~
  - Requires Java 17+ (not supported on Pi Zero)
  - Would require migrating `javax.*` → `jakarta.*` (persistence, annotation)
  - Would require Pi4j update if compatible version exists

- [ ] ~~**Upgrade Java 11 → Java 17 or 21 LTS**~~
  - Not supported on Raspberry Pi Zero hardware

### Feature Backlog

- [ ] Add health check for door physical position
- [ ] Add Micrometer metrics for observability
- [ ] Add configuration hot-reload without restart
- [ ] Add event sourcing for door state history
- [ ] Improve test coverage (currently ~21%: 18 test files / 84 source files)
  - Add `@SpringBootTest` integration tests
  - Add `@WebMvcTest` for REST layer
  - Add `@DataJpaTest` for repositories

### Deprecated Components (EOL) - Cannot Be Updated

> **Note:** Due to Raspberry Pi Zero hardware limitations (Java 11 max), these deprecated components cannot be updated. The deprecation warnings must be accepted as technical debt until hardware is upgraded.

| Component | Status | Action |
|-----------|--------|--------|
| Spring Boot 2.7.18 | EOL Nov 2023 | **Blocked** - Pi Zero cannot run Java 17+ |
| `WebSecurityConfigurerAdapter` | Deprecated | ✅ **FIXED** - Replaced with `SecurityFilterChain` bean (Phase 4) |
| `@EnableGlobalMethodSecurity` | Deprecated | **Blocked** - Replacement requires Spring Boot 3.x |
| `javax.persistence.*` | Legacy | **Blocked** - Jakarta namespace requires Spring Boot 3.x |
| `javax.annotation.*` | Legacy | **Blocked** - Jakarta namespace requires Spring Boot 3.x |
