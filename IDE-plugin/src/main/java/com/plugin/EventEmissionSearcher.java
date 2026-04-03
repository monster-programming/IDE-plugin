package com.plugin;

import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ProjectFileIndex;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReference;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.searches.ReferencesSearch;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.psi.KtClassOrObject;
import org.jetbrains.kotlin.psi.KtImportDirective;
import org.jetbrains.kotlin.psi.KtTypeReference;

import java.util.ArrayList;
import java.util.List;


public class EventEmissionSearcher {
    public static List<PsiElement> findEmissions(@NotNull KtClassOrObject target, @NotNull GlobalSearchScope scope) {
        List<PsiElement> emissionPlaces = new ArrayList<>();

        for (PsiReference ref : ReferencesSearch.search(target, scope, false).findAll()) {
            PsiElement el = ref.getElement();
            VirtualFile file = el.getContainingFile().getVirtualFile();

            if (file == null) continue;

            // exclude update
            String fileName = file.getName();
            if (fileName.contains("Update")) {
                continue;
            }

            // exclude imports
            if (PsiTreeUtil.getParentOfType(el, KtImportDirective.class) != null) {
                continue;
            }

            // exclude type ref
            if (PsiTreeUtil.getParentOfType(el, KtTypeReference.class) != null) {
                continue;
            }

            emissionPlaces.add(el);
        }
        return emissionPlaces;
    }
}
