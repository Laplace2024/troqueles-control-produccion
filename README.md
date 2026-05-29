# Troqueles - Control de produccion

![Java](https://img.shields.io/badge/Java-21-007396?logo=openjdk&logoColor=white)
![Maven](https://img.shields.io/badge/build-Maven-C71A36?logo=apachemaven&logoColor=white)
![Swing](https://img.shields.io/badge/UI-Swing-4DABCF)
![Tests](https://img.shields.io/badge/tests-JUnit%205-25A162?logo=junit5&logoColor=white)
![Platform](https://img.shields.io/badge/platform-Windows%20%7C%20Linux%20%7C%20macOS-lightgrey)

Aplicacion de escritorio en Java/Swing para llevar el control diario de produccion en un taller de troqueles: hoja editable estilo Excel con autocompletado de clientes, validaciones, miniaturas de imagen por fila, exportacion a CSV / HTML / PDF y un dashboard web local sobre los datos visibles.

> Diseñada para uso interno de taller. El repositorio publico contiene solo el codigo y la documentacion tecnica; los catalogos reales de clientes y los registros de actividad se cargan desde ficheros locales que no forman parte del codigo fuente.

## Tabla de contenido

- [Capturas](#capturas)
- [Caracteristicas](#caracteristicas)
- [Stack tecnico](#stack-tecnico)
- [Requisitos](#requisitos)
- [Como ejecutar](#como-ejecutar)
- [Instalador nativo (jpackage)](#instalador-nativo-jpackage)
- [Estructura del proyecto](#estructura-del-proyecto)
- [Tests](#tests)
- [Datos privados](#datos-privados)
- [Roadmap](#roadmap)
- [Licencia](#licencia)

## Capturas

> Coloca tus capturas de pantalla en `docs/img/` y descomenta las referencias.

<!--
![Vista general de la hoja](docs/img/hoja-principal.png)
![Reporte PDF con prefijos X / Y](docs/img/reporte-pdf.png)
![Dashboard web local](docs/img/dashboard-web.png)
-->

## Caracteristicas

### Hoja de trabajo

- Tabla editable con columnas tipadas: `Cod. cliente`, `Nombre`, `Nº`, `X`, `Y`, `Madera`, `Corte`, `Hendido`, `Goma`, `G.tamaño`, `Hecho`.
- Banner agrupador encima de la cabecera (CLIENTE / MEDIDAS / CORTE / HENDIDO ...).
- Autocompletado bidireccional cliente: al escribir el codigo se rellena el nombre y viceversa, leyendo de un CSV externo opcional.
- Validaciones en caliente: medidas X / Y numericas, cliente incompleto destacado en color, recuento de invalidaciones en pie de pagina.
- Suma automatica de margen `+5 mm` en cada edicion de las columnas `X` o `Y` (configurable en codigo).
- Columna `Imagen` con miniatura real dentro de la celda; al borrar el path la fila vuelve a su altura por defecto.
- Combos editables para `Madera`, `Goma`, `G.tamaño`, `C.tamaño` y `H.tamaño`.
- Deshacer / rehacer mediante snapshots completos del modelo.

### Busqueda, filtros y exportacion

- Busqueda libre o por ambito (`Todo`, `Cod. cliente`, `Nombre`, `Madera`).
- Filtro por filas terminadas / pendientes y combinacion con busqueda.
- Exportacion a CSV completo o solo filas visibles.
- Guardado y carga de la hoja activa en PostgreSQL (`Guardar BD` / `Cargar BD`) para trabajo compartido.
- Sincronizacion automatica con BD cada 45 s: aviso de cambios remotos y recarga opcional.
- Bloqueo suave de fila mientras un trabajador edita (visible para el resto del equipo).
- Control de conflicto de guardado en BD por version de hoja (avisa antes de sobrescribir cambios de otro puesto).
- Registro de guardados en `audit_events` con identificador de trabajador (`usuario@equipo`).
- Consulta de ranking en BD (`Ranking BD`) para ver quien compro mas y quien compro menos.
- Reporte HTML autocontenido con cabecera, filtro aplicado y totales.
- Reporte PDF generado en codigo propio (sin dependencias externas) con cada medida etiquetada (`X = 30`, `Y = 40`).
- Plantilla de nombre de fichero configurable: `titulo_tipo`, `tipo_titulo`, `titulo_fecha_tipo`, `fecha_titulo_tipo`.
- Versionado automatico (`_v2`, `_v3` ...) si el fichero destino ya existe.

### Historial

- Cada operacion relevante (edicion de celda, alta/baja de fila, exportacion, reset, etc.) se registra en `~/.troqueles/cambios.log`.
- Dialogo `Historial` accesible desde la UI con buscador y opcion de limpiar.

### Dashboard web local

- Servidor HTTP embebido (sin dependencias externas, `com.sun.net.httpserver`) que arranca al iniciar la app en `127.0.0.1:<puerto-libre>`.
- Endpoint `/api/rows` que devuelve las filas visibles serializadas en JSON.
- Pagina estatica servida desde `dashboard/` con `index.html`, `app.js` y `styles.css`.

## Stack tecnico

| Capa            | Tecnologia                                  |
| --------------- | ------------------------------------------- |
| Lenguaje        | Java 21 (Temurin / OpenJDK)                 |
| UI              | Swing                                       |
| Build           | Maven 3.9+                                  |
| Tests           | JUnit Jupiter 5.10                          |
| HTTP local      | `com.sun.net.httpserver` (JDK)              |
| Persistencia    | CSV plano + `Preferences` + base PostgreSQL (fase LAN) |
| PDF             | Generador propio, sin librerias de terceros |

## Requisitos

- JDK 21 o superior.
- Maven 3.9 o superior (recomendado).
- Sistema operativo: Windows 10/11, Linux o macOS.

## Como ejecutar

### Con Maven

```bash
mvn clean compile
mvn exec:java
```

Ejecutar los tests:

```bash
mvn test
```

Empaquetar (genera `target/*.jar`):

```bash
mvn clean package
```

### Con script PowerShell (Windows)

```powershell
.\compilar-y-ejecutar.ps1
```

### Servidor LAN (misma WiFi)

Este modo arranca un proceso servidor para que varios puestos del taller se conecten a la misma base PostgreSQL central.

```powershell
.\scripts\lan-server.ps1
```

Opciones:

```powershell
.\scripts\lan-server.ps1 -Host 0.0.0.0 -Port 9010
```

Comprobacion desde otro equipo de la red:

```text
http://IP_DEL_SERVIDOR:9010/health
```

Sincronizacion automatica en clientes Swing:

- Al abrir la app se activa polling cada 45 s contra la hoja actual.
- Si la version remota es mas nueva, aparece el aviso **Hay cambios en BD, ¿recargar?**
- Mientras editas una celda, se reserva la fila en BD durante 90 s (bloqueo suave visible para otros puestos).
- Puedes desactivar la sync desde **Archivo > Sincronizacion automatica BD** o forzar comprobacion con **Comprobar BD ahora**.

Configuracion de PostgreSQL:

- Copia `docs/db.properties.example` como `%USERPROFILE%\.troqueles\db.properties` en el equipo servidor.
- O usa variables de entorno `TROQUELES_DB_HOST`, `TROQUELES_DB_PORT`, `TROQUELES_DB_NAME`, `TROQUELES_DB_USER`, `TROQUELES_DB_PASS`, `TROQUELES_DB_SSL`.

### Con el lanzador `.bat`

Doble click sobre `Troqueles.bat` desde la carpeta del proyecto una vez compilado.

## Instalador nativo (jpackage)

Requisitos adicionales:

- `jpackage` incluido en el JDK 21 instalado.
- Para generar `.exe` o `.msi` en Windows: WiX Toolset 3.x con `candle.exe` y `light.exe` en `PATH`.
- Sin WiX, el script genera una carpeta portable (`app-image`) lista para copiar o comprimir.

Generar el instalador desde la raiz del repositorio (recomendado si no tienes Maven en PATH):

```powershell
.\scripts\crear-instalador.ps1
```

Alternativa directa (requiere Maven y JDK 21+ para `--app-content`):

```powershell
.\scripts\jpackage.ps1
```

Opciones utiles:

```powershell
.\scripts\jpackage.ps1 -PackageType app-image
.\scripts\jpackage.ps1 -PackageType exe
.\scripts\jpackage.ps1 -SkipTests
```

El resultado se escribe en `dist/`. El empaquetado incluye la carpeta `dashboard/` junto a la aplicacion para que el servidor web local funcione tras la instalacion.

Icono opcional: coloca `docs/icon.ico` antes de empaquetar para aplicarlo al lanzador.

## Estructura del proyecto

```
troqueles-control-produccion/
├── src/
│   ├── main/java/com/trabajo/troqueles/
│   │   ├── Main.java                # Punto de entrada
│   │   ├── SpreadsheetFrame.java    # Ventana principal y flujos UI
│   │   ├── SpreadsheetCsv.java      # Import / export CSV
│   │   ├── SpreadsheetReport.java   # Reporte HTML
│   │   ├── PdfReportWriter.java     # Reporte PDF (sin libs externas)
│   │   ├── SpreadsheetHistory.java  # Undo / redo
│   │   ├── SpreadsheetSchema.java   # Resolucion de columnas por nombre
│   │   ├── SpreadsheetStats.java    # Utilidades numericas
│   │   ├── ChangeLog.java           # Log de acciones del usuario
│   │   ├── DashboardServer.java     # Servidor HTTP local
│   │   ├── DashboardSnapshot.java   # Snapshot de filas para el dashboard
│   │   └── CsvUtils.java            # Parser CSV simple
│   └── test/java/com/trabajo/troqueles/   # Suite JUnit 5
├── dashboard/                       # Front estatico del dashboard
├── docs/                            # Documentacion adicional
├── scripts/                         # Empaquetado con jpackage
├── pom.xml
└── README.md
```

## Tests

La suite cubre normalizacion de clientes, snapshots de historial, generacion de reportes, parser de formulas, atajos y base tecnica de configuracion para BD.

```
$ mvn test
[INFO] Tests run: 22, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

## Datos privados

El repositorio publico contiene unicamente codigo y documentacion. La aplicacion espera, de forma opcional, un CSV de clientes para activar el autocompletado:

- `clientes_codigos.csv` o `clientes_import.csv` en el directorio de trabajo, con formato:

  ```csv
  codigo;Nombre del cliente
  100;CLIENTE EJEMPLO S.L.
  101;OTRO CLIENTE
  ```

Sin esos ficheros la app sigue funcionando, simplemente sin autocompletado. Los CSV reales y el `cambios.log` estan excluidos por `.gitignore`.

## Roadmap

- Columna de fecha automatica con filtro por rango.
- Estados intermedios (`Pendiente / En curso / Terminado`) en lugar del booleano `Hecho`.
- Modo multiusuario LAN por fases (cliente-servidor + PostgreSQL central).
- CI con GitHub Actions y badge dinamico de build.

## Licencia

Proyecto distribuido con fines academicos / profesionales. Si publicas un fork, conserva la atribucion al autor original.
