package org.jetbrains.research.refactoringminer.test.counters;

import org.jetbrains.research.refactoringminer.test.DetectingRefactoring;

import static org.jetbrains.research.refactoringminer.test.GitRepository.GitCommit;

public interface Counter {
    void newResult(Result result, GitCommit commit, DetectingRefactoring refactoring);

    enum Result {
        TRUE_POSITIVE, FALSE_POSITIVE, FALSE_NEGATIVE, ERROR
    }
}
