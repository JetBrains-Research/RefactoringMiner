package org.jetbrains.research.refactoringminer;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.refactoringminer.api.Refactoring;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class JsonOutput implements AutoCloseable {
    @NotNull
    private static final OutputStream defaultOutput = System.out;
    @NotNull
    private final OutputStream output;
    private boolean isEmpty = true;

    public JsonOutput(@Nullable Path pathToJson) throws IOException {
        if (pathToJson != null) {
            output = Files.newOutputStream(pathToJson);
        } else {
            output = defaultOutput;
        }
    }

    public void commit(@NotNull String repositoryURL, @NotNull String revisionURL, @NotNull String revisionId,
                       @NotNull List<@NotNull Refactoring> refactoringsAtRevision) throws IOException {
        if (isEmpty) {
            startJson();
            isEmpty = false;
        } else {
            betweenCommits();
        }
        StringBuilder sb = new StringBuilder();
        sb.append("{").append("\n");
        sb.append("\t").append("\"").append("repository").append("\"").append(": ").append("\"")
            .append(repositoryURL).append("\"").append(",").append("\n");
        sb.append("\t").append("\"").append("sha1").append("\"").append(": ").append("\"")
            .append(revisionId).append("\"").append(",").append("\n");
        sb.append("\t").append("\"").append("url").append("\"").append(": ").append("\"")
            .append(revisionURL).append("\"").append(",").append("\n");
        sb.append("\t").append("\"").append("refactorings").append("\"").append(": ");
        sb.append("[");
        int counter = 0;
        for (Refactoring refactoring : refactoringsAtRevision) {
            sb.append(refactoring.toJSON());
            if (counter < refactoringsAtRevision.size() - 1) {
                sb.append(",");
            }
            sb.append("\n");
            counter++;
        }
        sb.append("]").append("\n");
        sb.append("}");
        output.write(sb.toString().getBytes());
    }

    private void betweenCommits() throws IOException {
        output.write(",\n".getBytes());
    }

    private void startJson() throws IOException {
        String header = "{\n" +
            "\"commits\": [\n";
        output.write(header.getBytes());
    }

    @Override
    public void close() throws Exception {
        if (!isEmpty) {
            endJson();
        }
        if (output != defaultOutput) {
            output.close();
        }
    }

    private void endJson() throws IOException {
        output.write("]\n}".getBytes());
    }
}
