package org.refactoringminer.test;

import com.intellij.testFramework.fixtures.LightJavaCodeInsightFixtureTestCase;
import org.refactoringminer.rm1.GitHistoryRefactoringMinerImpl;
import org.refactoringminer.test.RefactoringPopulator.Refactorings;
import org.refactoringminer.test.RefactoringPopulator.Systems;
import java.math.BigInteger;

public class TestAllRefactorings extends LightJavaCodeInsightFixtureTestCase {

    public static final String commit = "04bcfe98dbe7b05e508559930c21379ece845732";
    public final BigInteger refactoringsToTest = Refactorings.All.getValue();
    public final String dataFile = "dataMy.json";

    public void testAllRefactorings() throws Exception {
        GitHistoryRefactoringMinerImpl detector = new GitHistoryRefactoringMinerImpl();
        TestBuilder test = new TestBuilder(detector, dataDirectory, refactoringsToTest, "src/test/resources/" + dataFile);
        RefactoringPopulator.feedRefactoringsInstances(refactoringsToTest, Systems.FSE.getValue(), test);
        test.assertExpectations(10474, 36, 383);
    }

    public final String dataDirectory = "/media/roman/Roman's backup/tmp1";

    public void testExactCommit() throws Exception {
        GitHistoryRefactoringMinerImpl detector = new GitHistoryRefactoringMinerImpl();
        TestBuilder test = new TestBuilder(detector, dataDirectory, refactoringsToTest, "src/test/resources/" + dataFile);
        RefactoringPopulator.prepareRefactoringsAtCommit(test, refactoringsToTest, commit);
        test.assertExpectations(0, 0, 0);
    }
}
