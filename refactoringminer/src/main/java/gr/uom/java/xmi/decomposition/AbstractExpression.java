package gr.uom.java.xmi.decomposition;

import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import gr.uom.java.xmi.LocationInfo;
import gr.uom.java.xmi.LocationInfo.CodeElementType;
import gr.uom.java.xmi.diff.CodeRange;
import org.jetbrains.annotations.NotNull;
import java.util.List;
import java.util.Map;

public class AbstractExpression extends AbstractCodeFragment {
    private final String expression;
    private final LocationInfo locationInfo;
    private CompositeStatementObject owner;
    private final List<String> variables;
    private final List<String> types;
    private final List<VariableDeclaration> variableDeclarations;
    private final Map<String, List<OperationInvocation>> methodInvocationMap;
    private final List<AnonymousClassDeclarationObject> anonymousClassDeclarations;
    private final List<String> stringLiterals;
    private final List<String> numberLiterals;
    private final List<String> nullLiterals;
    private final List<String> booleanLiterals;
    private final List<String> typeLiterals;
    private final Map<String, List<ObjectCreation>> creationMap;
    private final List<String> infixExpressions;
    private final List<String> infixOperators;
    private final List<String> arrayAccesses;
    private final List<String> prefixExpressions;
    private final List<String> postfixExpressions;
    private final List<String> arguments;
    private final List<TernaryOperatorExpression> ternaryOperatorExpressions;
    private final List<LambdaExpressionObject> lambdas;

    public AbstractExpression(@NotNull PsiFile file, @NotNull String filePath, @NotNull PsiElement expression,
                              @NotNull CodeElementType codeElementType) {
        this.locationInfo = new LocationInfo(file, filePath, expression, codeElementType);
        Visitor visitor = new Visitor(file, filePath);
        expression.accept(visitor);
        this.variables = visitor.getVariables();
        this.types = visitor.getTypes();
        this.variableDeclarations = visitor.getVariableDeclarations();
        this.methodInvocationMap = visitor.getMethodInvocationMap();
        this.anonymousClassDeclarations = visitor.getAnonymousClassDeclarations();
        this.stringLiterals = visitor.getStringLiterals();
        this.numberLiterals = visitor.getNumberLiterals();
        this.nullLiterals = visitor.getNullLiterals();
        this.booleanLiterals = visitor.getBooleanLiterals();
        this.typeLiterals = visitor.getTypeLiterals();
        this.creationMap = visitor.getCreationMap();
        this.infixExpressions = visitor.getInfixExpressions();
        this.infixOperators = visitor.getInfixOperators();
        this.arrayAccesses = visitor.getArrayAccesses();
        this.prefixExpressions = visitor.getPrefixExpressions();
        this.postfixExpressions = visitor.getPostfixExpressions();
        this.arguments = visitor.getArguments();
        this.ternaryOperatorExpressions = visitor.getTernaryOperatorExpressions();
        this.lambdas = visitor.getLambdas();
        this.expression = expression.getText();
        this.owner = null;
    }

    public void setOwner(CompositeStatementObject owner) {
        this.owner = owner;
    }

    public CompositeStatementObject getOwner() {
        return this.owner;
    }

    @Override
    public CompositeStatementObject getParent() {
        return getOwner();
    }

    public String getExpression() {
        return expression;
    }

    public String getString() {
        return toString();
    }

    public String toString() {
        return getExpression();
    }

    @Override
    public List<String> getVariables() {
        return variables;
    }

    @Override
    public List<String> getTypes() {
        return types;
    }

    @Override
    public List<VariableDeclaration> getVariableDeclarations() {
        return variableDeclarations;
    }

    @Override
    public Map<String, List<OperationInvocation>> getMethodInvocationMap() {
        return methodInvocationMap;
    }

    @Override
    public List<AnonymousClassDeclarationObject> getAnonymousClassDeclarations() {
        return anonymousClassDeclarations;
    }

    @Override
    public List<String> getStringLiterals() {
        return stringLiterals;
    }

    @Override
    public List<String> getNumberLiterals() {
        return numberLiterals;
    }

    @Override
    public List<String> getNullLiterals() {
        return nullLiterals;
    }

    @Override
    public List<String> getBooleanLiterals() {
        return booleanLiterals;
    }

    @Override
    public List<String> getTypeLiterals() {
        return typeLiterals;
    }

    @Override
    public Map<String, List<ObjectCreation>> getCreationMap() {
        return creationMap;
    }

    @Override
    public List<String> getInfixExpressions() {
        return infixExpressions;
    }

    @Override
    public List<String> getInfixOperators() {
        return infixOperators;
    }

    @Override
    public List<String> getArrayAccesses() {
        return arrayAccesses;
    }

    @Override
    public List<String> getPrefixExpressions() {
        return prefixExpressions;
    }

    @Override
    public List<String> getPostfixExpressions() {
        return postfixExpressions;
    }

    @Override
    public List<String> getArguments() {
        return arguments;
    }

    @Override
    public List<TernaryOperatorExpression> getTernaryOperatorExpressions() {
        return ternaryOperatorExpressions;
    }

    @Override
    public List<LambdaExpressionObject> getLambdas() {
        return lambdas;
    }

    public LocationInfo getLocationInfo() {
        return locationInfo;
    }

    public VariableDeclaration searchVariableDeclaration(String variableName) {
        VariableDeclaration variableDeclaration = this.getVariableDeclaration(variableName);
        if (variableDeclaration != null) {
            return variableDeclaration;
        } else if (owner != null) {
            return owner.searchVariableDeclaration(variableName);
        }
        return null;
    }

    public VariableDeclaration getVariableDeclaration(String variableName) {
        List<VariableDeclaration> variableDeclarations = getVariableDeclarations();
        for (VariableDeclaration declaration : variableDeclarations) {
            if (declaration.getVariableName().equals(variableName)) {
                return declaration;
            }
        }
        return null;
    }

    public CodeRange codeRange() {
        return locationInfo.codeRange();
    }
}
