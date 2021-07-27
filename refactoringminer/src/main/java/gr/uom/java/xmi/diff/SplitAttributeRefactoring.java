package gr.uom.java.xmi.diff;

import gr.uom.java.xmi.UMLAttribute;
import gr.uom.java.xmi.decomposition.VariableDeclaration;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.refactoringminer.api.Refactoring;
import org.refactoringminer.api.RefactoringType;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class SplitAttributeRefactoring implements Refactoring {
    private final UMLAttribute oldAttribute;
    private final Set<UMLAttribute> splitAttributes;
    private final Set<CandidateSplitVariableRefactoring> attributeSplits;
    private final String classNameBefore;
    private final String classNameAfter;

    public SplitAttributeRefactoring(UMLAttribute oldAttribute, Set<UMLAttribute> splitAttributes,
                                     String classNameBefore, String classNameAfter, Set<CandidateSplitVariableRefactoring> attributeSplits) {
        this.oldAttribute = oldAttribute;
        this.splitAttributes = splitAttributes;
        this.classNameBefore = classNameBefore;
        this.classNameAfter = classNameAfter;
        this.attributeSplits = attributeSplits;
    }

    public Set<UMLAttribute> getSplitAttributes() {
        return splitAttributes;
    }

    public Set<CandidateSplitVariableRefactoring> getAttributeSplits() {
        return attributeSplits;
    }

    public String toString() {
        String sb = getName() + "\t" +
            oldAttribute.getVariableDeclaration() +
            " to " +
            getSplitVariables() +
            " in class " + classNameAfter;
        return sb;
    }

    @Override
    public String getName() {
        return this.getRefactoringType().getDisplayName();
    }

    @Override
    public RefactoringType getRefactoringType() {
        return RefactoringType.SPLIT_ATTRIBUTE;
    }

    public Set<VariableDeclaration> getSplitVariables() {
        return splitAttributes.stream().map(UMLAttribute::getVariableDeclaration).collect(Collectors.toCollection(LinkedHashSet::new));
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((classNameAfter == null) ? 0 : classNameAfter.hashCode());
        result = prime * result + ((classNameBefore == null) ? 0 : classNameBefore.hashCode());
        result = prime * result + ((oldAttribute == null || oldAttribute.getVariableDeclaration() == null) ? 0 : oldAttribute.getVariableDeclaration().hashCode());
        Set<VariableDeclaration> splitVariables = getSplitVariables();
        result = prime * result + ((splitVariables.isEmpty()) ? 0 : splitVariables.hashCode());
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
        SplitAttributeRefactoring other = (SplitAttributeRefactoring) obj;
        if (classNameAfter == null) {
            if (other.classNameAfter != null)
                return false;
        } else if (!classNameAfter.equals(other.classNameAfter))
            return false;
        if (classNameBefore == null) {
            if (other.classNameBefore != null)
                return false;
        } else if (!classNameBefore.equals(other.classNameBefore))
            return false;
        if (oldAttribute == null) {
            if (other.oldAttribute != null)
                return false;
        } else if (oldAttribute.getVariableDeclaration() == null) {
            if (other.oldAttribute.getVariableDeclaration() != null)
                return false;
        } else if (!oldAttribute.getVariableDeclaration().equals(other.oldAttribute.getVariableDeclaration()))
            return false;
        if (splitAttributes == null) {
            if (other.splitAttributes != null)
                return false;
        }
        return this.getSplitVariables().equals(other.getSplitVariables());
    }

    @Override
    public Set<ImmutablePair<String, String>> getInvolvedClassesBeforeRefactoring() {
        Set<ImmutablePair<String, String>> pairs = new LinkedHashSet<>();
        pairs.add(new ImmutablePair<>(getOldAttribute().getLocationInfo().getFilePath(), getClassNameBefore()));
        return pairs;
    }

    public UMLAttribute getOldAttribute() {
        return oldAttribute;
    }

    public String getClassNameBefore() {
        return classNameBefore;
    }

    @Override
    public Set<ImmutablePair<String, String>> getInvolvedClassesAfterRefactoring() {
        Set<ImmutablePair<String, String>> pairs = new LinkedHashSet<>();
        for (UMLAttribute splitAttribute : this.splitAttributes) {
            pairs.add(new ImmutablePair<>(splitAttribute.getLocationInfo().getFilePath(), getClassNameAfter()));
        }
        return pairs;
    }

    public String getClassNameAfter() {
        return classNameAfter;
    }

    @Override
    public List<CodeRange> leftSide() {
        List<CodeRange> ranges = new ArrayList<>();
        ranges.add(oldAttribute.getVariableDeclaration().codeRange()
            .setDescription("original attribute declaration")
            .setCodeElement(oldAttribute.getVariableDeclaration().toString()));
        return ranges;
    }

    @Override
    public List<CodeRange> rightSide() {
        List<CodeRange> ranges = new ArrayList<>();
        for (VariableDeclaration splitVariableDeclaration : getSplitVariables()) {
            ranges.add(splitVariableDeclaration.codeRange()
                .setDescription("split attribute declaration")
                .setCodeElement(splitVariableDeclaration.toString()));
        }
        return ranges;
    }
}
