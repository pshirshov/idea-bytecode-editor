package com.github.pshirshov.util;

import com.intellij.byteCodeViewer.ClassSearcher;
import com.intellij.ide.util.JavaAnonymousClassesHelper;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.extensions.ExtensionPointName;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiAnonymousClass;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiCompiledFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiTypeParameter;
import com.intellij.psi.impl.source.tree.injected.InjectedLanguageUtil;
import com.intellij.psi.presentation.java.SymbolPresentationUtil;
import com.intellij.psi.util.ClassUtil;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.psi.util.PsiUtilBase;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class PsiUtils {
    private PsiUtils() {
    }


    private static final ExtensionPointName<ClassSearcher> CLASS_SEARCHER_EP = ExtensionPointName
            .create("ByteCodeViewer.classSearcher");


    public static PsiClass getContainingClass(PsiElement psiElement) {
        for (ClassSearcher searcher : CLASS_SEARCHER_EP.getExtensions()) {
            PsiClass aClass = searcher.findClass(psiElement);
            if (aClass != null) {
                return aClass;
            }
        }
        return findClass(psiElement);
    }


    @Nullable
    public static String getTitle(PsiElement element) {
        PsiClass aClass = getContainingClass(element);
        if (aClass == null) {
            return null;
        }
        return SymbolPresentationUtil.getSymbolPresentableText(aClass);
    }


    @Nullable
    public static String getClassVMName(PsiClass containingClass) {
        if (containingClass instanceof PsiAnonymousClass) {
            final PsiClass containingClassOfAnonymous = PsiTreeUtil.getParentOfType(containingClass, PsiClass.class);
            if (containingClassOfAnonymous == null) {
                return null;
            }
            return getClassVMName(containingClassOfAnonymous) +
                    JavaAnonymousClassesHelper.getName((PsiAnonymousClass) containingClass);
        }
        return ClassUtil.getJVMClassName(containingClass);
    }


    private static PsiClass findClass(@NotNull PsiElement psiElement) {
        PsiClass containingClass = PsiTreeUtil.getParentOfType(psiElement, PsiClass.class, false);
        while (containingClass instanceof PsiTypeParameter) {
            containingClass = PsiTreeUtil.getParentOfType(containingClass, PsiClass.class);
        }
        if (containingClass == null) {
            return null;
        }

        return containingClass;
    }


    @Nullable
    public static PsiElement getPsiElement(DataContext dataContext, Project project, Editor editor) {
        PsiElement psiElement = null;
        if (editor == null) {
            psiElement = (PsiElement) dataContext.getData(CommonDataKeys.PSI_ELEMENT.getName());
        } else {
            final PsiFile file = PsiUtilBase.getPsiFileInEditor(editor, project);
            final Editor injectedEditor = InjectedLanguageUtil.getEditorForInjectedLanguageNoCommit(editor, file);
            if (injectedEditor != null) {
                psiElement = findElementInFile(PsiUtilBase.getPsiFileInEditor(injectedEditor, project), injectedEditor);
            }

            if (file != null && psiElement == null) {
                psiElement = findElementInFile(file, editor);
            }
        }

        return psiElement;
    }


    private static PsiElement findElementInFile(@Nullable PsiFile psiFile, Editor editor) {
        if (psiFile == null) {
            return null;
        }
        if (psiFile instanceof PsiCompiledFile) {
            psiFile = ((PsiCompiledFile) psiFile).getDecompiledPsiFile();
        }
        return psiFile.findElementAt(editor.getCaretModel().getOffset());
    }
}
