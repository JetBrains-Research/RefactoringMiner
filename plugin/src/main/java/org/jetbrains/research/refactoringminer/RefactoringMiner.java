package org.jetbrains.research.refactoringminer;

import com.google.common.base.Strings;
import com.intellij.openapi.application.ApplicationStarter;
import org.eclipse.jgit.lib.Repository;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.refactoringminer.api.GitHistoryRefactoringMiner;
import org.refactoringminer.api.GitService;
import org.refactoringminer.rm1.GitHistoryRefactoringMinerImpl;
import org.refactoringminer.util.GitServiceImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

public class RefactoringMiner implements ApplicationStarter {
    private static final int OPERATION = 1;
    private static final int PROJECT_PATH = 2;
    private static final int START_POSITION = 3;
    private static final int END_POSITION = 4;
    private static final int TIMEOUT = 5;
    private static final int JSON = 6;
    private static final Logger logger = LoggerFactory.getLogger(RefactoringMiner.class);

    @Override
    public String getCommandName() {
        return "RefactoringMiner";
    }

    @Override
    public void main(@NotNull List<String> args) {
        args = args.stream().map(Strings::emptyToNull).collect(Collectors.toList());
        Path jsonPath = args.get(JSON) != null ? Path.of(args.get(JSON)) : null;
        try (JsonOutput out = new JsonOutput(jsonPath)) {
            switch (args.get(OPERATION)) {
                case "h":
                case "help":
                    printTips();
                    break;
                case "detectAll":
                    detectAll(out, args.get(PROJECT_PATH), args.get(START_POSITION));
                    break;
                case "detectBetweenCommits":
                    detectBetweenCommits(out,
                        args.get(PROJECT_PATH), args.get(START_POSITION), args.get(END_POSITION));
                    break;
                case "detectBetweenTags":
                    detectBetweenTags(out, args.get(PROJECT_PATH), args.get(START_POSITION), args.get(END_POSITION));
                    break;
                case "detectAtCommit":
                    detectAtCommit(out, args.get(PROJECT_PATH), args.get(START_POSITION));
                    break;
                case "detectAtGitHubCommit":
                    detectAtGitHubCommit(out,
                        args.get(PROJECT_PATH), args.get(START_POSITION), Integer.parseInt(args.get(TIMEOUT)));
                    break;
                case "detectAtGitHubPullRequest":
                    detectAtGitHubPullRequest(out, args.get(PROJECT_PATH),
                        Integer.parseInt(args.get(START_POSITION)), Integer.parseInt(args.get(TIMEOUT)));
                    break;
                default:
                    System.out.println("Unknown command.");
                    System.out.println("Call help to show usage.");
            }
        } catch (IllegalArgumentException e) {
            System.out.println("Call help to show usage.");
            System.out.println(e.getMessage());
            logger.error("Error processing arguments", e);
        } catch (Exception e) {
            System.out.println(e.getMessage());
            logger.error("Error processing arguments", e);
        }
        System.exit(0);
    }

    private void detectAll(@NotNull JsonOutput out, @NotNull String folder, @Nullable String branch) throws Exception {
        GitService gitService = new GitServiceImpl();
        try (Repository repo = gitService.openRepository(folder)) {
            String gitURL = repo.getConfig().getString("remote", "origin", "url");
            GitHistoryRefactoringMiner detector = new GitHistoryRefactoringMinerImpl();
            OutputRefactoringHandler handler = new OutputRefactoringHandler(gitURL, out, true);
            detector.detectAll(repo, branch, handler);
        }
    }

    private void detectBetweenCommits(@NotNull JsonOutput out, @NotNull String folder,
                                      @NotNull String startCommit, @NotNull String endCommit) throws Exception {
        GitService gitService = new GitServiceImpl();
        try (Repository repo = gitService.openRepository(folder)) {
            String gitURL = repo.getConfig().getString("remote", "origin", "url");
            GitHistoryRefactoringMiner detector = new GitHistoryRefactoringMinerImpl();
            OutputRefactoringHandler handler = new OutputRefactoringHandler(gitURL, out, true);
            detector.detectBetweenCommits(repo, startCommit, endCommit, handler);
        }
    }

    private void detectBetweenTags(@NotNull JsonOutput out, @NotNull String folder,
                                   @NotNull String startTag, @NotNull String endTag) throws Exception {
        GitService gitService = new GitServiceImpl();
        try (Repository repo = gitService.openRepository(folder)) {
            String gitURL = repo.getConfig().getString("remote", "origin", "url");
            GitHistoryRefactoringMiner detector = new GitHistoryRefactoringMinerImpl();
            OutputRefactoringHandler handler = new OutputRefactoringHandler(gitURL, out, true);
            detector.detectBetweenTags(repo, startTag, endTag, handler);
        }
    }

    private void detectAtCommit(@NotNull JsonOutput out,
                                @NotNull String folder, @NotNull String commitId) throws Exception {
        GitService gitService = new GitServiceImpl();
        try (Repository repo = gitService.openRepository(folder)) {
            String gitURL = repo.getConfig().getString("remote", "origin", "url");
            GitHistoryRefactoringMiner detector = new GitHistoryRefactoringMinerImpl();
            OutputRefactoringHandler handler = new OutputRefactoringHandler(gitURL, out, false);
            detector.detectAtCommit(repo, commitId, handler);
        }
    }

    private void detectAtGitHubCommit(@NotNull JsonOutput out,
                                      @NotNull String gitURL, @NotNull String commitId, int timeout) {
        GitHistoryRefactoringMiner detector = new GitHistoryRefactoringMinerImpl();
        OutputRefactoringHandler handler = new OutputRefactoringHandler(gitURL, out, false);
        detector.detectAtCommit(gitURL, commitId, handler, timeout);
    }

    private void detectAtGitHubPullRequest(@NotNull JsonOutput out,
                                           @NotNull String gitURL, int pullId, int timeout) throws Exception {
        GitHistoryRefactoringMiner detector = new GitHistoryRefactoringMinerImpl();
        OutputRefactoringHandler handler = new OutputRefactoringHandler(gitURL, out, false);
        detector.detectAtPullRequest(gitURL, pullId, handler, timeout);
    }

    private void printTips() {
        new HelpPrinter(System.out).print();
    }
}
