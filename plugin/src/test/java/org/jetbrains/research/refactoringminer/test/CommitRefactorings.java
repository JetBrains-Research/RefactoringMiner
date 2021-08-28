package org.jetbrains.research.refactoringminer.test;

import java.util.List;

public class CommitRefactorings {
    public final GitRepository repository;
    public final String sha1;
    public final List<DetectingRefactoring> refactorings;

    public CommitRefactorings(String repository, String sha1, List<DetectingRefactoring> refactorings) {
        this.repository = new GitRepository(repository);
        this.sha1 = sha1;
        this.refactorings = refactorings;
    }
}

