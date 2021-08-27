package org.refactoringminer.test;

import com.intellij.testFramework.fixtures.LightJavaCodeInsightFixtureTestCase;
import org.refactoringminer.rm1.GitHistoryRefactoringMinerImpl;
import org.refactoringminer.test.RefactoringPopulator.Refactorings;
import org.refactoringminer.test.RefactoringPopulator.Systems;
import java.math.BigInteger;

public class TestAllRefactorings extends LightJavaCodeInsightFixtureTestCase {

    public final String dataFile = "data.json";
    public final BigInteger refactoringsToTest = Refactorings.All.getValue();
    public final String dataDirectory = "tmp1";

    public void testAllRefactorings() throws Exception {
        GitHistoryRefactoringMinerImpl detector = new GitHistoryRefactoringMinerImpl();
        TestBuilder test = new TestBuilder(detector, dataDirectory, refactoringsToTest, "src/test/resources/" + dataFile);
        RefactoringPopulator.feedRefactoringsInstances(refactoringsToTest, Systems.FSE.getValue(), test);
        test.assertExpectations(10474, 36, 383);
    }
}
