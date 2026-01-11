# Hermanas Copilot Instructions

## Project Overview
Hermanas is a Spring Boot 2.7 application automating chicken coop operations on Raspberry Pi Zero hardware. It controls door opening/closing based on sunrise/sunset, manages lighting/fan/music, monitors sensors, and provides camera surveillance.

**Hardware Constraint:** Limited to Java 11 and Spring Boot 2.x due to Pi Zero capabilities.

## Architecture
- **Web Layer** (`web/`): REST controllers (`*RestController`) with `/api/v1/*` endpoints
- **Service Layer** (`controller/`): Hardware abstraction services (`*Service`) 
- **Data Layer** (`data/`): JPA repositories and entities
- **Scheduling** (`scheduler/`): Quartz jobs for sun-based automation
- **GPIO Pattern**: `GpioHermanasService` interface with Rpi/Fake implementations

## Key Conventions
- **Logger Declaration**: `private static final Logger logger = LoggerFactory.getLogger(ClassName.class)`
- **Package Structure**: Controllers in `web/`, services in `controller/` (despite naming)
- **API Responses**: Use DTOs, not entities (e.g., `SensorDTO` via `SensorMapper`)
- **Configuration**: Externalized in `application.properties`, hot-reload via `POST /api/v1/config/refresh`
- **Security**: `@AuditLog` annotation for sensitive operations, rate limiting on shutdown/reboot
- **Testing**: Use `gpio-fake` profile, exclude image processing tests with `-DexcludedGroups=image_processing`

## Development Workflow
```bash
# Build and run with fake GPIO (development)
mvn clean package && mvn spring-boot:run -Dspring.profiles.active=gpio-fake

# Run tests (excludes image processing)
mvn test

# Run all tests including image processing
mvn test -DexcludedGroups=

# Deploy to Pi
./deploy.sh
```

## Common Patterns
- **Door Control**: Check position sensors before operations, record events (`DoorEventService`)
- **Sun Scheduling**: Use `SunTimeUtils` for astronomical calculations with caching
- **Health Checks**: Implement `HealthIndicator` for component monitoring
- **WebSocket**: Send real-time updates via `SimpMessagingTemplate` to `/topic/status`
- **Metrics**: Use `HermanasMetrics` for counters/gauges/timers (e.g., `door.operations.total`)

## Key Files
- `application.properties`: GPIO pins, timing, location coordinates
- `pom.xml`: Dependencies (Pi4J 2.4.0, Spring Boot 2.7.18)
- `GlobalExceptionHandler`: Centralized error handling with domain-specific responses
- `SecurityConfig`: Authentication setup with `/api/v1/*` paths
- `deploy.sh`: Production deployment script

## Hardware Integration
- **GPIO Pins**: Servo (25), buttons (15/18), light (14), fan (23), sensor (4)
- **Camera**: Requires `picam-2.0.1.so` native library, streaming on port 8081
- **Database**: MariaDB production, H2 testing
- **Scheduling**: `SunRelatedJob` (60s), `CameraJob` (3h), `DiskSpaceJob` (daily)

## API Patterns
- **Versioning**: All endpoints prefixed `/api/v1/`
- **Validation**: `@Valid` with `@Min/@Max` on parameters
- **Documentation**: OpenAPI annotations on all controllers
- **State Changes**: Use POST for mutations, GET for queries
- **Force Parameter**: Optional `?force=true` for door operations bypassing safety checks

## Event Sourcing
Door operations logged as events (`DOOR_OPENED`, `DOOR_CLOSED`, etc.) via `EventRepository`. Query history with `GET /api/v1/door/events`.

## Testing Notes
- Mock GPIO with `@Profile("gpio-fake")`
- Image processing tests tagged `@Tag("image_processing")`
- Use `@WebMvcTest` for REST layer, `@DataJpaTest` for repositories
- Current coverage: ~21% (18 test files / 84 source files)</content>
<parameter name="filePath">/Users/jean-baptisterenaux/GitHub/hermanas/.github/copilot-instructions.md