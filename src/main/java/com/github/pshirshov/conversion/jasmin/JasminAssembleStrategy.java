package com.github.pshirshov.conversion.jasmin;

import com.github.pshirshov.conversion.AssembleStrategy;
import com.github.pshirshov.util.BCEVirtualFile;
import jasmin.Main;

import java.io.*;

public class JasminAssembleStrategy implements AssembleStrategy {
    @Override
    public void assemble(BCEVirtualFile file, String targetDirectory) throws IOException {

        try (
                InputStream inputStream = new ByteArrayInputStream(file.getContent());
        ) {
            // TODO shit logic
            final String targetFileName = file.getPresentableName().replace(
                    ".bc",
                    ".class"
            );
            OutputStream outputStream = new FileOutputStream(
                    targetDirectory + "/" + targetFileName
            );
            Main.assemble(
                    inputStream,
                    outputStream,
                    true
            );
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
