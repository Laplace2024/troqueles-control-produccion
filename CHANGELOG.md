# Changelog - Trabajo Troqueles

## 2026-05-29 - Historial: Nº troquel al eliminar fila (fix registro directo)

### Objetivo
Corregir que el historial no mostraba el Nº de troquel al borrar fila (el registro dependia solo del listener del modelo).

### Cambios
- `deleteSelectedRow`: lee el Nº antes de borrar y llama a `changeLog.record` directamente.
- `readTroquelNumberForModelRow`: lectura desde modelo y tabla visible (fallback de columna `Nº`).
- `suppressNextDeleteChangeLog`: evita entrada duplicada en el listener DELETE.

### Archivos tocados
- `src/main/java/com/trabajo/troqueles/SpreadsheetFrame.java`
- `CHANGELOG.md`

### Verificacion
- Recompilar y ejecutar; borrar fila con Nº relleno; comprobar en Historial: `fila X, Nº troquel YYYY`.

## 2026-05-29 - Columna Goma: nuevas opciones en el desplegable (Nivel 1)

### Objetivo
Ampliar el combo de la columna `Goma` con los tipos de goma que usa el taller.

### Cambios
- `SpreadsheetFrame.GOMA_OPCIONES`: anadidas `Blanco + Negro`, `Roja`, `Roja + Negro`, `Roja + Amarillo` y `Plancha negra` (se mantienen Amarillo, Negro, Blanco y Rosa).

### Archivos tocados
- `src/main/java/com/trabajo/troqueles/SpreadsheetFrame.java`
- `CHANGELOG.md`

### Verificacion
- Revision manual: el editor de celda de la columna Goma lista las nuevas opciones.

## 2026-05-25 - Instalador portable generado (JDK 17 + Maven bootstrap)

### Objetivo
Permitir crear el instalador en equipos sin Maven global ni JDK 21, dejando la aplicacion lista en `dist/`.

### Dificultad del bloque
- **Nivel 4 (Muy avanzado):** empaquetado jpackage, uber-JAR y compatibilidad JDK 17.

### Cambios
- `scripts/crear-instalador.ps1`: descarga Maven portable, compila y ejecuta `jpackage.ps1`.
- `scripts/jpackage.ps1`: compatibilidad JDK 17 (sin `--app-content`), copia de `dashboard/` al paquete final.
- `pom.xml`: `maven-shade-plugin` para incluir PostgreSQL en el JAR de distribucion.
- `dist/Troqueles/`: app-image portable con `Troqueles.exe` y runtime embebido.
- `dist/LEEME-INSTALACION.txt`: instrucciones de uso.

### Archivos tocados
- `scripts/crear-instalador.ps1`
- `scripts/jpackage.ps1`
- `pom.xml`
- `dist/` (generado localmente, ignorado por git)
- `CHANGELOG.md`

### Verificacion
- `.\scripts\crear-instalador.ps1 -PackageType app-image -SkipTests`: BUILD SUCCESS, salida en `dist/Troqueles/`.

### Siguiente paso
- Probar `Troqueles.exe` en un equipo sin JDK instalado y, si se requiere `.exe` instalable, instalar WiX y regenerar con `-PackageType exe`.

## 2026-05-18 - Multiusuario (Bloque 7): sincronizacion automatica + bloqueo suave de filas

### Objetivo
Detectar cambios remotos en PostgreSQL sin depender de `Cargar BD` manual, avisar al operario y evitar ediciones simultaneas sobre la misma fila.

### Dificultad del bloque
- **Nivel 4 (Muy avanzado):** polling en segundo plano, conciliacion de versiones y bloqueos transitorios compartidos.

### Cambios
- `DbSchemaBootstrap.java`: tabla `workbook_row_locks` con expiracion automatica.
- `DbWorkbookRepository.java`:
  - consulta de version remota (`fetchSheetVersionSnapshot`),
  - adquisicion/renovacion/liberacion de bloqueos por fila,
  - listado de bloqueos activos por hoja.
- `DbAutoSyncService.java`: polling cada 45 s, aviso de cambios remotos y refresco de bloqueos ajenos.
- `SpreadsheetFrame.java`:
  - indicador `Sync BD` en pie de pagina,
  - menu `Sincronizacion automatica BD` y `Comprobar BD ahora`,
  - dialogo `Hay cambios en BD, ¿recargar?` con opciones Recargar / Ignorar / Desactivar sync,
  - filas bloqueadas resaltadas y no editables mientras otro trabajador edita.
- `DbAutoSyncServiceTest.java`: pruebas de reglas de aviso y filtrado de bloqueos.
- `README.md`: documentada sincronizacion automatica y bloqueo suave.

### Archivos tocados
- `src/main/java/com/trabajo/troqueles/DbSchemaBootstrap.java`
- `src/main/java/com/trabajo/troqueles/DbWorkbookRepository.java`
- `src/main/java/com/trabajo/troqueles/DbAutoSyncService.java`
- `src/main/java/com/trabajo/troqueles/SpreadsheetFrame.java`
- `src/test/java/com/trabajo/troqueles/DbAutoSyncServiceTest.java`
- `README.md`
- `CHANGELOG.md`

