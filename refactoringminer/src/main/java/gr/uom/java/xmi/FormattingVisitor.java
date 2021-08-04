package gr.uom.java.xmi;

import com.intellij.psi.JavaTokenType;
import com.intellij.psi.PsiComment;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiRecursiveElementWalkingVisitor;
import com.intellij.psi.PsiWhiteSpace;
import com.intellij.psi.tree.TokenSet;
import com.intellij.psi.util.PsiUtil;
import org.jetbrains.annotations.NotNull;

/**
 * Visitor for building string representation of PsiElement
 * 1. Without commentaries
 * 2. With formatting similar to Eclipse format
 */
public class FormattingVisitor extends PsiRecursiveElementWalkingVisitor {
    private static final TokenSet noSpaces = TokenSet.create(
        JavaTokenType.EQ, JavaTokenType.RBRACKET, JavaTokenType.RBRACE, JavaTokenType.LBRACE, JavaTokenType.LBRACKET,
        JavaTokenType.LT, JavaTokenType.GT, JavaTokenType.DOT, JavaTokenType.COMMA,
        JavaTokenType.RPARENTH, JavaTokenType.LPARENTH
    );
    private static final TokenSet noSpaceAfter = TokenSet.create(
        JavaTokenType.ASTERISK
    );
    private static final TokenSet noSpaceBefore = TokenSet.create(
        JavaTokenType.SEMICOLON, JavaTokenType.ELLIPSIS
    );

    private final StringBuilder sb = new StringBuilder();
    private boolean previousNeedSpaceAfter = false;

    @Override
    public void visitElement(@NotNull PsiElement element) {
        if (element.getFirstChild() == null) {
            // Add text from leaves. Excluding comments and user-specific whitespaces
            if (!(element instanceof PsiWhiteSpace || element instanceof PsiComment)) {
                String text = element.getText().trim();
                if (!text.isEmpty()) {
                    if (needSpaceBefore(element, text) && previousNeedSpaceAfter) {
                        sb.append(' ');
                    }
                    sb.append(element.getText());
                    previousNeedSpaceAfter = needSpaceAfter(element, text);
                }
            }
        } else {
            super.visitElement(element);
        }
    }

    private static boolean needSpaceBefore(PsiElement element, String elementText) {
        return !(PsiUtil.isJavaToken(element, noSpaces) || PsiUtil.isJavaToken(element, noSpaceBefore));
    }

    private static boolean needSpaceAfter(PsiElement element, String elementText) {
        return !(PsiUtil.isJavaToken(element, noSpaces) || PsiUtil.isJavaToken(element, noSpaceAfter));
    }

    public String getText() {
        return sb.toString();
    }
}
