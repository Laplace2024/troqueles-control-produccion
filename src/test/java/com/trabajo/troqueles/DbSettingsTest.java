package com.trabajo.troqueles;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class DbSettingsTest {
    @Test
    void construyeJdbcUrlConParametrosEsperados() {
        DbSettings settings = DbSettings.loadDefault();
        String url = settings.jdbcUrl();
        assertTrue(url.startsWith("jdbc:postgresql://"));
        assertTrue(url.contains("ApplicationName=troqueles-app"));
        assertTrue(url.contains("sslmode="));
    }
}

