package com.plugin;

import com.intellij.codeInsight.navigation.NavigationUtil;
import com.intellij.psi.search.searches.ReferencesSearch.SearchParameters;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
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
import org.jetbrains.kotlin.psi.KtImportDirective;
import org.jetbrains.kotlin.psi.KtTypeReference;

import java.util.ArrayList;
import java.util.List;

import static com.intellij.openapi.actionSystem.CommonDataKeys.*;

public class GoToEmissionEventAction extends AnAction {

    @Override
    public void actionPerformed(@NotNull AnActionEvent anActionEvent) {
        Project project = anActionEvent.getProject();
        Editor editor = anActionEvent.getData(EDITOR);
        PsiElement target = anActionEvent.getData(PSI_ELEMENT);

        if (project == null || editor == null || target == null) return;

        GlobalSearchScope searchScope = GlobalSearchScope.projectScope(project);
        ProjectFileIndex fileIndex = ProjectFileIndex.getInstance(project);

        SearchParameters params = new SearchParameters(target, searchScope, true);

        List<PsiElement> emissionPlaces = new ArrayList<>();

        // params of target and scope
        for (PsiReference ref : ReferencesSearch.search(params).findAll()) {
            PsiElement foundElement = ref.getElement();
            VirtualFile file = foundElement.getContainingFile().getVirtualFile();

            if (file == null) continue;

            // find in source files only
            if (!fileIndex.isInSource(file)) continue;

            // exclude tests
            if (fileIndex.isInTestSourceContent(file)) continue;
            String filePath = file.getPath();
            if (filePath.contains("/test/")) {
                continue;
            }

            // exclude imports
            if (PsiTreeUtil.getParentOfType(foundElement, KtImportDirective.class) != null) {
                continue;
            }

            // exclude type ref
            if (PsiTreeUtil.getParentOfType(foundElement, KtTypeReference.class) != null) {
                continue;
            }


            emissionPlaces.add(foundElement);
        }

        // show the result
        if (emissionPlaces.isEmpty()) return;

        NavigationUtil.getPsiElementPopup(
                emissionPlaces.toArray(new PsiElement[0]),
                "Emissions Event (" + emissionPlaces.size() + ")"
        ).showInBestPositionFor(editor);
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.BGT;
    }

}
