package gr.uom.java.xmi;

import com.intellij.psi.PsiElement;
import gr.uom.java.xmi.decomposition.PsiUtils;
import java.util.regex.Pattern;

public class Formatter {
    public static final Pattern spaces = Pattern.compile("\\s+");

    public static String format(PsiElement expression) {
        if (PsiUtils.isConstructor(expression)) {
            return format(expression.getParent());
        }

        FormattingVisitor formatter = new FormattingVisitor();
        expression.accept(formatter);
        String text = formatter.getText();

        if (PsiUtils.isForInitializer(expression)) {
            // remove ";\n"
            text = text.substring(0, text.length() - 2);
        }
        return text;
    }

    // Temporarily fix
    public static String format(String text) {
        text = text.trim();
        text = spaces.matcher(text).replaceAll(" ");
        text = text.replace(" = ", "=");
        text = text.replace(", ", ",");
        text = text.replace(" ;", ";");
        text = text.replace(" .", ".");
        text = text.replace(". ", ".");
        return text;
    }
}
