package org.jetbrains.research.refactoringminer.test;

import org.eclipse.jgit.lib.Repository;
import org.refactoringminer.api.GitService;
import org.refactoringminer.util.GitServiceImpl;
import java.io.File;
import java.util.Objects;

public class GitRepository {
    private static final GitService service = new GitServiceImpl();
    private final String url;

    public GitRepository(String url) {
        this.url = url;
    }

    public Repository getJGitRepository() throws Exception {
        String folder = TestStatistics.foldersRepository
            .resolve(url.substring(url.lastIndexOf(File.separatorChar) + 1, url.lastIndexOf('.')))
            .toString();
        return service.cloneIfNotExists(folder, url);
    }

    public GitCommit commit(String commit) {
        return new GitCommit(this, commit);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        GitRepository that = (GitRepository) o;
        return Objects.equals(url, that.url);
    }

    @Override
    public int hashCode() {
        return Objects.hash(url);
    }

    @Override
    public String toString() {
        return url;
    }

    public static class GitCommit {
        private final GitRepository repository;
        private final String commit;

        public GitCommit(GitRepository repository, String commit) {
            this.repository = repository;
            this.commit = commit;
        }

        public String getLink() {
            return repository.url.substring(0, repository.url.length() - 4) + "/commit/" + commit;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            GitCommit location = (GitCommit) o;
            return repository.equals(location.repository) && commit.equals(location.commit);
        }

        @Override
        public int hashCode() {
            return Objects.hash(repository, commit);
        }
    }
}