### Verificacion
- `mvn -B test`: suite en verde tras integrar sync automatico y bloqueos.

### Siguiente paso
- Bloque 6: dashboard multiusuario (endpoint de ranking y actividad reciente para supervisores).

## 2026-05-12 - Multiusuario (Bloque 5): registro simultaneo + ranking de compras

### Objetivo
Guardar cambios en BD y dejar trazabilidad simultanea, ademas de poder consultar quien compro mas/menos desde datos persistidos.

### Dificultad del bloque
- **Nivel 4 (Muy avanzado):** auditoria transaccional y analitica de clientes sobre datos centrales.

### Cambios
- `DbSchemaBootstrap.java`:
  - nueva tabla `workbook_client_stats` para ranking por hoja,
  - mantenimiento de `audit_events` como log de acciones en BD.
- `DbWorkbookRepository.java`:
  - en cada guardado (normal o forzado) registra evento en `audit_events` con trabajador,
  - recalcula `workbook_client_stats` en la misma transaccion de guardado,
  - nuevo `loadClientRanking(...)` para consulta ordenada de clientes.
- `SpreadsheetFrame.java`:
  - trabajador detectado automaticamente (`usuario@equipo`),
  - `Guardar BD` registra auditoria en BD al mismo tiempo que persiste la hoja,
  - nueva consulta `Ranking clientes BD` (menu, toolbar y menu rapido) con mayor/menor compra.
- `README.md`: documentado registro simultaneo y consulta de ranking.

### Archivos tocados
- `src/main/java/com/trabajo/troqueles/DbSchemaBootstrap.java`
- `src/main/java/com/trabajo/troqueles/DbWorkbookRepository.java`
- `src/main/java/com/trabajo/troqueles/SpreadsheetFrame.java`
- `README.md`
- `CHANGELOG.md`

### Verificacion
- `mvn -B test`: suite en verde tras integrar auditoria y ranking en BD.

### Siguiente paso
- Bloque 6: dashboard multiusuario (endpoint de ranking y actividad reciente para supervisores).

## 2026-05-12 - Multiusuario (Bloque 4): control de conflicto al guardar

### Objetivo
Evitar sobreescrituras silenciosas cuando dos trabajadores guardan la misma hoja en paralelo.

### Dificultad del bloque
- **Nivel 4 (Muy avanzado):** control de concurrencia optimista con versionado de hoja.

### Cambios
- `DbSchemaBootstrap.java`: `workbook_sheets` ahora incluye `sheet_version` con migracion idempotente.
- `DbWorkbookRepository.java`:
  - guardado optimista `saveSheetOptimistic(...)` con comprobacion de version esperada,
  - guardado forzado `saveSheetForce(...)` para escenarios de sobrescritura intencional,
  - `WorkbookData` expone version cargada y `SaveResult` informa conflictos.
- `SpreadsheetFrame.java`:
  - cache local `dbSheetVersionByName` por hoja,
  - al guardar en BD detecta conflicto y ofrece opciones: `Recargar BD`, `Sobrescribir`, `Cancelar`.
- `README.md`: añadido comportamiento de conflicto para trabajo multiusuario.

### Archivos tocados
- `src/main/java/com/trabajo/troqueles/DbSchemaBootstrap.java`
- `src/main/java/com/trabajo/troqueles/DbWorkbookRepository.java`
- `src/main/java/com/trabajo/troqueles/SpreadsheetFrame.java`
- `README.md`
- `CHANGELOG.md`

### Verificacion
- `mvn -B test`: suite en verde tras integrar versionado optimista.

### Siguiente paso
- Bloque 5: auditoria por trabajador (identidad) y panel simple de revisiones/conflictos.

## 2026-05-12 - Multiusuario (Bloque 3): guardar/cargar en PostgreSQL

### Objetivo
Permitir persistir la hoja de trabajo directamente en la base de datos central para que el equipo comparta los mismos datos por LAN.

### Dificultad del bloque
- **Nivel 3 (Avanzado):** repositorio SQL con integracion en flujos de UI existentes.

### Cambios
- `DbSchemaBootstrap.java`: nueva tabla `workbook_sheets` para cabeceras por hoja.
- `DbWorkbookRepository.java`: repositorio SQL para guardar/cargar por `sheet_name` en `workbook_sheets` + `workbook_rows`.
- `SpreadsheetFrame.java`:
  - nuevas acciones `Guardar BD` y `Cargar BD` (menu Archivo, toolbar y menu rapido de datos),
  - reutilizacion de pipeline de recarga de modelo (`applyLoadedRows`) para CSV y BD.
- `README.md`: reflejada la persistencia en PostgreSQL desde la UI.

### Archivos tocados
- `src/main/java/com/trabajo/troqueles/DbSchemaBootstrap.java`
- `src/main/java/com/trabajo/troqueles/DbWorkbookRepository.java`
- `src/main/java/com/trabajo/troqueles/SpreadsheetFrame.java`
- `README.md`
- `CHANGELOG.md`

