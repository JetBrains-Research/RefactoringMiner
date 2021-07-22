package gr.uom.java.xmi.diff;

import gr.uom.java.xmi.UMLClass;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.refactoringminer.api.Refactoring;
import org.refactoringminer.api.RefactoringType;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class RenameClassRefactoring implements Refactoring {

    private final UMLClass originalClass;
    private final UMLClass renamedClass;

    public RenameClassRefactoring(UMLClass originalClass, UMLClass renamedClass) {
        this.originalClass = originalClass;
        this.renamedClass = renamedClass;
    }

    public String toString() {
		String sb = getName() + "\t" +
			originalClass.getName() +
			" renamed to " +
			renamedClass.getName();
		return sb;
	}

    public String getName() {
        return this.getRefactoringType().getDisplayName();
    }

    public RefactoringType getRefactoringType() {
		return RefactoringType.RENAME_CLASS;
	}

	public String getOriginalClassName() {
		return originalClass.getName();
	}

	public String getRenamedClassName() {
		return renamedClass.getName();
	}

	public UMLClass getOriginalClass() {
		return originalClass;
	}

	public UMLClass getRenamedClass() {
		return renamedClass;
	}

	public Set<ImmutablePair<String, String>> getInvolvedClassesBeforeRefactoring() {
		Set<ImmutablePair<String, String>> pairs = new LinkedHashSet<>();
		pairs.add(new ImmutablePair<>(getOriginalClass().getLocationInfo().getFilePath(), getOriginalClass().getName()));
		return pairs;
	}

	public Set<ImmutablePair<String, String>> getInvolvedClassesAfterRefactoring() {
		Set<ImmutablePair<String, String>> pairs = new LinkedHashSet<>();
		pairs.add(new ImmutablePair<>(getRenamedClass().getLocationInfo().getFilePath(), getRenamedClass().getName()));
		return pairs;
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
		ranges.add(renamedClass.codeRange()
			.setDescription("renamed type declaration")
			.setCodeElement(renamedClass.getName()));
		return ranges;
	}
}
