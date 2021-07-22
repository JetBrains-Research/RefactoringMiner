package gr.uom.java.xmi.diff;

import gr.uom.java.xmi.UMLClass;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.refactoringminer.api.RefactoringType;
import org.refactoringminer.util.PrefixSuffixUtils;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class MoveClassRefactoring implements PackageLevelRefactoring {
    private final UMLClass originalClass;
    private final UMLClass movedClass;

    public MoveClassRefactoring(UMLClass originalClass, UMLClass movedClass) {
        this.originalClass = originalClass;
        this.movedClass = movedClass;
    }

    public String toString() {
        String sb = getName() + "\t" +
            originalClass.getName() +
            " moved to " +
            movedClass.getName();
        return sb;
    }

    public String getName() {
        return this.getRefactoringType().getDisplayName();
    }

    public RefactoringType getRefactoringType() {
        return RefactoringType.MOVE_CLASS;
    }

    public RenamePattern getRenamePattern() {
        int separatorPos = PrefixSuffixUtils.separatorPosOfCommonSuffix('.', originalClass.getName(), movedClass.getName());
        if (separatorPos == -1) {
            return new RenamePattern(originalClass.getName(), movedClass.getName());
        }
        String originalPath = originalClass.getName().substring(0, originalClass.getName().length() - separatorPos);
        String movedPath = movedClass.getName().substring(0, movedClass.getName().length() - separatorPos);
        return new RenamePattern(originalPath, movedPath);
    }

    public String getOriginalClassName() {
        return originalClass.getName();
    }

    public String getMovedClassName() {
        return movedClass.getName();
    }

    public Set<ImmutablePair<String, String>> getInvolvedClassesBeforeRefactoring() {
        Set<ImmutablePair<String, String>> pairs = new LinkedHashSet<>();
        pairs.add(new ImmutablePair<>(getOriginalClass().getLocationInfo().getFilePath(), getOriginalClass().getName()));
        return pairs;
    }

    public UMLClass getOriginalClass() {
        return originalClass;
    }

    public Set<ImmutablePair<String, String>> getInvolvedClassesAfterRefactoring() {
        Set<ImmutablePair<String, String>> pairs = new LinkedHashSet<>();
        pairs.add(new ImmutablePair<>(getMovedClass().getLocationInfo().getFilePath(), getMovedClass().getName()));
        return pairs;
    }

    public UMLClass getMovedClass() {
        return movedClass;
    }

    @Override
    public List<CodeRange> leftSide() {
        List<CodeRange> ranges = new ArrayList<>();
        ranges.add(originalClass.codeRange()
            .setDescription("original type declaration")
            .setCodeElement(originalClass.getName()));
        return ranges;
    }

    @Override
    public List<CodeRange> rightSide() {
        List<CodeRange> ranges = new ArrayList<>();
        ranges.add(movedClass.codeRange()
            .setDescription("moved type declaration")
            .setCodeElement(movedClass.getName()));
        return ranges;
    }
}
