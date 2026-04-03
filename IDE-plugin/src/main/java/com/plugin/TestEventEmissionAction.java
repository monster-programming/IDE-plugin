package com.plugin;

import com.intellij.psi.PsiElement;
import com.intellij.psi.search.GlobalSearchScope;

public class TestEventEmissionAction extends BaseEventEmissionAction {
    @Override
    protected GlobalSearchScope getScope(PsiElement element) {
        return ScopeBuilder.getTestScope(element);
    }
}