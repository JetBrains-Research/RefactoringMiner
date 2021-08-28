package org.jetbrains.research.refactoringminer.test;

import org.apache.commons.text.similarity.LongestCommonSubsequence;
import org.refactoringminer.api.RefactoringType;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Objects;
import java.util.stream.Collectors;

public class DetectingRefactoring {
    public final RefactoringType type;
    public final String description;

    private DetectingRefactoring(RefactoringType type, String description) {
        this.type = type;
        this.description = preprocessDescription(description);
    }

    /**
     * Split aggregate refactoring into separate ones
     */
    public static Collection<DetectingRefactoring> of(RefactoringType type, String description) {
        int aggregationMarker = description.indexOf("from classes");
        if (aggregationMarker == -1) {
            return Collections.singletonList(new DetectingRefactoring(type, description));
        } else {
            String[] classes = description.substring(aggregationMarker + 14, description.length() - 1).split(", ");
            String prefix = description.substring(0, aggregationMarker) + "from class ";
            return Arrays.stream(classes)
                .map(clazz -> prefix + clazz)
                .map(descr -> new DetectingRefactoring(type, descr))
                .collect(Collectors.toList());
        }
    }

    private static String preprocessDescription(String description) {
        return description.replace('\t', ' ');
    }

    /**
     * Check if refactoring fits to another
     */
    public boolean detect(DetectingRefactoring other) {
        if (description.equals(other.description)) {
            return true;
        }
        if (type.getDisplayName().contains("Annotation")) {
            // Ignore formatting
            String nonSpacedThisDescription = description.replaceAll("\\s", "");
            String nonSpacedOtherDescription = other.description.replaceAll("\\s", "");
            if (nonSpacedThisDescription.equals(nonSpacedOtherDescription)) {
                return true;
            }
        }
        if (type == RefactoringType.RENAME_ATTRIBUTE && other.type == RefactoringType.RENAME_ATTRIBUTE) {
            // Enum names non-qualified in dataset
            int lcs = new LongestCommonSubsequence().apply(description, other.description);
            return lcs == Math.min(description.length(), other.description.length());
        }
        return false;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DetectingRefactoring that = (DetectingRefactoring) o;
        return type == that.type && description.equals(that.description);
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, description);
    }
}
