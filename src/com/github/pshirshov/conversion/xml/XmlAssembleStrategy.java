package com.github.pshirshov.conversion.xml;

import com.github.pshirshov.conversion.AssembleStrategy;
import com.github.pshirshov.org.objectweb.asm.ClassWriter;
import com.github.pshirshov.org.objectweb.asm.xml.ASMContentHandler;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.nio.file.Paths;

public class XmlAssembleStrategy implements AssembleStrategy {
    @Override
    public void assemble(InputStream assembly, String targetDirectory, String fileName) {
        final ClassWriter classWriter = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        final ASMContentHandler handler = new ASMContentHandler(classWriter);
        final SAXParserFactory factory = SAXParserFactory.newInstance();
        try {
            final String targetPath = Paths.get(targetDirectory).resolve(fileName).toFile().getCanonicalPath();

            final SAXParser saxParser = factory.newSAXParser();
            saxParser.parse(assembly, handler);

            final byte[] bytes = classWriter.toByteArray();
            try (FileOutputStream fos = new FileOutputStream(targetPath)) {
                fos.write(bytes);
            }
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }
}
