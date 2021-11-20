package com.github.pshirshov.conversion.asm;

import com.github.pshirshov.conversion.DisassembleStrategy;
import com.github.pshirshov.org.objectweb.asm.ClassReader;
import com.github.pshirshov.org.objectweb.asm.util.Textifier;
import com.github.pshirshov.org.objectweb.asm.util.TraceClassVisitor;
import com.github.pshirshov.util.IdeaUtils;
import com.intellij.openapi.editor.Document;

import java.io.PrintWriter;
import java.io.StringWriter;

public class AsmAssemblyDisassembleStrategy implements DisassembleStrategy {
    @Override
    public String disassemble(byte[] classfile) {
        final ClassReader classReader = new ClassReader(classfile);
        final StringWriter writer = new StringWriter();

        try (final PrintWriter printWriter = new PrintWriter(writer)) {
            classReader.accept(new TraceClassVisitor(null, new Textifier(), printWriter), 0);
        }

        return writer.toString();

    }


    @Override
    public int getLineOffset(String bytecode, Document document, int lineNumber) {
        return IdeaUtils.findSubstringOffset(bytecode, document, lineNumber, "LINENUMBER ");
    }
}
