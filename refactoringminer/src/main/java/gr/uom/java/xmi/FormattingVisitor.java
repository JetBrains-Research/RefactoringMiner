package gr.uom.java.xmi;

import com.intellij.psi.PsiComment;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiRecursiveElementWalkingVisitor;
import com.intellij.psi.PsiWhiteSpace;
import org.jetbrains.annotations.NotNull;
import java.util.regex.Pattern;

public class FormattingVisitor extends PsiRecursiveElementWalkingVisitor {
    private static final Pattern nonWordPattern = Pattern.compile("\\W*");
    StringBuilder sb = new StringBuilder();
    boolean isPreviousSpaceSensitive = false;

    @Override
    public void visitElement(@NotNull PsiElement element) {
        if (element.getFirstChild() == null) {
            if (!(element instanceof PsiWhiteSpace || element instanceof PsiComment)) {
                String text = element.getText().trim();
                if (!text.isEmpty()) {
                    boolean isSpaceSensitive = isSpaceSensitive(element, text);
                    if (isSpaceSensitive && isPreviousSpaceSensitive) {
                        sb.append(' ');
                    }
                    sb.append(element.getText());
                    isPreviousSpaceSensitive = isSpaceSensitive;
                }
            }
        } else {
            super.visitElement(element);
        }
    }

    // TODO:
    private static boolean isSpaceSensitive(PsiElement element, String elementText) {
        return !nonWordPattern.matcher(elementText).matches();
    }

    public String getText() {
        return sb.toString();
    }
}
