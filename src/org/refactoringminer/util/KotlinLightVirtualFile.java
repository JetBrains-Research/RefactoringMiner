package org.refactoringminer.util;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.com.intellij.testFramework.LightVirtualFile;
import org.jetbrains.kotlin.idea.KotlinLanguage;

import java.io.File;
import java.io.IOException;

/**
 * Wrapper for VirtualFile that retains path on machine
 */
public class KotlinLightVirtualFile extends LightVirtualFile {
    private String path;

    public KotlinLightVirtualFile(File file, String text) throws IOException {
        super(file.getName(), KotlinLanguage.INSTANCE, text);
        this.path = file.getCanonicalPath();
    }

    @NotNull
    @Override
    public String getPath() {
        return this.path;
    }
}
