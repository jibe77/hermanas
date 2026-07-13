package org.jibe77.hermanas.web;

import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.jibe77.hermanas.data.entity.EventType;
import org.jibe77.hermanas.service.event.EventService;
import org.jibe77.hermanas.service.system.SystemService;
import org.jibe77.hermanas.security.audit.AuditLog;
import org.jibe77.hermanas.security.ratelimit.RateLimited;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.info.BuildProperties;
import org.springframework.core.env.Environment;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/system")
@Tag(name = "System", description = "System control endpoints for shutdown and reboot operations")
public class SystemRestController {

    SystemService systemService;
    EventService eventService;
    /** Used by /snapshot to read JVM/process metrics without an HTTP self-call. */
    @Autowired(required = false)
    MeterRegistry meterRegistry;
    /** Spring's build-info bean — present when spring-boot-maven-plugin's
     *  build-info goal ran (it does in our pom). May be null in tests. */
    @Autowired(required = false)
    BuildProperties buildProperties;
    /** Used to read info.* properties for the snapshot's stack section. */
    @Autowired
    Environment environment;

    /**
     * Holds the last CPU jiffies snapshot read from {@code /proc/stat}. The
     * CPU percentage is the delta between two reads, so the first call after
     * boot necessarily returns 0 — the SPA polls every 2 s and the second
     * sample already produces a meaningful number. Volatile because the
     * controller is a singleton and concurrent polls from multiple admin
     * tabs would otherwise race on this field.
     */
    private volatile long[] lastCpuJiffies;

    public SystemRestController(SystemService systemService, EventService eventService) {
        this.systemService = systemService;
        this.eventService = eventService;
    }

