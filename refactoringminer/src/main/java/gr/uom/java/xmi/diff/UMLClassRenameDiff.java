package gr.uom.java.xmi.diff;

import gr.uom.java.xmi.UMLClass;

public class UMLClassRenameDiff extends UMLClassBaseDiff {

    public UMLClassRenameDiff(UMLClass originalClass, UMLClass renamedClass, UMLModelDiff modelDiff) {
        super(originalClass, renamedClass, modelDiff);
    }

    public UMLClass getRenamedClass() {
        return nextClass;
    }

    public boolean samePackage() {
        return originalClass.getPackageName().equals(nextClass.getPackageName());
    }

    public String toString() {
        String sb = "class " +
            originalClass.getName() +
            " was renamed to " +
            nextClass.getName() +
            "\n";
        return sb;
    }
}
