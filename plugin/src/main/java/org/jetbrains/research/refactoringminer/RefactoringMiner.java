package org.jetbrains.research.refactoringminer;

import com.intellij.openapi.application.ApplicationStarter;
import gr.uom.java.xmi.UMLModel;
import gr.uom.java.xmi.UMLModelASTReader;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class RefactoringMiner implements ApplicationStarter {
    @Override
    public @NonNls String getCommandName() {
        return "RefactoringMiner";
    }

    @Override
    public void main(@NotNull List<String> args) {
        System.out.println("Empty main");
    }
}
