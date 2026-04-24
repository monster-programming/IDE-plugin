package com.plugin;

import com.intellij.psi.PsiElement;
import com.intellij.psi.search.GlobalSearchScope;
import org.jetbrains.uast.*;

import java.util.List;
import java.util.Map;

public class EmissionAction extends BaseAction {
    @Override
    protected List<PsiElement> findTargets(PsiElement targetClass, GlobalSearchScope scope) {
        UClass uClass = UastContextKt.toUElement(targetClass, UClass.class);
        if (uClass == null) {
            return List.of();
        }
        return EmissionSearcher.findEmission(uClass, scope);
    }

    @Override
    protected String getTitle() {
        return "Command Emission";
    }

    @Override
    protected String getOperation() {
        return "Emission";
    }

    @Override
    protected void createLog(String className, int sizeSearch) {
        AnalyticsService.log("hot-key", Map.of(
                "type", "Command",
                "feature", "Go to Emission",
                "name", className,
                "size search", sizeSearch
        ));
    }

    @Override
    protected PsiElement findTargetClass(PsiElement element) {
        UElement uElement = UastContextKt.toUElement(element);
        UClass uClass = null;

        if (uElement instanceof UMethod method && method.isConstructor()) {
            uClass = UastUtils.getParentOfType(uElement, UClass.class);
        }
        if (uClass == null) {
            uClass = UastUtils.getParentOfType(uElement, UClass.class, false);
        }

        return uClass != null ? uClass.getSourcePsi() : null;
    }
}
