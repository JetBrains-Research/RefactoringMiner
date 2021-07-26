package gr.uom.java.xmi.decomposition;

import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiIdentifier;
import com.intellij.psi.PsiJavaToken;

public class PsiUtils {
    private PsiUtils() {}

    public static PsiJavaToken findLastCArrayBracket(PsiIdentifier identifier) {
        PsiJavaToken last = null;
        PsiElement current = identifier;
        while (current != null) {
            if (isToken(current, "RBRACKET")) {
                last = (PsiJavaToken) current;
            }
            current = current.getNextSibling();
        }
        return last;
    }

    public static boolean isToken(PsiElement element, String token) {
        return element instanceof PsiJavaToken && ((PsiJavaToken) element).getTokenType().toString().equals(token);
    }
}
