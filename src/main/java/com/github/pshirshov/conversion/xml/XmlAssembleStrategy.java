package com.github.pshirshov.conversion.xml;

import com.github.pshirshov.conversion.AssembleStrategy;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.xml.ASMContentHandler;
import com.github.pshirshov.util.BCEVirtualFile;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.ByteArrayInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.nio.file.Paths;

public class XmlAssembleStrategy implements AssembleStrategy {
    @Override
    public void assemble(BCEVirtualFile file, String targetDirectory) {
        final InputStream is = new ByteArrayInputStream(file.getContent());
        final String targetName = file.getPresentableName().replace(".bc", ".class");

        final ClassWriter classWriter = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        final ASMContentHandler handler = new ASMContentHandler(classWriter);
        final SAXParserFactory factory = SAXParserFactory.newInstance();
        try {
            final String targetPath = Paths.get(targetDirectory).resolve(targetName).toFile().getCanonicalPath();

            final SAXParser saxParser = factory.newSAXParser();
            saxParser.parse(is, handler);

            final byte[] bytes = classWriter.toByteArray();
            try (FileOutputStream fos = new FileOutputStream(targetPath)) {
                fos.write(bytes);
            }
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }
}
