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
        try {
            org.refactoringminer.RefactoringMiner.main(args.subList(1, args.size()).toArray(String[]::new));
        } catch (Exception e) {
            e.printStackTrace();
        }
        System.out.println("Finished");
    }
}
