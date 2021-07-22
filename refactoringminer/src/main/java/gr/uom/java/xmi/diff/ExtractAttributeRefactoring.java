package gr.uom.java.xmi.diff;

import gr.uom.java.xmi.UMLAttribute;
import gr.uom.java.xmi.UMLClass;
import gr.uom.java.xmi.decomposition.AbstractCodeMapping;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.refactoringminer.api.Refactoring;
import org.refactoringminer.api.RefactoringType;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class ExtractAttributeRefactoring implements Refactoring {
	private final UMLAttribute attributeDeclaration;
	private final UMLClass originalClass;
	private final UMLClass nextClass;
	private final Set<AbstractCodeMapping> references;

	public ExtractAttributeRefactoring(UMLAttribute variableDeclaration, UMLClass originalClass, UMLClass nextClass) {
		this.attributeDeclaration = variableDeclaration;
		this.originalClass = originalClass;
		this.nextClass = nextClass;
		this.references = new LinkedHashSet<>();
	}

	public void addReference(AbstractCodeMapping mapping) {
		references.add(mapping);
	}

	public RefactoringType getRefactoringType() {
		return RefactoringType.EXTRACT_ATTRIBUTE;
	}

	public String getName() {
		return this.getRefactoringType().getDisplayName();
	}

	public UMLAttribute getVariableDeclaration() {
		return attributeDeclaration;
	}

	public Set<AbstractCodeMapping> getReferences() {
		return references;
	}

	public String toString() {
		String sb = getName() + "\t" +
			attributeDeclaration +
			" in class " +
			attributeDeclaration.getClassName();
		return sb;
	}

	/**
	 * @return the code range of the extracted variable declaration in the <b>child</b> commit
	 */
	public CodeRange getExtractedVariableDeclarationCodeRange() {
		return attributeDeclaration.codeRange();
	}

	public UMLClass getOriginalClass() {
		return originalClass;
	}

	public UMLClass getNextClass() {
		return nextClass;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((attributeDeclaration == null) ? 0 : attributeDeclaration.hashCode());
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
		ExtractAttributeRefactoring other = (ExtractAttributeRefactoring) obj;
		if (attributeDeclaration == null) {
			return other.attributeDeclaration == null;
		} else return attributeDeclaration.equals(other.attributeDeclaration);
	}

	public Set<ImmutablePair<String, String>> getInvolvedClassesBeforeRefactoring() {
		Set<ImmutablePair<String, String>> pairs = new LinkedHashSet<>();
		pairs.add(new ImmutablePair<>(getOriginalClass().getLocationInfo().getFilePath(), getOriginalClass().getName()));
		return pairs;
	}

	public Set<ImmutablePair<String, String>> getInvolvedClassesAfterRefactoring() {
		Set<ImmutablePair<String, String>> pairs = new LinkedHashSet<>();
		pairs.add(new ImmutablePair<>(getNextClass().getLocationInfo().getFilePath(), getNextClass().getName()));
		return pairs;
	}

	@Override
	public List<CodeRange> leftSide() {
		List<CodeRange> ranges = new ArrayList<>();
		for (AbstractCodeMapping mapping : references) {
			ranges.add(mapping.getFragment1().codeRange().setDescription("statement with the initializer of the extracted attribute"));
		}
		return ranges;
	}

	@Override
	public List<CodeRange> rightSide() {
		List<CodeRange> ranges = new ArrayList<>();
		ranges.add(attributeDeclaration.codeRange()
			.setDescription("extracted attribute declaration")
			.setCodeElement(attributeDeclaration.toString()));
		for (AbstractCodeMapping mapping : references) {
			ranges.add(mapping.getFragment2().codeRange().setDescription("statement with the name of the extracted attribute"));
		}
		return ranges;
	}
}
