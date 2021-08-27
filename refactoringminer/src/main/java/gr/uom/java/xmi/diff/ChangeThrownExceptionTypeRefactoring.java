package gr.uom.java.xmi.diff;

import gr.uom.java.xmi.UMLOperation;
import gr.uom.java.xmi.UMLType;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.refactoringminer.api.Refactoring;
import org.refactoringminer.api.RefactoringType;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class ChangeThrownExceptionTypeRefactoring implements Refactoring {
    private final Set<UMLType> originalTypes;
    private final Set<UMLType> changedTypes;
    private final UMLOperation operationBefore;
    private final UMLOperation operationAfter;

    public ChangeThrownExceptionTypeRefactoring(Set<UMLType> originalTypes, Set<UMLType> changedTypes,
                                                UMLOperation operationBefore, UMLOperation operationAfter) {
        this.originalTypes = originalTypes;
        this.changedTypes = changedTypes;
        this.operationBefore = operationBefore;
        this.operationAfter = operationAfter;
    }

    public Set<UMLType> getOriginalTypes() {
        return originalTypes;
    }

    public Set<UMLType> getChangedTypes() {
        return changedTypes;
    }

    @Override
    public List<CodeRange> leftSide() {
        List<CodeRange> ranges = new ArrayList<>();
        for (UMLType originalType : originalTypes) {
            ranges.add(originalType.codeRange()
                .setDescription("original exception type")
                .setCodeElement(originalType.toString()));
        }
        ranges.add(operationBefore.codeRange()
            .setDescription("original method declaration")
            .setCodeElement(operationBefore.toString()));
        return ranges;
    }

    @Override
    public List<CodeRange> rightSide() {
        List<CodeRange> ranges = new ArrayList<>();
        for (UMLType changedType : changedTypes) {
            ranges.add(changedType.codeRange()
                .setDescription("changed exception type")
                .setCodeElement(changedType.toString()));
        }
        ranges.add(operationAfter.codeRange()
            .setDescription("method declaration with changed thrown exception type")
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
            (originalTypes.size() == 1 ? originalTypes.iterator().next() : originalTypes) +
            " to " +
            (changedTypes.size() == 1 ? changedTypes.iterator().next() : changedTypes) +
            " in method " +
            operationAfter.toString() +
            " from class " + operationAfter.getClassName();
        return sb;
    }

    @Override
    public String getName() {
        return getRefactoringType().getDisplayName();
    }

    @Override
    public RefactoringType getRefactoringType() {
        return RefactoringType.CHANGE_THROWN_EXCEPTION_TYPE;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((changedTypes == null) ? 0 : changedTypes.hashCode());
        result = prime * result + ((operationAfter == null) ? 0 : operationAfter.hashCode());
        result = prime * result + ((operationBefore == null) ? 0 : operationBefore.hashCode());
        result = prime * result + ((originalTypes == null) ? 0 : originalTypes.hashCode());
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
        ChangeThrownExceptionTypeRefactoring other = (ChangeThrownExceptionTypeRefactoring) obj;
        if (changedTypes == null) {
            if (other.changedTypes != null)
                return false;
        } else if (!changedTypes.equals(other.changedTypes))
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
        if (originalTypes == null) {
            return other.originalTypes == null;
        } else return originalTypes.equals(other.originalTypes);
    }
}
