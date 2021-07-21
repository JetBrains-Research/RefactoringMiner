package org.refactoringminer.test;

import com.intellij.testFramework.fixtures.LightJavaCodeInsightFixtureTestCase;
import org.refactoringminer.rm1.GitHistoryRefactoringMinerImpl;
import org.refactoringminer.test.RefactoringPopulator.Refactorings;
import org.refactoringminer.test.RefactoringPopulator.Systems;

public class TestAllRefactorings extends LightJavaCodeInsightFixtureTestCase {

    public void testAllRefactorings() throws Exception {
        GitHistoryRefactoringMinerImpl detector = new GitHistoryRefactoringMinerImpl();
        TestBuilder test = new TestBuilder(detector, "tmp1", Refactorings.All.getValue());
        RefactoringPopulator.feedRefactoringsInstances(Refactorings.All.getValue(), Systems.FSE.getValue(), test);
        test.assertExpectations(10474, 36, 383);
    }
}
