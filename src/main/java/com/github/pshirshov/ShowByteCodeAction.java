/*
 * Copyright 2000-2016 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.github.pshirshov;

import com.github.pshirshov.conversion.BytecodeConverter;
import com.github.pshirshov.conversion.xml.XmlDisassembleStrategy;
import com.github.pshirshov.util.BCEVirtualFile;
import com.github.pshirshov.util.PsiUtils;
import com.intellij.icons.AllIcons;
import com.intellij.ide.highlighter.JavaClassFileType;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.compiler.CompilerManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.Computable;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.*;
import com.intellij.psi.util.PsiUtilCore;
import org.jetbrains.annotations.NotNull;

/**
 * @author anna
 * @since 5/4/12
 */
public class ShowByteCodeAction extends AnAction {
    private final XmlDisassembleStrategy disassembleStrategy = new XmlDisassembleStrategy();


    @Override
    public void update(AnActionEvent e) {
        e.getPresentation().setEnabled(false);
        e.getPresentation().setIcon(AllIcons.Toolwindows.Documentation);
        final Project project = e.getData(CommonDataKeys.PROJECT);
        if (project != null) {
            final PsiElement psiElement = PsiUtils.getPsiElement(e.getDataContext(), project, e.getData(CommonDataKeys.EDITOR));
            if (psiElement != null) {
                if ((psiElement.getContainingFile() instanceof PsiClassOwner) &&
                        (PsiUtils.getContainingClass(psiElement) != null)) {
                    e.getPresentation().setEnabled(true);
                }
            }
        }
    }


    @Override
    public void actionPerformed(AnActionEvent e) {
        final DataContext dataContext = e.getDataContext();
        final Project project = e.getProject();
        if (project == null) {
            return;
        }
        final Editor editor = e.getData(CommonDataKeys.EDITOR);

        final PsiElement psiElement = PsiUtils.getPsiElement(dataContext, project, editor);
        if (psiElement == null) {
            return;
        }

        final String psiElementTitle = PsiUtils.getTitle(psiElement);

        final VirtualFile virtualFile = PsiUtilCore.getVirtualFile(psiElement);
        if (virtualFile == null) {
            return;
        }

        final SmartPsiElementPointer element = SmartPointerManager.getInstance(project)
                .createSmartPsiElementPointer(psiElement);

        ProgressManager.getInstance().run(new Task.Backgroundable(project, "Looking for bytecode...") {
            private String myByteCode;
            private String myErrorMessage;
            private String myErrorTitle;


            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                if (ProjectRootManager.getInstance(project).getFileIndex()
                        .isInContent(virtualFile) && isMarkedForCompilation(project, virtualFile)) {
                    myErrorMessage = "Unable to show bytecode for '" + psiElementTitle + "'. Class file does not exist or is out-of-date.";
                    myErrorTitle = "Class File Out-Of-Date";
                } else {
                    myByteCode = ApplicationManager.getApplication().runReadAction(new Computable<String>() {
                        @Override
                        public String compute() {
                            return new BytecodeConverter(disassembleStrategy).getByteCode(psiElement);
                        }
                    });
                }
            }


            @Override
            public void onSuccess() {
                if (project.isDisposed()) {
                    return;
                }

                if ((myErrorMessage != null) && (myTitle != null)) {
                    Messages.showWarningDialog(project, myErrorMessage, myErrorTitle);
                    return;
                }
                final PsiElement targetElement = element.getElement();
                if (targetElement == null) {
                    return;
                }

                if (myByteCode == null) {
                    Messages.showErrorDialog(project, "Unable to parse class file for '" + psiElementTitle + "'.",
                            "Bytecode not Found");
                    return;
                }

                PsiClass psiClass = PsiUtils.getContainingClass(psiElement);

                FileEditorManager manager = FileEditorManager.getInstance(project);

                final String filename = '/' + psiClass.getQualifiedName().replace('.', '/') + ".bc";
                BCEVirtualFile bceVirtualFile = new BCEVirtualFile(
                        filename
                        , JavaClassFileType.INSTANCE
                        , myByteCode.getBytes()
                        , psiElement
                        , disassembleStrategy
                );

                for (FileEditor fileEditor : FileEditorManager.getInstance(project).getAllEditors()) {
                    if (fileEditor instanceof ByteCodeEditor) {
                        final ByteCodeEditor asBce = (ByteCodeEditor) fileEditor;
                        if (asBce.getFile().getPath().equals(bceVirtualFile.getPath())) {
                            FileEditorManager.getInstance(project).openFile(asBce.getFile(), true, true);
                            asBce.update(bceVirtualFile);
                            return;
                        }
                    }
                }

                manager.openFile(bceVirtualFile, true, true);

            }
        });
    }


    private static boolean isMarkedForCompilation(Project project, VirtualFile virtualFile) {
        final CompilerManager compilerManager = CompilerManager.getInstance(project);
        return !compilerManager.isUpToDate(compilerManager.createFilesCompileScope(new VirtualFile[]{virtualFile}));
    }


}
