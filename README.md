# Hermanas

Hermanas is a comprehensive chicken coop automation system designed to run on Raspberry Pi hardware. It automates door operations based on sunrise/sunset times, manages environmental controls (lighting, ventilation, music), monitors temperature and humidity, and provides real-time camera surveillance with on-device AI scene analysis.

The Angular SPA used to live in a separate repository — it is now bundled inside this project (`frontend/` subdirectory) and built into the same JAR by the Maven build. A single `mvn package` produces a self-contained artifact that serves both the REST API and the web UI.

**Live Demo:** [http://www.hermanas.fr](http://www.hermanas.fr) (try the **Demo mode** button on the login screen — see the [Demo mode](#demo-mode) section).

## Features

### Core Automation
- **Intelligent Door Control** - Automatic door opening/closing based on sunrise/sunset calculations with customizable delays
- **Sun-Based Scheduling** - Location-aware scheduling using real-time astronomical calculations
- **Event Sourcing** - Complete audit trail of all door operations for troubleshooting and analytics

### Environmental Management
- **Temperature & Humidity Monitoring** - Real-time sensor data with historical tracking
- **Automated Ventilation** - Fan control with energy-conscious timer management
- **Smart Lighting** - Scheduled lighting with eco/regular/sunny mode support
- **Music Playback** - Audio stimulation for chickens with timer control

### Monitoring & Control
- **Live Camera Streaming** - Real-time video surveillance with image capture
- **Async Capture Pipeline** - `POST /api/v1/captures` kicks off a job that the SPA polls; image + AI analysis are produced in parallel and never trip the reverse-proxy timeout
- **AI Vision** - Multimodal LLM (default: a local Ollama-compatible server) describes the coop scene — hens, eggs, hay level, door state, dirt
- **AI-Powered Door Detection** - Automatic detection of door closing issues using computer vision
- **WebSocket Real-Time Updates** - Live status notifications via STOMP protocol
- **Web Push Notifications** - VAPID-signed PWA notifications for door failures / sensor errors (per-user opt-in)
- **Health Monitoring** - Spring Boot Actuator health checks for all components
- **Micrometer Metrics** - Door operations, sensors, switches, captures, configuration changes exposed at `/actuator/metrics/hermanas.*`

### Energy Management
- **Three Power Modes** - Eco (winter), Regular, and Sunny (summer) consumption profiles
- **Configurable Timers** - Mode-specific timer delays for all equipment
- **WiFi Power Control** - Automatic WiFi disable in eco mode
- **Machine Shutdown** - Scheduled system shutdown capabilities in low-power modes

### Remote Access
- **REST API** - Complete `/api/v1/*` endpoints for all operations
- **Swagger Documentation** - Interactive API documentation
- **Siri Integration** - Control via iOS shortcuts using a long-lived bearer token (see `SiriTokenAuthenticationFilter`)
- **Web Push (PWA)** - Browser notifications on Chrome/Edge/Firefox/iOS 16.4+
- **Security** - Spring Security 5 with file-based users, bcrypt passwords, CSRF + login rate limiting, audit logging
- **Demo mode** - Frontend-only "fake admin" mode for the live demo: every read returns synthetic data, every mutation triggers a confirmation modal and a no-op success response — no real backend traffic

## Hardware Requirements

### Required Components
- **Raspberry Pi Zero** (or any Raspberry Pi model with GPIO)
- **Servo Motor** - Door mechanism (GPIO 25)
- **Limit Switches** - Top (GPIO 15) and bottom (GPIO 18) door position sensors
- **DHT11/DHT22 Sensor** - Temperature and humidity (GPIO 4)
- **Relay Modules** - Light (GPIO 14), Fan (GPIO 23), Music control
- **Pi Camera Module** - Video surveillance
- **MariaDB Server** - Database (can run on separate machine)

### Optional Components
- **External Storage** - For camera image archives
- **UPS/Battery** - Power backup for safe shutdowns

## Quick Start

### Prerequisites
```bash
# Java 11 — runtime requirement (target bytecode is Java 11 for Pi Zero).
# The build JVM can be any 11+ version, including 17 / 21 / latest LTS — the
# emitted bytecode stays Java 11.
java -version  # Should show 11.x.y (runtime); build JDK may differ

# Maven 3.6+
mvn -version

# MariaDB 10.x (only for production; tests use embedded H2)
mysql --version

# Node.js + npm are NOT required as system dependencies — frontend-maven-plugin
# downloads them locally during `mvn package`. Pinned versions: Node 20.19.0,
# npm 10.8.2.
```

### Installation

1. **Clone the repository**
   ```bash
   git clone https://github.com/jibe77/hermanas.git
   cd hermanas
   ```

2. **Configure the application**

   Edit `src/main/resources/application.properties` directly, or — better — leave
   it alone and override the few host-specific properties on the deployment
   target (database credentials, location coordinates, `hermanas.security.users-file`,
   SMTP credentials). See the [Production Configuration](#production-configuration)
   section for the externalized-config pattern.

3. **Create a first user** (file-based auth, see [User management](#user-management))
   ```bash
   # Build the JAR first so the --hash CLI is available
   mvn -DskipTests package
   java -jar target/hermanas-0.8.2.jar --hash
   # Paste the {bcrypt} line into users.properties next to the JAR
   ```

4. **Build the application** (includes the Angular SPA)
   ```bash
   mvn clean package
   ```

5. **Run the application**
   ```bash
   # On Raspberry Pi with real GPIO hardware
   java -jar target/hermanas-0.8.2.jar

   # On development machine with fake GPIO + H2 in-memory DB
   mvn spring-boot:run -Dspring.profiles.active=gpio-fake
   ```

6. **Access the application**
   - Web UI: `http://localhost:8080/`
   - REST API: `http://localhost:8080/api/v1/`
   - Swagger UI: `http://localhost:8080/swagger-ui/index.html?configUrl=/v3/api-docs/swagger-config`
   - Health: `http://localhost:8080/actuator/health`
   - Metrics: `http://localhost:8080/actuator/metrics`

## Configuration

Key configuration parameters in `application.properties`:

### Database
```properties
spring.datasource.url=jdbc:mariadb://localhost:3306/hermanas
spring.datasource.username=hermanas
spring.datasource.password=your_password
```

### GPIO Pin Mapping
```properties
# Door control
gpio.pin.servo=25
gpio.pin.servo.button.up=15
gpio.pin.servo.button.bottom=18

# Environmental controls
gpio.pin.light=14
gpio.pin.fan=23
gpio.pin.sensor=4
```

### Door Timing
```properties
# Servo motor timing (milliseconds)
servo.pin.opening.duration=10000
servo.pin.closing.duration=2350
servo.door.closing.position=5
servo.door.opening.position=16
```

### Location for Sun Calculations
```properties
# Coordinates for sunrise/sunset calculations
location.latitude=48.8566
location.longitude=2.3522
```

### Sunrise/Sunset Delays
```properties
# Minutes after sunrise to open door
scheduler.sun.door.opening.delay=10

# Minutes before sunset to close door
scheduler.sun.door.closing.delay=30
```

### Energy Management
```properties
# Eco mode (winter solstice)
consumption.mode.eco.days.around.winter.solstice=30
consumption.mode.eco.light.timer.delay=300000
consumption.mode.eco.fan.timer.delay=300000
consumption.mode.eco.music.timer.delay=300000
consumption.mode.eco.machine.shutdown=true
consumption.mode.eco.wifi.disabled=true

# Sunny mode (summer solstice)
consumption.mode.sunny.days.around.summer.solstice=30
consumption.mode.sunny.light.timer.delay=900000
consumption.mode.sunny.fan.timer.delay=900000
consumption.mode.sunny.music.timer.delay=900000
```

### Camera
```properties
camera.resolution.width=1024
camera.resolution.height=768
camera.jpeg.quality=85
camera.rotation=180
camera.streaming.port=8081
```

### Hot-Reload Configuration

Configuration values are cached for performance. To reload without restarting:
```bash
curl -X POST http://localhost:8080/api/v1/config/refresh
```

## User management

Authentication is **self-hosted, file-based**. There is no external identity provider.

### Where users are stored

Users live in a `users.properties` file **outside the JAR** (so rebuilds don't overwrite it).
The path is configurable via `hermanas.security.users-file` in `application.properties`
(default: `./users.properties`, i.e. next to the JAR).

> **🔒 Never commit this file.** It is already listed in `.gitignore`.

### File format

```properties
# users.properties
# Each user has a .password (bcrypt hash, with the {bcrypt} prefix) and an optional .roles entry.
# Default role if .roles is omitted: USER

alice.password = {bcrypt}$2a$10$abcdefghijklmnopqrstuvABCDEFGHIJKLMNOPQRSTUVWXYZ012345
alice.roles    = USER

bob.password = {bcrypt}$2a$10$zyxwvutsrqponmlkjihgfeZYXWVUTSRQPONMLKJIHGFEDCBA9876
bob.roles    = USER
```

### Generating a bcrypt hash

The JAR ships with a CLI to generate hashes — no external tools needed:

```bash
# Interactive (password not shown in shell history)
java -jar target/hermanas-0.8.2.jar --hash
# Password: ********
# {bcrypt}$2a$10$....

# Inline (avoid in shared terminals — leaves the password in history)
java -jar target/hermanas-0.8.2.jar --hash "your-password"
```

Copy the `{bcrypt}...` line as-is into `users.properties` as the user's `.password` value.

### Adding a user — full workflow

```bash
# 1. Generate the hash
java -jar target/hermanas-0.8.2.jar --hash

# 2. Append to users.properties (next to the JAR, or wherever hermanas.security.users-file points)
echo "alice.password = {bcrypt}\$2a\$10\$..." >> users.properties
echo "alice.roles    = USER"                  >> users.properties

# 3. Restart the application (the file is read at startup)
sudo systemctl restart hermanas
```

### Authentication flow

- Login: `POST /api/v1/auth/login` with form fields `username` + `password`
  → returns **200** on success (sets a `JSESSIONID` cookie), **401** on failure
- Current session: `GET /api/v1/auth/me`
  → `{ authenticated: false }` when anonymous, `{ authenticated: true, username, roles }` otherwise
- Logout: `POST /api/v1/auth/logout` → **204**

CSRF is enabled: mutating requests must echo the `XSRF-TOKEN` cookie in the `X-XSRF-TOKEN`
header. Angular's `HttpClient` does this automatically.

A per-IP login rate limiter (`LoginRateLimitFilter`) and audit logging (`@AuditLog` on
sensitive operations) round out the authentication surface.

## Demo mode

The login screen exposes a **Demo mode** button intended for the public live demo at
[www.hermanas.fr](http://www.hermanas.fr). It's a *frontend-only* fiction:

- A synthetic ADMIN user is set in the `UserService` signal — every guard, role check,
  and admin-only panel unfolds as if the visitor were signed in.
- A dedicated HTTP interceptor (`demoMode.interceptor.ts`) sits in front of every
  outgoing call:
  - **GET** requests are sent normally; if the backend answers 401/403, the
    interceptor replaces the response with a plausible fixture from
    `DemoFixtureService` (sensors, scheduler, photos, users, full `/config`, …).
  - **POST / PUT / DELETE / PATCH** requests — plus a small allowlist of
    state-changing GETs (`/music/switch`, `/music/cocorico`, `/fan/switch`) —
    are intercepted *before* hitting the network. A confirmation modal explains
    what the action would do, and a synthetic 200 response is returned on
    confirmation.
- Sensitive screens that have no meaningful synthetic equivalent (Log browser,
  Log content) fall back to a neutral "Information non disponible" placeholder.

The demo flag lives only in memory: a page refresh drops it. There is no backend
support for demo mode — the backend never sees that the visitor is "pretending".

The full fixture catalogue and mutation-confirmation labels are maintained in
`frontend/src/modules/app-common/services/demo-fixture/demo-fixture.service.ts`
and `frontend/src/modules/app-common/interceptors/demoMode.interceptor.ts`.
Adding a new screen typically means adding one entry to each.

## API Documentation

### Door Control
```bash
# Open door
POST /api/v1/door/open?force=false

# Close door
POST /api/v1/door/close?force=false

# Get door status
GET /api/v1/door/status

# Get door event history
GET /api/v1/door/events

# Manual servo control (testing)
GET /api/v1/door/turnClockwise?duration=50
GET /api/v1/door/turnCounterClockwise?duration=50
```

### Camera
```bash
# Take a one-shot JPEG (synchronous — used by widgets)
GET /api/v1/camera/takePicture

# Start/stop the MJPEG video stream
GET /api/v1/camera/stream
GET /api/v1/camera/stopStream

# Get door closing rate (AI analysis on the last picture)
GET /api/v1/camera/closingRate

# Browse the on-disk photo archive (admin)
GET /api/v1/camera/photos?path=2024/05
GET /api/v1/camera/photos/file?path=2024/05/IMG_0001.jpg

# Synchronous AI scene analysis (legacy — prefer the async capture API below)
GET /api/v1/camera/analyze?lang=fr
```

### Async capture pipeline

The Webcam page no longer waits for the synchronous snapshot + analyze duo.
Instead it kicks an async job and polls its status:

```bash
# Start a capture + AI analysis job
POST /api/v1/captures?lang=fr
# → { "captureId": "abc-123" }

# Fetch the captured JPEG (404 until ready — client retries with backoff)
GET /api/v1/captures/{captureId}/image

# Poll job status (CAPTURING → ANALYZING → DONE / ERROR)
GET /api/v1/captures/{captureId}/status
```

### Sensors
```bash
# Get current sensor reading
GET /api/v1/sensor/info

# Get historical data
GET /api/v1/sensor/history/today
GET /api/v1/sensor/history/week
GET /api/v1/sensor/history/month
GET /api/v1/sensor/history/year
GET /api/v1/sensor/history/year/{year}
GET /api/v1/sensor/history/{from}/{to}    # ISO date range
GET /api/v1/sensor/history/years          # list of years with data
GET /api/v1/sensor/history/all
```

### Environmental Controls
```bash
# Light (POST is the new convention)
POST /api/v1/light/switch?param=true
GET /api/v1/light/status

# Fan (still GET on the backend; the frontend interceptor treats it as a mutation)
GET /api/v1/fan/switch?param=true
GET /api/v1/fan/status

# Music (still GET on the backend — historical reasons)
GET /api/v1/music/switch?param=true[&playlist=Pop]
GET /api/v1/music/status
GET /api/v1/music/cocorico                # one-shot rooster sound
GET /api/v1/music/playlists
GET /api/v1/music/playlists/{name}/songs
GET /api/v1/music/selected-playlist
PUT /api/v1/music/selected-playlist        # { "playlist": "Pop" }
```

### Scheduling
```bash
# Get next scheduled events
GET /api/v1/scheduler/nextEvents

# Get specific times
GET /api/v1/scheduler/doorOpeningTime
GET /api/v1/scheduler/doorClosingTime
GET /api/v1/scheduler/lightOnTime
```

### Energy Management
```bash
# Get current consumption mode
GET /api/v1/energy/currentMode
GET /api/v1/energy/currentConfigMode
GET /api/v1/energy/configMode

# WiFi control
GET /api/v1/energy/wifi/wifiSwitchEnabled
GET /api/v1/energy/wifi/stopUntilNextDoorEvent

# Mutate consumption configuration
PUT /api/v1/energy/updateMode
PUT /api/v1/energy/monthlyMapping
PUT /api/v1/energy/forceEco
```

### Configuration

`GET /api/v1/config` returns the full bundle consumed by every admin screen
(timers, sun offsets, music + camera settings, AI config, email/SMTP, …).
Every setter clears its Spring cache so the change is effective immediately;
`POST /api/v1/config/refresh` is the manual catch-all.

```bash
# Full bundle
GET /api/v1/config

# Refresh caches (hot-reload)
POST /api/v1/config/refresh

# Per-equipment, per-mode timers (light / fan / music × eco / regular / sunny)
PUT /api/v1/config/light/eco?delayMs=300000
PUT /api/v1/config/light/regular?delayMs=600000
PUT /api/v1/config/light/sunny?delayMs=900000
PUT /api/v1/config/fan/{eco|regular|sunny}?delayMs=...
PUT /api/v1/config/music/{eco|regular|sunny}?delayMs=...

# Consumption + sun + door + audio + camera + AI + weather + email/SMTP
PUT /api/v1/config/consumption/force-eco?force=true
PUT /api/v1/config/sun/light-on-before-sunset?minutes=20
PUT /api/v1/config/sun/door-close-after-sunset?minutes=30
PUT /api/v1/config/sun/door-open-after-sunrise?minutes=0
PUT /api/v1/config/sun/force-at-8?enabled=true
PUT /api/v1/config/door/opening-position?value=16
PUT /api/v1/config/door/closing-position?value=5
PUT /api/v1/config/door/opening-duration?ms=10000
PUT /api/v1/config/door/closing-duration?ms=2350
PUT /api/v1/config/music/volume?percent=78
PUT /api/v1/config/audio/cocorico-at-sunrise?enabled=true
PUT /api/v1/config/audio/song-at-sunset?enabled=false
PUT /api/v1/config/notifications/weather?enabled=true
PUT /api/v1/config/camera/brightness?value=60
PUT /api/v1/config/camera/rotation?value=180
PUT /api/v1/config/weather/url?value=https://api.openweathermap.org
PUT /api/v1/config/weather/key             # body = raw key
PUT /api/v1/config/ai/inference-url?value=http://localhost:11434
PUT /api/v1/config/ai/inference-model?value=focus
PUT /api/v1/config/ai/cache-ttl-ms?value=120000
PUT /api/v1/config/ai/prompt               # body = raw prompt (empty = built-in default)
PUT /api/v1/config/location/{latitude|longitude}?value=...
PUT /api/v1/config/email/from?value=...
PUT /api/v1/config/mail/{host|port|username|password|auth|starttls}?value=...
```

### System
```bash
# Version + build info (public)
GET /api/v1/info

# Power management (rate-limited: 2 requests per 5 minutes, audit-logged)
POST /api/v1/system/shutdown
POST /api/v1/system/reboot

# Diagnostics (admin)
GET /api/v1/system/disk-usage
GET /api/v1/system/cpu
GET /api/v1/system/memory
GET /api/v1/system/snapshot       # disk + memory + CPU + stack info in one call
                                  # (polled every 2 s by the System page)
```

The `Software stack` panel on the System page surfaces three distinct
pieces of information:

- **Java runtime (JVM)** — read from `System.getProperty("java.version")` at
  request time. Reflects the JVM actually executing the JAR on the Pi.
- **Java build-time JVM** — frozen at `mvn package` time via Maven token
  filtering. Reflects the JDK that compiled the artifact.
- **Bytecode target** — `maven.compiler.target` (currently Java 11 — the Pi
  Zero constraint).

### Logs & events (admin)
```bash
# Application log files
GET /api/v1/logs                              # list files
GET /api/v1/logs/{filename}?lines=200&level=INFO&search=DoorService

# Business event journal (door, sensor, scheduler)
GET /api/v1/events/business?window=7d&category=door

# Authentication event journal
GET /api/v1/events/auth
```

### Users (admin)
```bash
GET /api/v1/users
POST /api/v1/users
GET /api/v1/users/me
PUT /api/v1/users/me                # self-service: email, password, language, notifications
PUT /api/v1/users/{login}
DELETE /api/v1/users/{login}
```

### Residents (admin)
```bash
GET /api/v1/residents
POST /api/v1/residents
GET /api/v1/residents/{id}
PUT /api/v1/residents/{id}
DELETE /api/v1/residents/{id}
GET /api/v1/residents/{id}/photo
POST /api/v1/residents/{id}/photo   # multipart/form-data
DELETE /api/v1/residents/{id}/photo
```

### Web Push (PWA)
```bash
GET /api/v1/push/vapid-public-key    # public — needed to subscribe in-browser
POST /api/v1/push/subscribe
POST /api/v1/push/unsubscribe
POST /api/v1/push/test               # admin — fire a test notification
```

### Diagnostics
```bash
# GPIO state + button status (admin)
GET /api/v1/electronics/gpio
GET /api/v1/buttons/status

# Test the configured weather provider end-to-end (admin)
POST /api/v1/weather/test

# Test the configured SMTP relay (admin)
POST /api/v1/email/test
```

### Swagger UI
Interactive API documentation with request/response examples:
```
http://localhost:8080/swagger-ui/index.html?configUrl=/v3/api-docs/swagger-config
```

## Architecture

### Technology Stack

Backend:
- **Framework**: Spring Boot 2.7.18 (Java 11) — pinned by Pi Zero hardware
- **Database**: MariaDB (production), H2 (tests)
- **Hardware Interface**: Pi4J 2.4.0 + Picam 2.0.2 (native: `picam-2.0.1.so`)
- **Security**: Spring Security 5 (file-based users, bcrypt, CSRF, login rate limiter, audit log)
- **API Documentation**: SpringDoc OpenAPI 1.8.0
- **Scheduling**: Quartz
- **Caching**: Spring Cache (Caffeine)
- **Real-Time**: WebSocket with STOMP
- **AI Vision**: Ollama-compatible multimodal client (default model: `focus` = qwen2.5-vl)
- **Web Push**: VAPID-signed payloads via `web-push` (Pushpad / nl.martijndwars)
- **Monitoring**: Spring Boot Actuator + Micrometer
- **Testing**: JUnit 5, MockMvc, Spring Security Test

Frontend (bundled into the JAR):
- **Framework**: Angular 20 standalone components, zoneless change detection where possible
- **Routing**: Angular Router with lazy-loaded modules
- **i18n**: Angular `$localize`, French + English + Romanian locales pre-built
- **Build**: `frontend-maven-plugin` (Node 20, npm 10) bound to `prepare-package` so `mvn test` stays fast and `mvn package` produces the self-contained JAR
- **Testing**: Vitest 3 with `@analogjs/vitest-angular` (jsdom + zone.js, no headless browser)

### Layer Architecture
```
┌─────────────────────────────────────────┐
│   REST Controllers (/web package)      │  ← HTTP endpoints
│   *RestController classes               │
└─────────────────┬───────────────────────┘
                  │
┌─────────────────▼───────────────────────┐
│   Services (/controller package)       │  ← Business logic & hardware abstraction
│   *Service classes                      │
└─────────────────┬───────────────────────┘
                  │
┌─────────────────▼───────────────────────┐
│   Repositories (/data/repository)       │  ← Database access
│   Spring Data JPA interfaces            │
└─────────────────────────────────────────┘
```

### GPIO Strategy Pattern
```
GpioHermanasService (interface)
├── GpioHermanasRpiService  - Real Pi4j hardware (default)
└── GpioHermanasFakeService - Mock for testing (@Profile("gpio-fake"))
```

### Key Components

| Component | Purpose | Location |
|-----------|---------|----------|
| **REST Controllers** | HTTP API endpoints | `org.jibe77.hermanas.web.*RestController` |
| **Services** | Business logic & hardware control | `org.jibe77.hermanas.controller.*Service` + `org.jibe77.hermanas.service.*` |
| **Capture pipeline** | Async snapshot + AI analysis jobs | `org.jibe77.hermanas.service.capture.*` |
| **AI vision client** | Multimodal LLM client + cache + prompt builder | `org.jibe77.hermanas.client.ai.*` |
| **Schedulers** | Automated jobs (sun, camera, disk) | `org.jibe77.hermanas.scheduler.*` |
| **Event Sourcing** | Door operation audit trail | `org.jibe77.hermanas.controller.event.*` |
| **Health Indicators** | Component health checks | `org.jibe77.hermanas.health.*` |
| **Metrics** | Micrometer metrics collection | `org.jibe77.hermanas.metrics.*` |
| **WebSocket** | Real-time notifications | `org.jibe77.hermanas.websocket.*` |
| **Security** | Auth, audit, login rate limit, Siri token | `org.jibe77.hermanas.security.*` |
| **SPA fallback** | Forwards unknown routes to `index.html` | `org.jibe77.hermanas.web.SpaErrorController` |

### Scheduled Jobs
- **SunRelatedJob** - Every 60 seconds (door/light scheduling based on sun position)
- **CameraJob** - Every 3 hours (automated photo capture)
- **DiskSpaceJob** - Daily (storage monitoring)

## Development

### Setup Development Environment

1. **Install dependencies**
   ```bash
   # macOS
   brew install maven openjdk@11 mariadb

   # Ubuntu/Debian
   apt-get install maven openjdk-11-jdk mariadb-server
   ```

2. **Setup database**
   ```bash
   mysql -u root -p
   CREATE DATABASE hermanas;
   CREATE USER 'hermanas'@'localhost' IDENTIFIED BY 'password';
   GRANT ALL PRIVILEGES ON hermanas.* TO 'hermanas'@'localhost';
   FLUSH PRIVILEGES;
   ```

3. **Use fake GPIO profile**
   ```bash
   mvn spring-boot:run -Dspring.profiles.active=gpio-fake
   ```
   This allows development on non-Raspberry Pi hardware.

### Project Structure
```
hermanas/
├── src/main/java/org/jibe77/hermanas/
│   ├── web/                    # REST controllers (*RestController) + SPA fallback
│   ├── controller/             # Hardware-facing services (door/light/fan/…)
│   ├── service/                # Application services (capture, config, sensor, …)
│   ├── client/                 # Outbound clients (ai, email, weather)
│   ├── data/                   # Entities, repositories, DTOs, migrations
│   ├── scheduler/              # Quartz jobs & sun calculations
│   ├── security/               # Spring Security config + audit + rate limit + Siri token
│   ├── health/                 # Actuator health indicators
│   ├── metrics/                # Micrometer metrics
│   ├── websocket/              # STOMP configuration + notifications
│   └── HermanasApplication.java
├── src/main/resources/
│   ├── application.properties  # Main configuration
│   ├── messages*.properties    # Backend i18n bundles
│   └── (no /static — the SPA is copied here at package time)
├── src/test/java/              # Test suite
├── frontend/                   # Angular SPA (bundled into the JAR)
│   ├── src/                    # Angular sources
│   ├── package.json            # npm dependencies (Angular 20, Vitest 3)
│   └── angular.json            # Angular CLI workspace
├── docs/                       # Generators for security report + test plan
├── pom.xml                     # Maven (+ frontend-maven-plugin)
├── CLAUDE.md                   # AI assistant instructions
└── README.md                   # This file
```

### Build Commands

```bash
# Clean and build (also runs the frontend npm build and bundles the SPA)
mvn clean package

# Run tests (excludes image processing tests; SKIPS the npm build)
mvn test

# Run all tests including image processing
mvn test -DexcludedGroups=

# Frontend-only builds while iterating on the SPA
cd frontend && npm run build      # production build
cd frontend && npm start          # ng serve at http://localhost:4200

# Check for dependency / plugin updates
mvn versions:display-dependency-updates
mvn versions:display-plugin-updates

# Generate code coverage report
mvn test jacoco:report
# Report: target/site/jacoco/index.html
```

The `frontend-maven-plugin` runs `npm install` + `npm run build` during the
Maven `prepare-package` phase and `maven-resources-plugin` copies the
generated `dist/hermanas-client/...` directory into `target/classes/static/`
so Spring Boot serves it automatically. `mvn test` stays fast because the
frontend hooks are not bound to the `test` phase.

### Testing

The project includes multiple test types:

- **Unit Tests** - Service layer testing with mocked dependencies
- **REST Layer Tests** - `@WebMvcTest` with MockMvc
- **Integration Tests** - `@SpringBootTest` for end-to-end testing
- **Repository Tests** - `@DataJpaTest` for database operations

Run tests without Raspberry Pi hardware:
```bash
mvn test -Dspring.profiles.active=gpio-fake
```

Image processing tests are tagged and excluded by default:
```bash
# Run only image processing tests
mvn test -Dgroups=image_processing
```

#### Frontend tests (Vitest)

The Angular frontend uses **Vitest** with `@analogjs/vitest-angular`. Tests run
under jsdom + zone.js (no headless browser needed) — typically a few seconds
per run.

```bash
cd frontend

# Single pass (CI mode, exits with non-zero on failure)
npm test

# Watch mode while developing
npm run test:watch

# With coverage report (HTML at coverage/index.html)
npm run test:coverage
```

Assertion style is Jest-compatible (`expect`, `vi.fn()`, `vi.spyOn()`). No
Jasmine globals — the suite was migrated when Karma was deprecated in
Angular 20.

The suite covers the auth flow (LoginService outcomes, AuthGuard, AdminGuard,
`UserService.isAdmin`), the EnergyService HTTP surface, the Energy admin
ChartsComponent (ms ↔ minutes conversion, save fan-out, force-ECO radio),
PhotosService URL encoding, the side-nav signal derivations, and the
existing dashboard websocket / HTTP-error-interceptor specs that were
ported from Jasmine.

### Code Quality

- **Logger Pattern**: All classes use `private static final Logger logger = LoggerFactory.getLogger(ClassName.class)`
- **Exception Handling**: Global exception handler with `@ControllerAdvice`
- **Input Validation**: `@Valid`, `@Min`, `@Max` on request parameters
- **API Documentation**: OpenAPI annotations on all endpoints
- **DTOs**: Separate DTOs from JPA entities for API responses
- **Security**: Audit logging with `@AuditLog` annotation

## Deployment

### Raspberry Pi Setup

1. **Install Java 11**
   ```bash
   sudo apt-get update
   sudo apt-get install openjdk-11-jdk
   ```

2. **Install MariaDB**
   ```bash
   sudo apt-get install mariadb-server
   sudo mysql_secure_installation
   ```

3. **Install Pi4J native libraries**
   ```bash
   # Copy picam native library
   sudo cp native/picam-2.0.1.so /home/pi/
   ```

4. **Configure GPIO permissions**
   ```bash
   sudo usermod -a -G gpio pi
   sudo usermod -a -G video pi
   ```

5. **Deploy application**
   ```bash
   scp target/hermanas-0.8.2.jar pi@raspberrypi:/home/pi/
   ```

6. **Create systemd service**
   ```bash
   sudo nano /etc/systemd/system/hermanas.service
   ```

   ```ini
   [Unit]
   Description=Hermanas Chicken Coop Automation
   After=mariadb.service

   [Service]
   Type=simple
   User=pi
   ExecStart=/usr/bin/java -jar /home/pi/hermanas-0.8.2.jar
   SuccessExitStatus=143
   Restart=on-failure
   RestartSec=10

   [Install]
   WantedBy=multi-user.target
   ```

7. **Enable and start service**
   ```bash
   sudo systemctl daemon-reload
   sudo systemctl enable hermanas
   sudo systemctl start hermanas
   ```

8. **View logs**
   ```bash
   sudo journalctl -u hermanas -f
   ```

### Production Configuration

For production, externalize sensitive configuration:
```bash
# Create external config
sudo nano /etc/hermanas/application.properties

# Run with external config
java -jar hermanas-0.8.2.jar --spring.config.location=/etc/hermanas/application.properties
```

## Monitoring

### Health Checks
```bash
# Overall health
curl http://localhost:8080/actuator/health

# Component-specific health
curl http://localhost:8080/actuator/health/door
curl http://localhost:8080/actuator/health/sensor
```

### Metrics
```bash
# Available metrics
curl http://localhost:8080/actuator/metrics

# Door operations
curl http://localhost:8080/actuator/metrics/hermanas.door.operations

# Temperature gauge
curl http://localhost:8080/actuator/metrics/hermanas.sensor.temperature

# Door operation duration
curl http://localhost:8080/actuator/metrics/hermanas.door.operation.duration
```

### Event History
```bash
# View all door events
curl http://localhost:8080/api/v1/door/events

# Events include: DOOR_OPENED, DOOR_CLOSED, DOOR_OPEN_FAILED,
#                 DOOR_CLOSE_FAILED, DOOR_POSITION_UNKNOWN
```

## Siri Integration

Hermanas REST API can be called from any iOS device using Siri Shortcuts:

1. Open **Shortcuts** app on iOS
2. Create a new shortcut
3. Add **Get Contents of URL** action
4. Configure:
   - **URL**: `http://your-pi-address:8080/api/v1/door/open`
   - **Method**: POST
   - **Headers**: Add Authorization header if needed
5. Name the shortcut: "Open chicken door"
6. Say: "Hey Siri, open chicken door"

Example shortcuts:
- "Open chicken door" → `POST /api/v1/door/open`
- "Close chicken door" → `POST /api/v1/door/close`
- "Check coop temperature" → `GET /api/v1/sensor/info`
- "Turn on coop light" → `POST /api/v1/light/switch`

## Troubleshooting

### Door Issues
```bash
# Check door health
curl http://localhost:8080/actuator/health/door

# View door event history
curl http://localhost:8080/api/v1/door/events

# Check door status
curl http://localhost:8080/api/v1/door/status

# Manual servo test (small movements)
curl "http://localhost:8080/api/v1/door/turnClockwise?duration=50"
```

### Camera Issues
```bash
# Verify picam native library is loaded
ls -la /home/pi/picam-2.0.1.so

# Check camera permissions
groups pi | grep video

# Test camera
curl -X POST http://localhost:8080/api/v1/camera/takePicture
```

### Sensor Issues
```bash
# Check sensor health
curl http://localhost:8080/actuator/health/sensor

# Get current reading
curl http://localhost:8080/api/v1/sensor/info

# Verify GPIO pin configuration
cat /boot/config.txt | grep gpio
```

### Database Issues
```bash
# Check database connection
mysql -u hermanas -p hermanas

# View application logs
sudo journalctl -u hermanas -n 100

# Check database health
curl http://localhost:8080/actuator/health/db
```

## Hardware Constraints

This application is designed for **Raspberry Pi Zero**, which imposes the following limitations:

- **Java 11 Maximum** - Cannot upgrade to Java 17+ due to hardware constraints
- **Spring Boot 2.x** - Cannot upgrade to Spring Boot 3.x (requires Java 17+)
- **Pi4J 2.4.0 Maximum** - Newer versions incompatible with Pi Zero + Java 11
- **Picam 2.0.2 Maximum** - Newer versions incompatible with Pi Zero + Java 11

These constraints are documented in `CLAUDE.md`. Upgrading to a more powerful Raspberry Pi model would remove these limitations.

## Contributing

Contributions are welcome! Please read `CLAUDE.md` for development guidelines and architecture decisions.

### Development Workflow
1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Make your changes with tests
4. Ensure all tests pass (`mvn test`)
5. Commit your changes (`git commit -m 'Add amazing feature'`)
6. Push to the branch (`git push origin feature/amazing-feature`)
7. Open a Pull Request

## License

This project is licensed under the MIT License.

## Support

- **Issues**: [GitHub Issues](https://github.com/jibe77/hermanas/issues)
- **Live Demo**: [http://www.hermanas.fr](http://www.hermanas.fr) — try the "Demo mode" button

## Acknowledgments

- **Spring Boot** — Application framework
- **Pi4J** — Raspberry Pi GPIO library
- **SunriseSunset Library** — Astronomical calculations
- **Picam** — Raspberry Pi camera interface
- **Ollama** — Local multimodal LLM serving for the AI scene analysis
- **Angular 20** — Frontend framework
- **Vitest** — Frontend test runner

---

Built with care for our feathered friends.
