/*
 * Copyright 2000-2014 JetBrains s.r.o.
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
package com.github.pshirshov.byteCodeViewer;

import com.github.pshirshov.org.objectweb.asm.ClassReader;
import com.github.pshirshov.org.objectweb.asm.util.Textifier;
import com.github.pshirshov.org.objectweb.asm.util.TraceClassVisitor;
import com.intellij.byteCodeViewer.ClassSearcher;
import com.intellij.ide.util.JavaAnonymousClassesHelper;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.extensions.ExtensionPointName;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.CompilerModuleExtension;
import com.intellij.openapi.roots.ProjectFileIndex;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiAnonymousClass;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiTypeParameter;
import com.intellij.psi.presentation.java.SymbolPresentationUtil;
import com.intellij.psi.util.ClassUtil;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.psi.util.PsiUtil;
import com.intellij.psi.util.PsiUtilCore;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;

/**
 * @author anna
 * @since 5/7/12
 */
public class ByteCodeViewerManager {
  private static final ExtensionPointName<ClassSearcher> CLASS_SEARCHER_EP = ExtensionPointName.create("ByteCodeViewer.classSearcher");
  private static final Logger LOG = Logger.getInstance(ByteCodeViewerManager.class);


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
    if (aClass == null) return null;
    return SymbolPresentationUtil.getSymbolPresentableText(aClass);
  }

  @Nullable
  public static String getByteCode(@NotNull PsiElement psiElement) {
    PsiClass containingClass = getContainingClass(psiElement);
    //todo show popup
    if (containingClass == null) return null;
    final String classVMName = getClassVMName(containingClass);
    if (classVMName == null) return null;

    Module module = ModuleUtilCore.findModuleForPsiElement(psiElement);
    if (module == null){
      final Project project = containingClass.getProject();
      final PsiClass topLevelClass = PsiUtil.getTopLevelClass(psiElement);
      final String qualifiedName = topLevelClass != null ? topLevelClass.getQualifiedName() : null;
      final PsiClass aClass = qualifiedName != null 
                              ? JavaPsiFacade.getInstance(project).findClass(qualifiedName, psiElement.getResolveScope()) 
                              : null;
      if (aClass != null) {
        final VirtualFile virtualFile = PsiUtilCore.getVirtualFile(aClass);
        final ProjectFileIndex fileIndex = ProjectRootManager.getInstance(project).getFileIndex();
        if (virtualFile != null && fileIndex.isInLibraryClasses(virtualFile)) {
          try {
            final VirtualFile rootForFile = fileIndex.getClassRootForFile(virtualFile);
            if (rootForFile != null) {
              final VirtualFile classFile = rootForFile.findFileByRelativePath("/" + classVMName.replace('.', '/') + ".class");
              if (classFile != null) {
                return processClassFile(classFile.contentsToByteArray());
              }
            }
          }
          catch (IOException e) {
            LOG.error(e);
          }
          return null;
        }
      }
      return null;
    }

    try {
      final PsiFile containingFile = containingClass.getContainingFile();
      final VirtualFile virtualFile = containingFile.getVirtualFile();
      if (virtualFile == null) return null;
      final CompilerModuleExtension moduleExtension = CompilerModuleExtension.getInstance(module);
      if (moduleExtension == null) return null;
      String classPath;
      if (ProjectRootManager.getInstance(module.getProject()).getFileIndex().isInTestSourceContent(virtualFile)) {
        final VirtualFile pathForTests = moduleExtension.getCompilerOutputPathForTests();
        if (pathForTests == null) return null;
        classPath = pathForTests.getPath();
      } else {
        final VirtualFile compilerOutputPath = moduleExtension.getCompilerOutputPath();
        if (compilerOutputPath == null) return null;
        classPath = compilerOutputPath.getPath();
      }

      classPath += "/" + classVMName.replace('.', '/') + ".class";

      final File classFile = new File(classPath);
      if (!classFile.exists()) {
        LOG.info("search in: " + classPath);
        return null;
      }
      return processClassFile(FileUtil.loadFileBytes(classFile));
    }
    catch (Exception e1) {
      LOG.error(e1);
    }
    return null;
  }

  private static String processClassFile(byte[] bytes) {
    final ClassReader classReader = new ClassReader(bytes);
    final StringWriter writer = new StringWriter();

    try(final PrintWriter printWriter = new PrintWriter(writer)) {
      if (false) { // TODO: another assembly format
        classReader.accept(new TraceClassVisitor(null, new Textifier(), printWriter), 0);
      } else {
        int flags = ClassReader.EXPAND_FRAMES;
        classReader.accept(new JasminifierClassAdapter(printWriter), flags);
      }
    } catch (Throwable e) {
      throw e; // TODO: better signaling
//      String message = "ASM+Jasmin disassembler failed: "+e.getMessage();
//      JBPopupFactory.getInstance()
//                    .createHtmlTextBalloonBuilder(message, MessageType.ERROR, null)
//                    .createBalloon()
//                    .showInCenterOf(editor.getComponent());
    }
    return writer.toString();
  }

  @Nullable
  private static String getClassVMName(PsiClass containingClass) {
    if (containingClass instanceof PsiAnonymousClass) {
      final PsiClass containingClassOfAnonymous = PsiTreeUtil.getParentOfType(containingClass, PsiClass.class);
      if (containingClassOfAnonymous == null) {
        return null;
      }
      return getClassVMName(containingClassOfAnonymous) +
             JavaAnonymousClassesHelper.getName((PsiAnonymousClass)containingClass);
    }
    return ClassUtil.getJVMClassName(containingClass);
  }

  private static PsiClass findClass(@NotNull PsiElement psiElement) {
    PsiClass containingClass = PsiTreeUtil.getParentOfType(psiElement, PsiClass.class, false);
    while (containingClass instanceof PsiTypeParameter) {
      containingClass = PsiTreeUtil.getParentOfType(containingClass, PsiClass.class);
    }
    if (containingClass == null) return null;

    return containingClass;
  }
}
