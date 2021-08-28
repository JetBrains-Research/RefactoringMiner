package org.jetbrains.research.refactoringminer.test.counters;

import org.jetbrains.research.refactoringminer.test.DetectingRefactoring;
import org.jetbrains.research.refactoringminer.test.GitRepository.GitCommit;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MistakesCounter implements Counter {
    private final Map<GitCommit, Mistakes> mistakes = new HashMap<>();

    @Override
    public synchronized void newResult(Result result, GitCommit commit, DetectingRefactoring refactoring) {
        mistakes.computeIfAbsent(commit, (ignored) -> new Mistakes()).newResult(result, refactoring);
    }

    public void printMistakes(PrintStream out) {
        for (Map.Entry<GitCommit, Mistakes> mistake : mistakes.entrySet()) {
            if (!mistake.getValue().isEmpty()) {
                out.println("at " + mistake.getKey().getLink());
                mistake.getValue().printMistakes(out);
            }
        }
    }

    private static class Mistakes {
        private final List<DetectingRefactoring> falsePositives = new ArrayList<>();
        private final List<DetectingRefactoring> falseNegatives = new ArrayList<>();

        public void newResult(Result result, DetectingRefactoring refactoring) {
            switch (result) {
                case FALSE_POSITIVE:
                    falsePositives.add(refactoring);
                    break;
                case FALSE_NEGATIVE:
                    falseNegatives.add(refactoring);
                    break;
            }
        }

        public void printMistakes(PrintStream out) {
            if (!falsePositives.isEmpty()) {
                out.println("  false positives:");
                for (DetectingRefactoring falsePositive : falsePositives) {
                    out.println("    " + falsePositive.description);
                }
            }
            if (!falseNegatives.isEmpty()) {
                out.println("  false negatives:");
                for (DetectingRefactoring falseNegative : falseNegatives) {
                    out.println("    " + falseNegative.description);
                }
            }
        }

        public boolean isEmpty() {
            return falsePositives.isEmpty() && falseNegatives.isEmpty();
        }
    }
}
