package gr.uom.java.xmi.diff;

import gr.uom.java.xmi.UMLClass;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.refactoringminer.api.Refactoring;
import org.refactoringminer.api.RefactoringType;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class ExtractSuperclassRefactoring implements Refactoring {
    private final UMLClass extractedClass;
    private final Set<UMLClass> subclassSet;

    public ExtractSuperclassRefactoring(UMLClass extractedClass, Set<UMLClass> subclassSet) {
        this.extractedClass = extractedClass;
        this.subclassSet = subclassSet;
    }

    public String toString() {
        String sb = getName() + "\t" +
            extractedClass +
            " from classes " +
            subclassSet;
        return sb;
    }

    public String getName() {
        return this.getRefactoringType().getDisplayName();
    }

    public RefactoringType getRefactoringType() {
        if (extractedClass.isInterface())
            return RefactoringType.EXTRACT_INTERFACE;
        else
            return RefactoringType.EXTRACT_SUPERCLASS;
    }

    public Set<String> getSubclassSet() {
        Set<String> subclassSet = new LinkedHashSet<>();
        for (UMLClass umlClass : this.subclassSet) {
            subclassSet.add(umlClass.getName());
        }
        return subclassSet;
    }

    public Set<UMLClass> getUMLSubclassSet() {
        return new LinkedHashSet<>(subclassSet);
    }

    public Set<ImmutablePair<String, String>> getInvolvedClassesBeforeRefactoring() {
        Set<ImmutablePair<String, String>> pairs = new LinkedHashSet<>();
        for (UMLClass umlClass : this.subclassSet) {
            pairs.add(new ImmutablePair<>(umlClass.getLocationInfo().getFilePath(), umlClass.getName()));
        }
        return pairs;
    }

    public Set<ImmutablePair<String, String>> getInvolvedClassesAfterRefactoring() {
        Set<ImmutablePair<String, String>> pairs = new LinkedHashSet<>();
        pairs.add(new ImmutablePair<>(getExtractedClass().getLocationInfo().getFilePath(), getExtractedClass().getName()));
        return pairs;
    }

    public UMLClass getExtractedClass() {
        return extractedClass;
    }

    @Override
    public List<CodeRange> leftSide() {
        List<CodeRange> ranges = new ArrayList<>();
        for (UMLClass subclass : subclassSet) {
            ranges.add(subclass.codeRange()
                .setDescription("sub-type declaration")
                .setCodeElement(subclass.getName()));
        }
        return ranges;
    }

    @Override
    public List<CodeRange> rightSide() {
        List<CodeRange> ranges = new ArrayList<>();
        ranges.add(extractedClass.codeRange()
            .setDescription("extracted super-type declaration")
            .setCodeElement(extractedClass.getName()));
        return ranges;
    }
}
