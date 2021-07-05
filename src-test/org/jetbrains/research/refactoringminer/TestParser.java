package org.jetbrains.research.refactoringminer;

import gr.uom.java.xmi.UMLModel;
import gr.uom.java.xmi.UMLModelASTReader;
import org.junit.Test;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Set;

public class TestParser {
    @Test
    public void simple() throws IOException {
        String src = Files.readString(Path.of("src-test", "org", "jetbrains", "research", "refactoringminer", "data", "TestClass.java"));
        UMLModelASTReader reader = new UMLModelASTReader(Map.of("test/file", src), Set.of());
        UMLModel model = reader.getUmlModel();
    }
}
