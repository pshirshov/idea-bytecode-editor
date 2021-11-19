package com.github.pshirshov.conversion;

import com.intellij.openapi.editor.Document;

public interface DisassembleStrategy {
    String disassemble(byte[] classfile);

    int getLineOffset(String bytecode, Document document, int lineNumber);
}
