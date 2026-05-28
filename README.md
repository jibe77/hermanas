# Hermanas

Hermanas is a comprehensive chicken coop automation system designed to run on Raspberry Pi hardware. It automates door operations based on sunrise/sunset times, manages environmental controls (lighting, ventilation, music), monitors temperature and humidity, and provides real-time camera surveillance.

**Live Demo:** [http://www.hermanas.fr](http://www.hermanas.fr)
**Frontend Repository:** [https://github.com/jibe77/hermanasclient](https://github.com/jibe77/hermanasclient)

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
- **AI-Powered Door Detection** - Automatic detection of door closing issues using computer vision
- **WebSocket Real-Time Updates** - Live status notifications via STOMP protocol
- **Health Monitoring** - Spring Boot Actuator health checks for all components

### Energy Management
- **Three Power Modes** - Eco (winter), Regular, and Sunny (summer) consumption profiles
- **Configurable Timers** - Mode-specific timer delays for all equipment
- **WiFi Power Control** - Automatic WiFi disable in eco mode
- **Machine Shutdown** - Scheduled system shutdown capabilities in low-power modes

### Remote Access
- **REST API** - Complete `/api/v1/*` endpoints for all operations
- **Swagger Documentation** - Interactive API documentation
- **Siri Integration** - Control via iOS shortcuts and voice commands
- **Security** - Spring Security with role-based access control

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
# Java 11 (required for Pi Zero compatibility)
java -version  # Should show 1.11.x

# Maven 3.6+
mvn -version

# MariaDB 10.x
mysql --version
```

### Installation

1. **Clone the repository**
   ```bash
   git clone https://github.com/jibe77/hermanas.git
   cd hermanas
   ```

2. **Configure the application**
   ```bash
   cp src/main/resources/application.properties.example src/main/resources/application.properties
   # Edit application.properties with your settings (see Configuration section)
   ```

3. **Build the application**
   ```bash
   mvn clean package
   ```

4. **Run the application**
   ```bash
   # On Raspberry Pi with real GPIO hardware
   java -jar target/hermanas-0.8.1.jar

   # On development machine with fake GPIO
   mvn spring-boot:run -Dspring.profiles.active=gpio-fake
   ```

5. **Access the application**
   - API: `http://localhost:8080/api/v1/`
   - Swagger UI: `http://localhost:8080/swagger-ui/index.html`
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
java -jar target/hermanas-0.8.1.jar --hash
# Password: ********
# {bcrypt}$2a$10$....

# Inline (avoid in shared terminals — leaves the password in history)
java -jar target/hermanas-0.8.1.jar --hash "your-password"
```

Copy the `{bcrypt}...` line as-is into `users.properties` as the user's `.password` value.

### Adding a user — full workflow

```bash
# 1. Generate the hash
java -jar target/hermanas-0.8.1.jar --hash

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
# Take picture
POST /api/v1/camera/takePicture

# Get door closing rate (AI analysis)
GET /api/v1/camera/closingRate

# Start/stop video stream
POST /api/v1/camera/stream
POST /api/v1/camera/stopStream
```

### Sensors
```bash
# Get current sensor reading
GET /api/v1/sensor/info

# Get historical data
GET /api/v1/sensor/history/day
GET /api/v1/sensor/history/week
GET /api/v1/sensor/history/month
GET /api/v1/sensor/history/year
GET /api/v1/sensor/history/all
```

### Environmental Controls
```bash
# Light
POST /api/v1/light/switch
GET /api/v1/light/status

# Fan
POST /api/v1/fan/switch
GET /api/v1/fan/status

# Music
POST /api/v1/music/switch
GET /api/v1/music/status
POST /api/v1/music/cocorico  # Play rooster sound
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

# WiFi control
GET /api/v1/energy/wifi/status
POST /api/v1/energy/wifi/enable
POST /api/v1/energy/wifi/disable

# Historical energy data
GET /api/v1/energy/dateRange?startDate=2024-01-01&endDate=2024-01-31
```

### Configuration
```bash
# View current configuration
GET /api/v1/config

# Refresh caches (hot-reload)
POST /api/v1/config/refresh

# Update light timers
PUT /api/v1/config/light/eco?delayMs=300000
PUT /api/v1/config/light/regular?delayMs=600000
PUT /api/v1/config/light/sunny?delayMs=900000

# Update consumption mode
PUT /api/v1/config/consumption/eco-days?days=45
PUT /api/v1/config/consumption/force-eco?force=true

# Update system behavior
PUT /api/v1/config/system/eco/shutdown?enabled=false
PUT /api/v1/config/system/eco/wifi?disabled=true
```

### System
```bash
# System information
GET /api/v1/info

# Power management
POST /api/v1/system/shutdown
POST /api/v1/system/reboot
```

### Swagger UI
Interactive API documentation with request/response examples:
```
http://localhost:8080/swagger-ui/index.html?configUrl=/v3/api-docs/swagger-config
```

## Architecture

### Technology Stack
- **Framework**: Spring Boot 2.7.18 (Java 11)
- **Database**: MariaDB (production), H2 (testing)
- **Hardware Interface**: Pi4J 2.4.0
- **Security**: Spring Security with role-based access
- **API Documentation**: SpringDoc OpenAPI 1.8.0
- **Scheduling**: Quartz Scheduler
- **Caching**: Spring Cache
- **Real-Time**: WebSocket with STOMP
- **Monitoring**: Spring Boot Actuator + Micrometer
- **Testing**: JUnit 5, MockMvc, Spring Security Test

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
| **Services** | Business logic & hardware control | `org.jibe77.hermanas.controller.*Service` |
| **Schedulers** | Automated jobs (sun, camera, disk) | `org.jibe77.hermanas.scheduler.*` |
| **Event Sourcing** | Door operation audit trail | `org.jibe77.hermanas.controller.event.*` |
| **Health Indicators** | Component health checks | `org.jibe77.hermanas.health.*` |
| **Metrics** | Micrometer metrics collection | `org.jibe77.hermanas.metrics.*` |
| **WebSocket** | Real-time notifications | `org.jibe77.hermanas.websocket.*` |
| **Security** | Authentication & authorization | `org.jibe77.hermanas.security.*` |

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
│   ├── web/                    # REST controllers (*RestController)
│   ├── controller/             # Services (*Service)
│   ├── data/                   # Entities, repositories, DTOs
│   ├── scheduler/              # Quartz jobs & sun calculations
│   ├── security/               # Authentication & authorization
│   ├── health/                 # Health indicators
│   ├── metrics/                # Micrometer metrics
│   ├── websocket/              # WebSocket configuration
│   └── HermanasApplication.java
├── src/main/resources/
│   ├── application.properties  # Main configuration
│   └── static/                 # Static web resources
├── src/test/java/              # Test suite
├── pom.xml                     # Maven dependencies
├── CLAUDE.md                   # AI assistant instructions
└── README.md                   # This file
```

### Build Commands

```bash
# Clean and build
mvn clean package

# Run tests (excludes image processing tests)
mvn test

# Run all tests including image processing
mvn test -DexcludedGroups=

# Check for dependency updates
mvn versions:display-dependency-updates

# Check for plugin updates
mvn versions:display-plugin-updates

# Generate code coverage report
mvn test jacoco:report
# Report: target/site/jacoco/index.html
```

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

The suite covers the auth flow (LoginService outcomes, AuthGuard, AdminGuard,
UserService.isAdmin), the EnergyService HTTP surface, the Energy admin
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
   scp target/hermanas-0.8.1.jar pi@raspberrypi:/home/pi/
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
   ExecStart=/usr/bin/java -jar /home/pi/hermanas-0.8.1.jar
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
java -jar hermanas-0.8.1.jar --spring.config.location=/etc/hermanas/application.properties
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
- **Live Demo**: [http://www.hermanas.fr](http://www.hermanas.fr)
- **Frontend**: [https://github.com/jibe77/hermanasclient](https://github.com/jibe77/hermanasclient)

## Acknowledgments

- **Spring Boot** - Application framework
- **Pi4J** - Raspberry Pi GPIO library
- **SunriseSunset Library** - Astronomical calculations by Carmen
- **Picam** - Raspberry Pi camera interface

---

Built with care for our feathered friends.
