package gr.uom.java.xmi.diff;

import gr.uom.java.xmi.UMLAttribute;
import org.refactoringminer.api.RefactoringType;
import java.util.ArrayList;
import java.util.List;

public class PullUpAttributeRefactoring extends MoveAttributeRefactoring {

    public PullUpAttributeRefactoring(UMLAttribute originalAttribute, UMLAttribute movedAttribute) {
        super(originalAttribute, movedAttribute);
    }

    public String toString() {
        String sb = getName() + "\t" +
            getOriginalAttribute().toQualifiedString() +
            " from class " +
            getSourceClassName() +
            " to " +
            getMovedAttribute().toQualifiedString() +
            " from class " +
            getTargetClassName();
        return sb;
    }

    public RefactoringType getRefactoringType() {
        return RefactoringType.PULL_UP_ATTRIBUTE;
    }

    @Override
    public List<CodeRange> rightSide() {
        List<CodeRange> ranges = new ArrayList<>();
        ranges.add(movedAttribute.codeRange()
            .setDescription("pulled up attribute declaration")
            .setCodeElement(movedAttribute.toString()));
        return ranges;
    }
}
