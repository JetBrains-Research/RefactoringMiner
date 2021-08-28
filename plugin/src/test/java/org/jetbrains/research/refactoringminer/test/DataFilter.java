package org.jetbrains.research.refactoringminer.test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class DataFilter {
    private DataFilter() {}

    public static List<CommitRefactorings> filterRemoved(Path removedPath, List<CommitRefactorings> commits) throws IOException {
        List<Removed> removed = Files.readAllLines(removedPath).stream()
            .map(String::trim)
            .filter(line -> !line.isEmpty())
            .map(Removed::new)
            .collect(Collectors.toList());
        return commits.stream()
            .filter(commitData -> notRemoved(commitData, removed))
            .collect(Collectors.toList());
    }

    private static boolean notRemoved(CommitRefactorings commitData, List<Removed> removed) {
        return removed.stream().noneMatch(rem -> rem.match(commitData));
    }

    private static class Removed {
        public final GitRepository repository;
        public final Set<String> sha1;

        private Removed(String url) {
            if (url.contains("#")) {
                repository = new GitRepository(url.substring(0, url.indexOf("#")));
                sha1 = Arrays.stream(url.split("#")).skip(1).collect(Collectors.toUnmodifiableSet());
            } else {
                repository = new GitRepository(url);
                sha1 = null;
            }
        }

        private boolean match(CommitRefactorings commitData) {
            return commitData.repository.equals(repository) && (sha1 == null || sha1.contains(commitData.sha1));
        }
    }
}
