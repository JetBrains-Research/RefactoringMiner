package gr.uom.java.xmi.decomposition;

import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiStatement;
import gr.uom.java.xmi.Formatter;
import gr.uom.java.xmi.LocationInfo;
import gr.uom.java.xmi.LocationInfo.CodeElementType;
import gr.uom.java.xmi.diff.CodeRange;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class StatementObject extends AbstractStatement {

    private final String statement;
    private final LocationInfo locationInfo;
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

    public StatementObject(PsiFile file, String filePath, PsiStatement statement, int depth, CodeElementType codeElementType) {
        super();
        this.locationInfo = new LocationInfo(file, filePath, statement, codeElementType);
        Visitor visitor = new Visitor(file, filePath);
        statement.accept(visitor);
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
        setDepth(depth);
        this.statement = Formatter.format(statement);
    }

    @Override
    public List<StatementObject> getLeaves() {
        List<StatementObject> leaves = new ArrayList<>();
        leaves.add(this);
        return leaves;
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

    @Override
    public int statementCount() {
        return 1;
    }

    public LocationInfo getLocationInfo() {
        return locationInfo;
    }

    public CodeRange codeRange() {
        return locationInfo.codeRange();
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

    @Override
    public List<VariableDeclaration> getVariableDeclarations() {
        return variableDeclarations;
    }

    @Override
    public List<String> stringRepresentation() {
        List<String> stringRepresentation = new ArrayList<>();
        stringRepresentation.add(this.toString());
        return stringRepresentation;
    }

    public String toString() {
        return statement;
    }
}