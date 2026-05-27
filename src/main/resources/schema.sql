-- Spring Security "remember-me" persistent tokens table.
-- Schema is dictated by org.springframework.security.web.authentication.rememberme
-- .JdbcTokenRepositoryImpl#CREATE_TABLE_SQL; we recreate it here with IF NOT EXISTS so the
-- statement is idempotent and survives application restarts.
CREATE TABLE IF NOT EXISTS persistent_logins (
    username  VARCHAR(64) NOT NULL,
    series    VARCHAR(64) PRIMARY KEY,
    token     VARCHAR(64) NOT NULL,
    last_used TIMESTAMP   NOT NULL
);
