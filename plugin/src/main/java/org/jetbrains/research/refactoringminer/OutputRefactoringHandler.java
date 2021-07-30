package org.jetbrains.research.refactoringminer;

import org.jetbrains.annotations.NotNull;
import org.refactoringminer.api.Refactoring;
import org.refactoringminer.api.RefactoringHandler;
import org.refactoringminer.rm1.GitHistoryRefactoringMinerImpl;
import java.io.IOException;
import java.util.List;

public class OutputRefactoringHandler extends RefactoringHandler {
    private final boolean verbose;
    @NotNull
    private final JsonOutput output;
    @NotNull
    private final String repositoryURL;

    public OutputRefactoringHandler(@NotNull String repositoryURL, @NotNull JsonOutput output, boolean verbose) {
        this.verbose = verbose;
        this.output = output;
        this.repositoryURL = repositoryURL;
    }

    @Override
    public void handle(String revisionId, List<Refactoring> refactorings) {
        String revisionURL = GitHistoryRefactoringMinerImpl.extractCommitURL(repositoryURL, revisionId);
        try {
            output.commit(repositoryURL, revisionURL, revisionId, refactorings);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onFinish(int refactoringsCount, int commitsCount, int errorCommitsCount) {
        if (verbose) {
            System.out.printf("Total count: [Commits: %d, Errors: %d, Refactorings: %d]%n",
                commitsCount, errorCommitsCount, refactoringsCount);
        }
    }

    @Override
    public void handleException(String commit, Exception e) {
        System.err.println("Error processing commit " + commit);
        e.printStackTrace();
    }
}
