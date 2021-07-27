package gr.uom.java.xmi.diff;

import gr.uom.java.xmi.UMLClass;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.refactoringminer.api.Refactoring;
import org.refactoringminer.api.RefactoringType;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class ChangeClassAccessModifierRefactoring implements Refactoring {
    private final String originalAccessModifier;
    private final String changedAccessModifier;
    private final UMLClass classBefore;
    private final UMLClass classAfter;

    public ChangeClassAccessModifierRefactoring(String originalAccessModifier, String changedAccessModifier,
                                                UMLClass classBefore, UMLClass classAfter) {
        this.originalAccessModifier = originalAccessModifier;
        this.changedAccessModifier = changedAccessModifier;
        this.classBefore = classBefore;
        this.classAfter = classAfter;
    }

    public String getOriginalAccessModifier() {
        return originalAccessModifier;
    }

    public String getChangedAccessModifier() {
        return changedAccessModifier;
    }

    @Override
    public List<CodeRange> leftSide() {
        List<CodeRange> ranges = new ArrayList<>();
        ranges.add(classBefore.codeRange()
            .setDescription("original class declaration")
            .setCodeElement(classBefore.toString()));
        return ranges;
    }

    @Override
    public List<CodeRange> rightSide() {
        List<CodeRange> ranges = new ArrayList<>();
        ranges.add(classAfter.codeRange()
            .setDescription("class declaration with changed access modifier")
            .setCodeElement(classAfter.toString()));
        return ranges;
    }

    @Override
    public Set<ImmutablePair<String, String>> getInvolvedClassesBeforeRefactoring() {
        Set<ImmutablePair<String, String>> pairs = new LinkedHashSet<>();
        pairs.add(new ImmutablePair<>(getClassBefore().getLocationInfo().getFilePath(), getClassBefore().getName()));
        return pairs;
    }

    public UMLClass getClassBefore() {
        return classBefore;
    }

    @Override
    public Set<ImmutablePair<String, String>> getInvolvedClassesAfterRefactoring() {
        Set<ImmutablePair<String, String>> pairs = new LinkedHashSet<>();
        pairs.add(new ImmutablePair<>(getClassAfter().getLocationInfo().getFilePath(), getClassAfter().getName()));
        return pairs;
    }

    public UMLClass getClassAfter() {
        return classAfter;
    }

    public String toString() {
        String sb = getName() + "\t" +
            originalAccessModifier +
            " to " +
            changedAccessModifier +
            " in class " +
            classAfter.getName();
        return sb;
    }

    @Override
    public String getName() {
        return this.getRefactoringType().getDisplayName();
    }

    @Override
    public RefactoringType getRefactoringType() {
        return RefactoringType.CHANGE_CLASS_ACCESS_MODIFIER;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((changedAccessModifier == null) ? 0 : changedAccessModifier.hashCode());
        result = prime * result + ((classAfter == null) ? 0 : classAfter.hashCode());
        result = prime * result + ((classBefore == null) ? 0 : classBefore.hashCode());
        result = prime * result + ((originalAccessModifier == null) ? 0 : originalAccessModifier.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        ChangeClassAccessModifierRefactoring other = (ChangeClassAccessModifierRefactoring) obj;
        if (changedAccessModifier == null) {
            if (other.changedAccessModifier != null)
                return false;
        } else if (!changedAccessModifier.equals(other.changedAccessModifier))
            return false;
        if (classAfter == null) {
            if (other.classAfter != null)
                return false;
        } else if (!classAfter.equals(other.classAfter))
            return false;
        if (classBefore == null) {
            if (other.classBefore != null)
                return false;
        } else if (!classBefore.equals(other.classBefore))
            return false;
        if (originalAccessModifier == null) {
            return other.originalAccessModifier == null;
        } else return originalAccessModifier.equals(other.originalAccessModifier);
    }
}
