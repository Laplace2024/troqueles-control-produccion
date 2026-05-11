# Changelog - Trabajo Troqueles

## 2026-05-11 - Refactor por fases + hardening de tests y logging

### Objetivo
Reducir acoplamiento de `SpreadsheetFrame` sin cambiar el comportamiento funcional visible, reforzar cobertura de pruebas en rutas críticas (CSV/PDF/fórmulas) y añadir trazas técnicas con `java.util.logging`.

### Cambios por bloque
- **Fase 1 - ClientLookup:** extraída la lógica de autocompletado/normalización de clientes a `ClientLookup.java` (carga de CSV, sugerencias por prefijo y búsquedas por código/nombre). `SpreadsheetFrame` conserva wrappers para compatibilidad de tests por reflexión.
- **Fase 1 - SearchAndFilter:** extraída la lógica de búsqueda y filtrado a `SearchAndFilter.java` (filtro combinado, ayuda y wiring de listeners).
- **Fase 1 - ImageColumn:** extraído soporte de miniaturas y sizing de filas/columnas a `ImageColumnSupport.java` (cache, ajuste de altura, eventos de edición de celda imagen).
- **Fase 1 - Logging técnico:** incorporado `java.util.logging` en `ClientLookup`, `SearchAndFilter`, `ImageColumnSupport`, `SpreadsheetCsv`, `PdfReportWriter` y `DashboardServer` para diagnóstico de I/O y errores internos sin alterar los mensajes de UI.
- **Fase 2 - Actions compartidas toolbar/atajos:** añadido `ToolbarActionRegistry.java` para registrar acciones comunes usadas por botones y atajos.
- **Fase 2 - Atajos desacoplados:** extraído instalador de atajos a `ShortcutBindingsInstaller.java`, consumiendo las acciones compartidas del registro.

### Tests añadidos / hardening
- `SpreadsheetCsvRoundtripTest`: roundtrip exportar/importar CSV con comillas, comas y booleanos (`terminado/no terminado`).
- `PdfReportWriterTest`: validación ligera de texto en PDF generado (`%PDF`, cabeceras y prefijos `X = ...`, `Y = ...`).
- `SpreadsheetFormulaParserTest`: cobertura de `SUMAR`, `CONTAR` con rango, división por cero y función no soportada en parser de fórmulas.
- `ShortcutBindingsInstallerTest`: validación de paridad entre bindings de teclado y acciones registradas.

### Archivos principales tocados
- `src/main/java/com/trabajo/troqueles/SpreadsheetFrame.java`
- `src/main/java/com/trabajo/troqueles/ClientLookup.java`
- `src/main/java/com/trabajo/troqueles/SearchAndFilter.java`
- `src/main/java/com/trabajo/troqueles/ImageColumnSupport.java`
- `src/main/java/com/trabajo/troqueles/ToolbarActionRegistry.java`
- `src/main/java/com/trabajo/troqueles/ShortcutBindingsInstaller.java`
- `src/main/java/com/trabajo/troqueles/SpreadsheetCsv.java`
- `src/main/java/com/trabajo/troqueles/PdfReportWriter.java`
- `src/main/java/com/trabajo/troqueles/DashboardServer.java`
- `src/test/java/com/trabajo/troqueles/SpreadsheetCsvRoundtripTest.java`
- `src/test/java/com/trabajo/troqueles/PdfReportWriterTest.java`
- `src/test/java/com/trabajo/troqueles/SpreadsheetFormulaParserTest.java`
- `src/test/java/com/trabajo/troqueles/ShortcutBindingsInstallerTest.java`

### Verificación
- `mvn -B test`: **17 tests OK**, `BUILD SUCCESS`.
- Sin regresión en flujos ya cubiertos: normalización cliente, historial, reportes HTML/PDF y esquema.

## 2026-05-11 - Mejoras de UI, exportacion y revision de bugs

### Cambios funcionales
- Columna `Imagen` con miniatura real en la celda (escalada y cacheada). Editar el path desde la celda ajusta automaticamente la altura de la fila; si se vacia, la fila vuelve a su altura por defecto.
- Reporte PDF: cada fila etiqueta las medidas con su nombre (por ejemplo `X = 30`, `Y = 40`) para que sea evidente que cifra corresponde a cada eje.
- Suma automatica de +5 mm en las columnas X / Y al introducir o corregir un valor, util para anadir el margen estandar sobre la medida real del troquel.
- Reset de tabla mantiene filas y columnas y solo vacia el contenido; no borra estructura.
- Desplegable de `Tamaño` (Corte / Hendido) sin la opcion vacia, solo `2P` y `3P`.

### Limpieza tecnica
- `DashboardSnapshot` adaptado al modelo real de la hoja (Cliente + Nombre, X y Madera). Antes consultaba columnas que ya no existian.
- `SpreadsheetCsv`: si el CSV no trae cabecera, se generan nombres neutros `Columna 1..N` en lugar de inyectar columnas obsoletas.
- Validacion numerica de celda movida a las columnas reales X / Y.
- `cambios.log` se persiste en `~/.troqueles/cambios.log` para que el historial sobreviva a accesos directos lanzados desde rutas distintas.
- Eliminado codigo muerto (`SpreadsheetStats.categorySums`, constantes obsoletas en `SpreadsheetSchema`) y tests reescritos sobre el esquema actual sin datos reales de clientes.

### Verificacion
- `mvn test`: 10 tests OK, BUILD SUCCESS.
- `javac -Xlint:all` sin warnings.

## 2026-05-11 - Preparacion para publicacion en GitHub

### Objetivo
Publicar el proyecto con contenido tecnico y reutilizable, sin datos operativos privados.

### Cambios
- Limpieza de referencias de datos sensibles en codigo y documentacion publica.
- Actualizacion de `README.md` con descripcion, funcionalidades y pasos de ejecucion.
- Ajustes de estructura para subida a repositorio publico.

### Verificacion
- Compilacion local correcta.
- Revision manual de contenido tecnico a publicar.

