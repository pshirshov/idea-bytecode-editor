package com.github.pshirshov.conversion;

import com.github.pshirshov.util.BCEVirtualFile;

import java.io.IOException;

public interface AssembleStrategy {
    void assemble(BCEVirtualFile file, String targetDirectory) throws IOException;
}
