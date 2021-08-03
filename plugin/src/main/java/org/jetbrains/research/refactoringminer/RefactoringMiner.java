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
    private static final int PATH = 2;
    private static final int POSITION = 3;
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
                case "a":
                    detectAll(out, args.get(PATH), args.get(POSITION));
                    break;
                case "bc":
                    detectBetweenCommits(out, args.get(PATH), args.get(POSITION), args.get(END_POSITION));
                    break;
                case "bt":
                    detectBetweenTags(out, args.get(PATH), args.get(POSITION), args.get(END_POSITION));
                    break;
                case "c":
                    detectAtCommit(out, args.get(PATH), args.get(POSITION));
                    break;
                case "gc":
                    detectAtGitHubCommit(out, args.get(PATH), args.get(POSITION), Integer.parseInt(args.get(TIMEOUT)));
                    break;
                case "gp":
                    detectAtGitHubPullRequest(out, args.get(PATH),
                        Integer.parseInt(args.get(POSITION)), Integer.parseInt(args.get(TIMEOUT)));
                    break;
            }
        } catch (IllegalArgumentException e) {
            System.out.println("Call help to show usage.");
            System.out.println(e.getMessage());
            logger.error("Error processing arguments", e);
        } catch (Exception e) {
            System.out.println(e.getMessage());
            logger.error("Error processing arguments", e);
        }
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
        String delimiter = "  ";
        List<String> headers = List.of("operation", "pathToGit", "startPosition", "endPosition", "timeout", "output", "");
        List<String> detectAll = List.of("a", "<git-repo-folder>", "<branch>?", "", "", "+",
            "Detect all refactorings at <branch> for <git-repo-folder>. " +
                "If <branch> is not specified, commits from all branches are analyzed");
        List<String> betweenCommits = List.of("bc", "<git-repo-folder>",
            "<start-commit-sha1>", "<end-commit-sha1>", "", "+",
            "Detect refactorings between <start-commit-sha1> and <end-commit-sha1> for project <git-repo-folder>");
        List<String> betweenTags = List.of("bt", "<git-repo-folder>", "<start-tag>", "<end-tag>", "", "+",
            "Detect refactorings between <start-tag> and <end-tag> for project <git-repo-folder>");
        List<String> atCommit = List.of("c", "<git-repo-folder>", "<commit-sha1>", "", "", "+",
            "Detect refactorings at specified commit <commit-sha1> for project <git-repo-folder>");
        List<String> githubCommit = List.of("gc", "<git-URL>", "<commit-sha1>", "", "+", "+",
            "Detect refactorings at specified commit <commit-sha1> " +
                "for project <git-URL> within the given <timeout> in seconds. " +
                "All required information is obtained directly from GitHub " +
                "using the OAuth token in github-oauth.properties");
        List<String> githubPR = List.of("gp", "<git-URL>", "<pull-request>", "", "+", "+",
            "Detect refactorings at specified pull request <pull-request> " +
                "for project <git-URL> within the given <timeout> in seconds for each commit in the pull request. " +
                "All required information is obtained directly from GitHub " +
                "using the OAuth token in github-oauth.properties");
        List<List<String>> table =
            List.of(headers, detectAll, betweenCommits, betweenTags, atCommit, githubCommit, githubPR);
        List<Integer> length = headers.stream().map(String::length).collect(Collectors.toList());
        for (List<String> line : table) {
            for (int i = 0; i < line.size(); i++) {
                if (line.get(i).length() > length.get(i)) {
                    length.set(i, line.get(i).length());
                }
            }
        }
        for (List<String> line : table) {
            for (int i = 0; i < line.size(); i++) {
                System.out.print(toSize(line.get(i), length.get(i)));
                if (i + 1 < line.size()) {
                    System.out.print(delimiter);
                } else {
                    System.out.println();
                }
            }
        }
    }

    private static String toSize(String string, int size) {
        return string + Strings.repeat(" ", size - string.length());
    }
}
