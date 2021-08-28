package org.jetbrains.research.refactoringminer.test;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

public class RepositoryTestsBuilder {
    public static Collection<RepositoryTest> build(List<CommitRefactorings> commits) {
        return commits.stream()
            .collect(Collectors.groupingBy(commit -> commit.repository,
                Collectors.collectingAndThen(Collectors.toList(), RepositoryTest::new)))
            .values();
    }
}
