package gr.uom.java.xmi;

import com.intellij.psi.PsiElement;
import java.util.regex.Pattern;

/**
 * Temporarily fix for uniform formatting requirement.
 * In future should traverse tree and build correct string in all cases
 */
public class Formatter {
    public static final Pattern spaces = Pattern.compile("\\s+");
    public static final Pattern statementEnd = Pattern.compile(" ?;");

    public static String format(PsiElement expression) {
        return format(expression.getText());
    }

    public static String format(String text) {
        text = text.trim();
        text = spaces.matcher(text).replaceAll(" ");
        text = text.replace(" = ", "=");
        text = text.replace(", ", ",");
        text = statementEnd.matcher(text).replaceAll(";\n");
        return text;
    }
}
