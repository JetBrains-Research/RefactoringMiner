package org.jetbrains.research.refactoringminer;

import com.intellij.openapi.application.ApplicationStarter;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import java.util.List;

public class RefactoringMiner implements ApplicationStarter {
    @Override
    public @NonNls
    String getCommandName() {
        return "RefactoringMiner";
    }

    @Override
    public void main(@NotNull List<String> args) {
        System.out.println("Empty main");
    }
}
