package org.jetbrains.research.refactoringminer.test;

import com.intellij.testFramework.fixtures.LightJavaCodeInsightFixtureTestCase;
import org.jetbrains.research.refactoringminer.test.counters.AggregateCounter;
import org.jetbrains.research.refactoringminer.test.counters.MistakesCounter;
import org.jetbrains.research.refactoringminer.test.counters.StatisticsCounter;
import org.refactoringminer.api.GitHistoryRefactoringMiner;
import org.refactoringminer.api.RefactoringType;
import org.refactoringminer.rm1.GitHistoryRefactoringMinerImpl;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.jetbrains.research.refactoringminer.test.counters.Counter.Result.FALSE_NEGATIVE;
import static org.jetbrains.research.refactoringminer.test.counters.Counter.Result.FALSE_POSITIVE;
import static org.jetbrains.research.refactoringminer.test.counters.Counter.Result.TRUE_POSITIVE;

public class TestStatistics extends LightJavaCodeInsightFixtureTestCase {

    public static final Path resources = Path.of("src", "test", "resources");
    public static final Path fileOfRemoved = resources.resolve("deleted_commits.txt");

    public static final Path dataFile = resources.resolve("data.json");
    public static final Set<RefactoringType> types = EnumSet.allOf(RefactoringType.class);
    public static final Path foldersRepository = Path.of("tmp1");
    public static final boolean printMistakes = true;

    public static final int expectedTruePositives = 10242;
    public static final int expectedFalsePositives = 166;
    public static final int expectedFalseNegatives = 554;

    public void test() throws IOException, InterruptedException {
        List<CommitRefactorings> data = DataReader.read(dataFile, types);
        data = DataFilter.filterRemoved(fileOfRemoved, data);
        Collection<RepositoryTest> tests = RepositoryTestsBuilder.build(data);
        GitHistoryRefactoringMiner miner = new GitHistoryRefactoringMinerImpl();

        AggregateCounter aggregateCounter = new AggregateCounter();

        StatisticsCounter statisticsCounter = new StatisticsCounter();
        aggregateCounter.register(statisticsCounter);

        MistakesCounter mistakesCounter;
        if (printMistakes) {
            mistakesCounter = new MistakesCounter();
            aggregateCounter.register(mistakesCounter);
        } else {
            mistakesCounter = null;
        }

        runTests(tests, miner, aggregateCounter);

        if (printMistakes) {
            mistakesCounter.printMistakes(System.out);
        }
        statisticsCounter.printStatistics(System.out);

        assertTrue(checkExpected(statisticsCounter.total()));
    }

    private void runTests(Collection<RepositoryTest> tests, GitHistoryRefactoringMiner miner,
                          AggregateCounter aggregateCounter) throws InterruptedException {
        ExecutorService executor = Executors.newFixedThreadPool(4);
        for (RepositoryTest test : tests) {
            executor.submit(() -> test.run(miner, aggregateCounter));
        }
        executor.shutdown();
        executor.awaitTermination(1, TimeUnit.HOURS);
    }

    private boolean checkExpected(StatisticsCounter.Statistics statistics) {
        return statistics.count(TRUE_POSITIVE) == expectedTruePositives
            && statistics.count(FALSE_POSITIVE) == expectedFalsePositives
            && statistics.count(FALSE_NEGATIVE) == expectedFalseNegatives;
    }
}
