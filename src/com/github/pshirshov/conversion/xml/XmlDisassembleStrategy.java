package com.github.pshirshov.conversion.xml;

import com.github.pshirshov.conversion.DisassembleStrategy;
import com.github.pshirshov.org.objectweb.asm.ClassReader;
import com.github.pshirshov.org.objectweb.asm.xml.SAXClassAdapter;
import com.github.pshirshov.util.IdeaUtils;
import com.intellij.openapi.editor.Document;
import org.xml.sax.helpers.DefaultHandler;

import java.io.StringWriter;

public class XmlDisassembleStrategy implements DisassembleStrategy {
    @Override
    public String disassemble(byte[] classfile) {
        final ClassReader classReader = new ClassReader(classfile);
        final StringWriter output = new StringWriter();

        final DefaultHandler handler = new DisassembleHandler(output);
        classReader.accept(new SAXClassAdapter(handler, true), ClassReader.EXPAND_FRAMES);
        return output.toString();
    }


    @Override
    public int getLineOffset(String bytecode, Document document, int lineNumber) {
        return IdeaUtils.findSubstringOffset(bytecode, document, lineNumber, "<LineNumber line=\"");
    }


}
