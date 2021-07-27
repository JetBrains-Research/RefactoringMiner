package gr.uom.java.xmi.diff;

import gr.uom.java.xmi.UMLOperation;
import gr.uom.java.xmi.decomposition.AbstractCodeMapping;
import gr.uom.java.xmi.decomposition.VariableDeclaration;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.refactoringminer.api.Refactoring;
import org.refactoringminer.api.RefactoringType;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class SplitVariableRefactoring implements Refactoring {
    private final Set<VariableDeclaration> splitVariables;
    private final VariableDeclaration oldVariable;
    private final UMLOperation operationBefore;
    private final UMLOperation operationAfter;
    private final Set<AbstractCodeMapping> variableReferences;

    public SplitVariableRefactoring(VariableDeclaration oldVariable, Set<VariableDeclaration> splitVariables,
                                    UMLOperation operationBefore, UMLOperation operationAfter, Set<AbstractCodeMapping> variableReferences) {
        this.splitVariables = splitVariables;
        this.oldVariable = oldVariable;
        this.operationBefore = operationBefore;
        this.operationAfter = operationAfter;
        this.variableReferences = variableReferences;
    }

    public Set<VariableDeclaration> getSplitVariables() {
        return splitVariables;
    }

    public VariableDeclaration getOldVariable() {
        return oldVariable;
    }

    public Set<AbstractCodeMapping> getVariableReferences() {
        return variableReferences;
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
            oldVariable +
            " to " +
            splitVariables +
            " in method " +
            operationAfter +
            " from class " + operationAfter.getClassName();
        return sb;
    }

    @Override
    public String getName() {
        return this.getRefactoringType().getDisplayName();
    }

    @Override
    public RefactoringType getRefactoringType() {
        if (allVariablesAreParameters())
            return RefactoringType.SPLIT_PARAMETER;
        return RefactoringType.SPLIT_VARIABLE;
    }

    private boolean allVariablesAreParameters() {
        for (VariableDeclaration declaration : splitVariables) {
            if (!declaration.isParameter()) {
                return false;
            }
        }
        return oldVariable.isParameter();
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((oldVariable == null) ? 0 : oldVariable.hashCode());
        result = prime * result + ((operationAfter == null) ? 0 : operationAfter.hashCode());
        result = prime * result + ((operationBefore == null) ? 0 : operationBefore.hashCode());
        result = prime * result + ((splitVariables == null) ? 0 : splitVariables.hashCode());
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
        SplitVariableRefactoring other = (SplitVariableRefactoring) obj;
        if (oldVariable == null) {
            if (other.oldVariable != null)
                return false;
        } else if (!oldVariable.equals(other.oldVariable))
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
        if (splitVariables == null) {
            return other.splitVariables == null;
        } else return splitVariables.equals(other.splitVariables);
    }

    @Override
    public List<CodeRange> leftSide() {
        List<CodeRange> ranges = new ArrayList<>();
        ranges.add(oldVariable.codeRange()
            .setDescription("original variable declaration")
            .setCodeElement(oldVariable.toString()));
        ranges.add(operationBefore.codeRange()
            .setDescription("original method declaration")
            .setCodeElement(operationBefore.toString()));
        return ranges;
    }

    @Override
    public List<CodeRange> rightSide() {
        List<CodeRange> ranges = new ArrayList<>();
        for (VariableDeclaration splitVariable : splitVariables) {
            ranges.add(splitVariable.codeRange()
                .setDescription("split variable declaration")
                .setCodeElement(splitVariable.toString()));
        }
        ranges.add(operationAfter.codeRange()
            .setDescription("method declaration with split variable")
            .setCodeElement(operationAfter.toString()));
        return ranges;
    }
}
