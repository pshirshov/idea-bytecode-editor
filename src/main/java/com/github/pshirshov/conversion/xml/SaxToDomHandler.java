package com.github.pshirshov.conversion.xml;

import org.w3c.dom.*;
import org.xml.sax.Attributes;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;

class SaxToDomHandler
        extends DefaultHandler {
    public SaxToDomHandler(Document doc) {
        myDoc = doc;
        myCurrentNode = myDoc;
    }

    // Add it in the DOM tree, at the right place.
    @Override
    public void startElement(String uri, String name, String qName, Attributes attrs) {
        // Create the element.
        Element elem = myDoc.createElementNS(uri, qName);
        // Add each attribute.
        for (int i = 0; i < attrs.getLength(); ++i) {
            String ns_uri = attrs.getURI(i);
            String qname = attrs.getQName(i);
            String value = attrs.getValue(i);
            Attr attr = myDoc.createAttributeNS(ns_uri, qname);
            attr.setValue(value);
            elem.setAttributeNodeNS(attr);
        }
        // Actually add it in the tree, and adjust the right place.
        myCurrentNode.appendChild(elem);
        myCurrentNode = elem;
    }

    // Adjust the current place for subsequent additions.
    @Override
    public void endElement(String uri, String name, String qName) {
        myCurrentNode = myCurrentNode.getParentNode();
    }

    // Add a new text node in the DOM tree, at the right place.
    @Override
    public void characters(char[] ch, int start, int length) {
        String str = new String(ch, start, length);
        Text text = myDoc.createTextNode(str);
        myCurrentNode.appendChild(text);
    }

    // Add a new text node in the DOM tree, at the right place.
    @Override
    public void ignorableWhitespace(char[] ch, int start, int length) {
        String str = new String(ch, start, length);
        Text text = myDoc.createTextNode(str);
        myCurrentNode.appendChild(text);
    }

    // Add a new text PI in the DOM tree, at the right place.
    @Override
    public void processingInstruction(String target, String data) {
        ProcessingInstruction pi = myDoc.createProcessingInstruction(target, data);
        myCurrentNode.appendChild(pi);
    }

    // For the handlers below, use your usual logging facilities.
    @Override
    public void error(SAXParseException e) {
        throw new RuntimeException(e);
    }

    @Override
    public void fatalError(SAXParseException e) {
        throw new RuntimeException(e);
    }

    @Override
    public void warning(SAXParseException e) {

    }

    private Document myDoc;
    private Node myCurrentNode;
}
