package com.github.pshirshov.byteCodeViewer;

import com.intellij.codeHighlighting.BackgroundEditorHighlighter;
import com.intellij.icons.AllIcons;
import com.intellij.ide.structureView.StructureViewBuilder;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.fileChooser.FileChooser;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorLocation;
import com.intellij.openapi.fileEditor.FileEditorState;
import com.intellij.openapi.fileEditor.FileEditorStateLevel;
import com.intellij.openapi.fileEditor.impl.text.TextEditorState;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.MessageType;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.util.UserDataHolderBase;
import com.intellij.openapi.vfs.VirtualFile;
import jasmin.Main;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.beans.PropertyChangeListener;
import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class ByteCodeEditor extends UserDataHolderBase implements FileEditor {
    private static final Logger LOG = Logger.getInstance(ByteCodeEditor.class);

    private final ByteCodeViewerComponent component;
    private FileEditorState textEditorState = new TextEditorState();
    private VFile file;


    public VFile getFile() {
        return file;
    }


    public ByteCodeEditor(Project project, VFile virtualFile) {
        AnAction[] additionalActions = new AnAction[] { new AnAction() {

            @Override
            public void update(AnActionEvent e) {
                e.getPresentation().setVisible(true);
                e.getPresentation().setIcon(AllIcons.Actions.Export);
                e.getPresentation().setDescription("Compile into class file");
            }


            @Override
            public void actionPerformed(AnActionEvent anActionEvent) {
                InputStream is = new ByteArrayInputStream(component.myEditor.getDocument().getText().getBytes());
                final String replace = virtualFile.getPresentableName().replace(".bc", ".class");
                final FileChooserDescriptor fcd =
                        new FileChooserDescriptor(false, true, false, false, false, false);
                VirtualFile selection = FileChooser.chooseFile(fcd, project, null);
                if (selection!= null) {
                    final String destDir = selection.getPath();

                    try {
                        LOG.info("Assembling classfile into "+destDir);
                        new Main().assemble(is, replace, destDir);
                    } catch (Throwable e) {
                        LOG.error("Assembly failed", e);
                          String message = "Jasmin assembler failed: "+e.getMessage();
                          JBPopupFactory.getInstance()
                                        .createHtmlTextBalloonBuilder(message, MessageType.ERROR, null)
                                        .createBalloon()
                                        .showInCenterOf(getComponent());
                    }
                }
            }
        }};
        this.component = new ByteCodeViewerComponent(project, additionalActions);
        this.file = virtualFile;
        updateEditor(virtualFile);
    }


    private void updateEditor(VFile virtualFile) {
        try {
            try (BufferedInputStream bis = new BufferedInputStream(virtualFile.getInputStream());
                 ByteArrayOutputStream buf = new ByteArrayOutputStream();) {
                int result = bis.read();
                while (result != -1) {
                    buf.write((byte) result);
                    result = bis.read();
                }
                component.setText(buf.toString(), virtualFile.getElement());
            }
            ApplicationManager.getApplication().runWriteAction(new Runnable() {
                @Override
                public void run() {
                    try {
                        file.setBinaryContent(virtualFile.getContent());
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                    file.setElement(virtualFile.getElement());
                }
            });

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }


    @NotNull
    @Override
    public JComponent getComponent() {
        return component;
    }


    @Nullable
    @Override
    public JComponent getPreferredFocusedComponent() {
        return component;
    }


    @NotNull
    @Override
    public String getName() {
        return file.getPresentableName();
    }


    @NotNull
    @Override
    public FileEditorState getState(@NotNull FileEditorStateLevel fileEditorStateLevel) {
        return textEditorState;
    }


    @Override
    public void setState(@NotNull FileEditorState fileEditorState) {
        textEditorState = fileEditorState;
    }


    @Override
    public boolean isModified() {
        return false;
    }


    @Override
    public boolean isValid() {
        return true;
    }


    @Override
    public void selectNotify() {

    }


    @Override
    public void deselectNotify() {

    }


    @Override
    public void addPropertyChangeListener(@NotNull PropertyChangeListener propertyChangeListener) {

    }


    @Override
    public void removePropertyChangeListener(@NotNull PropertyChangeListener propertyChangeListener) {

    }


    @Nullable
    @Override
    public BackgroundEditorHighlighter getBackgroundHighlighter() {
        return null;
    }


    @Nullable
    @Override
    public FileEditorLocation getCurrentLocation() {
        return null;
    }


    @Nullable
    @Override
    public StructureViewBuilder getStructureViewBuilder() {
        return null;
    }


    @Override
    public void dispose() {
        component.dispose();
    }


    public void update(VFile vFile) {
        updateEditor(vFile);
    }
}
