/**
 * Paquete principal de la aplicacion de escritorio Trabajo Troqueles.
 *
 * <p>Este paquete concentra los componentes de interfaz (Swing), utilidades de persistencia
 * en CSV, calculo de estadisticas, historial de cambios y generacion de reportes.
 *
 * <h2>Responsabilidades principales</h2>
 * <ul>
 *   <li>Ejecucion de la aplicacion y carga de la ventana principal.</li>
 *   <li>Gestion de datos tabulares en memoria con esquema de columnas definido.</li>
 *   <li>Importacion/exportacion de datos CSV y normalizacion de codificacion.</li>
 *   <li>Calculo de indicadores y resumenes para uso en interfaz y dashboard.</li>
 *   <li>Publicacion de datos hacia dashboard web mediante servidor HTTP local.</li>
 *   <li>Generacion de reportes en formatos HTML y PDF.</li>
 * </ul>
 *
 * <h2>Clases destacadas</h2>
 * <ul>
 *   <li>{@link com.trabajo.troqueles.Main}: punto de entrada del programa.</li>
 *   <li>{@link com.trabajo.troqueles.SpreadsheetFrame}: interfaz principal y flujo de usuario.</li>
 *   <li>{@link com.trabajo.troqueles.SpreadsheetCsv}: capa de lectura y escritura CSV.</li>
 *   <li>{@link com.trabajo.troqueles.SpreadsheetStats}: logica de calculo estadistico.</li>
 *   <li>{@link com.trabajo.troqueles.SpreadsheetHistory}: deshacer y rehacer por snapshots.</li>
 *   <li>{@link com.trabajo.troqueles.DashboardServer}: exposicion de datos al dashboard.</li>
 *   <li>{@link com.trabajo.troqueles.SpreadsheetReport} y
 *       {@link com.trabajo.troqueles.PdfReportWriter}: salida de reportes.</li>
 * </ul>
 */
package com.trabajo.troqueles;
