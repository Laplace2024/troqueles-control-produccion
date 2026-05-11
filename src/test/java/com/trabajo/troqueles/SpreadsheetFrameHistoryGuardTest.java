package com.trabajo.troqueles;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import org.junit.jupiter.api.Test;

class SpreadsheetFrameHistoryGuardTest {

    @Test
    void executeHistoryOperationRestauraBanderaAunqueHayaExcepcion() throws Exception {
        SpreadsheetFrame frame = allocateWithoutConstructor();

        Field flagField = SpreadsheetFrame.class.getDeclaredField("historyOperationInProgress");
        flagField.setAccessible(true);

        Method executeMethod = SpreadsheetFrame.class.getDeclaredMethod("executeHistoryOperation", Runnable.class);
        executeMethod.setAccessible(true);

        InvocationTargetException ex = assertThrows(
            InvocationTargetException.class,
            () -> executeMethod.invoke(frame, (Runnable) () -> {
                throw new IllegalStateException("boom");
            })
        );
        assertThrows(IllegalStateException.class, () -> {
            throw (RuntimeException) ex.getTargetException();
        });

        assertFalse(flagField.getBoolean(frame));
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
