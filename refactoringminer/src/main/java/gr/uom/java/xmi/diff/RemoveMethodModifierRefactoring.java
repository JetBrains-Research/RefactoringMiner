package gr.uom.java.xmi.diff;

import gr.uom.java.xmi.UMLOperation;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.refactoringminer.api.Refactoring;
import org.refactoringminer.api.RefactoringType;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class RemoveMethodModifierRefactoring implements Refactoring {
    private final String modifier;
    private final UMLOperation operationBefore;
    private final UMLOperation operationAfter;

    public RemoveMethodModifierRefactoring(String modifier, UMLOperation operationBefore, UMLOperation operationAfter) {
        this.modifier = modifier;
        this.operationBefore = operationBefore;
        this.operationAfter = operationAfter;
    }

    public String getModifier() {
        return modifier;
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
            .setDescription("method declaration with removed modifier")
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
            modifier +
            " in method " +
            operationBefore +
            " from class " +
            operationBefore.getClassName();
        return sb;
    }

    @Override
    public String getName() {
        return this.getRefactoringType().getDisplayName();
    }

    @Override
    public RefactoringType getRefactoringType() {
        return RefactoringType.REMOVE_METHOD_MODIFIER;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((modifier == null) ? 0 : modifier.hashCode());
        result = prime * result + ((operationAfter == null) ? 0 : operationAfter.hashCode());
        result = prime * result + ((operationBefore == null) ? 0 : operationBefore.hashCode());
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
        RemoveMethodModifierRefactoring other = (RemoveMethodModifierRefactoring) obj;
        if (modifier == null) {
            if (other.modifier != null)
                return false;
        } else if (!modifier.equals(other.modifier))
            return false;
        if (operationAfter == null) {
            if (other.operationAfter != null)
                return false;
        } else if (!operationAfter.equals(other.operationAfter))
            return false;
        if (operationBefore == null) {
            return other.operationBefore == null;
        } else return operationBefore.equals(other.operationBefore);
    }
}
