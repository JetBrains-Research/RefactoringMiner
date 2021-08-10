package org.refactoringminer.test;

import com.google.common.base.Strings;
import com.intellij.openapi.util.text.StringUtil;
import org.apache.commons.text.similarity.LongestCommonSubsequence;
import org.eclipse.jgit.lib.Repository;
import org.junit.Assert;
import org.refactoringminer.api.GitHistoryRefactoringMiner;
import org.refactoringminer.api.GitService;
import org.refactoringminer.api.Refactoring;
import org.refactoringminer.api.RefactoringHandler;
import org.refactoringminer.api.RefactoringType;
import org.refactoringminer.rm1.GitHistoryRefactoringMinerImpl;
import org.refactoringminer.test.RefactoringPopulator.Refactorings;
import org.refactoringminer.util.GitServiceImpl;
import java.io.PrintStream;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class TestBuilder {

    private static final int TP = 0;
    private static final int FP = 1;
    private static final int FN = 2;
    private static final int TN = 3;
    private static final int UNK = 4;
    private final String tempDir;
    private final Map<String, ProjectMatcher> map;
    private final GitHistoryRefactoringMiner refactoringDetector;
    private boolean verbose;
    private boolean aggregate;
    private int commitsCount;
    private int errorCommitsCount;
    private Counter c;// = new Counter();
    private Map<RefactoringType, Counter> cMap;
    private BigInteger refactoringFilter;
    public final String dataFile;

    public TestBuilder(GitHistoryRefactoringMiner detector, String tempDir, BigInteger refactorings, String dataFile) {
        this(detector, tempDir, dataFile);

        this.refactoringFilter = refactorings;
    }

    public TestBuilder(GitHistoryRefactoringMiner detector, String tempDir, String dataFile) {
        this.map = new HashMap<>();
        this.refactoringDetector = detector;
        this.tempDir = tempDir;
        this.verbose = false;
        this.aggregate = false;
        this.dataFile = dataFile;
    }

    public TestBuilder() {
        this(new GitHistoryRefactoringMinerImpl(), "tmp", "data.json");
    }

    /**
     * Remove generics type information.
     */
    private static String normalizeSingle(String refactoring) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < refactoring.length(); i++) {
            char c = refactoring.charAt(i);
            if (c == '\t') {
                c = ' ';
            }
            sb.append(c);
        }
        return sb.toString();
    }

    public TestBuilder verbose() {
        this.verbose = true;
        return this;
    }

    public TestBuilder withAggregation() {
        this.aggregate = true;
        return this;
    }

    public void assertExpectations(int expectedTPs, int expectedFPs, int expectedFNs) throws Exception {
        c = new Counter();
        cMap = new HashMap<>();
        commitsCount = 0;
        errorCommitsCount = 0;
        GitService gitService = new GitServiceImpl();

        for (ProjectMatcher m : map.values()) {
            String folder = tempDir + "/"
                + m.cloneUrl.substring(m.cloneUrl.lastIndexOf('/') + 1, m.cloneUrl.lastIndexOf('.'));
            try (Repository rep = gitService.cloneIfNotExists(folder, m.cloneUrl/* , m.branch */)) {
                if (m.ignoreNonSpecifiedCommits) {
                    // It is faster to only look at particular commits
                    for (String commitId : m.getCommits()) {
                        refactoringDetector.detectAtCommit(rep, commitId, m);
                    }
                } else {
                    // Iterate over each commit
                    refactoringDetector.detectAll(rep, m.branch, m);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        System.out.printf("Commits: %d  Errors: %d%n", commitsCount, errorCommitsCount);

        String mainResultMessage = buildResultMessage(c);
        System.out.println("Total  " + mainResultMessage);
        int maxDisplayName =
            Arrays.stream(RefactoringType.values()).mapToInt(ref -> ref.getDisplayName().length()).max().getAsInt();
        for (RefactoringType refType : RefactoringType.values()) {
            Counter refTypeCounter = cMap.get(refType);
            if (refTypeCounter != null) {
                System.out.println(refType.getDisplayName()
                    + Strings.repeat(" ", maxDisplayName - refType.getDisplayName().length())
                    + buildResultMessage(refTypeCounter));
            }
        }

        boolean success = get(FP) == expectedFPs && get(FN) == expectedFNs && get(TP) == expectedTPs;
        var path = Path.of("src", "test", "resources", "verbose.txt");
        try (PrintStream output = new PrintStream(Files.newOutputStream(path))) {
            for (ProjectMatcher m : map.values()) {
                m.printResults(output);
            }
        }
        Assert.assertTrue(mainResultMessage, success);
    }

    private int get(int type) {
        return c.c[type];
    }

    private String buildResultMessage(Counter c) {
        double precision = ((double) get(TP, c) / (get(TP, c) + get(FP, c)));
        double recall = ((double) get(TP, c)) / (get(TP, c) + get(FN, c));
        String mainResultMessage = String.format(
            "TP: %5d  FP: %5d  FN: %5d  TN: %2d  Unk.: %2d  Prec.: %.3f  Recall: %.3f", get(TP, c), get(FP, c),
            get(FN, c), get(TN, c), get(UNK, c), precision, recall);
        return mainResultMessage;
    }

    private void count(int type, String refactoring) {
        c.c[type]++;
        RefactoringType refType = RefactoringType.extractFromDescription(refactoring);
        Counter refTypeCounter = cMap.get(refType);
        if (refTypeCounter == null) {
            refTypeCounter = new Counter();
            cMap.put(refType, refTypeCounter);
        }
        refTypeCounter.c[type]++;
    }

    public final ProjectMatcher project(String cloneUrl, String branch) {
        ProjectMatcher projectMatcher = this.map.get(cloneUrl);
        if (projectMatcher == null) {
            projectMatcher = new ProjectMatcher(cloneUrl, branch);
            this.map.put(cloneUrl, projectMatcher);
        }
        return projectMatcher;
    }

    private List<String> normalize(String refactoring) {
        RefactoringType refType = RefactoringType.extractFromDescription(refactoring);
        refactoring = normalizeSingle(refactoring);
        if (aggregate) {
            refactoring = refType.aggregate(refactoring);
        } else {
            int begin = refactoring.indexOf("from classes [");
            if (begin != -1) {
                int end = refactoring.lastIndexOf(']');
                String types = refactoring.substring(begin + "from classes [".length(), end);
                String[] typesArray = types.split(", ");
                List<String> refactorings = new ArrayList<>();
                for (String type : typesArray) {
                    refactorings.add(refactoring.substring(0, begin) + "from class " + type);
                }
                return refactorings;
            }
        }
        return Collections.singletonList(refactoring);
    }

    private int get(int type, Counter counter) {
        return counter.c[type];
    }

    private static class Counter {
        final int[] c = new int[5];
    }

    public class ProjectMatcher extends RefactoringHandler {

        private final String cloneUrl;
        private final String branch;
        private final Map<String, CommitMatcher> expected = new HashMap<>();
        private boolean ignoreNonSpecifiedCommits = true;
        private int truePositiveCount = 0;
        private int falsePositiveCount = 0;
        private int falseNegativeCount = 0;
        private int trueNegativeCount = 0;
        private int unknownCount = 0;
        // private int errorsCount = 0;

        private ProjectMatcher(String cloneUrl, String branch) {
            this.cloneUrl = cloneUrl;
            this.branch = branch;
        }

        public ProjectMatcher atNonSpecifiedCommitsContainsNothing() {
            this.ignoreNonSpecifiedCommits = false;
            return this;
        }

        public Set<String> getCommits() {
            return expected.keySet();
        }

        @Override
        public boolean skipCommit(String commitId) {
            if (this.ignoreNonSpecifiedCommits) {
                return !this.expected.containsKey(commitId);
            }
            return false;
        }

        @Override
        public void handle(String commitId, List<Refactoring> refactorings) {
            refactorings = filterRefactoring(refactorings);
            CommitMatcher matcher;
            commitsCount++;
            //String commitId = curRevision.getId().getName();
            if (expected.containsKey(commitId)) {
                matcher = expected.get(commitId);
            } else if (!this.ignoreNonSpecifiedCommits) {
                matcher = this.atCommit(commitId);
                matcher.containsOnly();
            } else {
                // ignore this commit
                matcher = null;
            }
            if (matcher != null) {
                matcher.analyzed = true;
                Set<String> refactoringsFound = new HashSet<>();
                for (Refactoring refactoring : refactorings) {
                    refactoringsFound.addAll(normalize(refactoring.toString()));
                }
                // count true positives
                Iterator<String> iterExpected = matcher.expected.iterator();
                while (iterExpected.hasNext()) {
                    String expected = iterExpected.next();
                    Iterator<String> iterFound = refactoringsFound.iterator();
                    while (iterFound.hasNext()) {
                        String found = iterFound.next();
                        if (found.equals(expected) || checkSpaces(found, expected)) {
                            iterExpected.remove();
                            iterFound.remove();
                            this.truePositiveCount++;
                            count(TP, expected);
                            matcher.truePositive.add(expected);
                            break;
                        }
                    }
                }

                // count false positives
                for (Iterator<String> iter = matcher.notExpected.iterator(); iter.hasNext(); ) {
                    String notExpectedRefactoring = iter.next();
                    if (refactoringsFound.contains(notExpectedRefactoring)) {
                        refactoringsFound.remove(notExpectedRefactoring);
                        this.falsePositiveCount++;
                        count(FP, notExpectedRefactoring);
                    } else {
                        this.trueNegativeCount++;
                        count(TN, notExpectedRefactoring);
                        iter.remove();
                    }
                }
                // count false positives when using containsOnly
                if (matcher.ignoreNonSpecified) {
                    for (String refactoring : refactoringsFound) {
                        matcher.unknown.add(refactoring);
                        this.unknownCount++;
                        count(UNK, refactoring);
                    }
                } else {
                    for (String refactoring : refactoringsFound) {
                        matcher.notExpected.add(refactoring);
                        this.falsePositiveCount++;
                        count(FP, refactoring);
                    }
                }

                // count false negatives
                for (String expectedButNotFound : matcher.expected) {
                    this.falseNegativeCount++;
                    count(FN, expectedButNotFound);
                }
            }
        }

        private boolean checkSpaces(String found, String expected) {
            String _found = found.replaceAll("\\s", "");
            String _expected = expected.replaceAll("\\s", "");
            if (_found.equals(_expected)) {
                return true;
            }
            int prefix = StringUtil.commonPrefixLength(_found, _expected);
            int suffix = StringUtil.commonSuffixLength(_found, _expected);
            if (prefix + suffix <= Math.min(_found.length(), _expected.length())) {
                String _foundMid = _found.substring(prefix, _found.length() - suffix);
                String _expectedMid = _expected.substring(prefix, _expected.length() - suffix);
                if (_foundMid.equals("from") && _expectedMid.equals("in")) {
                    return true;
                }
                if (_expectedMid.isEmpty() && _foundMid.equals(",")) {
                    return true;
                }
            }
            String prefixCommon = _found.substring(0, prefix);
            if (prefixCommon.contains("inclass") || prefixCommon.contains("fromclass")) {
                if (_found.substring(prefix).matches("[.\\p{Lower}_]*")
                    || _expected.substring(prefix).matches("[.\\p{Lower}_]*")) {
                    System.err.println("Location Diff");
                    System.err.println(found);
                    System.err.println(expected);
                    return true;
                }
            }
            if (_found.startsWith("RenameAttribute") && _expected.startsWith("RenameAttribute")) {
                int len = new LongestCommonSubsequence().apply(_found, _expected);
                return len == _expected.length();
            }
            return false;
        }

        public CommitMatcher atCommit(String commitId) {
            CommitMatcher m = expected.get(commitId);
            if (m == null) {
                m = new CommitMatcher();
                expected.put(commitId, m);
            }
            return m;
        }

        private List<Refactoring> filterRefactoring(List<Refactoring> refactorings) {
            List<Refactoring> filteredRefactorings = new ArrayList<>();

            for (Refactoring refactoring : refactorings) {
                BigInteger value = Enum.valueOf(Refactorings.class, refactoring.getName().replace(" ", "")).getValue();
                if (value.and(refactoringFilter).compareTo(BigInteger.ZERO) == 1) {
                    filteredRefactorings.add(refactoring);
                }
            }

            return filteredRefactorings;
        }

        @Override
        public void handleException(String commitId, Exception e) {
            if (expected.containsKey(commitId)) {
                CommitMatcher matcher = expected.get(commitId);
                matcher.error = e.toString();
            }
            errorCommitsCount++;
        }

        private void printResults(PrintStream output) {
            String baseUrl = this.cloneUrl.substring(0, this.cloneUrl.length() - 4) + "/commit/";
            for (Map.Entry<String, CommitMatcher> entry : this.expected.entrySet()) {
                String commitUrl = baseUrl + entry.getKey();
                CommitMatcher matcher = entry.getValue();
                if (matcher.error != null) {
                    output.println("error at " + commitUrl + ": " + matcher.error);
                } else {
                    if (!matcher.expected.isEmpty() || !matcher.notExpected.isEmpty() || !matcher.unknown.isEmpty()) {
                        if (!matcher.analyzed) {
                            output.println("at not analyzed " + commitUrl);
                        } else {
                            output.println("at " + commitUrl);
                        }
                            /*if (!matcher.truePositive.isEmpty()) {
                                output.println(" true positives");
                                for (String ref : matcher.truePositive) {
                                    output.println("  " + ref);
                                }
                            }*/
                        if (!matcher.notExpected.isEmpty()) {
                            output.println(" false positives");
                            for (String ref : matcher.notExpected) {
                                output.println("  " + ref);
                            }
                        }
                        if (!matcher.expected.isEmpty()) {
                            output.println(" false negatives");
                            for (String ref : matcher.expected) {
                                output.println("  " + ref);
                            }
                        }
                        if (!matcher.unknown.isEmpty()) {
                            output.println(" unknown");
                            for (String ref : matcher.unknown) {
                                output.println("  " + ref);
                            }
                        }
                    }
                }
            }
        }

        public class CommitMatcher {
            private final Set<String> truePositive = new HashSet<>();
            private final Set<String> unknown = new HashSet<>();
            private Set<String> expected = new HashSet<>();
            private Set<String> notExpected = new HashSet<>();
            private boolean ignoreNonSpecified = true;
            private boolean analyzed = false;
            private String error = null;

            private CommitMatcher() {
            }

            public ProjectMatcher contains(String... refactorings) {
                for (String refactoring : refactorings) {
                    expected.addAll(normalize(refactoring));
                }
                return ProjectMatcher.this;
            }

            public ProjectMatcher containsNothing() {
                return containsOnly();
            }

            public ProjectMatcher containsOnly(String... refactorings) {
                this.ignoreNonSpecified = false;
                this.expected = new HashSet<>();
                this.notExpected = new HashSet<>();
                for (String refactoring : refactorings) {
                    expected.addAll(normalize(refactoring));
                }
                return ProjectMatcher.this;
            }

            public ProjectMatcher notContains(String... refactorings) {
                for (String refactoring : refactorings) {
                    notExpected.addAll(normalize(refactoring));
                }
                return ProjectMatcher.this;
            }
        }
    }
}
