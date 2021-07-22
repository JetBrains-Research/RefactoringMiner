package gr.uom.java.xmi.diff;

import gr.uom.java.xmi.UMLAnnotation;
import gr.uom.java.xmi.UMLClass;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.refactoringminer.api.Refactoring;
import org.refactoringminer.api.RefactoringType;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class AddClassAnnotationRefactoring implements Refactoring {
	private final UMLAnnotation annotation;
	private final UMLClass classBefore;
	private final UMLClass classAfter;

	public AddClassAnnotationRefactoring(UMLAnnotation annotation, UMLClass classBefore, UMLClass classAfter) {
		this.annotation = annotation;
		this.classBefore = classBefore;
		this.classAfter = classAfter;
	}

	public UMLAnnotation getAnnotation() {
		return annotation;
	}

	public UMLClass getClassBefore() {
		return classBefore;
	}

	public UMLClass getClassAfter() {
		return classAfter;
	}

	@Override
	public List<CodeRange> leftSide() {
		List<CodeRange> ranges = new ArrayList<>();
		ranges.add(classBefore.codeRange()
			.setDescription("original class declaration")
			.setCodeElement(classBefore.toString()));
		return ranges;
	}

	@Override
	public List<CodeRange> rightSide() {
		List<CodeRange> ranges = new ArrayList<>();
		ranges.add(annotation.codeRange()
			.setDescription("added annotation")
			.setCodeElement(annotation.toString()));
		ranges.add(classAfter.codeRange()
			.setDescription("class declaration with added annotation")
			.setCodeElement(classAfter.toString()));
		return ranges;
	}

	@Override
	public RefactoringType getRefactoringType() {
		return RefactoringType.ADD_CLASS_ANNOTATION;
	}

	@Override
	public String getName() {
		return this.getRefactoringType().getDisplayName();
	}

	@Override
	public Set<ImmutablePair<String, String>> getInvolvedClassesBeforeRefactoring() {
		Set<ImmutablePair<String, String>> pairs = new LinkedHashSet<>();
		pairs.add(new ImmutablePair<>(getClassBefore().getLocationInfo().getFilePath(), getClassBefore().getName()));
		return pairs;
	}

	@Override
	public Set<ImmutablePair<String, String>> getInvolvedClassesAfterRefactoring() {
		Set<ImmutablePair<String, String>> pairs = new LinkedHashSet<>();
		pairs.add(new ImmutablePair<>(getClassAfter().getLocationInfo().getFilePath(), getClassAfter().getName()));
		return pairs;
	}

	public String toString() {
		String sb = getName() + "\t" +
			annotation +
			" in class " +
			classAfter.getName();
		return sb;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((annotation == null) ? 0 : annotation.hashCode());
		result = prime * result + ((classAfter == null) ? 0 : classAfter.hashCode());
		result = prime * result + ((classBefore == null) ? 0 : classBefore.hashCode());
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
		AddClassAnnotationRefactoring other = (AddClassAnnotationRefactoring) obj;
		if (annotation == null) {
			if (other.annotation != null)
				return false;
		} else if (!annotation.equals(other.annotation))
			return false;
		if (classAfter == null) {
			if (other.classAfter != null)
				return false;
		} else if (!classAfter.equals(other.classAfter))
			return false;
		if (classBefore == null) {
			return other.classBefore == null;
		} else return classBefore.equals(other.classBefore);
	}
}