    @Operation(
            summary = "Shutdown system",
            description = "Initiates a system shutdown (Raspberry Pi will power off)"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Shutdown initiated successfully"
            ),
            @ApiResponse(
                    responseCode = "429",
                    description = "Rate limit exceeded - too many shutdown attempts"
            )
    })
    @AuditLog(category = "SYSTEM", operation = "System shutdown initiated")
    @RateLimited(maxRequests = 2, windowSeconds = 300, message = "Too many shutdown attempts. Please wait 5 minutes.")
    @PostMapping(value = "/shutdown")
    public void shutdown() {
        eventService.record(EventType.SHUTDOWN_REQUESTED);
        systemService.shutdown();
    }

    @Operation(
            summary = "Reboot system",
            description = "Initiates a system reboot (Raspberry Pi will restart)"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Reboot initiated successfully"
            ),
            @ApiResponse(
                    responseCode = "429",
                    description = "Rate limit exceeded - too many reboot attempts"
            )
    })
    @AuditLog(category = "SYSTEM", operation = "System reboot initiated")
    @RateLimited(maxRequests = 2, windowSeconds = 300, message = "Too many reboot attempts. Please wait 5 minutes.")
    @PostMapping(value = "/reboot")
    public void reboot() {
        eventService.record(EventType.REBOOT_REQUESTED);
        systemService.reboot();
    }

    @Operation(
            summary = "Disk usage of the partition hosting the application",
            description = "Returns total, used and free bytes plus the usage percentage of the filesystem " +
                    "partition that contains the application working directory. Intended for the diagnostics " +
                    "panel — admin only."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Disk usage retrieved successfully"),
            @ApiResponse(responseCode = "403", description = "Authenticated user is not an administrator")
    })
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping(value = "/disk-usage")
    public Map<String, Object> diskUsage() {
        File root = new File(".").getAbsoluteFile();
        long total = root.getTotalSpace();
        long free = root.getFreeSpace();
        long used = total - free;
        double usedPercent = total > 0 ? (used * 100.0) / total : 0.0;

        Map<String, Object> response = new HashMap<>();
        response.put("path", root.getAbsolutePath());
        response.put("totalBytes", total);
        response.put("usedBytes", used);
        response.put("freeBytes", free);
        response.put("usedPercent", Math.round(usedPercent * 10.0) / 10.0);
        return response;
    }

    @Operation(
            summary = "OS-level CPU usage",
            description = "Returns the CPU utilisation percentage computed from the delta between two " +
                    "/proc/stat reads (so the first call after server start returns 0.0). Also reports " +
                    "the 1-minute load average and the core count for context. Admin only."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "CPU usage retrieved successfully"),
            @ApiResponse(responseCode = "403", description = "Authenticated user is not an administrator")
    })
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping(value = "/cpu")
    public Map<String, Object> cpuUsage() {
        // /proc/stat first line: "cpu  user nice system idle iowait irq softirq steal guest guest_nice"
        // Field semantics — total = sum of all columns, idle = idle + iowait.
        // The trick is to compute the delta between two snapshots, otherwise we
        // get the average since boot which barely moves.
        long[] now = readCpuJiffies();
        double usedPercent = 0.0;
        long[] previous = lastCpuJiffies;
        if (previous != null && now != null) {
            long idleDelta = (now[3] + now[4]) - (previous[3] + previous[4]);
            long totalDelta = 0;
            for (int i = 0; i < now.length; i++) {
                totalDelta += now[i] - previous[i];
            }
            if (totalDelta > 0) {
                double ratio = 1.0 - ((double) idleDelta / (double) totalDelta);
                usedPercent = Math.max(0.0, Math.min(100.0, ratio * 100.0));
            }
        }
        if (now != null) {
            lastCpuJiffies = now;
        }

        Map<String, Object> response = new HashMap<>();
        response.put("readable", now != null);
        response.put("usedPercent", Math.round(usedPercent * 10.0) / 10.0);
        response.put("coreCount", Runtime.getRuntime().availableProcessors());
        // System load average — the OS rolling 1-min figure. java.lang's
        // OperatingSystemMXBean exposes only the 1-min average; getting 5/15
        // would require the com.sun.management subclass which is JVM-specific.
        double load = java.lang.management.ManagementFactory
                .getOperatingSystemMXBean().getSystemLoadAverage();
        response.put("loadAverage1m", load >= 0 ? Math.round(load * 100.0) / 100.0 : null);
        // OS uptime (seconds since boot). Piggybacks on this endpoint rather
        // than spinning up a dedicated one — uptime changes linearly with time
        // so the 2 s polling cadence used by the SPA for CPU is fine here too.
        response.put("uptimeSeconds", readSystemUptimeSeconds());
        return response;
    }

    /**
     * Reads {@code /proc/uptime} (first field: seconds since boot, fractional).
     * Returns {@code null} on non-Linux hosts so the SPA can hide the row.
     */
    private Double readSystemUptimeSeconds() {
        File uptime = new File("/proc/uptime");
        if (!uptime.canRead()) {
            return null;
        }
        try (BufferedReader br = new BufferedReader(new FileReader(uptime))) {
            String line = br.readLine();
            if (line == null) {
                return null;
            }
            // Format: "12345.67 9876.54" — uptime then idle. We only want the first column.
            int space = line.indexOf(' ');
            String first = space > 0 ? line.substring(0, space) : line;
            return Double.parseDouble(first);
        } catch (IOException | NumberFormatException e) {
            return null;
        }
    }

    /**
     * Reads the first {@code cpu} line of {@code /proc/stat} and returns the
     * jiffies columns. Returns {@code null} on non-Linux hosts (e.g. dev mac)
     * so callers can degrade gracefully.
     */
    private long[] readCpuJiffies() {
        File stat = new File("/proc/stat");
        if (!stat.canRead()) {
            return null;
        }
        try (BufferedReader br = new BufferedReader(new FileReader(stat))) {
            String line = br.readLine();
            if (line == null || !line.startsWith("cpu")) {
                return null;
            }
            // "cpu  user nice system idle iowait irq softirq steal guest guest_nice"
            // Note the double space after "cpu" — split on whitespace and drop the label.
            String[] parts = line.trim().split("\\s+");
            long[] out = new long[parts.length - 1];
            for (int i = 0; i < out.length; i++) {
                out[i] = Long.parseLong(parts[i + 1]);
            }
            // We need at least 5 columns (idle + iowait used below).
            return out.length >= 5 ? out : null;
        } catch (IOException | NumberFormatException e) {
            return null;
        }
    }

    @Operation(
            summary = "OS-level memory and swap usage",
            description = "Reads /proc/meminfo on the host (Linux only) and returns RAM and swap " +
                    "totals/used/free in bytes plus the usage percentage. JVM heap statistics are " +
                    "available separately via /actuator/metrics/jvm.memory.*. Admin only since this " +
                    "is diagnostics data."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Memory usage retrieved successfully"),
            @ApiResponse(responseCode = "403", description = "Authenticated user is not an administrator"),
            @ApiResponse(responseCode = "501", description = "/proc/meminfo not readable (non-Linux host)")
    })
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping(value = "/memory")
    public Map<String, Object> memoryUsage() {
        // Reads /proc/meminfo line by line — each row looks like:
        //   "MemTotal:         499540 kB"
        // We only need MemTotal/MemAvailable/SwapTotal/SwapFree, the kernel
        // exposes them in kB regardless of the architecture (the trailing
        // " kB" suffix is part of the spec, not a unit conversion hint).
        File meminfo = new File("/proc/meminfo");
        Map<String, Long> kb = new HashMap<>();
        if (meminfo.canRead()) {
            try (BufferedReader br = new BufferedReader(new FileReader(meminfo))) {
                String line;
                while ((line = br.readLine()) != null) {
                    int colon = line.indexOf(':');
                    if (colon < 0) continue;
                    String key = line.substring(0, colon).trim();
                    String value = line.substring(colon + 1).trim();
                    // Strip trailing " kB" if present.
                    int space = value.indexOf(' ');
                    String num = space > 0 ? value.substring(0, space) : value;
                    try {
                        kb.put(key, Long.parseLong(num));
                    } catch (NumberFormatException ignore) {
                        // Skip non-numeric lines (e.g. Hugepagesize: 4 kB on some kernels).
                    }
                }
            } catch (IOException e) {
                // Fall through: kb stays empty, response below reports zeros + readable=false.
            }
        }

        long totalKb = kb.getOrDefault("MemTotal", 0L);
        // MemAvailable is the kernel's own estimate of how much memory is
        // available for a new workload without swapping (includes reclaimable
        // page cache). Prefer it over MemFree, which under-reports on Linux
        // because page cache pollutes the "free" number.
        long availableKb = kb.getOrDefault("MemAvailable", kb.getOrDefault("MemFree", 0L));
        long usedKb = Math.max(0L, totalKb - availableKb);
        long swapTotalKb = kb.getOrDefault("SwapTotal", 0L);
        long swapFreeKb = kb.getOrDefault("SwapFree", 0L);
        long swapUsedKb = Math.max(0L, swapTotalKb - swapFreeKb);

        Map<String, Object> response = new HashMap<>();
        response.put("readable", meminfo.canRead() && !kb.isEmpty());
        response.put("totalBytes", totalKb * 1024L);
        response.put("usedBytes", usedKb * 1024L);
        response.put("availableBytes", availableKb * 1024L);
        response.put("usedPercent", totalKb > 0
                ? Math.round(((double) usedKb * 100.0 / totalKb) * 10.0) / 10.0
                : 0.0);
        response.put("swapTotalBytes", swapTotalKb * 1024L);
        response.put("swapUsedBytes", swapUsedKb * 1024L);
        response.put("swapFreeBytes", swapFreeKb * 1024L);
        response.put("swapUsedPercent", swapTotalKb > 0
                ? Math.round(((double) swapUsedKb * 100.0 / swapTotalKb) * 10.0) / 10.0
                : 0.0);
        return response;
    }

    @Operation(
            summary = "Server-side wall-clock time",
            description = "Returns the coop's internal clock as ISO-8601 with offset, the zone ID, " +
                    "and epoch millis so the SPA can tick locally between polls without hammering the Pi."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Server time retrieved successfully"),
            @ApiResponse(responseCode = "403", description = "Authenticated user is not an administrator")
    })
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping(value = "/time")
    public Map<String, Object> serverTime() {
        java.time.ZonedDateTime now = java.time.ZonedDateTime.now();
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("iso", now.format(java.time.format.DateTimeFormatter.ISO_OFFSET_DATE_TIME));
        response.put("zoneId", now.getZone().getId());
        response.put("epochMs", now.toInstant().toEpochMilli());
        return response;
    }

    @Operation(
            summary = "All system + stack diagnostics in one shot",
            description = "Aggregates disk, memory, CPU, OS uptime, /actuator/info-style stack details " +
                    "and a handful of JVM metrics (uptime, heap, threads, requests, process CPU) into a " +
                    "single response. Intended for the System page which polls this every 2 s — one " +
                    "request instead of nine, much friendlier to the Pi Zero. Admin only."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Snapshot retrieved successfully"),
            @ApiResponse(responseCode = "403", description = "Authenticated user is not an administrator")
    })
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping(value = "/snapshot")
    public Map<String, Object> snapshot() {
        Map<String, Object> root = new LinkedHashMap<>();
        root.put("disk", diskUsage());
        root.put("memory", memoryUsage());
        root.put("cpu", cpuUsage());
        root.put("stack", buildStackSection());
        return root;
    }

    /**
     * Builds the {@code stack} part of the snapshot — flat map mirroring what
     * the SPA used to fetch via /actuator/info + /actuator/metrics/*. Values
     * are read straight from the {@link MeterRegistry} (no HTTP self-call)
     * which is both faster and avoids an extra authentication round-trip.
     */
    private Map<String, Object> buildStackSection() {
        Map<String, Object> stack = new LinkedHashMap<>();
        // Static stuff: app name + java version + git commit if available.
        stack.put("appName", environment.getProperty("info.app.name"));
        stack.put("appDescription", environment.getProperty("info.app.description"));
        stack.put("appEncoding", environment.getProperty("info.app.encoding"));
        // Build-time identity: which JVM compiled the JAR and to what bytecode level.
        // Frozen at package time by Maven token replacement, so these stay stable
        // across restarts on different hosts.
        stack.put("javaSource", environment.getProperty("info.app.java.source"));
        stack.put("javaVendor", environment.getProperty("info.app.java.vendor"));
        stack.put("javaTarget", environment.getProperty("info.app.java.target"));
        // Runtime identity: the JVM actually executing the app right now. Read
        // every call — cheap, and on the Pi we sometimes restart with a
        // different JRE without rebuilding.
        stack.put("javaRuntimeVersion", System.getProperty("java.version"));
        stack.put("javaRuntimeVendor", System.getProperty("java.vendor"));
        stack.put("javaRuntimeName", System.getProperty("java.runtime.name"));
        stack.put("hostname", resolveHostname());
        if (buildProperties != null) {
            stack.put("buildVersion", buildProperties.getVersion());
            stack.put("buildTime", buildProperties.getTime() != null
                    ? buildProperties.getTime().toString() : null);
        }
        // Runtime metrics from Micrometer. registry.get(name).gauge() throws
        // MeterNotFoundException when the meter is absent (e.g. some JREs don't
        // surface process.cpu.usage) — we degrade silently per metric instead
        // of failing the whole call.
        stack.put("uptimeSeconds", meterValue("process.uptime", Tags.empty()));
        stack.put("jvmHeapUsed", meterValue("jvm.memory.used", Tags.of("area", "heap")));
        stack.put("jvmHeapMax", meterValue("jvm.memory.max", Tags.of("area", "heap")));
        stack.put("jvmThreads", meterValue("jvm.threads.live", Tags.empty()));
        // http.server.requests is a Timer — its count() lives on the timer itself,
        // not on a gauge. We fall back to summing all matching timers.
        stack.put("httpRequests", timerCount("http.server.requests"));
        stack.put("processCpu", meterValue("process.cpu.usage", Tags.empty()));
        return stack;
    }

    /** Returns the current value of a gauge meter, or null if it does not exist. */
    private Double meterValue(String name, Tags tags) {
        if (meterRegistry == null) {
            return null;
        }
        try {
            Meter meter = meterRegistry.get(name).tags(tags).meter();
            // Walk measurements: take the first VALUE measurement we find.
            for (io.micrometer.core.instrument.Measurement m : meter.measure()) {
                if (m.getStatistic() == io.micrometer.core.instrument.Statistic.VALUE) {
                    return m.getValue();
                }
            }
        } catch (Exception e) {
            // MeterNotFoundException + anything else — silent miss.
        }
        return null;
    }

    /** Sums the COUNT statistic across every {@code http.server.requests} timer. */
    private Double timerCount(String name) {
        if (meterRegistry == null) {
            return null;
        }
        try {
            double total = 0.0;
            for (Meter m : meterRegistry.find(name).meters()) {
                for (io.micrometer.core.instrument.Measurement measure : m.measure()) {
                    if (measure.getStatistic() == io.micrometer.core.instrument.Statistic.COUNT) {
                        total += measure.getValue();
                    }
                }
            }
            return total;
        } catch (Exception e) {
            return null;
        }
    }

    private String resolveHostname() {
        try {
            return InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
            return null;
        }
    }
}
