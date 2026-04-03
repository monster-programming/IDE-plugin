package com.plugin;

import com.intellij.psi.PsiElement;
import com.intellij.psi.search.GlobalSearchScope;

public class ProjectEventEmissionAction extends BaseEventEmissionAction{
    @Override
    protected GlobalSearchScope getScope(PsiElement element) {
        return ScopeBuilder.getProductionScope(element);
    }
}
