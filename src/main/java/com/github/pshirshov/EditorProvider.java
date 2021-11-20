package com.github.pshirshov;

import com.github.pshirshov.util.BCEVirtualFile;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorPolicy;
import com.intellij.openapi.fileEditor.FileEditorProvider;
import com.intellij.openapi.fileEditor.FileEditorState;
import com.intellij.openapi.fileEditor.impl.text.TextEditorState;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;

public class EditorProvider implements FileEditorProvider {

    @Override
    public boolean accept(@NotNull Project project, @NotNull VirtualFile virtualFile) {
        return virtualFile instanceof BCEVirtualFile;
    }


    @NotNull
    @Override
    public FileEditor createEditor(@NotNull Project project, @NotNull VirtualFile virtualFile) {
        final BCEVirtualFile asVfile = (BCEVirtualFile) virtualFile;
        return new ByteCodeEditor(project, asVfile);
    }


    @Override
    public void disposeEditor(@NotNull FileEditor fileEditor) {
    }


    @NotNull
    @Override
    public FileEditorState readState(@NotNull Element element, @NotNull Project project,
                                     @NotNull VirtualFile virtualFile) {
        return new TextEditorState();
    }


    @Override
    public void writeState(@NotNull FileEditorState fileEditorState, @NotNull Project project,
                           @NotNull Element element) {

    }


    @NotNull
    @Override
    public String getEditorTypeId() {
        return "com.github.pshirshov.bce";
    }


    @NotNull
    @Override
    public FileEditorPolicy getPolicy() {
        return FileEditorPolicy.NONE;
    }
}
