# Hermanas — Constitution

> **Spec Kit format.** This document states the **principles** any contributor (human or LLM) must follow when working on Hermanas. It is paired with `hermanas.spec.md` (the *what*) and complements it with the *how we work*. Where this document and a feature spec disagree, **this document wins**.

**Version:** 1.0
**Status:** Active
**Last revised:** 2026-06-21
**Maintainer:** jibe77

---

## Article I — Hardware comes first

The deployment target is a **Raspberry Pi Zero** (ARMv6, 512 MB RAM, single slow core). Every decision must answer: *will this run on the Pi?*

1. **Java 11.** No language feature, library, or plugin that requires Java 17+. No exceptions until the hardware is upgraded.
2. **Spring Boot 2.7.x.** Do not introduce code paths that assume Jakarta namespace, Spring Security 6/7, or Spring Boot 3 conventions.
3. **No heavy runtime dependencies.** Before adding a library, check its transitive footprint, its native dependencies, and whether it loads classes eagerly. If a library exists in a "lite" form, prefer it.
4. **No sub-second polling loops.** Background work is scheduled, not busy-waited.
5. **External I/O is timeout-bounded and cached.** Weather API, vision LLM, SMTP — none may block a scheduler tick indefinitely. Use `resilience4j` (1.7.1, last Java 11 line) for circuit breakers.
6. **Logs default to INFO.** DEBUG floods the SD card and the Pi will eventually refuse to boot. Anyone bumping log levels in committed code must justify it in the commit message.

**Pinned versions that must not be touched without explicit decision:**

| Component | Pinned | Reason |
|---|---|---|
| Pi4j | 2.4.0 | >2.4.0 broken on Pi Zero + Java 11 |
| picam JNI | 2.0.1 | >2.4.0 incompatible with Pi Zero + Java 11 |
| resilience4j | 1.7.1 | 2.x requires Java 17 |
| commons-lang3 | 3.20.0 | last Java 8-compatible line |
| CycloneDX maven plugin | 2.9.1 | last Java 11-compatible release |

---

## Article II — Architecture is binding

The layer layout described in `hermanas.spec.md` §5 is not advisory. It exists to keep hardware concerns out of the web layer and web concerns out of services.

1. **Web layer** lives in `web/`. Classes end in `RestController`. They handle HTTP, validation, OpenAPI annotations, and DTO mapping. They contain **no business logic** and **no hardware calls**.
2. **Service layer** lives in `controller/`. Classes end in `Service`. They orchestrate business logic and hardware. They never touch `HttpServletRequest` or Spring MVC types.
3. **Hardware access** goes through `GpioHermanasService`. No service may import `com.pi4j.*` directly. The fake implementation (`@Profile("gpio-fake")`) must be a drop-in replacement.
4. **REST returns DTOs.** Never serialize a JPA entity directly. Even if the shape is identical today, the DTO exists so persistence and API can diverge later.
5. **All endpoints under `/api/v1/`.** No unversioned paths. New endpoints use this prefix; renaming an existing one is a breaking change (announce it, bump the frontend).
6. **State-changing endpoints use `POST` / `PUT` / `DELETE`.** `GET` is reserved for safe, idempotent reads.

Violations of this article are not "style" — they are bugs.

---

## Article III — Data has memory

The system records what it does, so the keeper can investigate when the door does something unexpected at 4 a.m.

1. **Every state change emits an `Event`.** Door open, door close, light on, fan on, config change, login, manual override — all of them. The event journal is the user's only forensic tool.
2. **`EventType` is an append-only enum.** It is persisted as `ORDINAL`. Reordering or inserting values in the middle silently rewrites history. New types go at the end. Removed types stay as `@Deprecated` placeholders.
3. **`Parameter.entryValue` is `TEXT` (`@Lob`).** Not `VARCHAR(255)`. Some config values (AI prompts, monthly mode mappings) exceed 1.5 kB and broke the column historically.
4. **Audit log is separate from app log.** Security-sensitive operations (login, system reboot/shutdown, manual door override, user creation) write to a dedicated `AUDIT` logger. This log is the one the keeper reads if something feels wrong.
5. **Metrics under `hermanas.*`.** Door counters, sensor gauges, appliance switches. Do not invent ad-hoc metric namespaces.

---

## Article IV — Security is local

Hermanas does not call out to an identity provider, ever. The whole point of a personal coop system is that it works when the internet does not.

1. **Local file-backed users.** `users.properties` (bcrypt) is the source of truth for authentication. Path configurable.
2. **Session-based auth.** Form login at `POST /api/v1/auth/login`, returning 200/401 as JSON. Session cookie (`JSESSIONID`), optional remember-me cookie. No JWT, no OAuth.
3. **CSRF is on.** `CookieCsrfTokenRepository.withHttpOnlyFalse()`. The SPA reads `XSRF-TOKEN` and echoes `X-XSRF-TOKEN`. Do not disable CSRF "to make tests easier."
4. **No CORS.** The SPA is bundled in the same JAR and served from the same origin. If you find yourself adding a CORS bean, you are about to break the deployment model.
5. **Rate limiting is mandatory** for `/auth/login`, `/system/reboot`, `/system/shutdown`. Implemented in-house (no extra dependency for the Pi).
6. **Secrets are not in source.** `users.properties`, SMTP credentials, weather API key, VAPID keys, remember-me key — all via environment or external files, all in `.gitignore`.
7. **Roles, minimum two.** `USER` and `ADMIN`. Admin-only: system control, user CRUD, electronics debug, config writes.

