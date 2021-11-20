package com.github.pshirshov;

import com.github.pshirshov.util.BCEVirtualFile;
import com.intellij.codeHighlighting.BackgroundEditorHighlighter;
import com.intellij.execution.filters.LineNumbersMapping;
import com.intellij.ide.structureView.StructureViewBuilder;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.ActionPlaces;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.*;
import com.intellij.openapi.editor.colors.EditorColorsManager;
import com.intellij.openapi.editor.ex.EditorEx;
import com.intellij.openapi.editor.highlighter.EditorHighlighterFactory;
import com.intellij.openapi.editor.impl.EditorFactoryImpl;
import com.intellij.openapi.fileEditor.*;
import com.intellij.openapi.fileEditor.impl.text.TextEditorState;
import com.intellij.openapi.fileTypes.StdFileTypes;
import com.intellij.openapi.fileTypes.SyntaxHighlighter;
import com.intellij.openapi.fileTypes.SyntaxHighlighterFactory;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.UserDataHolderBase;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiUtilCore;
import com.intellij.util.DocumentUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.beans.PropertyChangeListener;
import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class ByteCodeEditor extends UserDataHolderBase implements FileEditor {

    private final JPanel component;
    private FileEditorState textEditorState = new TextEditorState();
    private BCEVirtualFile file;
    private final Editor editor;


    public String getText() {
        return editor.getDocument().getText();
    }


    @Override
    public BCEVirtualFile getFile() {
        return file;
    }


    public ByteCodeEditor(Project project, BCEVirtualFile virtualFile) {
        AnAction[] additionalActions = new AnAction[]{new AssembleAction(this, project, virtualFile)};

        final JPanel panel = new JPanel(new BorderLayout());

        final EditorFactory factory = EditorFactory.getInstance();
        final Document doc = ((EditorFactoryImpl) factory).createDocument("", true, false);
        doc.setReadOnly(false);

        editor = factory.createEditor(doc, project, StdFileTypes.PLAIN_TEXT, false);

        final EditorEx editorEx = (EditorEx) editor;

        EditorHighlighterFactory editorHighlighterFactory = EditorHighlighterFactory.getInstance();
        final SyntaxHighlighter syntaxHighlighter = SyntaxHighlighterFactory
                .getSyntaxHighlighter(StdFileTypes.XML, project, null);
        editorEx.setHighlighter(editorHighlighterFactory.createEditorHighlighter(syntaxHighlighter,
                EditorColorsManager.getInstance()
                        .getGlobalScheme()));
        editorEx.setCaretVisible(true);
        editorEx.setViewer(false);
        editorEx.setInsertMode(true);

        final EditorSettings settings = editor.getSettings();
        settings.setLineMarkerAreaShown(true);
        settings.setIndentGuidesShown(true);
        settings.setLineNumbersShown(true);
        settings.setFoldingOutlineShown(true);

        editor.setBorder(null);
        panel.add(editor.getComponent(), BorderLayout.CENTER);

        final ActionManager actionManager = ActionManager.getInstance();
        final DefaultActionGroup actions = new DefaultActionGroup();
        for (final AnAction action : additionalActions) {
            actions.add(action);
        }
        panel.add(actionManager.createActionToolbar(ActionPlaces.JAVADOC_TOOLBAR, actions, true).getComponent(),
                BorderLayout.NORTH);

        this.component = panel;
        this.file = virtualFile;
        update(virtualFile);
    }


    public void update(BCEVirtualFile virtualFile) {
        try {
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

            try (
                    BufferedInputStream bis = new BufferedInputStream(virtualFile.getInputStream());
                    ByteArrayOutputStream buf = new ByteArrayOutputStream();
            ) {
                int result = bis.read();
                while (result != -1) {
                    buf.write((byte) result);
                    result = bis.read();
                }
                setText(buf.toString(), virtualFile.getElement());
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }


    private void setText(
            @NotNull final String bytecodeXmlString,
            PsiElement element
    ) {
        int offset = 0;

        VirtualFile sourceFile = PsiUtilCore.getVirtualFile(element);
        if (sourceFile != null) {
            final Document document = FileDocumentManager.getInstance().getDocument(sourceFile);
            if (document != null) {
                int lineNumber = document.getLineNumber(element.getTextOffset());
                LineNumbersMapping mapping = sourceFile.getUserData(LineNumbersMapping.LINE_NUMBERS_MAPPING_KEY);
                if (mapping != null) {
                    int mappedLine = mapping.sourceToBytecode(lineNumber);
                    while (mappedLine == -1 && lineNumber < document.getLineCount()) {
                        mappedLine = mapping.sourceToBytecode(++lineNumber);
                    }
                    if (mappedLine > 0) {
                        lineNumber = mappedLine;
                    }
                }
                offset = this.file.getStrategy().getLineOffset(bytecodeXmlString, document, lineNumber);
            }
        }

        setText(bytecodeXmlString, Math.max(0, offset));
    }


    private void setText(final String bytecodeXmlString, final int offset) {
        DocumentUtil.writeInRunUndoTransparentAction(() -> {
            Document fragmentDoc = editor.getDocument();
            fragmentDoc.replaceString(0, fragmentDoc.getTextLength(), bytecodeXmlString);
        });

        editor.getCaretModel().moveToOffset(offset);
        final VisualPosition position = editor.getCaretModel().getVisualPosition();
        editor.getCaretModel().moveToVisualPosition(new VisualPosition(position.line, 0));
        editor.getScrollingModel().scrollToCaret(ScrollType.CENTER);
    }


    @NotNull
    @Override
    public JComponent getComponent() {
        return component;
    }


    @Nullable
    @Override
    public JComponent getPreferredFocusedComponent() {
        return editor.getContentComponent();
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
        EditorFactory.getInstance().releaseEditor(editor);
    }
}
