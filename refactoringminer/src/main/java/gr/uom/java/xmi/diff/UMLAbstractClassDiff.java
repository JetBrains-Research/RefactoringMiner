package gr.uom.java.xmi.diff;

import gr.uom.java.xmi.UMLAttribute;
import gr.uom.java.xmi.UMLOperation;
import gr.uom.java.xmi.decomposition.OperationInvocation;
import gr.uom.java.xmi.decomposition.UMLOperationBodyMapper;
import org.refactoringminer.api.Refactoring;
import org.refactoringminer.api.RefactoringMinerTimedOutException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public abstract class UMLAbstractClassDiff {
    protected final List<UMLOperation> addedOperations;
    protected final List<UMLOperation> removedOperations;
    protected final List<UMLAttribute> addedAttributes;
    protected final List<UMLAttribute> removedAttributes;
    protected final List<UMLOperationBodyMapper> operationBodyMapperList;
    protected final List<UMLOperationDiff> operationDiffList;
    protected final List<UMLAttributeDiff> attributeDiffList;
    protected final List<Refactoring> refactorings;
    protected final UMLModelDiff modelDiff;

    public UMLAbstractClassDiff(UMLModelDiff modelDiff) {
        this.addedOperations = new ArrayList<>();
        this.removedOperations = new ArrayList<>();
        this.addedAttributes = new ArrayList<>();
        this.removedAttributes = new ArrayList<>();
        this.operationBodyMapperList = new ArrayList<>();
        this.operationDiffList = new ArrayList<>();
        this.attributeDiffList = new ArrayList<>();
        this.refactorings = new ArrayList<>();
        this.modelDiff = modelDiff;
    }

    public List<UMLOperation> getAddedOperations() {
        return addedOperations;
    }

    public List<UMLOperation> getRemovedOperations() {
        return removedOperations;
    }

    public List<UMLAttribute> getAddedAttributes() {
        return addedAttributes;
    }

    public List<UMLAttribute> getRemovedAttributes() {
        return removedAttributes;
    }

    public List<UMLOperationBodyMapper> getOperationBodyMapperList() {
        return operationBodyMapperList;
    }

    public List<UMLOperationDiff> getOperationDiffList() {
        return operationDiffList;
    }

    public List<UMLAttributeDiff> getAttributeDiffList() {
        return attributeDiffList;
    }

    public abstract void process() throws RefactoringMinerTimedOutException;

    protected abstract void checkForAttributeChanges() throws RefactoringMinerTimedOutException;

    protected abstract void createBodyMappers() throws RefactoringMinerTimedOutException;

    protected boolean isPartOfMethodExtracted(UMLOperation removedOperation, UMLOperation addedOperation) {
        List<OperationInvocation> removedOperationInvocations = removedOperation.getAllOperationInvocations();
        List<OperationInvocation> addedOperationInvocations = addedOperation.getAllOperationInvocations();
        Set<OperationInvocation> intersection = new LinkedHashSet<>(removedOperationInvocations);
        intersection.retainAll(addedOperationInvocations);
        int numberOfInvocationsMissingFromRemovedOperation = new LinkedHashSet<>(removedOperationInvocations).size() - intersection.size();

        Set<OperationInvocation> operationInvocationsInMethodsCalledByAddedOperation = new LinkedHashSet<>();
        for (OperationInvocation addedOperationInvocation : addedOperationInvocations) {
            if (!intersection.contains(addedOperationInvocation)) {
                for (UMLOperation operation : addedOperations) {
                    if (!operation.equals(addedOperation) && operation.getBody() != null) {
                        if (addedOperationInvocation.matchesOperation(operation, addedOperation, modelDiff)) {
                            //addedOperation calls another added method
                            operationInvocationsInMethodsCalledByAddedOperation.addAll(operation.getAllOperationInvocations());
                        }
                    }
                }
            }
        }
        Set<OperationInvocation> newIntersection = new LinkedHashSet<>(removedOperationInvocations);
        newIntersection.retainAll(operationInvocationsInMethodsCalledByAddedOperation);

        Set<OperationInvocation> removedOperationInvocationsWithIntersectionsAndGetterInvocationsSubtracted = new LinkedHashSet<>(removedOperationInvocations);
        removedOperationInvocationsWithIntersectionsAndGetterInvocationsSubtracted.removeAll(intersection);
        removedOperationInvocationsWithIntersectionsAndGetterInvocationsSubtracted.removeAll(newIntersection);
        removedOperationInvocationsWithIntersectionsAndGetterInvocationsSubtracted.removeIf(invocation -> invocation.getMethodName().startsWith("get"));
        int numberOfInvocationsOriginallyCalledByRemovedOperationFoundInOtherAddedOperations = newIntersection.size();
        int numberOfInvocationsMissingFromRemovedOperationWithoutThoseFoundInOtherAddedOperations = numberOfInvocationsMissingFromRemovedOperation - numberOfInvocationsOriginallyCalledByRemovedOperationFoundInOtherAddedOperations;
        return numberOfInvocationsOriginallyCalledByRemovedOperationFoundInOtherAddedOperations > numberOfInvocationsMissingFromRemovedOperationWithoutThoseFoundInOtherAddedOperations ||
            numberOfInvocationsOriginallyCalledByRemovedOperationFoundInOtherAddedOperations > removedOperationInvocationsWithIntersectionsAndGetterInvocationsSubtracted.size();
    }

    protected boolean isPartOfMethodInlined(UMLOperation removedOperation, UMLOperation addedOperation) {
        List<OperationInvocation> removedOperationInvocations = removedOperation.getAllOperationInvocations();
        List<OperationInvocation> addedOperationInvocations = addedOperation.getAllOperationInvocations();
        Set<OperationInvocation> intersection = new LinkedHashSet<>(removedOperationInvocations);
        intersection.retainAll(addedOperationInvocations);
        int numberOfInvocationsMissingFromAddedOperation = new LinkedHashSet<>(addedOperationInvocations).size() - intersection.size();

        Set<OperationInvocation> operationInvocationsInMethodsCalledByRemovedOperation = new LinkedHashSet<>();
        for (OperationInvocation removedOperationInvocation : removedOperationInvocations) {
            if (!intersection.contains(removedOperationInvocation)) {
                for (UMLOperation operation : removedOperations) {
                    if (!operation.equals(removedOperation) && operation.getBody() != null) {
                        if (removedOperationInvocation.matchesOperation(operation, removedOperation, modelDiff)) {
                            //removedOperation calls another removed method
                            operationInvocationsInMethodsCalledByRemovedOperation.addAll(operation.getAllOperationInvocations());
                        }
                    }
                }
            }
        }
        Set<OperationInvocation> newIntersection = new LinkedHashSet<>(addedOperationInvocations);
        newIntersection.retainAll(operationInvocationsInMethodsCalledByRemovedOperation);

        int numberOfInvocationsCalledByAddedOperationFoundInOtherRemovedOperations = newIntersection.size();
        int numberOfInvocationsMissingFromAddedOperationWithoutThoseFoundInOtherRemovedOperations = numberOfInvocationsMissingFromAddedOperation - numberOfInvocationsCalledByAddedOperationFoundInOtherRemovedOperations;
        return numberOfInvocationsCalledByAddedOperationFoundInOtherRemovedOperations > numberOfInvocationsMissingFromAddedOperationWithoutThoseFoundInOtherRemovedOperations;
    }
}
