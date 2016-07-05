package com.github.pshirshov.byteCodeViewer;

import com.intellij.openapi.fileTypes.FileType;
import com.intellij.psi.PsiElement;
import com.intellij.testFramework.BinaryLightVirtualFile;

public class VFile extends BinaryLightVirtualFile {
    private PsiElement element;


    public VFile(String name, FileType fileType, byte[] content, PsiElement element) {
        super(name, fileType, content);
        this.element = element;
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
}
