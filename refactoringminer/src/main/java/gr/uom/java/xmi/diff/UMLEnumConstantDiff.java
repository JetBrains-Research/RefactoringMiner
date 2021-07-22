package gr.uom.java.xmi.diff;

import gr.uom.java.xmi.UMLAnnotation;
import gr.uom.java.xmi.UMLEnumConstant;
import org.refactoringminer.api.Refactoring;
import java.util.LinkedHashSet;
import java.util.Set;

public class UMLEnumConstantDiff {
    private final UMLEnumConstant removedEnumConstant;
    private final UMLEnumConstant addedEnumConstant;
    private final UMLAnnotationListDiff annotationListDiff;
    private boolean renamed;

    public UMLEnumConstantDiff(UMLEnumConstant removedEnumConstant, UMLEnumConstant addedEnumConstant) {
        this.removedEnumConstant = removedEnumConstant;
        this.addedEnumConstant = addedEnumConstant;
        if (!removedEnumConstant.getName().equals(addedEnumConstant.getName()))
            renamed = true;
        this.annotationListDiff = new UMLAnnotationListDiff(removedEnumConstant.getAnnotations(), addedEnumConstant.getAnnotations());
    }

    public UMLEnumConstant getRemovedEnumConstant() {
        return removedEnumConstant;
    }

    public UMLEnumConstant getAddedEnumConstant() {
        return addedEnumConstant;
    }

    public boolean isRenamed() {
        return renamed;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        if (!isEmpty())
            sb.append("\t").append(removedEnumConstant).append("\n");
        if (renamed)
            sb.append("\t").append("renamed from ").append(removedEnumConstant.getName()).append(" to ").append(addedEnumConstant.getName()).append("\n");
        for (UMLAnnotation annotation : annotationListDiff.getRemovedAnnotations()) {
            sb.append("\t").append("annotation ").append(annotation).append(" removed").append("\n");
        }
        for (UMLAnnotation annotation : annotationListDiff.getAddedAnnotations()) {
            sb.append("\t").append("annotation ").append(annotation).append(" added").append("\n");
        }
        for (UMLAnnotationDiff annotationDiff : annotationListDiff.getAnnotationDiffList()) {
            sb.append("\t").append("annotation ").append(annotationDiff.getRemovedAnnotation()).append(" modified to ").append(annotationDiff.getAddedAnnotation()).append("\n");
        }
        return sb.toString();
    }

    public boolean isEmpty() {
        return !renamed && annotationListDiff.isEmpty();
    }

    public Set<Refactoring> getRefactorings() {
        Set<Refactoring> refactorings = new LinkedHashSet<>(getAnnotationRefactorings());
        return refactorings;
    }

    private Set<Refactoring> getAnnotationRefactorings() {
        Set<Refactoring> refactorings = new LinkedHashSet<>();
        for (UMLAnnotation annotation : annotationListDiff.getAddedAnnotations()) {
            AddAttributeAnnotationRefactoring refactoring = new AddAttributeAnnotationRefactoring(annotation, removedEnumConstant, addedEnumConstant);
            refactorings.add(refactoring);
        }
        for (UMLAnnotation annotation : annotationListDiff.getRemovedAnnotations()) {
            RemoveAttributeAnnotationRefactoring refactoring = new RemoveAttributeAnnotationRefactoring(annotation, removedEnumConstant, addedEnumConstant);
            refactorings.add(refactoring);
        }
        for (UMLAnnotationDiff annotationDiff : annotationListDiff.getAnnotationDiffList()) {
            ModifyAttributeAnnotationRefactoring refactoring = new ModifyAttributeAnnotationRefactoring(annotationDiff.getRemovedAnnotation(), annotationDiff.getAddedAnnotation(), removedEnumConstant, addedEnumConstant);
            refactorings.add(refactoring);
        }
        return refactorings;
    }
}
