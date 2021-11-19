package com.github.pshirshov.conversion.xml;

import com.github.pshirshov.conversion.DisassembleStrategy;
import com.github.pshirshov.org.objectweb.asm.ClassReader;
import com.github.pshirshov.org.objectweb.asm.xml.SAXClassAdapter;
import com.github.pshirshov.util.IdeaUtils;
import org.w3c.dom.DOMImplementation;
import org.w3c.dom.Document;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.StringWriter;
import java.io.Writer;

public class XmlDisassembleStrategy implements DisassembleStrategy {

    @Override
    public String disassemble(byte[] classfile) {
        final ClassReader classReader = new ClassReader(classfile);


        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            DocumentBuilder builder = factory.newDocumentBuilder();
            DOMImplementation impl = builder.getDOMImplementation();
            Document doc = impl.createDocument(null, null, null);
            SaxToDomHandler handlers = new SaxToDomHandler(doc);
            classReader.accept(new SAXClassAdapter(handlers, true), ClassReader.EXPAND_FRAMES);
            return prettyPrint(doc);

        } catch (Throwable e) {
            throw new RuntimeException(e);
        }


    }


    public static String prettyPrint(Document xml) throws Exception {
        Transformer tf = TransformerFactory.newInstance().newTransformer();
        tf.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
        tf.setOutputProperty(OutputKeys.INDENT, "yes");
        tf.setOutputProperty(OutputKeys.METHOD, "xml");
        tf.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
        Writer out = new StringWriter();
        tf.transform(new DOMSource(xml), new StreamResult(out));
        return out.toString();
    }


    @Override
    public int getLineOffset(String bytecode, com.intellij.openapi.editor.Document document, int lineNumber) {
        return IdeaUtils.findSubstringOffset(bytecode, document, lineNumber, "<LineNumber line=\"");
    }


}
