package com.trabajo.troqueles;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

class DbAutoSyncServiceTest {

    @Test
    void shouldNotifyRemoteChangeSoloCuandoRemotoEsMasNuevo() {
        assertTrue(DbAutoSyncService.shouldNotifyRemoteChange(2L, 5L, -1L));
        assertFalse(DbAutoSyncService.shouldNotifyRemoteChange(5L, 5L, -1L));
        assertFalse(DbAutoSyncService.shouldNotifyRemoteChange(6L, 5L, -1L));
    }

    @Test
    void shouldNotifyRemoteChangeNoRepiteMismaVersionYaAvisada() {
        assertFalse(DbAutoSyncService.shouldNotifyRemoteChange(2L, 5L, 5L));
        assertTrue(DbAutoSyncService.shouldNotifyRemoteChange(2L, 6L, 5L));
    }

    @Test
    void filterForeignLocksExcluyeTrabajadorLocal() {
        Map<Integer, String> all = new HashMap<Integer, String>();
        all.put(Integer.valueOf(0), "ana@pc1");
        all.put(Integer.valueOf(3), "bob@pc2");
        all.put(Integer.valueOf(7), "ana@pc1");

        Map<Integer, String> foreign = DbAutoSyncService.filterForeignLocks(all, "ana@pc1");

        assertEquals(1, foreign.size());
        assertEquals("bob@pc2", foreign.get(Integer.valueOf(3)));
    }

    @Test
    void filterForeignLocksIgnoraEntradasVacias() {
        Map<Integer, String> all = new HashMap<Integer, String>();
        all.put(Integer.valueOf(1), " ");
        all.put(Integer.valueOf(2), "luis@pc3");

        Map<Integer, String> foreign = DbAutoSyncService.filterForeignLocks(all, "ana@pc1");

        assertEquals(1, foreign.size());
        assertEquals("luis@pc3", foreign.get(Integer.valueOf(2)));
    }
}
