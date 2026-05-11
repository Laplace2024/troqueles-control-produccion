# Changelog - Trabajo Troqueles

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

