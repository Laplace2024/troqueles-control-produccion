package com.trabajo.troqueles;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Servicio de busqueda/autocompletado de clientes.
 *
 * <p>Lee opcionalmente catalogos CSV locales (no versionados) y ofrece:</p>
 * <ul>
 *   <li>busqueda de codigo por nombre</li>
 *   <li>busqueda de nombre por codigo</li>
 *   <li>sugerencias por prefijo para nombre y codigo</li>
 * </ul>
 */
final class ClientLookup {
    private static final Logger LOGGER = Logger.getLogger(ClientLookup.class.getName());
    private final Map<String, String> codeByName;
    private final Map<String, String> nameByCode;
    private final Map<String, String> canonicalNameByNormalized;
    private final NavigableMap<String, String> canonicalNameSorted;
    private final NavigableMap<String, String> nameByCodeSorted;

    private ClientLookup(ClientData data) {
        this.codeByName = data.byName;
        this.nameByCode = data.byCode;
        this.canonicalNameByNormalized = data.canonicalNameByNormalized;
        this.canonicalNameSorted = new TreeMap<String, String>(canonicalNameByNormalized);
        this.nameByCodeSorted = new TreeMap<String, String>(nameByCode);
    }

    static ClientLookup fromDefaultFiles() {
        ClientData data = new ClientData();
        loadClientCodeMapFromCsv(data, new File("clientes_codigos.csv"));
        if (data.byName.isEmpty()) {
            loadClientCodeMapFromCsv(data, new File("clientes_import.csv"));
        }
        LOGGER.log(Level.FINE, "Catalogo de clientes cargado: {0} entradas", data.byName.size());
        return new ClientLookup(data);
    }

    String findClientCodeByName(String rawName) {
        String normalized = normalizeClientNameForLookup(rawName);
        if (normalized.isEmpty()) {
            return null;
        }
        String code = codeByName.get(normalized);
        if (code != null) {
            return code;
        }

        // Fallback: si el usuario pega nombre largo con direccion, intenta por prefijo.
        for (Map.Entry<String, String> entry : codeByName.entrySet()) {
            String key = entry.getKey();
            if (normalized.startsWith(key + " ") || normalized.equals(key)) {
                return entry.getValue();
            }
        }
        return null;
    }

    String findCanonicalClientNameByName(String rawName) {
        String normalized = normalizeClientNameForLookup(rawName);
        if (normalized.isEmpty()) {
            return null;
        }
        String canonical = canonicalNameByNormalized.get(normalized);
        if (canonical != null) {
            return canonical;
        }
        for (Map.Entry<String, String> entry : canonicalNameByNormalized.entrySet()) {
            String key = entry.getKey();
            if (normalized.startsWith(key + " ") || normalized.equals(key)) {
                return entry.getValue();
            }
        }
        return null;
    }

    String suggestClientByName(String rawName) {
        String normalized = normalizeClientNameForLookup(rawName);
        if (normalized.isEmpty()) {
            return null;
        }
        Map.Entry<String, String> ceiling = canonicalNameSorted.ceilingEntry(normalized);
        if (ceiling != null && ceiling.getKey().startsWith(normalized)) {
            return ceiling.getValue();
        }
        return null;
    }

    String suggestClientByCode(String rawCode) {
        String normalizedCode = normalizeClientCode(rawCode);
        if (normalizedCode.isEmpty()) {
            return null;
        }
        Map.Entry<String, String> ceiling = nameByCodeSorted.ceilingEntry(normalizedCode);
        if (ceiling == null || !ceiling.getKey().startsWith(normalizedCode)) {
            return null;
        }
        return ceiling.getKey() + " - " + ceiling.getValue();
    }

    String findClientNameByCode(String rawCode) {
        String normalized = normalizeClientCode(rawCode);
        if (normalized.isEmpty()) {
            return null;
        }
        return nameByCode.get(normalized);
    }

    static String normalizeClientCode(String raw) {
        if (raw == null) {
            return "";
        }
        String code = raw.trim();
        if (code.startsWith("\uFEFF")) {
            code = code.substring(1).trim();
        }
        code = code.replaceAll("[^0-9]", "");
        if (code.isEmpty()) {
            return "";
        }
        code = code.replaceFirst("^0+(?!$)", "");
        return code;
    }

