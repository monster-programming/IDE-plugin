package com.plugin;

import com.intellij.codeInsight.daemon.GutterIconNavigationHandler;
import com.intellij.codeInsight.daemon.RelatedItemLineMarkerInfo;
import com.intellij.codeInsight.daemon.RelatedItemLineMarkerProvider;
import com.intellij.codeInsight.hint.HintManager;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.markup.GutterIconRenderer;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.ui.popup.JBPopup;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.ui.popup.PopupStep;
import com.intellij.openapi.ui.popup.util.BaseListPopupStep;
import com.intellij.pom.Navigatable;
import com.intellij.psi.*;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.ui.awt.RelativePoint;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.uast.*;

import javax.swing.*;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;

public class LineMarkerProviderCommand extends RelatedItemLineMarkerProvider {

    @Override
    protected void collectNavigationMarkers(@NotNull PsiElement element,
                                            @NotNull Collection<? super RelatedItemLineMarkerInfo<?>> result) {
        addClassMarker(element, result);
        addConstructorCallMarker(element, result);
        addObjectMarker(element, result);
        addInHandle(element, result);
    }

    private void addClassMarker(PsiElement element, Collection<? super RelatedItemLineMarkerInfo<?>> result) {
        UClass uCommand = UastContextKt.toUElement(element, UClass.class);

        if (uCommand == null || uCommand.getName() == null) return;

        PsiClass psiClass = uCommand.getJavaPsi();

        if (psiClass.isInterface()) return;

        if (isCommand(psiClass)) {

            PsiElement identifier = psiClass.getNameIdentifier();
            if (identifier == null) identifier = element;

           RelatedItemLineMarkerInfo<PsiElement> emissionMarker = getMarker(identifier, psiClass, AllIcons.Actions.Find, "Emission", EmissionSearcher::findEmission);
           result.add(emissionMarker);

           RelatedItemLineMarkerInfo<PsiElement> processingMarker = getMarker(identifier, psiClass, AllIcons.Actions.Execute, "Processing", ProcessingSearcher::findProcessing);
           result.add(processingMarker);
        }
    }

    private void addConstructorCallMarker(PsiElement element, Collection<? super RelatedItemLineMarkerInfo<?>> result) {
        UElement uElement = UastContextKt.toUElement(element);
        if (!(uElement instanceof UCallExpression callExpression)) return;
        PsiMethod constructor = callExpression.resolve();
        if (constructor != null && constructor.isConstructor()) {
            PsiClass constructedClass = constructor.getContainingClass();
            if (constructedClass != null && isCommand(constructedClass)) {
                UClass uCommand = UastContextKt.toUElement(constructedClass, UClass.class);
                if (uCommand != null) {
                    RelatedItemLineMarkerInfo<PsiElement> marker = getMarker(element, constructedClass, AllIcons.Actions.Execute, "Processing", ProcessingSearcher::findProcessing);
                    result.add(marker);
                }
            }
        }
    }

    private void addObjectMarker(PsiElement element, Collection<? super RelatedItemLineMarkerInfo<?>> result) {
        UElement uElement = UastContextKt.toUElement(element);
        if (uElement instanceof USimpleNameReferenceExpression ref) {
            UElement parent = uElement.getUastParent();
            if (parent == null) return;

            UCallExpression call = UastUtils.getParentOfType(uElement, UCallExpression.class);
            if (call != null) {
                if (call.getValueArguments().contains(uElement) || call.getValueArguments().contains(parent)) {
                    PsiElement res = ref.resolve();
                    if (res instanceof PsiClass psiClass && isCommand(psiClass)) {
                        RelatedItemLineMarkerInfo<PsiElement> marker = getMarker(element, psiClass, AllIcons.Actions.Execute, "Processing", ProcessingSearcher::findProcessing);
                        result.add(marker);
                    }
                }
            }
        }
    }

    private void addInHandle(PsiElement element, Collection<? super RelatedItemLineMarkerInfo<?>> result) {
        UElement uElement = UastContextKt.toUElement(element);

        if (!(uElement instanceof USimpleNameReferenceExpression ref)) return;

        PsiElement resolved = ref.resolve();
        if (!(resolved instanceof PsiClass targetCommand) || targetCommand.isInterface()) return;

        UClass uCommand = UastUtils.getParentOfType(ref, UClass.class);
        if (uCommand == null) return;

        if (!isCommandsHandler(uCommand, targetCommand)) return;


        RelatedItemLineMarkerInfo<PsiElement> marker = getMarker(element, targetCommand, AllIcons.Actions.Find, "Emission", EmissionSearcher::findEmission);

        result.add(marker);
    }

