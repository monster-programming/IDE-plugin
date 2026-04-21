package com.plugin;

import com.intellij.codeInsight.daemon.RelatedItemLineMarkerInfo;
import com.intellij.codeInsight.daemon.RelatedItemLineMarkerProvider;
import com.intellij.codeInsight.navigation.NavigationGutterIconBuilder;
import com.intellij.codeInsight.navigation.impl.PsiTargetPresentationRenderer;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.util.NotNullLazyValue;
import com.intellij.platform.backend.presentation.TargetPresentation;
import com.intellij.psi.PsiElement;
import com.intellij.psi.impl.source.tree.LeafPsiElement;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.psi.*;
import com.intellij.psi.PsiReference;


import java.util.Collection;

public class EventLineMarkerProvider extends RelatedItemLineMarkerProvider {

    @Override
    protected void collectNavigationMarkers(@NotNull PsiElement element,
                                            @NotNull Collection<? super RelatedItemLineMarkerInfo<?>> result) {

        if (!(element instanceof LeafPsiElement)) return;

        PsiElement parent = element.getParent();
        if (parent instanceof KtNameReferenceExpression) {
            PsiElement grandParent = parent.getParent();
            if (grandParent instanceof KtDotQualifiedExpression && ((KtDotQualifiedExpression) grandParent).getReceiverExpression() == parent) return;

            if (grandParent instanceof KtUserType) {
                PsiElement greatGrandParent = grandParent.getParent();
                if (greatGrandParent instanceof KtUserType && ((KtUserType) greatGrandParent).getQualifier() == grandParent) return;
            }
        }

        if (PsiTreeUtil.getParentOfType(element, KtSuperTypeList.class) != null) return;

        KtClassOrObject targetClass = KtEventUtil.tryResolveToClass(element);

        if (targetClass == null || !KtEventUtil.isEventClass(targetClass)) return;

        GlobalSearchScope scope = ScopeBuilder.getProductionScope(element);

        boolean isDeclaration = element.getParent() == targetClass;
        boolean isInsideUpdateFile = element.getContainingFile().getName().contains("Update");

        if (isDeclaration || isInsideUpdateFile) {
            result.add(NavigationGutterIconBuilder.create(AllIcons.Actions.Find)
                    .setTargets(NotNullLazyValue.lazy(() ->
                            EventEmissionSearcher.findEmissions(targetClass, scope)))
                    .setTooltipText("Go to emission")
                    .setEmptyPopupText("No emissions found")
                    .setTargetRenderer(() -> new PsiTargetPresentationRenderer<PsiElement>() {
                        @Override
                        public TargetPresentation getPresentation(@NotNull PsiElement element) {
                            return ContextPresentationProvider.getPresentation(element);
                        }
                    })
                    .createLineMarkerInfo(element));
        }


        if (!isInsideUpdateFile || isDeclaration) {
            result.add(NavigationGutterIconBuilder.create(AllIcons.Actions.Execute)
                    .setTargets(NotNullLazyValue.lazy(() ->
                            EventProcessingSearcher.findProcessing(targetClass, scope)))
                    .setTooltipText("Go to processing")
                    .setEmptyPopupText("No processing found")
                    .setTargetRenderer(() -> new PsiTargetPresentationRenderer<PsiElement>() {
                        @Override
                        public TargetPresentation getPresentation(@NotNull PsiElement element) {
                            return ContextPresentationProvider.getPresentation(element);
                        }
                    })
                    .createLineMarkerInfo(element));
        }
    }
}