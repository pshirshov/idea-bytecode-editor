package com.github.pshirshov.util;

import com.github.pshirshov.conversion.xml.XmlDisassembleStrategy;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.psi.PsiElement;
import com.intellij.testFramework.BinaryLightVirtualFile;

public class BCEVirtualFile extends BinaryLightVirtualFile {
    private XmlDisassembleStrategy strategy;
    private PsiElement element;


    public BCEVirtualFile(String name, FileType fileType, byte[] content, PsiElement element,
                          XmlDisassembleStrategy disassembleStrategy) {
        super(name, fileType, content);
        this.element = element;
        this.strategy = disassembleStrategy;
    }


    @Override
    public String getPresentableName() {
        final String[] parts = getPath().split("/");
        return parts[parts.length - 1];
    }


    public PsiElement getElement() {
        return element;
    }


    public void setElement(PsiElement element) {
        this.element = element;
    }


    public XmlDisassembleStrategy getStrategy() {
        return strategy;
    }


    public void setStrategy(XmlDisassembleStrategy strategy) {
        this.strategy = strategy;
    }
}
