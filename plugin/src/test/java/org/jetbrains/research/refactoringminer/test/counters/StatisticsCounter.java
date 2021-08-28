package org.jetbrains.research.refactoringminer.test.counters;

import com.google.common.base.Strings;
import org.jetbrains.research.refactoringminer.test.DetectingRefactoring;
import org.jetbrains.research.refactoringminer.test.GitRepository.GitCommit;
import org.refactoringminer.api.RefactoringType;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.Collection;
import java.util.EnumMap;
import java.util.Map;

import static org.jetbrains.research.refactoringminer.test.counters.Counter.Result.ERROR;
import static org.jetbrains.research.refactoringminer.test.counters.Counter.Result.FALSE_NEGATIVE;
import static org.jetbrains.research.refactoringminer.test.counters.Counter.Result.FALSE_POSITIVE;
import static org.jetbrains.research.refactoringminer.test.counters.Counter.Result.TRUE_POSITIVE;

public class StatisticsCounter implements Counter {
    private final Map<RefactoringType, Statistics> statistics = new EnumMap<>(RefactoringType.class);

    @Override
    public synchronized void newResult(Result result, GitCommit commit, DetectingRefactoring refactoring) {
        statistics.computeIfAbsent(refactoring.type, (ignored) -> new Statistics()).newResult(result);
    }

    public Statistics total() {
        return Statistics.sumOf(statistics.values());
    }

    public void printStatistics(PrintStream out) {
        int maxLength = Arrays.stream(RefactoringType.values())
            .mapToInt(ref -> ref.getDisplayName().length())
            .max().getAsInt();
        printLine(out, toSize("Total", maxLength), total());
        for (Map.Entry<RefactoringType, Statistics> statistic : statistics.entrySet()) {
            printLine(out, toSize(statistic.getKey().getDisplayName(), maxLength), statistic.getValue());
        }
        out.flush();
    }

    private static void printLine(PrintStream out, String title, Statistics total) {
        out.printf("%s TP: %5d  FP: %5d  FN: %5d  Err.: %2d  Prec.: %.3f  Recall: %.3f%n",
            title,
            total.count(TRUE_POSITIVE), total.count(FALSE_POSITIVE),
            total.count(FALSE_NEGATIVE), total.count(ERROR),
            total.precision(), total.recall());
    }

    private static String toSize(String string, int size) {
        return string + Strings.repeat(" ", size - string.length());
    }

    public static class Statistics {
        private final int[] count = new int[Result.values().length];

        public static Statistics sumOf(Collection<Statistics> statistics) {
            Statistics summary = new Statistics();
            for (Statistics statistic : statistics) {
                for (int i = 0; i < summary.count.length; i++) {
                    summary.count[i] += statistic.count[i];
                }
            }
            return summary;
        }

        private void newResult(Result result) {
            count[result.ordinal()]++;
        }

        public int count(Result result) {
            return count[result.ordinal()];
        }

        public double precision() {
            return ((double) count(TRUE_POSITIVE)) / (count(TRUE_POSITIVE) + count(FALSE_POSITIVE));
        }

        public double recall() {
            return ((double) count(TRUE_POSITIVE)) / (count(TRUE_POSITIVE) + count(FALSE_NEGATIVE));
        }
    }
}
