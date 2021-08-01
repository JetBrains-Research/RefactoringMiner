package gr.uom.java.xmi.decomposition;

import com.intellij.psi.JavaTokenType;
import com.intellij.psi.PsiDeclarationStatement;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiForStatement;
import com.intellij.psi.PsiJavaToken;
import com.intellij.psi.PsiKeyword;
import com.intellij.psi.PsiMethodCallExpression;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.tree.TokenSet;
import com.intellij.psi.util.PsiUtil;
import java.util.function.Predicate;

public class PsiUtils {
    private PsiUtils() {}

    private static final TokenSet typeKeywords =
        TokenSet.create(JavaTokenType.BOOLEAN_KEYWORD, JavaTokenType.BYTE_KEYWORD, JavaTokenType.INT_KEYWORD,
            JavaTokenType.CHAR_KEYWORD, JavaTokenType.DOUBLE_KEYWORD, JavaTokenType.FLOAT_KEYWORD,
            JavaTokenType.LONG_KEYWORD, JavaTokenType.SHORT_KEYWORD, JavaTokenType.VOID_KEYWORD,
            JavaTokenType.VAR_KEYWORD);

    public static boolean isTypeKeyword(PsiKeyword keyword) {
        return typeKeywords.contains(keyword.getTokenType());
    }

    public static boolean isTypeKeyword(PsiElement keyword) {
        return PsiUtil.isJavaToken(keyword, typeKeywords);
    }

    public static PsiJavaToken findLastCArrayBracket(PsiElement element) {
        PsiJavaToken last = null;
        PsiElement current = element;
        while (current != null) {
            if (PsiUtil.isJavaToken(current, JavaTokenType.RBRACKET)) {
                last = (PsiJavaToken) current;
            }
            current = current.getNextSibling();
        }
        return last;
    }

    @SuppressWarnings("unchecked")
    public static <T> T findFirstForwardSiblingOfType(PsiElement element, Class<T> type) {
        return (T) findFirstForwardSibling(element, type::isInstance);
    }

    public static PsiElement findFirstForwardSibling(PsiElement element, Predicate<? super PsiElement> predicate) {
        while (element != null) {
            if (predicate.test(element)) {
                return element;
            }
            element = element.getNextSibling();
        }
        return null;
    }

    public static PsiJavaToken findFirstForwardSiblingToken(PsiElement element, IElementType token) {
        return (PsiJavaToken) findFirstForwardSibling(element, (elem) -> PsiUtil.isJavaToken(elem, token));
    }

    // Special cases differs from eclipse
    public static boolean isForInitializer(PsiElement element) {
        return element instanceof PsiDeclarationStatement && element.getParent() instanceof PsiForStatement;
    }

    public static boolean isConstructor(PsiElement element) {
        return element instanceof PsiMethodCallExpression &&
            (isThisConstructorInvocation((PsiMethodCallExpression) element)
                || isSuperConstructorInvocation((PsiMethodCallExpression) element));
    }

    public static boolean isThisConstructorInvocation(PsiMethodCallExpression callExpression) {
        return PsiUtil.isJavaToken(callExpression.getMethodExpression().getLastChild(), JavaTokenType.THIS_KEYWORD);
    }

    public static boolean isSuperConstructorInvocation(PsiMethodCallExpression callExpression) {
        return PsiUtil.isJavaToken(callExpression.getMethodExpression().getLastChild(), JavaTokenType.SUPER_KEYWORD);
    }
}
