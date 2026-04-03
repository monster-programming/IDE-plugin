package com.plugin;

import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReference;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.searches.ReferencesSearch;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.uast.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class EmissionSearcher {

    public static List<PsiElement> findEmission(@NotNull UClass uClass, GlobalSearchScope scope) {

        List<PsiElement> targets = new ArrayList<>();
        if (!isCommand(uClass)) return targets;

        PsiClass psiClass = uClass.getJavaPsi();

        ReferencesSearch.search(psiClass, scope)
                        .forEach(usage -> {
                            PsiElement element = usage.getElement();

                            UElement uElement = UastContextKt.toUElement(element);
                            if (uElement == null) return true;

                            // 1. Пропускаем импорты
                            if (UastUtils.getParentOfType(uElement, UImportStatement.class) != null) return true;

                            // 2. Пропускаем type usage
                            if (UastUtils.getParentOfType(uElement, UTypeReferenceExpression.class) != null ||
                                    UastUtils.getParentOfType(uElement, UAnnotation.class) != null) return true;

                            // 3. Конструктор
                            UCallExpression call = UastUtils.getParentOfType(uElement, UCallExpression.class, false);
                            if (call != null) {
                                targets.add(element);
                                return true;
                            }

                            // 4. Object usage
                            if (uElement instanceof USimpleNameReferenceExpression || uElement instanceof UQualifiedReferenceExpression) {
                                UElement parent = uElement.getUastParent();

                                if (parent instanceof UVariable variable) {
                                    if (!uElement.equals(variable.getTypeReference())) {
                                        targets.add(element);
                                    }
                                }
                            }
                            return true;
                        });


        return targets;
    }

    private static boolean isCommand(UClass uClass) {
        List<UTypeReferenceExpression> supers = uClass.getUastSuperTypes();

        if (!supers.isEmpty()) {

            UTypeReferenceExpression firstParent = supers.getFirst();
            String parentName = firstParent.getType().getPresentableText();

            return parentName.contains("Command");
        }
        return false;
    }
}