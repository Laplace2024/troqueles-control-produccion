# Trabajo_Troqueles

Aplicacion de escritorio en Java para la gestion de trabajos de troquelado mediante una tabla editable, con filtros, exportaciones y utilidades de seguimiento.

## Descripcion

El proyecto ofrece una interfaz Swing orientada a operativa diaria:

- edicion de filas y columnas en formato tabla,
- control de estados de fabricacion,
- busqueda y filtrado de registros,
- exportacion de datos y reporte HTML,
- dashboard web local para visualizacion de datos.

## Funcionalidades principales

- Tabla editable con validaciones basicas.
- Alta, borrado, duplicado e insercion de filas.
- Insercion de columnas a izquierda/derecha de la seleccion.
- Campos con desplegables configurables por columna.
- Columna de estado (`Hecho`) con resaltado visual de fila.
- Busqueda por texto (alcance configurable).
- Deshacer y rehacer con snapshots de tabla.
- Exportacion a CSV (completo y visible) y reporte HTML.
- Dashboard web local (`dashboard/`) con vista de datos.
- Historial de cambios funcionales (registro y consulta desde la UI).

## Estructura del codigo

- `src/main/java/com/trabajo/troqueles/Main.java`: punto de entrada.
- `SpreadsheetFrame.java`: ventana principal y flujos de interfaz.
- `SpreadsheetCsv.java`: lectura/escritura CSV.
- `SpreadsheetHistory.java`: snapshots para deshacer/rehacer.
- `SpreadsheetStats.java`: utilidades de calculo y agregados.
- `SpreadsheetReport.java` y `PdfReportWriter.java`: reportes.
- `DashboardServer.java` y `DashboardSnapshot.java`: API local para dashboard.

## Requisitos

- JDK 21 o superior.
- PowerShell (Windows) para scripts incluidos.
- Maven opcional (si se desea usar `pom.xml`).

## Ejecucion (PowerShell)

Desde la carpeta `Trabajo_Troqueles`:

```powershell
.\compilar-y-ejecutar.ps1
```

## Ejecucion con Maven (opcional)

```powershell
mvn clean compile
mvn exec:java
```

## Documentacion tecnica

- Guia JavaDoc: `docs/JAVADOC.md`
- Generacion local:

```powershell
mvn clean javadoc:javadoc
start .\target\reports\apidocs\index.html
```

## Publicacion y datos sensibles

Este repositorio esta preparado para publicar solo codigo y documentacion tecnica.

- No se incluyen ficheros de clientes ni logs operativos.
- La aplicacion admite catalogos CSV locales privados fuera del control de versiones.
