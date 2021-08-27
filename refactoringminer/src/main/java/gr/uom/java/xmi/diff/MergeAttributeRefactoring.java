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

public class MergeAttributeRefactoring implements Refactoring {
    private final Set<UMLAttribute> mergedAttributes;
    private final UMLAttribute newAttribute;
    private final Set<CandidateMergeVariableRefactoring> attributeMerges;
    private final String classNameBefore;
    private final String classNameAfter;

    public MergeAttributeRefactoring(Set<UMLAttribute> mergedAttributes, UMLAttribute newAttribute,
                                     String classNameBefore, String classNameAfter, Set<CandidateMergeVariableRefactoring> attributeMerges) {
        this.mergedAttributes = mergedAttributes;
        this.newAttribute = newAttribute;
        this.classNameBefore = classNameBefore;
        this.classNameAfter = classNameAfter;
        this.attributeMerges = attributeMerges;
    }

    public Set<UMLAttribute> getMergedAttributes() {
        return mergedAttributes;
    }

    public Set<CandidateMergeVariableRefactoring> getAttributeMerges() {
        return attributeMerges;
    }

    public String toString() {
        String sb = getName() + "\t" +
            getMergedVariables() +
            " to " +
            newAttribute.getVariableDeclaration() +
            " in class " + classNameAfter;
        return sb;
    }

    public String getName() {
        return this.getRefactoringType().getDisplayName();
    }

    public RefactoringType getRefactoringType() {
        return RefactoringType.MERGE_ATTRIBUTE;
    }

    public Set<VariableDeclaration> getMergedVariables() {
        return mergedAttributes.stream().map(UMLAttribute::getVariableDeclaration).collect(Collectors.toCollection(LinkedHashSet::new));
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((classNameAfter == null) ? 0 : classNameAfter.hashCode());
        result = prime * result + ((classNameBefore == null) ? 0 : classNameBefore.hashCode());
        Set<VariableDeclaration> mergedVariables = getMergedVariables();
        result = prime * result + ((mergedVariables.isEmpty()) ? 0 : mergedVariables.hashCode());
        result = prime * result + ((newAttribute == null || newAttribute.getVariableDeclaration() == null) ? 0 : newAttribute.getVariableDeclaration().hashCode());
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
        MergeAttributeRefactoring other = (MergeAttributeRefactoring) obj;
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
        if (!this.getMergedVariables().equals(other.getMergedVariables()))
            return false;
        if (newAttribute == null) {
            return other.newAttribute == null;
        } else if (newAttribute.getVariableDeclaration() == null) {
            return other.newAttribute.getVariableDeclaration() == null;
        } else return newAttribute.getVariableDeclaration().equals(other.newAttribute.getVariableDeclaration());
    }

    public Set<ImmutablePair<String, String>> getInvolvedClassesBeforeRefactoring() {
        Set<ImmutablePair<String, String>> pairs = new LinkedHashSet<>();
        for (UMLAttribute mergedAttribute : this.mergedAttributes) {
            pairs.add(new ImmutablePair<>(mergedAttribute.getLocationInfo().getFilePath(), getClassNameBefore()));
        }
        return pairs;
    }

    public String getClassNameBefore() {
        return classNameBefore;
    }

    public Set<ImmutablePair<String, String>> getInvolvedClassesAfterRefactoring() {
        Set<ImmutablePair<String, String>> pairs = new LinkedHashSet<>();
        pairs.add(new ImmutablePair<>(getNewAttribute().getLocationInfo().getFilePath(), getClassNameAfter()));
        return pairs;
    }

    public UMLAttribute getNewAttribute() {
        return newAttribute;
    }

    public String getClassNameAfter() {
        return classNameAfter;
    }

    @Override
    public List<CodeRange> leftSide() {
        List<CodeRange> ranges = new ArrayList<>();
        for (VariableDeclaration mergedAttribute : getMergedVariables()) {
            ranges.add(mergedAttribute.codeRange()
                .setDescription("merged attribute declaration")
                .setCodeElement(mergedAttribute.toString()));
        }
        return ranges;
    }

    @Override
    public List<CodeRange> rightSide() {
        List<CodeRange> ranges = new ArrayList<>();
        ranges.add(newAttribute.getVariableDeclaration().codeRange()
            .setDescription("new attribute declaration")
            .setCodeElement(newAttribute.getVariableDeclaration().toString()));
        return ranges;
    }
}
