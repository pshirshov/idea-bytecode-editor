package com.github.pshirshov.conversion.jasmin;

import com.github.pshirshov.conversion.AssembleStrategy;
import com.github.pshirshov.util.BCEVirtualFile;
import jasmin.Main;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

public class JasminAssembleStrategy implements AssembleStrategy {
    @Override
    public void assemble(BCEVirtualFile file, String targetDirectory) {
        final InputStream is = new ByteArrayInputStream(file.getContent());
        final String targetName = file.getPresentableName().replace(".bc", ".class");
        new Main().assemble(is, targetName, targetDirectory);
    }
}
