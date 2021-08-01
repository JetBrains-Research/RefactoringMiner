package gr.uom.java.xmi;

import com.intellij.psi.JavaTokenType;
import com.intellij.psi.PsiAnonymousClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiJavaCodeReferenceElement;
import com.intellij.psi.PsiKeyword;
import com.intellij.psi.PsiNewExpression;
import com.intellij.psi.PsiTypeElement;
import com.intellij.psi.PsiVariable;
import com.intellij.psi.util.PsiUtil;
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
                return UMLType.extractTypeObject(file, filePath, (PsiKeyword) children[3]);
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
    public static UMLType extractType(PsiFile file, String filePath, PsiAnonymousClass anonymousClass) {
        return UMLType.extractTypeObject(file, filePath, anonymousClass.getBaseClassReference());
    }

    public static UMLType extractType(PsiFile file, String filePath, PsiVariable variable) {
        PsiTypeElement typeElement = variable.getTypeElement();
        if (typeElement.isInferredType()) {
            return UMLType.extractVarType(file, filePath, typeElement);
        } else {
            return UMLType.extractTypeObject(file, filePath, typeElement, variable.getType());
        }
    }

    public static int arrayDimensions(PsiElement element) {
        int arrayDimensions = 0;
        PsiElement next = element.getNextSibling();
        while (next != null) {
            if (PsiUtil.isJavaToken(next, JavaTokenType.LBRACKET)) {
                arrayDimensions++;
            }
            next = next.getNextSibling();
        }
        return arrayDimensions;
    }
}
