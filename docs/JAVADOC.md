# JavaDoc - Trabajo Troqueles

## Objetivo

Definir una referencia tecnica del programa `Trabajo_Troqueles` en formato JavaDoc, incluyendo:

- alcance funcional,
- componentes principales,
- comandos de generacion de documentacion,
- criterios minimos de calidad para comentarios JavaDoc nuevos.

## Alcance del sistema

`Trabajo_Troqueles` es una aplicacion de escritorio Java (Swing) orientada a la gestion tabular de produccion de troqueles, con:

- edicion de datos en tabla,
- importacion y exportacion CSV,
- estadisticas sobre datos visibles,
- deshacer/rehacer con historial,
- reporte HTML/PDF,
- dashboard web conectado por servidor local.

## Mapa de clases publicas (API interna del modulo)

- `com.trabajo.troqueles.Main`  
  Punto de entrada de la aplicacion Swing.

- `com.trabajo.troqueles.SpreadsheetFrame`  
  Ventana principal, eventos de interfaz, flujos de usuario y coordinacion de servicios.

- `com.trabajo.troqueles.SpreadsheetCsv`  
  Lectura/escritura CSV con soporte de UTF-8 y fallback `windows-1252`.

- `com.trabajo.troqueles.SpreadsheetStats`  
  Calculos estadisticos, agregados y resumenes.

- `com.trabajo.troqueles.SpreadsheetHistory`  
  Historial de cambios para operaciones de deshacer/rehacer.

- `com.trabajo.troqueles.SpreadsheetSchema`  
  Esquema de columnas y reglas base de estructura.

- `com.trabajo.troqueles.SpreadsheetReport`  
  Generacion de reportes en HTML.

- `com.trabajo.troqueles.PdfReportWriter`  
  Generacion de reporte PDF a partir de datos tabulares.

- `com.trabajo.troqueles.DashboardServer`  
  Servidor HTTP local para exponer datos al dashboard web.

- `com.trabajo.troqueles.DashboardSnapshot`  
  Construccion de payloads para panel/datos de dashboard.

- `com.trabajo.troqueles.ChangeLog`  
  Registro de eventos funcionales de la aplicacion.

## Generacion de JavaDoc con Maven (PowerShell)

Desde la carpeta `Trabajo_Troqueles`:

```powershell
mvn clean javadoc:javadoc
```

Salida esperada:

- Carpeta generada: `target\reports\apidocs\`
- Archivo principal: `target\reports\apidocs\index.html`

Para abrirlo rapidamente en Windows:

```powershell
start .\target\reports\apidocs\index.html
```

## Generacion de JavaDoc por paquete principal

Si se quiere limitar al paquete principal:

```powershell
mvn javadoc:javadoc "-Dsubpackages=com.trabajo.troqueles"
```

## Convenciones de JavaDoc para este proyecto

1. **Clases publicas**  
   Describir responsabilidad principal y limites de uso.

2. **Metodos publicos**  
   Incluir:
   - que hace,
   - parametros (`@param`),
   - retorno (`@return`) si aplica,
   - errores (`@throws`) cuando proceda.

3. **Constantes o estructuras de dominio**  
   Explicar impacto funcional (no solo tipo tecnico).

4. **Estilo**  
   Tono profesional, tecnico y conciso, alineado con `Trabajo_Troqueles`.

## Estado actual de documentacion JavaDoc

- Se incorpora documentacion de paquete mediante `package-info.java`.
- Esta guia centraliza el procedimiento para generar y revisar `apidocs`.

## Verificacion minima recomendada

Tras cambios relevantes:

1. Ejecutar `mvn clean test`.
2. Ejecutar `mvn javadoc:javadoc`.
3. Revisar que `target\reports\apidocs\index.html` abre sin errores y que las clases principales aparecen en navegacion.