### Verificacion
- `mvn -B test`: suite en verde tras integrar guardado/carga en BD.

### Siguiente paso
- Bloque 4: control de concurrencia (versionado por fila y conflicto de edicion) + auditoria por trabajador.

## 2026-05-12 - Multiusuario (Bloque 2): servidor LAN base

### Objetivo
Levantar un proceso servidor en red local (misma WiFi) para validar conectividad entre puestos y preparar el intercambio centralizado de datos.

### Dificultad del bloque
- **Nivel 3 (Avanzado):** servidor de red con bootstrap de base de datos y operacion por argumentos.

### Cambios
- `LanSyncServer.java`: servidor HTTP embebido con bind LAN configurable y endpoints:
  - `GET /health` para chequeo remoto desde otros equipos.
  - `POST /bootstrap` para inicializar esquema PostgreSQL de forma idempotente.
- `LanServerLauncher.java`: arranque bloqueante, lectura de argumentos/entorno y parada ordenada con shutdown hook.
- `Main.java`: nuevo modo `--lan-server` para ejecutar servidor en lugar de UI Swing.
- `scripts/lan-server.ps1`: script operativo para arrancar servidor LAN en Windows.
- `README.md`: seccion de ejecucion LAN y configuracion de `db.properties` / variables `TROQUELES_DB_*`.

### Archivos tocados
- `src/main/java/com/trabajo/troqueles/LanSyncServer.java`
- `src/main/java/com/trabajo/troqueles/LanServerLauncher.java`
- `src/main/java/com/trabajo/troqueles/Main.java`
- `scripts/lan-server.ps1`
- `README.md`
- `CHANGELOG.md`

### Verificacion
- `mvn -B test`: suite en verde.
- Arranque operativo pendiente de validacion final contra un PostgreSQL de red del taller.

### Siguiente paso
- Bloque 3: repositorio SQL para filas de trabajo y primer flujo sincronizado guardar/cargar contra PostgreSQL.

## 2026-05-12 - Multiusuario (Bloque 1): base PostgreSQL

### Objetivo
Preparar la base tecnica para modo multiusuario en red local (misma WiFi) sin alterar la UX actual de la aplicacion.

### Dificultad del bloque
- **Nivel 2 (Medio):** infraestructura inicial de configuracion y esquema de datos.

### Cambios
- `pom.xml`: dependencia `org.postgresql:postgresql` para habilitar JDBC PostgreSQL.
- `DbSettings.java`: carga de configuracion desde entorno / `~/.troqueles/db.properties` con defaults seguros.
- `DbConnections.java`: apertura de conexion JDBC centralizada.
- `DbSchemaBootstrap.java`: creacion idempotente de tablas base (`app_meta`, `workbook_rows`, `audit_events`) e indice de orden por hoja.
- `DbSettingsTest.java`: prueba unitaria minima de formato de URL JDBC.
- `docs/db.properties.example`: plantilla de configuracion para despliegue en taller.

### Archivos tocados
- `pom.xml`
- `src/main/java/com/trabajo/troqueles/DbSettings.java`
- `src/main/java/com/trabajo/troqueles/DbConnections.java`
- `src/main/java/com/trabajo/troqueles/DbSchemaBootstrap.java`
- `src/test/java/com/trabajo/troqueles/DbSettingsTest.java`
- `docs/db.properties.example`
- `CHANGELOG.md`

### Verificacion
- `mvn -B test`: suite en verde tras introducir la base PostgreSQL.

### Siguiente paso
- Bloque 2: levantar un proceso servidor LAN y conectar guardar/cargar contra repositorio SQL manteniendo CSV como import/export.

## 2026-05-12 - Instalador nativo con jpackage

### Objetivo
Permitir generar un instalador o imagen portable de la aplicacion de escritorio sin pasos manuales de copia de JAR y recursos.

### Cambios
- `pom.xml`: `finalName` fijo (`troqueles-app`) y `maven-jar-plugin` con `Main-Class` en el manifest.
- `scripts/jpackage.ps1`: compila con Maven, prepara el JAR y `dashboard` en un directorio temporal sin espacios y ejecuta `jpackage` con `--app-content`; copia el resultado a `dist/`.
- `.gitignore`: exclusion de `dist/` y carpetas de staging de empaquetado.
- `README.md`: seccion de instalador nativo y actualizacion del roadmap.

### Archivos tocados
- `pom.xml`
- `scripts/jpackage.ps1`
- `.gitignore`
- `README.md`
- `CHANGELOG.md`

### Verificacion
- `mvn -B test` y `mvn -B package`: JAR ejecutable en `target/troqueles-app.jar`.
- `.\scripts\jpackage.ps1 -SkipTests`: salida en `dist/` (tipo segun WiX disponible).

### Siguiente paso
- Probar el instalador o la carpeta portable en un equipo limpio y validar el dashboard web embebido.

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

