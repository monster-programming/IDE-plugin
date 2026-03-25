package com.plugin;

import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReference;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.searches.ReferencesSearch;
import com.intellij.util.Query;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.uast.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class EmissionSearcher {

    public static Collection<PsiElement> findEmission(@NotNull UClass uClass) {
        List<PsiElement> targets = new ArrayList<>();

        PsiClass psiClass = uClass.getJavaPsi();
        if (psiClass == null) return targets;

        Query<PsiReference> query =
                ReferencesSearch.search(psiClass, GlobalSearchScope.projectScope(psiClass.getProject()));

        for (PsiReference ref : query.findAll()) {
            PsiElement element = ref.getElement();

            UElement uElement = UastContextKt.toUElement(element);
            if (uElement == null) continue;

            // 1. Пропускаем импорты
            if (UastUtils.getParentOfType(uElement, UImportStatement.class) != null) continue;

            // 2. Пропускаем type usage
            if (isTypeUsage(uElement)) continue;

            // 3. Конструктор
            UCallExpression call = UastUtils.getParentOfType(uElement, UCallExpression.class);
            if (call != null) {
                targets.add(element);
                continue;
            }

            // 4. Object usage
            if (uElement instanceof USimpleNameReferenceExpression ||
                    uElement instanceof UQualifiedReferenceExpression) {
                targets.add(element);
            }
        }

        return targets;
    }

    private static boolean isTypeUsage(UElement element) {
        return UastUtils.getParentOfType(element, UTypeReferenceExpression.class) != null ||
                UastUtils.getParentOfType(element, UAnnotation.class) != null;
    }
}