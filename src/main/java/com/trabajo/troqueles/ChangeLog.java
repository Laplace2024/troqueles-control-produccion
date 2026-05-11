package com.trabajo.troqueles;

import java.io.BufferedWriter;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.List;

/**
 * Historial de cambios de la aplicacion. Mantiene una lista en memoria con limite y
 * persiste cada nueva entrada en un fichero (anexado por linea) para que sobreviva al cierre.
 *
 * Formato de cada linea persistida (TSV):
 *   ISO-8601 timestamp \t accion \t detalle
 *
 * El detalle puede contener espacios y signos de puntuacion; los tabuladores se reemplazan por espacios.
 */
public class ChangeLog {

    private static final DateTimeFormatter DISPLAY_FORMAT = DateTimeFormatter
        .ofPattern("yyyy-MM-dd HH:mm:ss")
        .withZone(ZoneId.systemDefault());

    private final File storageFile;
    private final int maxEntriesInMemory;
    private final List<Entry> entries = new ArrayList<Entry>();

    public ChangeLog(File storageFile, int maxEntriesInMemory) {
        this.storageFile = storageFile;
        this.maxEntriesInMemory = Math.max(50, maxEntriesInMemory);
        loadFromDisk();
    }

    /** Registra una entrada con accion y detalle (puede ser null o vacio). */
    public synchronized void record(String accion, String detalle) {
        Entry entry = new Entry(Instant.now(), safe(accion), safe(detalle));
        entries.add(entry);
        trimIfNeeded();
        appendToDisk(entry);
    }

    /** Atajo para registrar solo una accion sin detalle. */
    public void record(String accion) {
        record(accion, null);
    }

    public synchronized List<Entry> getEntries() {
        return Collections.unmodifiableList(new ArrayList<Entry>(entries));
    }

    public synchronized boolean isEmpty() {
        return entries.isEmpty();
    }

    public synchronized void clear() {
        entries.clear();
        if (storageFile != null) {
            try {
                if (storageFile.exists()) {
                    boolean ok = storageFile.delete();
                    if (!ok) {
                        // Si no se puede borrar, lo vaciamos.
                        try (BufferedWriter w = new BufferedWriter(new FileWriter(storageFile, false))) {
                            w.write("");
                        }
                    }
                }
            } catch (IOException ignored) {
                // No bloqueamos la limpieza de memoria por un error de E/S.
            }
        }
    }

    public static String formatTimestamp(Instant instant) {
        return DISPLAY_FORMAT.format(instant);
    }

    private void trimIfNeeded() {
        int excess = entries.size() - maxEntriesInMemory;
        if (excess > 0) {
            entries.subList(0, excess).clear();
        }
    }

    private void appendToDisk(Entry entry) {
        if (storageFile == null) {
            return;
        }
        try {
            File parent = storageFile.getParentFile();
            if (parent != null && !parent.exists()) {
                parent.mkdirs();
            }
            try (BufferedWriter w = Files.newBufferedWriter(
                storageFile.toPath(),
                StandardCharsets.UTF_8,
                StandardOpenOption.CREATE,
                StandardOpenOption.APPEND
            )) {
                w.write(entry.timestamp.toString());
                w.write('\t');
                w.write(escape(entry.accion));
                w.write('\t');
                w.write(escape(entry.detalle));
                w.newLine();
            }
        } catch (IOException ignored) {
            // No interrumpimos al usuario por un fallo de log.
        }
    }

    private void loadFromDisk() {
        if (storageFile == null || !storageFile.exists()) {
            return;
        }
        try {
            Deque<String> lastLines = new ArrayDeque<String>(maxEntriesInMemory);
            try (BufferedReader reader = Files.newBufferedReader(Path.of(storageFile.toURI()), StandardCharsets.UTF_8)) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.isEmpty()) {
                        continue;
                    }
                    if (lastLines.size() == maxEntriesInMemory) {
                        lastLines.removeFirst();
                    }
                    lastLines.addLast(line);
                }
            }
            for (String line : lastLines) {
                if (line == null || line.isEmpty()) {
                    continue;
                }
                String[] parts = line.split("\\t", -1);
                if (parts.length < 2) {
                    continue;
                }
                Instant ts;
                try {
                    ts = Instant.parse(parts[0]);
                } catch (Exception ex) {
                    continue;
                }
                String accion = parts.length > 1 ? parts[1] : "";
                String detalle = parts.length > 2 ? parts[2] : "";
                entries.add(new Entry(ts, accion, detalle));
            }
        } catch (IOException ignored) {
            // No bloqueamos el arranque por un fichero corrupto.
        }
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }

    private static String escape(String value) {
        return value == null ? "" : value.replace('\t', ' ').replace('\n', ' ').replace('\r', ' ');
    }

    /** Entrada inmutable del historial. */
    public static final class Entry {
        private final Instant timestamp;
        private final String accion;
        private final String detalle;

        public Entry(Instant timestamp, String accion, String detalle) {
            this.timestamp = timestamp;
            this.accion = accion == null ? "" : accion;
            this.detalle = detalle == null ? "" : detalle;
        }

        public Instant getTimestamp() {
            return timestamp;
        }

        public String getAccion() {
            return accion;
        }

        public String getDetalle() {
            return detalle;
        }
    }
}
