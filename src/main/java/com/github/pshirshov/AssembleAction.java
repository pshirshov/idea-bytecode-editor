package com.github.pshirshov;

import com.github.pshirshov.conversion.AssembleStrategy;
import com.github.pshirshov.conversion.xml.XmlAssembleStrategy;
import com.github.pshirshov.util.BCEVirtualFile;
import com.github.pshirshov.util.IdeaUtils;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.fileChooser.FileChooser;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import kotlin.text.Charsets;

import java.io.IOException;

class AssembleAction extends AnAction {
    private static final Logger LOG = Logger.getInstance(AssembleAction.class);

    private ByteCodeEditor byteCodeEditor;
    private final Project project;
    private final BCEVirtualFile virtualFile;


    AssembleAction(ByteCodeEditor byteCodeEditor, Project project, BCEVirtualFile virtualFile) {
        this.byteCodeEditor = byteCodeEditor;
        this.project = project;
        this.virtualFile = virtualFile;
    }


    @Override
    public void update(AnActionEvent e) {
        e.getPresentation().setVisible(true);
        e.getPresentation().setIcon(AllIcons.ToolbarDecorator.Export);
        e.getPresentation().setDescription("Compile into class file");
    }


    @Override
    public void actionPerformed(AnActionEvent anActionEvent) {
        final FileChooserDescriptor fcd =
                new FileChooserDescriptor(false, true, false, false, false, false);
        final VirtualFile selection = FileChooser.chooseFile(fcd, project, null);
        final AssembleStrategy strategy = new XmlAssembleStrategy(); //new JasminAssembleStrategy();

        if (selection != null) {
            final String destDir = selection.getPath();
            try {
                LOG.info("Assembling classfile into " + destDir);
                ApplicationManager.getApplication().runWriteAction(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            virtualFile.setBinaryContent(byteCodeEditor.getText().getBytes(Charsets.UTF_8));
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    }
                });

                strategy.assemble(virtualFile, destDir);
            } catch (Throwable e) {
                IdeaUtils.showErrorNotification("Assembler failed", e);
            }
        }
    }
}
