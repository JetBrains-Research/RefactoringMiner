package org.jetbrains.research.refactoringminer;

import com.google.common.base.Strings;
import java.io.PrintStream;
import java.util.List;
import java.util.stream.Collectors;

public class HelpPrinter {
    private static final String delimiter = "  ";
    private final PrintStream out;

    public HelpPrinter(PrintStream out) {
        this.out = out;
    }

    public void print() {
        printCommandsTable();
        printArgumentsTable();
        printCommonTips();
    }

    private void printCommandsTable() {
        List<String> detectAll = List.of("detectAll",
            "Detect all refactorings at <branch> for <git-repo-folder>. " +
                "If <branch> is not specified, commits from all branches are analyzed");
        List<String> betweenCommits = List.of("detectBetweenCommits",
            "Detect refactorings between <start-commit-sha1> and <end-commit-sha1> for project <git-repo-folder>");
        List<String> betweenTags = List.of("detectBetweenTags",
            "Detect refactorings between <start-tag> and <end-tag> for project <git-repo-folder>");
        List<String> atCommit = List.of("detectAtCommit",
            "Detect refactorings at specified commit <commit-sha1> for project <git-repo-folder>");
        List<String> githubCommit = List.of("detectAtGitHubCommit",
            "Detect refactorings at specified commit <commit-sha1> for project <git-URL>");
        List<String> githubPR = List.of("detectAtGitHubPullRequest",
            "Detect refactorings at specified pull request <pull-request> " +
                "for project <git-URL> for each commit in the pull request");
        List<List<String>> table = List.of(detectAll, betweenCommits, betweenTags, atCommit, githubCommit, githubPR);

        out.println("Command description:");
        printTable(table);
        out.println();
    }

    private void printCommonTips() {
        out.println("For all commands, you can define the <output> argument to save the output in a JSON file.");
        out.println("For detectAtGitHubCommit and detectAtGitHubPullRequest commands, " +
            "you can define the <timeout> argument to set the maximum execution time in seconds.");
        out.println("For commands using GitHub, you must provide a valid OAuthToken in github-oauth.properties.");
        out.println();
    }

    private void printArgumentsTable() {
        List<String> headers = List.of("command", "gitProjectPath", "startPosition", "endPosition");
        List<String> detectAll = List.of("detectAll", "<git-repo-folder>", "<branch>", "");
        List<String> betweenCommits = List.of("detectBetweenCommits", "<git-repo-folder>",
            "<start-commit-sha1>", "<end-commit-sha1>");
        List<String> betweenTags = List.of("detectBetweenTags", "<git-repo-folder>", "<start-tag>", "<end-tag>");
        List<String> atCommit = List.of("detectAtCommit", "<git-repo-folder>", "<commit-sha1>", "");
        List<String> githubCommit = List.of("detectAtGitHubCommit", "<git-URL>", "<commit-sha1>", "");
        List<String> githubPR = List.of("detectAtGitHubPullRequest", "<git-URL>", "<pull-request>", "");
        List<List<String>> table =
            List.of(headers, detectAll, betweenCommits, betweenTags, atCommit, githubCommit, githubPR);

        out.println("Command usage:");
        printTable(table);
        out.println();
    }

    private void printTable(List<List<String>> table) {
        List<Integer> length = table.get(0).stream().map(String::length).collect(Collectors.toList());
        for (List<String> line : table) {
            for (int i = 0; i < line.size(); i++) {
                if (line.get(i).length() > length.get(i)) {
                    length.set(i, line.get(i).length());
                }
            }
        }
        for (List<String> line : table) {
            for (int i = 0; i < line.size(); i++) {
                out.print(toSize(line.get(i), length.get(i)));
                if (i + 1 < line.size()) {
                    out.print(delimiter);
                } else {
                    out.println();
                }
            }
        }
    }

    private static String toSize(String string, int size) {
        return string + Strings.repeat(" ", size - string.length());
    }
}
