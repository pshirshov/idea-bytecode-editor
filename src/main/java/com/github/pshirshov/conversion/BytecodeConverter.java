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
package com.github.pshirshov.conversion;

import com.github.pshirshov.util.IdeaUtils;
import com.github.pshirshov.util.PsiUtils;
import com.intellij.ide.highlighter.JavaClassFileType;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.CompilerModuleExtension;
import com.intellij.openapi.roots.ProjectFileIndex;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.util.PsiUtil;
import com.intellij.psi.util.PsiUtilCore;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;

/**
 * @author anna
 * @since 5/7/12
 */
public class BytecodeConverter {
    private static final Logger LOG = Logger.getInstance(BytecodeConverter.class);
    private final DisassembleStrategy strategy;


    public BytecodeConverter(DisassembleStrategy strategy) {
        this.strategy = strategy;
    }

    @Nullable
    private String tryGetByteCodeNew(@NotNull PsiFile containingFile) {
        VirtualFile virtualFile = containingFile.getVirtualFile();
        if (
                virtualFile == null
                        ||
                        !(containingFile.getFileType() instanceof JavaClassFileType)
        ) {
            PsiFile originalContainingFile = containingFile.getOriginalFile();
            if (containingFile == originalContainingFile) {
                return null;
            }
            return tryGetByteCodeNew(originalContainingFile);
        }
        String res = null;
        try {
            res = processClassFile(virtualFile.contentsToByteArray());
        } catch (IOException e) {
            e.printStackTrace();
        }
        return res;
    }

    @Nullable
    public String getByteCode(@NotNull PsiElement psiElement) {
        PsiClass containingClass = PsiUtils.getContainingClass(psiElement);
        //todo show popup
        if (containingClass == null) {
            return null;
        }
        final PsiFile containingFile = containingClass.getContainingFile();
        String tryGetByteCodeNewRes = tryGetByteCodeNew(containingFile);
        if (tryGetByteCodeNewRes != null) {
            return tryGetByteCodeNewRes;
        }
        final String classVMName = PsiUtils.getClassVMName(containingClass);
        if (classVMName == null) {
            return null;
        }

        Module module = ModuleUtilCore.findModuleForPsiElement(psiElement);
        if (module == null) {
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
                            final VirtualFile classFile = rootForFile
                                    .findFileByRelativePath("/" + classVMName.replace('.', '/') + ".class");
                            if (classFile != null) {
                                return processClassFile(classFile.contentsToByteArray());
                            }
                        }
                    } catch (IOException e) {
                        LOG.error(e);
                    }
                    return null;
                }
            }
            return null;
        }

        try {
            PsiFile realUsePsiFile = containingFile;
            VirtualFile realUseVirtualFile = realUsePsiFile.getVirtualFile();
            if (realUseVirtualFile == null) {
                realUsePsiFile = containingFile.getOriginalFile();
                realUseVirtualFile = realUsePsiFile.getVirtualFile();
                if (realUseVirtualFile == null) {
                    return null;
                }
            }
            final CompilerModuleExtension moduleExtension = CompilerModuleExtension.getInstance(module);
            if (moduleExtension == null) {
                return null;
            }
            String classPath;
            if (ProjectRootManager.getInstance(module.getProject()).getFileIndex().isInTestSourceContent(realUseVirtualFile)) {
                final VirtualFile pathForTests = moduleExtension.getCompilerOutputPathForTests();
                if (pathForTests == null) {
                    return null;
                }
                classPath = pathForTests.getPath();
            } else {
                final VirtualFile compilerOutputPath = moduleExtension.getCompilerOutputPath();
                if (compilerOutputPath == null) {
                    return null;
                }
                classPath = compilerOutputPath.getPath();
            }

            classPath += "/" + classVMName.replace('.', '/') + ".class";

            final File classFile = new File(classPath);
            if (!classFile.exists()) {
                LOG.info("search in: " + classPath);
                return null;
            }
            return processClassFile(FileUtil.loadFileBytes(classFile));
        } catch (Exception e1) {
            LOG.error(e1);
        }
        return null;
    }


    private String processClassFile(byte[] bytes) {
        try {
            return strategy.disassemble(bytes);
        } catch (Throwable e) {
            IdeaUtils.showErrorNotification("Disassembler failed", e);
            return null;
        }
    }
}
