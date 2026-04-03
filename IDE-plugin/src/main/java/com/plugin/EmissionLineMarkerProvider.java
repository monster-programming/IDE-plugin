package com.plugin;

import com.intellij.codeInsight.daemon.RelatedItemLineMarkerInfo;
import com.intellij.codeInsight.daemon.RelatedItemLineMarkerProvider;
import com.intellij.codeInsight.navigation.NavigationGutterIconBuilder;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.util.NotNullLazyValue;
import com.intellij.psi.PsiElement;
import com.intellij.psi.impl.source.tree.LeafPsiElement;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.psi.*;
import com.intellij.psi.PsiReference;


import java.util.Collection;
import java.util.List;

public class EmissionLineMarkerProvider extends RelatedItemLineMarkerProvider {

    @Override
    protected void collectNavigationMarkers(@NotNull PsiElement element,
                                            @NotNull Collection<? super RelatedItemLineMarkerInfo<?>> result) {

        if (!(element instanceof LeafPsiElement)) return;

        KtClassOrObject targetClass = tryResolveToClass(element);
        if (targetClass == null || !isEventClass(targetClass)) return;

        GlobalSearchScope scope = ScopeBuilder.getProductionScope(element);

        NavigationGutterIconBuilder<PsiElement> builder =
                NavigationGutterIconBuilder.create(AllIcons.Actions.Find)
                        .setTargets(NotNullLazyValue.lazy(() ->
                                EventEmissionSearcher.findEmissions(targetClass, scope)))
                        .setTooltipText("Go to emission");

        result.add(builder.createLineMarkerInfo(element));
    }

    @Nullable
    private KtClassOrObject tryResolveToClass(PsiElement element) {
        PsiElement parent = element.getParent();

        // class event
        if (parent instanceof KtClassOrObject && element == ((KtClassOrObject) parent).getNameIdentifier()) {
            return (KtClassOrObject) parent;
        }

        if (parent instanceof KtNameReferenceExpression) {

            if (PsiTreeUtil.getParentOfType(element, KtImportDirective.class) != null) {
                return null;
            }

            KtTypeReference typeRef = PsiTreeUtil.getParentOfType(element, KtTypeReference.class);
            if (typeRef != null) {
                boolean isInsideIsPattern = PsiTreeUtil.getParentOfType(typeRef, KtWhenConditionIsPattern.class) != null;
                boolean isInsideIsExpression = PsiTreeUtil.getParentOfType(typeRef,  KtIsExpression.class) != null;

                if (!isInsideIsPattern && !isInsideIsExpression) {
                    return null;
                }
            }

            String text = element.getText();
            if (text != null && !text.isEmpty() && Character.isUpperCase(text.charAt(0))) {

                PsiReference reference = parent.getReference();
                if (reference != null) {
                    PsiElement resolved = reference.resolve();
                    if (resolved instanceof KtClassOrObject) {
                        return (KtClassOrObject) resolved;
                    }
                }
            }
        }
        return null;
    }

    private boolean isEventClass(KtClassOrObject ktClass) {
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
}