    private RelatedItemLineMarkerInfo<PsiElement> getMarker(PsiElement element, PsiClass targetCommand, Icon icon, String title ,BiFunction<UClass, GlobalSearchScope, List<PsiElement>> searchFunc) {
        GutterIconNavigationHandler<PsiElement> navHandler = ((mouseEvent, element1) -> {
            Editor e = FileEditorManager.getInstance(element1.getProject()).getSelectedTextEditor();
            if (e != null) {
                RelativePoint point = new RelativePoint(mouseEvent);
                showScopePopup(e, targetCommand, element1, title, searchFunc, point);
            }
        });

        return new RelatedItemLineMarkerInfo<>(
                element,
                element.getTextRange(),
                icon,
                elt -> "Go to " + title,
                navHandler,
                GutterIconRenderer.Alignment.CENTER,
                () -> List.of()
        );
    }

    private boolean isCommand(PsiClass psiClass) {
        for (PsiClass superClass : psiClass.getSupers()) {
            String name = superClass.getName();

            if (name != null && name.contains("Command") && !name.contains("CommandsFlowHandler")) return true;
        }

        return false;
    }

    private boolean isCommandsHandler(UClass uClass, PsiClass psiClass) {
        PsiElementFactory factory = JavaPsiFacade.getElementFactory(psiClass.getProject());
        PsiType commandType = factory.createType(psiClass);

        for (UTypeReferenceExpression interfaceRef : uClass.getUastSuperTypes()) {
            PsiType type = interfaceRef.getType();

            if (type instanceof PsiClassType classType) {
                PsiClassType.ClassResolveResult resolveResult = classType.resolveGenerics();
                PsiClass iClass = resolveResult.getElement();

                if (iClass != null && "CommandsFlowHandler".equals(iClass.getName())) {

                    PsiType[] params = classType.getParameters();

                    if (params.length > 0) {
                        PsiType firstParam = params[0];

                        if (firstParam.isAssignableFrom(commandType)) {
                            return true;
                        }
                    }
                }
            }
        }

        return false;
    }

    private Map<String, GlobalSearchScope> buildScope(PsiElement e) {
        Map<String, GlobalSearchScope> scopeMap = new HashMap<>();

        scopeMap.put("Production", ScopeBuilder.getProductionScope(e));
        scopeMap.put("Test", ScopeBuilder.getTestScope(e));
        scopeMap.put("Module", ScopeBuilder.getModuleScope(e));

        return scopeMap;
    }

    private GlobalSearchScope getScope(PsiElement e, String name) {
        return buildScope(e).get(name);
    }
    private void showScopePopup(Editor e, PsiElement targetClass, PsiElement element, String title, BiFunction<UClass, GlobalSearchScope, List<PsiElement>> searchFunc, RelativePoint point) {
        String[] scopes = buildScope(element).keySet().toArray(new String[0]);

        JBPopup popup = JBPopupFactory.getInstance()
                .createListPopup(new BaseListPopupStep<String>("Select Search Scope for " + title , scopes) {
                    @Override
                    public @Nullable PopupStep<?> onChosen(String selectedValue, boolean finalChoice) {
                        return doFinalStep(() -> {
                            GlobalSearchScope scope = getScope(element, selectedValue);
                            UClass uClass = UastContextKt.toUElement(targetClass, UClass.class);
                            List<PsiElement> targets = searchFunc.apply(uClass, scope);
                            navigation(e, targets, "Go to " + title, point);
                        });
                    }
                });
        popup.show(point);
    }

    private void navigation(Editor e, List<PsiElement> targets, String title, RelativePoint point) {
        if (targets.isEmpty()) {
            HintManager.getInstance().showInformationHint(e, "Not found for " + title);
            return;
        }

        if (targets.size() == 1) {
            ((Navigatable) targets.getFirst()).navigate(true);
        }
        else {
            JBPopup popup = JBPopupFactory.getInstance()
                    .createPopupChooserBuilder(targets)
                    .setTitle(title)
                    .setRenderer(ContextPresentationProvider.createCellRender())
                    .setItemChosenCallback(target -> {
                        if (target instanceof Navigatable navigatable) {
                            navigatable.navigate(true);
                        }
                    })
                    .createPopup();

            popup.show(point);
        }
    }
}