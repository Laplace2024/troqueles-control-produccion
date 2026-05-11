package com.trabajo.troqueles;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import javax.swing.table.DefaultTableModel;
import org.junit.jupiter.api.Test;

class SpreadsheetFormulaParserTest {

    @Test
    void sumarConRangoYSeparadorPuntoYComa() throws Exception {
        SpreadsheetFrame frame = allocateWithoutConstructor();
        setTableModel(frame, buildFormulaModel());
        assertEquals(10.0, invokeEvaluate(frame, "SUMAR(A1;A2;5)"), 0.0001);
    }

    @Test
    void contarSoloNumericosEnRango() throws Exception {
        SpreadsheetFrame frame = allocateWithoutConstructor();
        setTableModel(frame, buildFormulaModel());
        assertEquals(3.0, invokeEvaluate(frame, "CONTAR(A1:B2)"), 0.0001);
    }

    @Test
    void dividirPorCeroLanzaError() throws Exception {
        SpreadsheetFrame frame = allocateWithoutConstructor();
        setTableModel(frame, buildFormulaModel());
        InvocationTargetException ex = assertThrows(
            InvocationTargetException.class,
            () -> invokeEvaluateMethod(frame, "DIVIDIR(10;0)")
        );
        Throwable target = ex.getTargetException();
        assertEquals(IllegalArgumentException.class, target.getClass());
        assertEquals("division por cero", target.getMessage());
    }

    @Test
    void funcionNoSoportadaLanzaError() throws Exception {
        SpreadsheetFrame frame = allocateWithoutConstructor();
        setTableModel(frame, buildFormulaModel());
        InvocationTargetException ex = assertThrows(
            InvocationTargetException.class,
            () -> invokeEvaluateMethod(frame, "PROMEDIO(A1;A2)")
        );
        Throwable target = ex.getTargetException();
        assertEquals(IllegalArgumentException.class, target.getClass());
        assertEquals("funcion no soportada: PROMEDIO", target.getMessage());
    }

    private static DefaultTableModel buildFormulaModel() {
        DefaultTableModel model = new DefaultTableModel(new Object[]{"A", "B"}, 0);
        model.addRow(new Object[]{"2", "x"});
        model.addRow(new Object[]{"3", "4"});
        return model;
    }

    private static void setTableModel(SpreadsheetFrame frame, DefaultTableModel model) throws Exception {
        Field field = SpreadsheetFrame.class.getDeclaredField("tableModel");
        field.setAccessible(true);
        field.set(frame, model);
    }

    private static double invokeEvaluate(SpreadsheetFrame frame, String expression) throws Exception {
        return (double) invokeEvaluateMethod(frame, expression);
    }

    private static Object invokeEvaluateMethod(SpreadsheetFrame frame, String expression) throws Exception {
        Method method = SpreadsheetFrame.class.getDeclaredMethod("evaluateFormulaExpression", String.class);
        method.setAccessible(true);
        return method.invoke(frame, expression);
    }

    private static SpreadsheetFrame allocateWithoutConstructor() throws Exception {
        Class<?> unsafeClass = Class.forName("sun.misc.Unsafe");
        Field f = unsafeClass.getDeclaredField("theUnsafe");
        f.setAccessible(true);
        Object unsafe = f.get(null);
        Method allocateInstance = unsafeClass.getMethod("allocateInstance", Class.class);
        return (SpreadsheetFrame) allocateInstance.invoke(unsafe, SpreadsheetFrame.class);
    }
}
