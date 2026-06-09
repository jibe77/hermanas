package org.jibe77.hermanas.data.migration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.sql.DatabaseMetaData;
import java.sql.ResultSet;

/**
 * Idempotent one-shot migration: widens {@code parameter.entry_value} from the
 * default {@code VARCHAR(255)} to {@code TEXT}.
 *
 * <p>Why this exists: Hibernate's {@code ddl-auto=update} <em>creates</em>
 * missing tables and columns but <em>never</em> alters an existing column.
 * When we widened the JPA field annotation to {@code TEXT}, the live schema
 * stayed on the old narrow type and any prompt above ~1.5 kB still hit a
 * {@code "Data too long for column 'entry_value'"} error at INSERT/UPDATE.
 * This runner closes the gap on the next startup, then becomes a no-op.</p>
 *
 * <p>Runs on {@link ApplicationReadyEvent} rather than {@code @PostConstruct}
 * to guarantee the DataSource and connection pool are fully initialised.</p>
 */
@Component
public class ParameterColumnMigration {

    private static final Logger logger = LoggerFactory.getLogger(ParameterColumnMigration.class);

    private final JdbcTemplate jdbcTemplate;

    public ParameterColumnMigration(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void widenEntryValueIfNeeded() {
        String currentType = readColumnType();
        if (currentType == null) {
            // Either the table does not exist yet (fresh install — Hibernate
            // will create it with the right TEXT type from the @Column on the
            // entity) or we are on H2 in tests (where SYS.COLUMNS lookup with
            // DatabaseMetaData behaves slightly differently). Either way,
            // nothing to do.
            logger.debug("parameter.entry_value column not found; skipping migration.");
            return;
        }
        if (currentType.equalsIgnoreCase("TEXT")
                || currentType.equalsIgnoreCase("LONGTEXT")
                || currentType.equalsIgnoreCase("MEDIUMTEXT")
                || currentType.equalsIgnoreCase("CLOB")) {
            logger.debug("parameter.entry_value already {}, no migration needed.", currentType);
            return;
        }
        logger.info("Migrating parameter.entry_value from {} to TEXT (Hibernate ddl-auto cannot widen columns).",
                currentType);
        try {
            jdbcTemplate.execute("ALTER TABLE parameter MODIFY entry_value TEXT");
            logger.info("parameter.entry_value migrated to TEXT.");
        } catch (Exception e) {
            // Don't crash the app: surface the issue but let the rest of the
            // system come up. The next startup will retry.
            logger.error("Failed to widen parameter.entry_value to TEXT — saves of long prompts will still fail.", e);
        }
    }

    /**
     * Reads the SQL type currently advertised by the JDBC metadata for
     * {@code parameter.entry_value}. Returns {@code null} when the column
     * cannot be located.
     */
    private String readColumnType() {
        try {
            return jdbcTemplate.execute((java.sql.Connection conn) -> {
                DatabaseMetaData md = conn.getMetaData();
                // Try both the schema-less lookup and one scoped to the catalog,
                // since the behaviour varies between MariaDB and H2.
                try (ResultSet rs = md.getColumns(conn.getCatalog(), null, "parameter", "entry_value")) {
                    if (rs.next()) {
                        return rs.getString("TYPE_NAME");
                    }
                }
                try (ResultSet rs = md.getColumns(conn.getCatalog(), null, "PARAMETER", "ENTRY_VALUE")) {
                    if (rs.next()) {
                        return rs.getString("TYPE_NAME");
                    }
                }
                return null;
            });
        } catch (Exception e) {
            logger.warn("Could not read column metadata for parameter.entry_value: {}", e.getMessage());
            return null;
        }
    }
}
