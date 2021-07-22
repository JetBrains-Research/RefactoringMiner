package gr.uom.java.xmi.diff;

import gr.uom.java.xmi.UMLAttribute;
import gr.uom.java.xmi.UMLClass;
import gr.uom.java.xmi.UMLOperation;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.refactoringminer.api.Refactoring;
import org.refactoringminer.api.RefactoringType;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class ExtractClassRefactoring implements Refactoring {
    private final UMLClass extractedClass;
    private final UMLClassBaseDiff classDiff;
    private final Set<UMLOperation> extractedOperations;
    private final Set<UMLAttribute> extractedAttributes;
    private final UMLAttribute attributeOfExtractedClassTypeInOriginalClass;

    public ExtractClassRefactoring(UMLClass extractedClass, UMLClassBaseDiff classDiff,
                                   Set<UMLOperation> extractedOperations, Set<UMLAttribute> extractedAttributes, UMLAttribute attributeOfExtractedClassType) {
        this.extractedClass = extractedClass;
        this.classDiff = classDiff;
        this.extractedOperations = extractedOperations;
        this.extractedAttributes = extractedAttributes;
        this.attributeOfExtractedClassTypeInOriginalClass = attributeOfExtractedClassType;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(getName()).append("\t");
        sb.append(extractedClass);
        sb.append(" from class ");
        sb.append(classDiff.getOriginalClass());
        return sb.toString();
    }

	public RefactoringType getRefactoringType() {
		if(extractedClass.isSubTypeOf(classDiff.getOriginalClass()) || extractedClass.isSubTypeOf(classDiff.getNextClass()))
			return RefactoringType.EXTRACT_SUBCLASS;
		return RefactoringType.EXTRACT_CLASS;
	}

	public String getName() {
		return this.getRefactoringType().getDisplayName();
	}

	public UMLClass getExtractedClass() {
		return extractedClass;
	}

	public UMLClass getOriginalClass() {
		return classDiff.getOriginalClass();
	}

	public Set<UMLOperation> getExtractedOperations() {
		return extractedOperations;
	}

	public Set<UMLAttribute> getExtractedAttributes() {
		return extractedAttributes;
	}

	public UMLAttribute getAttributeOfExtractedClassTypeInOriginalClass() {
		return attributeOfExtractedClassTypeInOriginalClass;
	}

	public Set<ImmutablePair<String, String>> getInvolvedClassesBeforeRefactoring() {
		Set<ImmutablePair<String, String>> pairs = new LinkedHashSet<ImmutablePair<String, String>>();
		pairs.add(new ImmutablePair<String, String>(getOriginalClass().getLocationInfo().getFilePath(), getOriginalClass().getName()));
		return pairs;
	}

	public Set<ImmutablePair<String, String>> getInvolvedClassesAfterRefactoring() {
		Set<ImmutablePair<String, String>> pairs = new LinkedHashSet<ImmutablePair<String, String>>();
		pairs.add(new ImmutablePair<String, String>(getExtractedClass().getLocationInfo().getFilePath(), getExtractedClass().getName()));
		return pairs;
	}

	@Override
	public List<CodeRange> leftSide() {
		List<CodeRange> ranges = new ArrayList<CodeRange>();
		ranges.add(classDiff.getOriginalClass().codeRange()
				.setDescription("original type declaration")
				.setCodeElement(classDiff.getOriginalClass().getName()));
		return ranges;
	}

	@Override
	public List<CodeRange> rightSide() {
		List<CodeRange> ranges = new ArrayList<CodeRange>();
		ranges.add(extractedClass.codeRange()
				.setDescription("extracted type declaration")
				.setCodeElement(extractedClass.getName()));
		for(UMLOperation extractedOperation : extractedOperations) {
			ranges.add(extractedOperation.codeRange()
					.setDescription("extracted method declaration")
					.setCodeElement(extractedOperation.toString()));
		}
		for(UMLAttribute extractedAttribute : extractedAttributes) {
			ranges.add(extractedAttribute.codeRange()
					.setDescription("extracted attribute declaration")
					.setCodeElement(extractedAttribute.toString()));
		}
		return ranges;
	}
}
