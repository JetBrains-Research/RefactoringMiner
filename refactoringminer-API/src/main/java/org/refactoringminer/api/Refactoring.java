package org.refactoringminer.api;

import com.fasterxml.jackson.core.io.JsonStringEncoder;
import org.apache.commons.lang3.tuple.ImmutablePair;
import java.io.Serializable;
import java.util.Set;

public interface Refactoring extends Serializable, CodeRangeProvider {

    RefactoringType getRefactoringType();

    /**
     * @return a Set of ImmutablePair where left is the file path of a program element, and right is the qualified name of the class containing the program element
     */
    Set<ImmutablePair<String, String>> getInvolvedClassesBeforeRefactoring();

    /**
     * @return a Set of ImmutablePair where left is the file path of a program element, and right is the qualified name of the class containing the program element
     */
    Set<ImmutablePair<String, String>> getInvolvedClassesAfterRefactoring();

    default String toJSON() {
        StringBuilder sb = new StringBuilder();
        JsonStringEncoder encoder = JsonStringEncoder.getInstance();
        sb.append("{").append("\n");
        sb.append("\t").append("\"").append("type").append("\"").append(": ").append("\"").append(getName()).append("\"").append(",").append("\n");
        sb.append("\t").append("\"").append("description").append("\"").append(": ").append("\"");
        encoder.quoteAsString(toString().replace('\t', ' '), sb);
        sb.append("\"").append(",").append("\n");
        sb.append("\t").append("\"").append("leftSideLocations").append("\"").append(": ").append(leftSide()).append(",").append("\n");
        sb.append("\t").append("\"").append("rightSideLocations").append("\"").append(": ").append(rightSide()).append("\n");
        sb.append("}");
        return sb.toString();
    }

    String getName();

    String toString();
}