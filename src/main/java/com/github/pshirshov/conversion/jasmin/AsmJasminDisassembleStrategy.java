package com.github.pshirshov.conversion.jasmin;

import com.github.pshirshov.conversion.DisassembleStrategy;
import com.github.pshirshov.org.objectweb.asm.ClassReader;
import com.github.pshirshov.util.IdeaUtils;
import com.intellij.openapi.editor.Document;

import java.io.PrintWriter;
import java.io.StringWriter;

public class AsmJasminDisassembleStrategy implements DisassembleStrategy {
    @Override
    public String disassemble(byte[] classfile) {
        final ClassReader classReader = new ClassReader(classfile);
        final StringWriter writer = new StringWriter();

        try (final PrintWriter printWriter = new PrintWriter(writer)) {
            int flags = ClassReader.EXPAND_FRAMES;
            classReader.accept(new JasminifierClassAdapter(printWriter), flags);
        }

        return writer.toString();

    }


    @Override
    public int getLineOffset(String bytecode, Document document, int lineNumber) {
        return IdeaUtils.findSubstringOffset(bytecode, document, lineNumber, ".line ");
    }
}
