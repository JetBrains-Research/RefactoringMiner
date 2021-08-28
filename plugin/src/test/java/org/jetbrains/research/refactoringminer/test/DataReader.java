package org.jetbrains.research.refactoringminer.test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.CollectionType;
import org.refactoringminer.api.RefactoringType;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class DataReader {
    private static final ObjectMapper mapper = new ObjectMapper();

    private DataReader() {}

    public static List<CommitRefactorings> read(Path source, Set<RefactoringType> types) throws IOException {
        return readFile(source).stream()
            .map(commitData ->
                new CommitRefactorings(
                    commitData.repository,
                    commitData.sha1,
                    getRefactorings(commitData.refactorings, types)
                )
            )
            .filter(commitRefactorings -> !commitRefactorings.refactorings.isEmpty())
            .collect(Collectors.toList());
    }

    private static List<DetectingRefactoring> getRefactorings(List<RefactoringData> refactoringsData,
                                                              Set<RefactoringType> types) {
        return refactoringsData.stream()
            .filter(refactoringData ->
                refactoringData.validation.equals("TP") || refactoringData.validation.equals("CTP"))
            .flatMap(refactoringData ->
                DetectingRefactoring.of(
                    RefactoringType.fromName(refactoringData.type),
                    refactoringData.description
                ).stream()
            )
            .filter(refactoring -> types.contains(refactoring.type))
            .collect(Collectors.toList());
    }

    private static List<CommitData> readFile(Path source) throws IOException {
        CollectionType factory = mapper.getTypeFactory().constructCollectionType(List.class, CommitData.class);
        return mapper.readValue(source.toFile(), factory);
    }

    private static class CommitData {
        public int id;
        public String repository;
        public String sha1;
        public String url;
        public String author;
        public String time;
        public List<RefactoringData> refactorings;
        public long refDiffExecutionTime;
    }

    private static class RefactoringData {
        public String type;
        public String description;
        public String comment;
        public String validation;
        public String detectionTools;
        public String validators;
    }
}

