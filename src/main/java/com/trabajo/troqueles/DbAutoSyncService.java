package com.trabajo.troqueles;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Sincronizacion automatica con PostgreSQL: polling de version de hoja y bloqueos suaves por fila.
 */
final class DbAutoSyncService {
    private static final Logger LOGGER = Logger.getLogger(DbAutoSyncService.class.getName());
    static final long DEFAULT_POLL_INTERVAL_SECONDS = 45L;
    static final long DEFAULT_ROW_LOCK_TTL_SECONDS = 90L;
    private static final DateTimeFormatter STATUS_TIME = DateTimeFormatter.ofPattern("HH:mm:ss");

    interface Callbacks {
        void onRemoteVersionNewer(long remoteVersion, long localVersion);

        void onStatusUpdate(String status);

        void onForeignRowLocksChanged(Map<Integer, String> locksByOtherWorkers);

        void onSyncError(String message);
    }

    private final DbWorkbookRepository repository;
    private final String workerName;
    private final long pollIntervalSeconds;
    private final long rowLockTtlSeconds;
    private final ScheduledExecutorService scheduler;

    private volatile boolean enabled = true;
    private volatile String sheetName = "";
    private volatile long localVersion;
    private volatile long lastNotifiedRemoteVersion = -1L;
    private volatile int locallyEditingRow = -1;
    private volatile Callbacks callbacks;

    private ScheduledFuture<?> pollFuture;

    DbAutoSyncService(DbWorkbookRepository repository, String workerName) {
        this(repository, workerName, DEFAULT_POLL_INTERVAL_SECONDS, DEFAULT_ROW_LOCK_TTL_SECONDS);
    }

    DbAutoSyncService(
        DbWorkbookRepository repository,
        String workerName,
        long pollIntervalSeconds,
        long rowLockTtlSeconds
    ) {
        this.repository = repository;
        this.workerName = workerName;
        this.pollIntervalSeconds = pollIntervalSeconds;
        this.rowLockTtlSeconds = rowLockTtlSeconds;
        this.scheduler = Executors.newSingleThreadScheduledExecutor(runnable -> {
            Thread thread = new Thread(runnable, "troqueles-db-autosync");
            thread.setDaemon(true);
            return thread;
        });
    }

    void setCallbacks(Callbacks callbacks) {
        this.callbacks = callbacks;
    }

    void start(String initialSheetName, long initialLocalVersion) {
        this.sheetName = initialSheetName == null ? "" : initialSheetName.trim();
        this.localVersion = initialLocalVersion;
        this.lastNotifiedRemoteVersion = -1L;
        if (pollFuture != null) {
            pollFuture.cancel(false);
        }
        pollFuture = scheduler.scheduleAtFixedRate(
            this::pollSafely,
            pollIntervalSeconds,
            pollIntervalSeconds,
            TimeUnit.SECONDS
        );
        notifyStatus("Sync BD activo (cada " + pollIntervalSeconds + " s)");
    }

    void stop() {
        if (pollFuture != null) {
            pollFuture.cancel(false);
            pollFuture = null;
        }
        releaseLocalRowLockQuietly();
        releaseAllLocksQuietly();
        scheduler.shutdownNow();
    }

    void setEnabled(boolean enabled) {
        this.enabled = enabled;
        notifyStatus(enabled ? "Sync BD activado" : "Sync BD desactivado");
    }

    boolean isEnabled() {
        return enabled;
    }

    void updateSheetName(String sheetName) {
        this.sheetName = sheetName == null ? "" : sheetName.trim();
        this.lastNotifiedRemoteVersion = -1L;
    }

    void updateLocalVersion(long version) {
        this.localVersion = version;
        this.lastNotifiedRemoteVersion = -1L;
    }

    void acknowledgeRemoteVersion(long remoteVersion) {
        lastNotifiedRemoteVersion = remoteVersion;
    }

    boolean beginLocalRowEdit(int modelRow) {
        if (modelRow < 0 || sheetName.isEmpty()) {
            return true;
        }
        try {
            DbWorkbookRepository.RowLockAcquireResult result = repository.tryAcquireRowLock(
                sheetName,
                modelRow,
                workerName,
                rowLockTtlSeconds
            );
            if (result == DbWorkbookRepository.RowLockAcquireResult.BLOCKED) {
                String holder = repository.findRowLockHolder(sheetName, modelRow);
                notifyError("Fila " + (modelRow + 1) + " bloqueada por " + formatHolder(holder));
                refreshForeignLocksQuietly();
                return false;
            }
            locallyEditingRow = modelRow;
            return true;
        } catch (SQLException ex) {
            LOGGER.log(Level.FINE, "No se pudo adquirir bloqueo de fila", ex);
            return true;
        }
    }

