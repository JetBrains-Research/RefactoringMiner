package gr.uom.java.xmi.decomposition.replacement;

import gr.uom.java.xmi.decomposition.ObjectCreation;
import gr.uom.java.xmi.decomposition.OperationInvocation;

public class ClassInstanceCreationWithMethodInvocationReplacement extends Replacement {
    private final ObjectCreation objectCreationBefore;
    private final OperationInvocation invokedOperationAfter;

    public ClassInstanceCreationWithMethodInvocationReplacement(String before, String after, ReplacementType type,
                                                                ObjectCreation objectCreationBefore, OperationInvocation invokedOperationAfter) {
        super(before, after, type);
        this.objectCreationBefore = objectCreationBefore;
        this.invokedOperationAfter = invokedOperationAfter;
    }

    public ObjectCreation getObjectCreationBefore() {
        return objectCreationBefore;
    }

    public OperationInvocation getInvokedOperationAfter() {
        return invokedOperationAfter;
    }

}
