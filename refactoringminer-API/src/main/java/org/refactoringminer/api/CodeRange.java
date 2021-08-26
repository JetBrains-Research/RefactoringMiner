package org.refactoringminer.api;

public interface CodeRange {
    String getFilePath();

    int getStartLine();

    int getEndLine();

    int getStartColumn();

    int getEndColumn();

    String getDescription();

    String getCodeElement();
}
