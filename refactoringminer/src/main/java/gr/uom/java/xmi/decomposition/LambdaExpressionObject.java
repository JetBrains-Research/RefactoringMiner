package gr.uom.java.xmi.decomposition;

import gr.uom.java.xmi.LocationInfo;
import gr.uom.java.xmi.LocationInfo.CodeElementType;
import gr.uom.java.xmi.LocationInfoProvider;
import gr.uom.java.xmi.diff.CodeRange;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.LambdaExpression;

public class LambdaExpressionObject implements LocationInfoProvider {
    private final LocationInfo locationInfo;
    private OperationBody body;
    private AbstractExpression expression;

    public LambdaExpressionObject(CompilationUnit cu, String filePath, LambdaExpression lambda) {
        this.locationInfo = new LocationInfo(cu, filePath, lambda, CodeElementType.LAMBDA_EXPRESSION);
        if (lambda.getBody() instanceof Block) {
            this.body = new OperationBody(cu, filePath, (Block) lambda.getBody());
        } else if (lambda.getBody() instanceof Expression) {
            this.expression = new AbstractExpression(cu, filePath, (Expression) lambda.getBody(), CodeElementType.LAMBDA_EXPRESSION_BODY);
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
