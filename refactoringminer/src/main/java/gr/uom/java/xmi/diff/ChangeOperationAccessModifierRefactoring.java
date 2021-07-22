package gr.uom.java.xmi.diff;

import gr.uom.java.xmi.UMLOperation;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.refactoringminer.api.Refactoring;
import org.refactoringminer.api.RefactoringType;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class ChangeOperationAccessModifierRefactoring implements Refactoring {
    private final String originalAccessModifier;
    private final String changedAccessModifier;
    private final UMLOperation operationBefore;
    private final UMLOperation operationAfter;

    public ChangeOperationAccessModifierRefactoring(String originalAccessModifier, String changedAccessModifier,
                                                    UMLOperation operationBefore, UMLOperation operationAfter) {
        this.originalAccessModifier = originalAccessModifier;
        this.changedAccessModifier = changedAccessModifier;
        this.operationBefore = operationBefore;
        this.operationAfter = operationAfter;
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
        ranges.add(operationBefore.codeRange()
            .setDescription("original method declaration")
            .setCodeElement(operationBefore.toString()));
        return ranges;
    }

    @Override
    public List<CodeRange> rightSide() {
        List<CodeRange> ranges = new ArrayList<>();
        ranges.add(operationAfter.codeRange()
            .setDescription("method declaration with changed access modifier")
            .setCodeElement(operationAfter.toString()));
        return ranges;
    }

    @Override
    public Set<ImmutablePair<String, String>> getInvolvedClassesBeforeRefactoring() {
        Set<ImmutablePair<String, String>> pairs = new LinkedHashSet<>();
        pairs.add(new ImmutablePair<>(getOperationBefore().getLocationInfo().getFilePath(), getOperationBefore().getClassName()));
        return pairs;
    }

    public UMLOperation getOperationBefore() {
        return operationBefore;
    }

    @Override
    public Set<ImmutablePair<String, String>> getInvolvedClassesAfterRefactoring() {
        Set<ImmutablePair<String, String>> pairs = new LinkedHashSet<>();
        pairs.add(new ImmutablePair<>(getOperationAfter().getLocationInfo().getFilePath(), getOperationAfter().getClassName()));
        return pairs;
    }

    public UMLOperation getOperationAfter() {
        return operationAfter;
    }

    public String toString() {
        String sb = getName() + "\t" +
            originalAccessModifier +
            " to " +
            changedAccessModifier +
            " in method " +
            operationAfter +
            " from class " + operationAfter.getClassName();
        return sb;
    }

    @Override
    public String getName() {
        return getRefactoringType().getDisplayName();
    }

    @Override
    public RefactoringType getRefactoringType() {
        return RefactoringType.CHANGE_OPERATION_ACCESS_MODIFIER;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((changedAccessModifier == null) ? 0 : changedAccessModifier.hashCode());
        result = prime * result + ((operationAfter == null) ? 0 : operationAfter.hashCode());
        result = prime * result + ((operationBefore == null) ? 0 : operationBefore.hashCode());
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
        ChangeOperationAccessModifierRefactoring other = (ChangeOperationAccessModifierRefactoring) obj;
        if (changedAccessModifier == null) {
            if (other.changedAccessModifier != null)
                return false;
        } else if (!changedAccessModifier.equals(other.changedAccessModifier))
            return false;
        if (operationAfter == null) {
            if (other.operationAfter != null)
                return false;
        } else if (!operationAfter.equals(other.operationAfter))
            return false;
        if (operationBefore == null) {
            if (other.operationBefore != null)
                return false;
        } else if (!operationBefore.equals(other.operationBefore))
            return false;
        if (originalAccessModifier == null) {
            return other.originalAccessModifier == null;
        } else return originalAccessModifier.equals(other.originalAccessModifier);
    }
}
