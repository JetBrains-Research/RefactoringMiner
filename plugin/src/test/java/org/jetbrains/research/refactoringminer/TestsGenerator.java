package org.jetbrains.research.refactoringminer;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.refactoringminer.test.RefactoringPopulator;
import java.io.File;
import java.io.IOException;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Class for creating custom subsets of data for smaller tests
 */
public class TestsGenerator {
    static final ObjectMapper mapper = new ObjectMapper();
    static final String outputFilename = "dataSmall.json";

    public static void main(String[] args) throws IOException {
        List<RefactoringPopulator.Root> refactorings =
            RefactoringPopulator.getFSERefactorings(RefactoringPopulator.Refactorings.All.getValue(), "plugin/src/test/resources/data.json");
        List<RefactoringPopulator.Root> biggest = refactorings.stream()
            .collect(Collectors.groupingBy(root -> root.repository))
            .entrySet().stream()
            .sorted(Comparator.comparingInt(entry -> -countRefactorings(entry.getValue())))
            .limit(5)
            .flatMap(entry -> entry.getValue().stream())
            .collect(Collectors.toList());
        mapper.writeValue(new File("plugin/src/test/resources/" + outputFilename), biggest);
    }

    public static int countRefactorings(List<RefactoringPopulator.Root> commits) {
        int sum = 0;
        for (RefactoringPopulator.Root commit : commits) {
            sum += commit.refactorings.size();
        }
        return sum;
    }
}
