package gr.uom.java.xmi.diff;

import gr.uom.java.xmi.UMLClass;
import org.refactoringminer.api.Refactoring;

public interface PackageLevelRefactoring extends Refactoring {
    RenamePattern getRenamePattern();

    UMLClass getOriginalClass();

    UMLClass getMovedClass();

    String getOriginalClassName();

    String getMovedClassName();
}
