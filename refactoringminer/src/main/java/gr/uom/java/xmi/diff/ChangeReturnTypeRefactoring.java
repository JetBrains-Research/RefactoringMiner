package gr.uom.java.xmi.diff;

import gr.uom.java.xmi.UMLOperation;
import gr.uom.java.xmi.UMLType;
import gr.uom.java.xmi.decomposition.AbstractCodeMapping;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.refactoringminer.api.Refactoring;
import org.refactoringminer.api.RefactoringType;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class ChangeReturnTypeRefactoring implements Refactoring {
    private final UMLType originalType;
    private final UMLType changedType;
    private final UMLOperation operationBefore;
    private final UMLOperation operationAfter;
    private final Set<AbstractCodeMapping> returnReferences;

    public ChangeReturnTypeRefactoring(UMLType originalType, UMLType changedType,
                                       UMLOperation operationBefore, UMLOperation operationAfter, Set<AbstractCodeMapping> returnReferences) {
        this.originalType = originalType;
        this.changedType = changedType;
        this.operationBefore = operationBefore;
        this.operationAfter = operationAfter;
        this.returnReferences = returnReferences;
    }

    public UMLType getOriginalType() {
        return originalType;
    }

    public UMLType getChangedType() {
        return changedType;
    }

    public Set<AbstractCodeMapping> getReturnReferences() {
        return returnReferences;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        boolean qualified = originalType.equals(changedType) && !originalType.equalsQualified(changedType);
        sb.append(getName()).append("\t");
        sb.append(qualified ? originalType.toQualifiedString() : originalType.toString());
        sb.append(" to ");
        sb.append(qualified ? changedType.toQualifiedString() : changedType.toString());
        sb.append(" in method ");
        sb.append(qualified ? operationAfter.toQualifiedString() : operationAfter.toString());
        sb.append(" from class ").append(operationAfter.getClassName());
        return sb.toString();
    }

    public String getName() {
        return getRefactoringType().getDisplayName();
    }

    public RefactoringType getRefactoringType() {
        return RefactoringType.CHANGE_RETURN_TYPE;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((changedType == null) ? 0 : changedType.hashCode());
        result = prime * result + ((operationAfter == null) ? 0 : operationAfter.hashCode());
        result = prime * result + ((operationBefore == null) ? 0 : operationBefore.hashCode());
        result = prime * result + ((originalType == null) ? 0 : originalType.hashCode());
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
        ChangeReturnTypeRefactoring other = (ChangeReturnTypeRefactoring) obj;
        if (changedType == null) {
            if (other.changedType != null)
                return false;
        } else if (!changedType.equals(other.changedType))
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
        if (originalType == null) {
            return other.originalType == null;
        } else return originalType.equals(other.originalType);
    }

    public Set<ImmutablePair<String, String>> getInvolvedClassesBeforeRefactoring() {
        Set<ImmutablePair<String, String>> pairs = new LinkedHashSet<>();
        pairs.add(new ImmutablePair<>(getOperationBefore().getLocationInfo().getFilePath(), getOperationBefore().getClassName()));
        return pairs;
    }

    public UMLOperation getOperationBefore() {
        return operationBefore;
    }

    public Set<ImmutablePair<String, String>> getInvolvedClassesAfterRefactoring() {
        Set<ImmutablePair<String, String>> pairs = new LinkedHashSet<>();
        pairs.add(new ImmutablePair<>(getOperationAfter().getLocationInfo().getFilePath(), getOperationAfter().getClassName()));
        return pairs;
    }

    public UMLOperation getOperationAfter() {
        return operationAfter;
    }

    @Override
    public List<CodeRange> leftSide() {
        List<CodeRange> ranges = new ArrayList<>();
        ranges.add(originalType.codeRange()
            .setDescription("original return type")
            .setCodeElement(originalType.toString()));
        ranges.add(operationBefore.codeRange()
            .setDescription("original method declaration")
            .setCodeElement(operationBefore.toString()));
        return ranges;
    }

    @Override
    public List<CodeRange> rightSide() {
        List<CodeRange> ranges = new ArrayList<>();
        ranges.add(changedType.codeRange()
            .setDescription("changed return type")
            .setCodeElement(changedType.toString()));
        ranges.add(operationAfter.codeRange()
            .setDescription("method declaration with changed return type")
            .setCodeElement(operationAfter.toString()));
        return ranges;
    }
}
