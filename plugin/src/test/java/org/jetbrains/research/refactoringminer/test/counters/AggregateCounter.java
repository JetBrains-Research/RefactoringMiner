package org.jetbrains.research.refactoringminer.test.counters;

import org.jetbrains.research.refactoringminer.test.DetectingRefactoring;
import org.jetbrains.research.refactoringminer.test.GitRepository.GitCommit;
import java.util.ArrayList;
import java.util.Collection;

public class AggregateCounter implements Counter {
    private final Collection<Counter> counters = new ArrayList<>();

    public void register(Counter counter) {
        counters.add(counter);
    }

    @Override
    public void newResult(Result result, GitCommit commit, DetectingRefactoring refactoring) {
        for (Counter counter : counters) {
            counter.newResult(result, commit, refactoring);
        }
    }
}
