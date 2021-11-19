package com.github.pshirshov.util;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;

public final class ExceptionUtils {
    private ExceptionUtils() {
    }


    public static String toString(Throwable e) {
        try (StringWriter errors = new StringWriter()) {
            e.printStackTrace(new PrintWriter(errors));
            return errors.toString();
        } catch (IOException e1) {
            throw new RuntimeException(e1);
        }
    }
}