    void endLocalRowEdit() {
        releaseLocalRowLockQuietly();
    }

    void pollNow() {
        pollSafely();
    }

    static boolean shouldNotifyRemoteChange(long localVersion, long remoteVersion, long lastNotifiedRemoteVersion) {
        if (remoteVersion <= localVersion) {
            return false;
        }
        return remoteVersion != lastNotifiedRemoteVersion;
    }

    static Map<Integer, String> filterForeignLocks(Map<Integer, String> allLocks, String workerName) {
        if (allLocks == null || allLocks.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<Integer, String> foreign = new java.util.HashMap<Integer, String>();
        String normalizedWorker = workerName == null ? "" : workerName.trim();
        for (Map.Entry<Integer, String> entry : allLocks.entrySet()) {
            String holder = entry.getValue() == null ? "" : entry.getValue().trim();
            if (!holder.isEmpty() && !holder.equals(normalizedWorker)) {
                foreign.put(entry.getKey(), holder);
            }
        }
        return foreign;
    }

    private void pollSafely() {
        if (!enabled || callbacks == null || sheetName.isEmpty()) {
            return;
        }
        try {
            DbWorkbookRepository.SheetVersionSnapshot snapshot = repository.fetchSheetVersionSnapshot(sheetName);
            if (snapshot.exists()) {
                long remoteVersion = snapshot.version();
                if (shouldNotifyRemoteChange(localVersion, remoteVersion, lastNotifiedRemoteVersion)) {
                    callbacks.onRemoteVersionNewer(remoteVersion, localVersion);
                }
                notifyStatus("Sync OK · BD v" + remoteVersion + " · " + STATUS_TIME.format(LocalDateTime.now()));
            } else {
                notifyStatus("Sync OK · sin datos en BD · " + STATUS_TIME.format(LocalDateTime.now()));
            }

            if (locallyEditingRow >= 0) {
                repository.tryAcquireRowLock(sheetName, locallyEditingRow, workerName, rowLockTtlSeconds);
            }

            Map<Integer, String> foreignLocks = filterForeignLocks(
                repository.loadActiveRowLocks(sheetName),
                workerName
            );
            callbacks.onForeignRowLocksChanged(foreignLocks);
        } catch (SQLException ex) {
            LOGGER.log(Level.FINE, "Error en polling de sincronizacion BD", ex);
            notifyError("Sync BD: " + ex.getMessage());
        }
    }

    private void refreshForeignLocksQuietly() {
        if (callbacks == null || sheetName.isEmpty()) {
            return;
        }
        try {
            callbacks.onForeignRowLocksChanged(
                filterForeignLocks(repository.loadActiveRowLocks(sheetName), workerName)
            );
        } catch (SQLException ex) {
            LOGGER.log(Level.FINE, "No se pudieron refrescar bloqueos remotos", ex);
        }
    }

    private void releaseLocalRowLockQuietly() {
        int row = locallyEditingRow;
        locallyEditingRow = -1;
        if (row < 0 || sheetName.isEmpty()) {
            return;
        }
        try {
            repository.releaseRowLock(sheetName, row, workerName);
        } catch (SQLException ex) {
            LOGGER.log(Level.FINE, "No se pudo liberar bloqueo local de fila", ex);
        }
    }

    private void releaseAllLocksQuietly() {
        if (sheetName.isEmpty()) {
            return;
        }
        try {
            repository.releaseAllRowLocksForWorker(sheetName, workerName);
        } catch (SQLException ex) {
            LOGGER.log(Level.FINE, "No se pudieron liberar bloqueos del trabajador", ex);
        }
    }

    private void notifyStatus(String status) {
        if (callbacks != null) {
            callbacks.onStatusUpdate(status);
        }
    }

    private void notifyError(String message) {
        if (callbacks != null) {
            callbacks.onSyncError(message);
        }
    }

    private static String formatHolder(String holder) {
        if (holder == null || holder.trim().isEmpty()) {
            return "otro trabajador";
        }
        return holder.trim();
    }
}
