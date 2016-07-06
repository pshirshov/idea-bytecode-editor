package com.github.pshirshov.conversion;

import java.io.InputStream;

public interface AssembleStrategy {
    void assemble(InputStream assembly, String targetDirectory, String fileName);
}
