package gr.uom.java.xmi.decomposition;

import com.intellij.psi.PsiCodeBlock;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiExpression;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiLambdaExpression;
import gr.uom.java.xmi.LocationInfo;
import gr.uom.java.xmi.LocationInfo.CodeElementType;
import gr.uom.java.xmi.LocationInfoProvider;
import gr.uom.java.xmi.diff.CodeRange;

public class LambdaExpressionObject implements LocationInfoProvider {
    private final LocationInfo locationInfo;
    private OperationBody body;
    private AbstractExpression expression;

    public LambdaExpressionObject(PsiFile file, String filePath, PsiLambdaExpression lambda) {
        this.locationInfo = new LocationInfo(file, filePath, lambda, CodeElementType.LAMBDA_EXPRESSION);
        PsiElement body = lambda.getBody();
        if (body instanceof PsiCodeBlock) {
            this.body = new OperationBody(file, filePath, (PsiCodeBlock) body);
        } else if (body instanceof PsiExpression) {
            this.expression = new AbstractExpression(file, filePath, (PsiExpression) body, CodeElementType.LAMBDA_EXPRESSION_BODY);
        }
    }

    public OperationBody getBody() {
        return body;
    }

    public AbstractExpression getExpression() {
        return expression;
    }

    @Override
    public LocationInfo getLocationInfo() {
        return locationInfo;
    }

    public CodeRange codeRange() {
        return locationInfo.codeRange();
    }
}
