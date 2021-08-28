package org.jetbrains.research.refactoringminer.test;

import org.eclipse.jgit.lib.Repository;
import org.jetbrains.research.refactoringminer.test.GitRepository.GitCommit;
import org.jetbrains.research.refactoringminer.test.counters.Counter;
import org.refactoringminer.api.GitHistoryRefactoringMiner;
import org.refactoringminer.api.Refactoring;
import org.refactoringminer.api.RefactoringHandler;
import org.refactoringminer.rm1.GitHistoryRefactoringMinerImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.jetbrains.research.refactoringminer.test.counters.Counter.Result.ERROR;
import static org.jetbrains.research.refactoringminer.test.counters.Counter.Result.FALSE_NEGATIVE;
import static org.jetbrains.research.refactoringminer.test.counters.Counter.Result.FALSE_POSITIVE;
import static org.jetbrains.research.refactoringminer.test.counters.Counter.Result.TRUE_POSITIVE;

public class RepositoryTest {
    private static final Logger logger = LoggerFactory.getLogger(GitHistoryRefactoringMinerImpl.class);
    private final GitRepository repository;
    private final Map<String, Set<DetectingRefactoring>> commitsRefactorings = new HashMap<>();

    public RepositoryTest(List<CommitRefactorings> commitsRefactorings) {
        if (commitsRefactorings.isEmpty()) {
            throw new IllegalArgumentException("Empty list of commits");
        }
        this.repository = commitsRefactorings.get(0).repository;
        for (CommitRefactorings commit : commitsRefactorings) {
            if (!commit.repository.equals(repository)) {
                throw new IllegalArgumentException("Commits from different repositories");
            }
            if (this.commitsRefactorings.containsKey(commit.sha1)) {
                throw new IllegalArgumentException("Duplicate instances for commit");
            }
            this.commitsRefactorings.put(commit.sha1, Set.copyOf(commit.refactorings));
        }
    }

    public void run(GitHistoryRefactoringMiner miner, Counter counter) {
        TestRefactoringsHandler handler = new TestRefactoringsHandler(counter);
        try (Repository jGitRepository = repository.getJGitRepository()) {
            for (String commitId : commitsRefactorings.keySet()) {
                miner.detectAtCommit(jGitRepository, commitId, handler);
            }
        } catch (Exception e) {
            logger.warn(String.format("Ignored repository %s due to error", repository), e);
        }
    }

    private class TestRefactoringsHandler extends RefactoringHandler {
        private final Counter counter;

        private TestRefactoringsHandler(Counter counter) {
            this.counter = counter;
        }

        @Override
        public void handle(String commitId, List<Refactoring> refactorings) {
            Set<DetectingRefactoring> foundRefactorings = refactorings.stream()
                .flatMap(refactoring ->
                    DetectingRefactoring.of(refactoring.getRefactoringType(), refactoring.toString()).stream())
                .collect(Collectors.toUnmodifiableSet());
            Set<DetectingRefactoring> expectedRefactorings = commitsRefactorings.get(commitId);
            Set<DetectingRefactoring> foundSuccessful = new HashSet<>();
            Set<DetectingRefactoring> expectedSuccessful = new HashSet<>();
            for (DetectingRefactoring foundRefactoring : foundRefactorings) {
                for (DetectingRefactoring expectedRefactoring : expectedRefactorings) {
                    if (expectedSuccessful.contains(expectedRefactoring)) {
                        continue;
                    }
                    if (expectedRefactoring.detect(foundRefactoring)) {
                        foundSuccessful.add(foundRefactoring);
                        expectedSuccessful.add(expectedRefactoring);
                        break;
                    }
                }
            }

            GitCommit commit = repository.commit(commitId);
            for (DetectingRefactoring truePositive : expectedSuccessful) {
                counter.newResult(TRUE_POSITIVE, commit, truePositive);
            }
            for (DetectingRefactoring foundRefactoring : foundRefactorings) {
                if (!foundSuccessful.contains(foundRefactoring)) {
                    counter.newResult(FALSE_POSITIVE, commit, foundRefactoring);
                }
            }
            for (DetectingRefactoring expectedRefactoring : expectedRefactorings) {
                if (!expectedSuccessful.contains(expectedRefactoring)) {
                    counter.newResult(FALSE_NEGATIVE, commit, expectedRefactoring);
                }
            }
        }

        @Override
        public void handleException(String commitId, Exception e) {
            Set<DetectingRefactoring> expectedRefactorings = commitsRefactorings.get(commitId);
            GitCommit commit = repository.commit(commitId);
            for (DetectingRefactoring expectedRefactoring : expectedRefactorings) {
                counter.newResult(ERROR, commit, expectedRefactoring);
            }
        }
    }
}
