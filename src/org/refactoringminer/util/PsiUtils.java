package org.refactoringminer.util;

import org.jetbrains.kotlin.name.FqName;
import org.jetbrains.kotlin.psi.KtNamedDeclaration;

public class PsiUtils {

    public static String getQualifiedName(KtNamedDeclaration element) {
        FqName fqName = element.getFqName();
        return fqName == null ? null : fqName.asString();
    }

}
