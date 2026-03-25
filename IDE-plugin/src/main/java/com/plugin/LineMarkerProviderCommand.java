package com.plugin;

import com.intellij.codeInsight.daemon.RelatedItemLineMarkerInfo;
import com.intellij.codeInsight.daemon.RelatedItemLineMarkerProvider;
import com.intellij.codeInsight.navigation.NavigationGutterIconBuilder;
import com.intellij.icons.AllIcons;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.uast.UClass;
import org.jetbrains.uast.UastContextKt;

import java.util.Collection;

public class LineMarkerProviderCommand extends RelatedItemLineMarkerProvider {

    @Override
    protected void collectNavigationMarkers(@NotNull PsiElement element,
                                            @NotNull Collection<? super RelatedItemLineMarkerInfo<?>> result) {

        UClass uClass = UastContextKt.toUElement(element, UClass.class);

        if (uClass == null || uClass.getName() == null) return;

        if (uClass.getName().endsWith("Command")) {

            PsiElement identifier = uClass.getJavaPsi().getNameIdentifier();
            if (identifier == null) identifier = element;

            Collection<PsiElement> targets = EmissionSearcher.findEmission(uClass);

            NavigationGutterIconBuilder<PsiElement> builder =
                    NavigationGutterIconBuilder.create(AllIcons.Actions.Find)
                            .setTargets(targets)
                            .setTooltipText("Go to emission");

            result.add(builder.createLineMarkerInfo(identifier));
        }
    }
}