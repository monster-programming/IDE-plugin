package com.plugin;

import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReference;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.psi.*;

public class KtEventUtil {

    public static boolean isEventClass(KtClassOrObject ktClass) {
        String name = ktClass.getName();
        if (name == null || name.endsWith("Update") || name.endsWith("Handler")) return false;
        if (name.endsWith("Event")) return true;

        KtSuperTypeList superTypeList = ktClass.getSuperTypeList();
        if (superTypeList != null && superTypeList.getText().contains("Event")) return true;

        KtClassOrObject parent = PsiTreeUtil.getParentOfType(ktClass, KtClassOrObject.class);
        return parent != null && parent.getName() != null && parent.getName().endsWith("Event");
    }

    @Nullable
    public static KtClassOrObject tryResolveToClass(PsiElement element) {
        if (element instanceof KtClassOrObject cls) return cls;
        if (element == null || PsiTreeUtil.getParentOfType(element, KtImportDirective.class) != null) return null;

        PsiElement parent = element.getParent();
        if (parent instanceof KtClassOrObject cls && element == cls.getNameIdentifier()) return cls;

        if (parent instanceof KtReferenceExpression ref) {
            PsiReference reference = ref.getReference();
            PsiElement resolved = reference != null ? reference.resolve() : null;
            if (resolved instanceof KtClassOrObject cls) return cls;
            if (resolved instanceof KtConstructor<?> constructor)
                return constructor.getContainingClassOrObject();
        }

        return null;
    }
}