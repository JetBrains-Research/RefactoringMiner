package gr.uom.java.xmi;

import com.intellij.psi.PsiElement;
import gr.uom.java.xmi.decomposition.PsiUtils;
import java.util.regex.Pattern;

public class Formatter {
    public static final Pattern spaces = Pattern.compile("\\s+");

    public static String format(PsiElement element) {
        if (PsiUtils.isConstructor(element)) {
            return format(element.getParent());
        }

        FormattingVisitor formatter = new FormattingVisitor();
        element.accept(formatter);
        String text = formatter.getText();

        if (PsiUtils.isForInitializer(element)) {
            // remove ";\n"
            text = text.substring(0, text.length() - 2);
        }
        return text;
    }

    /**
     * Formats elements between siblings.
     * From begin inclusively to the end exclusively.
     */
    public static String format(PsiElement beginSibling, PsiElement endSibling) {
        FormattingVisitor formatter = new FormattingVisitor();
        do {
            beginSibling.accept(formatter);
            beginSibling = beginSibling.getNextSibling();
        } while (beginSibling != endSibling);
        return formatter.getText();
    }
}
