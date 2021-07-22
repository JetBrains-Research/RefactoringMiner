package gr.uom.java.xmi.diff;

import gr.uom.java.xmi.UMLClass;

public class UMLClassMoveDiff extends UMLClassBaseDiff {

    public UMLClassMoveDiff(UMLClass originalClass, UMLClass movedClass, UMLModelDiff modelDiff) {
        super(originalClass, movedClass, modelDiff);
    }

    public UMLClass getMovedClass() {
        return nextClass;
    }

    public String toString() {
        String sb = "class " +
            originalClass.getName() +
            " was moved to " +
            nextClass.getName() +
            "\n";
        return sb;
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (o instanceof UMLClassMoveDiff) {
            UMLClassMoveDiff classMoveDiff = (UMLClassMoveDiff) o;
            return this.originalClass.equals(classMoveDiff.originalClass) && this.nextClass.equals(classMoveDiff.nextClass);
        }
        return false;
    }
}