    static String normalizeClientNameForLookup(String raw) {
        if (raw == null) {
            return "";
        }
        String value = raw.trim().toLowerCase(Locale.ROOT);
        if (value.isEmpty()) {
            return "";
        }
        value = Normalizer.normalize(value, Normalizer.Form.NFD).replaceAll("\\p{M}+", "");
        value = value.replace('&', ' ');
        value = value.replaceAll("[^a-z0-9 ]+", " ");
        value = value.replaceAll("\\s+", " ").trim();

        // Elimina sufijos legales comunes al final (S.L., S.A., C.B., etc.).
        String legalSuffixRegex =
            "(?:"
                + "s l l|s l u|s l v|s l p|s l|sl|slu|sll|slv|slp|"
                + "sociedad limitada|sociedad anonima|"
                + "s a l|s a u|s a|sa|sau|"
                + "s c a|s c o o p and|s coop and|s coop andaluza|s c|sc|"
                + "c b|cb"
            + ")$";
        String previous;
        do {
            previous = value;
            value = value.replaceAll("\\s+" + legalSuffixRegex, "").trim();
        } while (!value.equals(previous));

        return value;
    }

    private static void loadClientCodeMapFromCsv(ClientData data, File csvFile) {
        if (csvFile == null || !csvFile.exists() || !csvFile.isFile()) {
            return;
        }
        Charset[] charsets = new Charset[]{StandardCharsets.UTF_8, Charset.forName("windows-1252")};
        for (Charset charset : charsets) {
            data.byName.clear();
            data.byCode.clear();
            data.canonicalNameByNormalized.clear();
            boolean replacementDetected = false;
            try (BufferedReader reader = new BufferedReader(new FileReader(csvFile, charset))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    String[] values = parseClientCodeLine(line);
                    if (values.length < 2) {
                        continue;
                    }
                    String code = values[0] == null ? "" : values[0].trim();
                    String name = cleanClientDisplayName(values[1]);
                    if (name.indexOf('\uFFFD') >= 0) {
                        replacementDetected = true;
                    }
                    String normalizedCode = normalizeClientCode(code);
                    if (normalizedCode.isEmpty() || name.isEmpty()) {
                        continue;
                    }
                    registerClientCode(data, normalizedCode, name);
                }
                if (!replacementDetected && !data.byName.isEmpty()) {
                    LOGGER.log(
                        Level.FINE,
                        "CSV de clientes cargado desde {0} con charset {1}: {2} entradas",
                        new Object[]{csvFile.getAbsolutePath(), charset.name(), data.byName.size()}
                    );
                    return;
                }
            } catch (IOException ignored) {
                LOGGER.log(
                    Level.FINE,
                    "No se pudo leer CSV de clientes con charset {0}: {1}",
                    new Object[]{charset.name(), csvFile.getAbsolutePath()}
                );
            }
        }
        if (!data.byName.isEmpty()) {
            LOGGER.log(
                Level.WARNING,
                "CSV de clientes cargado con caracteres de reemplazo desde {0}. Revisar codificacion del archivo.",
                csvFile.getAbsolutePath()
            );
        }
    }

    private static String[] parseClientCodeLine(String line) {
        if (line == null) {
            return new String[0];
        }
        String trimmed = line.trim();
        if (trimmed.isEmpty()) {
            return new String[0];
        }
        int semicolon = trimmed.indexOf(';');
        if (semicolon > 0) {
            String code = trimmed.substring(0, semicolon).trim();
            String name = trimmed.substring(semicolon + 1).trim();
            return new String[]{code, name};
        }
        return CsvUtils.parseCsvLine(trimmed);
    }

    private static void registerClientCode(ClientData data, String code, String name) {
        String cleanName = cleanClientDisplayName(name);
        String normalized = normalizeClientNameForLookup(cleanName);
        if (!normalized.isEmpty()) {
            data.byName.put(normalized, code);
            data.canonicalNameByNormalized.put(normalized, cleanName);
        }
        String normalizedCode = normalizeClientCode(code);
        if (!normalizedCode.isEmpty() && !cleanName.isEmpty()) {
            data.byCode.put(normalizedCode, cleanName);
        }
    }

    private static String cleanClientDisplayName(String raw) {
        if (raw == null) {
            return "";
        }
        String text = raw.trim();
        if (text.startsWith("\uFEFF")) {
            text = text.substring(1).trim();
        }
        text = text.replace('\u00A0', ' ');
        text = text.replaceAll("^[^\\p{L}\\p{N}]+", "");
        text = text.replaceAll("\\s+", " ").trim();
        return text;
    }

    private static final class ClientData {
        private final Map<String, String> byName = new HashMap<String, String>();
        private final Map<String, String> byCode = new HashMap<String, String>();
        private final Map<String, String> canonicalNameByNormalized = new HashMap<String, String>();
    }
}
