package org.jetbrains.research.refactoringminer;

import com.intellij.testFramework.LightProjectDescriptor;
import com.intellij.testFramework.fixtures.LightJavaCodeInsightFixtureTestCase;
import gr.uom.java.xmi.UMLModel;
import gr.uom.java.xmi.UMLModelASTReader;
import org.jetbrains.annotations.NotNull;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Set;

public class TestParser extends LightJavaCodeInsightFixtureTestCase {
    @NotNull
    @Override
    protected LightProjectDescriptor getProjectDescriptor() {
        return LightJavaCodeInsightFixtureTestCase.JAVA_15;
    }

    @NotNull
    @Override
    protected String getTestDataPath() {
        return Path.of("src-test", "org", "jetbrains", "research", "refactoringminer", "data").toString();
    }

    public void testSimple() throws IOException {
        String src = Files.readString(Path.of("src-test", "org", "jetbrains", "research", "refactoringminer", "data", "TestClass.java"));
        UMLModelASTReader reader = new UMLModelASTReader(Map.of("test/file", src), Set.of());
        UMLModel model = reader.getUmlModel();
    }
}
