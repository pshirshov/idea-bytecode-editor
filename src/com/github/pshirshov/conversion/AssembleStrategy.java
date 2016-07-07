package com.github.pshirshov.conversion;

import com.github.pshirshov.util.BCEVirtualFile;

public interface AssembleStrategy {
    void assemble(BCEVirtualFile file, String targetDirectory);
}
