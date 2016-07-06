package com.github.pshirshov.conversion.jasmin;

import com.github.pshirshov.conversion.AssembleStrategy;
import jasmin.Main;

import java.io.InputStream;

public class JasminAssembleStrategy implements AssembleStrategy {
    @Override
    public void assemble(InputStream assembly, String targetDirectory, String fileName) {
        new Main().assemble(assembly, fileName, targetDirectory);
    }
}
