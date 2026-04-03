package com.plugin;

import com.intellij.codeInsight.navigation.NavigationUtil;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.pom.Navigatable;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiReference;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.psi.*;

import java.util.List;

public abstract class BaseEventEmissionAction extends AnAction {

    protected abstract GlobalSearchScope getScope(PsiElement element);

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        PsiElement element = getTargetElement(e);;

        if (project == null || element == null) return;

        KtClassOrObject ktClass = tryResolveToClass(element);
        if (ktClass == null) return;

        List<PsiElement> targets = EventEmissionSearcher.findEmissions(ktClass, getScope(element));

        if (targets.isEmpty()) return;

        if (targets.size() == 1) {
            ((Navigatable) targets.get(0)).navigate(true);
        } else {
            Editor editor = e.getData(CommonDataKeys.EDITOR);
            if (editor != null) {
                NavigationUtil.getPsiElementPopup(targets.toArray(new PsiElement[0]), "Emissions")
                        .showInBestPositionFor(editor);
            } else {
                NavigationUtil.getPsiElementPopup(targets.toArray(new PsiElement[0]), "Emissions")
                        .showCenteredInCurrentWindow(project);
            }
        }
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
        PsiElement element = getTargetElement(e);
        KtClassOrObject ktClass = tryResolveToClass(element);

        boolean visible = ktClass != null && isEvent(ktClass);
        e.getPresentation().setEnabledAndVisible(visible);
    }

    @Nullable
    private PsiElement getTargetElement(AnActionEvent e) {
        PsiElement element = e.getData(CommonDataKeys.PSI_ELEMENT);
        if (element != null) return element;

        Editor editor = e.getData(CommonDataKeys.EDITOR);
        PsiFile file = e.getData(CommonDataKeys.PSI_FILE);
        if (editor != null && file != null) {
            return file.findElementAt(editor.getCaretModel().getOffset());
        }
        return null;
    }

    private KtClassOrObject tryResolveToClass(PsiElement element) {
        if (element == null) return null;

        PsiElement current = element;
        while (current != null && !(current instanceof KtFile)) {

            if (current instanceof KtClassOrObject) {
                return (KtClassOrObject) current;
            }

            if (current instanceof KtNameReferenceExpression) {
                KtNameReferenceExpression refExpr = (KtNameReferenceExpression) current;

                if (PsiTreeUtil.getParentOfType(refExpr, KtImportDirective.class) != null) return null;

                KtTypeReference typeRef = PsiTreeUtil.getParentOfType(refExpr, KtTypeReference.class);
                if (typeRef != null) {
                    boolean isInsideIs = PsiTreeUtil.getParentOfType(typeRef, KtWhenConditionIsPattern.class) != null ||
                            PsiTreeUtil.getParentOfType(typeRef, KtIsExpression.class) != null;
                    if (!isInsideIs) return null;
                }

                PsiReference reference = refExpr.getReference();
                if (reference != null) {
                    PsiElement resolved = reference.resolve();
                    if (resolved instanceof KtClassOrObject) return (KtClassOrObject) resolved;
                }
            }

            current = current.getParent();
        }
        return null;
    }

    private boolean isEvent(KtClassOrObject ktClass) {
        String name = ktClass.getName();
        // exclude update and handlers
        if (name == null || name.endsWith("Update") || name.endsWith("Handler")) return false;

        // check event
        if (name.endsWith("Event")) return true;

        // check supertypes
        if (ktClass.getSuperTypeList() != null) {
            String cleanSuperType = ktClass.getSuperTypeList().getText().replaceAll("<.*?>", "");
            return cleanSuperType.contains("Event");
        }

        return false;
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() { return ActionUpdateThread.BGT; }
}