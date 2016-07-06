package com.github.pshirshov.conversion.xml;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import java.io.StringWriter;

class DisassembleHandler extends DefaultHandler {
    private final StringWriter output;


    DisassembleHandler(StringWriter output) {
        this.output = output;
    }


    @Override
    public void startElement(String uri, String localName, String qName, Attributes atts) throws SAXException {
        StringBuilder builder = new StringBuilder();
        builder.append('<').append(localName);
        for (int i = 0; i < atts.getLength(); i++) {
            escape(builder.append(' ').append(atts.getLocalName(i)).append("=\""), atts.getValue(i))
                    .append('"');
        }
        builder.append(">\n");
        output.append(builder);
    }


    @Override
    public void endElement(String uri, String localName, String qName) {
        output.write("</" + localName + ">");
    }


    @Override
    public void characters(char[] ch, int start, int length) {
        output.write(ch, start, length);
    }


    static StringBuilder escape(StringBuilder builder, String value) {
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            switch (c) {
                case '<':
                    builder.append("&lt;");
                    continue;
                case '>':
                    builder.append("&gt;");
                    continue;
                default:
                    builder.append(c);
                    continue;
            }
        }
        return builder;
    }
}
