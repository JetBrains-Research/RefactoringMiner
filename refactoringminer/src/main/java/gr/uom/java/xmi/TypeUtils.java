package gr.uom.java.xmi;

import com.intellij.psi.PsiAnonymousClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiJavaCodeReferenceElement;
import com.intellij.psi.PsiKeyword;
import com.intellij.psi.PsiNewExpression;
import org.jetbrains.annotations.NotNull;

public class TypeUtils {
    public static UMLType extractType(PsiFile file, String filePath, PsiNewExpression newExpression) {
        PsiJavaCodeReferenceElement typeElement = newExpression.getClassReference();
        if (typeElement != null) {
            return UMLType.extractTypeObject(file, filePath, typeElement, newExpression.getType());
        } else {
            PsiElement[] children = newExpression.getChildren();
            if (children[3] instanceof PsiKeyword) {
                // array of primitives
                String typeString = Formatter.format(newExpression)
                    .substring(children[3].getStartOffsetInParent(), children[3].getStartOffsetInParent() + children[3].getTextLength())
                    .replaceAll("\\d", "");
                return UMLType.extractTypeObject(typeString);
            } else if (children[3] instanceof PsiAnonymousClass) {
                // anonymous class
                return extractType(file, filePath, (PsiAnonymousClass) children[3]);
            } else if (children[5] instanceof PsiAnonymousClass) {
                // `this.new` anonymous class
                return extractType(file, filePath, (PsiAnonymousClass) children[5]);
            } else {
                throw new IllegalArgumentException();
            }
        }
    }

    @NotNull
    private static UMLType extractType(PsiFile file, String filePath, PsiAnonymousClass anonymousClass) {
        return UMLType.extractTypeObject(file, filePath, anonymousClass.getBaseClassReference(), anonymousClass.getBaseClassType());
    }
}