---

## Article V — The keeper is one person

Design decisions assume **one operator, one coop, one Pi**. This shapes the UX and the implementation in ways that matter.

1. **No multi-tenancy.** No "organizations", no "households", no per-tenant data partitioning. If a feature requires that, the design is wrong.
2. **The dashboard is the home screen.** It must show door state, light/fan/music state, weather, and the latest sensor reading at a glance. Anything else is a separate screen.
3. **The keeper may not be technical.** Errors must be readable, in their language, with a suggested next action. Stack traces never reach the UI.
4. **The keeper is offline-tolerant.** A failed weather call or LLM call must not break the page. The page must degrade, not blank out.
5. **Demo mode is a real feature.** It exists so the keeper can show the system to a friend without granting them write access. Mutations are intercepted by a confirmation modal; error toasts are suppressed. Treat demo mode like a first-class user role.

---

## Article VI — Tests verify hardware-free

Tests run on a laptop. They must never assume a Pi is present.

1. **`gpio-fake` profile must work.** Every service that touches GPIO must be testable under this profile, with no native libraries loaded.
2. **H2 in-memory for tests.** MariaDB is the production database; H2 is the test database. Schema differences are caught by `@DataJpaTest` against H2 — if H2 cannot reproduce a MariaDB feature you depend on, redesign.
3. **Image-processing tests are tagged `image_processing`.** Excluded from `mvn test` by default because they require the picam `.so`. They are run on demand on the Pi.
4. **Frontend tests run in Vitest + jsdom.** No Karma, no headless browser. Jasmine globals (`spyOn`, `.and.returnValue`) are forbidden — they were translated during the Vitest migration and must not creep back.
5. **No `done()` callbacks in frontend tests.** Use `firstValueFrom` / `lastValueFrom`, or return a Promise. Vitest 3 forbids `done`.
6. **Playwright is the e2e suite.** It runs against `localhost:4200`. It is not in the default `npm test` (Vitest only) but is part of the release checklist.

---

## Article VII — Build produces one artifact

Deployment is one JAR copied to the Pi. The build pipeline must preserve that property.

1. **`mvn package` produces a self-contained JAR.** The Angular SPA is built and embedded at this phase, not earlier. `mvn test` and `mvn compile` must not trigger an npm build.
2. **`frontend-maven-plugin` executions are bound to `generate-resources` or `prepare-package`.** Never to `compile` or `test`.
3. **`mvn -P!with-frontend`** skips the npm build for backend-only iterations. Keep this profile working.
4. **CycloneDX SBOM is produced at every build** and embedded in `META-INF/sbom/` for downstream scanning. Do not remove this plugin without a security discussion.
5. **`deploy.sh` is the deployment path.** Changes that break it must update it in the same commit.

---

## Article VIII — Configuration is hot

The keeper should not need to SSH into the Pi to tune the system.

1. **All knobs externalized.** No hardcoded magic numbers in services. Servo positions, durations, GPIO pins, timer delays, locations, schedule offsets — all in `application.properties` and overridable.
2. **All config writes go through `ConfigRestController`.** Direct edits to `application.properties` on the Pi work but are not the normal path.
3. **Config setters use `@CacheEvict`.** Cache invalidation is automatic for most knobs. `POST /api/v1/config/refresh` clears everything manually as the escape hatch.
4. **Defaults are sensible for a beginner.** The Eiffel Tower latitude/longitude is a deliberate placeholder; nothing else should require an override to boot.

---

## Article IX — Code style

These are not aesthetics. They are debt-reduction.

1. **Loggers are `private static final`.** Always. Created via `LoggerFactory.getLogger(<Class>.class)`.
2. **No `e.printStackTrace()`.** Use `logger.error("message", e)`. Restore the interrupt flag after catching `InterruptedException`.
3. **Use `Integer.parseInt`** for String → int. `Integer.valueOf` allocates an Integer object you immediately unbox — wasteful on the Pi.
4. **No empty `catch (NullPointerException)`.** Add a null check upstream.
5. **No comments that restate the code.** Comments explain *why*, not *what*. Names carry the *what*.
6. **No "added for issue #X" or "used by Y" comments.** Those belong in the PR description; they rot in the source.
7. **Don't add features beyond what's asked.** A bug fix is not a refactor invitation. Three similar lines is better than a premature abstraction.

---

## Article X — Specs evolve with the code

This is the meta-rule.

1. **`specs/` is part of the codebase.** A PR that changes behavior must update the relevant spec section in the same commit.
2. **When a constraint becomes obsolete, retire it explicitly.** Crossing out, marking "**RETIRED:** reason (YYYY-MM-DD)", and moving on is fine. Silent deletion is not.
3. **When the hardware is upgraded, this document is the first thing to revisit.** Article I changes; Articles II–IX may follow. Do not start the Spring Boot 3 migration before updating the constitution.

---

*This constitution is short on purpose. If a rule isn't here, the team has not yet agreed it's a rule.*
