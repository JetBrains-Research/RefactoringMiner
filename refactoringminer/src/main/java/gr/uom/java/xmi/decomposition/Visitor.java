package gr.uom.java.xmi.decomposition;

import com.intellij.psi.*;
import org.jetbrains.annotations.NotNull;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

public class Visitor extends PsiElementVisitor {
    public static final Pattern METHOD_INVOCATION_PATTERN = Pattern.compile("!(\\w|\\.)*@\\w*");
    public static final Pattern METHOD_SIGNATURE_PATTERN = Pattern.compile("(public|protected|private|static|\\s) +[\\w\\<\\>\\[\\]]+\\s+(\\w+) *\\([^\\)]*\\) *(\\{?|[^;])");
    private PsiFile file;
    private String filePath;
    private List<String> variables = new ArrayList<String>();
    private List<String> types = new ArrayList<String>();
    private Map<String, List<OperationInvocation>> methodInvocationMap = new LinkedHashMap<String, List<OperationInvocation>>();
    private List<VariableDeclaration> variableDeclarations = new ArrayList<VariableDeclaration>();
    private List<AnonymousClassDeclarationObject> anonymousClassDeclarations = new ArrayList<AnonymousClassDeclarationObject>();
    private List<String> stringLiterals = new ArrayList<String>();
    private List<String> numberLiterals = new ArrayList<String>();
    private List<String> nullLiterals = new ArrayList<String>();
    private List<String> booleanLiterals = new ArrayList<String>();
    private List<String> typeLiterals = new ArrayList<String>();
    private Map<String, List<ObjectCreation>> creationMap = new LinkedHashMap<String, List<ObjectCreation>>();
    private List<String> infixExpressions = new ArrayList<String>();
    private List<String> infixOperators = new ArrayList<String>();
    private List<String> arrayAccesses = new ArrayList<String>();
    private List<String> prefixExpressions = new ArrayList<String>();
    private List<String> postfixExpressions = new ArrayList<String>();
    private List<String> arguments = new ArrayList<String>();
    private List<TernaryOperatorExpression> ternaryOperatorExpressions = new ArrayList<TernaryOperatorExpression>();
    private List<LambdaExpressionObject> lambdas = new ArrayList<LambdaExpressionObject>();
    private Deque<AnonymousClassDeclarationObject> stackAnonymous = new ArrayDeque<>();

    public Visitor(PsiFile file, String filePath) {
        this.file = file;
        this.filePath = filePath;
    }

    // TODO:
    @Override
    public void visitElement(@NotNull PsiElement element) {
        if (element instanceof PsiArrayAccessExpression) {
            arrayAccesses.add(element.getText());
        } else if (element instanceof PsiPrefixExpression) {

        } else if (element instanceof PsiPostfixExpression) {

        } else if (element instanceof PsiConditionalExpression) {

        } else if (element instanceof PsiBinaryExpression) {

        } else if (element instanceof PsiConstructorCall) {

        } else if (element instanceof PsiArrayInitializerExpression) {

        } else if (element instanceof PsiDeclarationStatement) {

        } else if (element instanceof PsiAnonymousClass) {

        } else if (element instanceof PsiLiteral) {

        } else if (element instanceof PsiThisExpression) {

        } else if (element instanceof PsiIdentifier) {

        } else if (element instanceof PsiType) {

        } else if (element instanceof PsiMethodCallExpression) {

        } else if (element instanceof PsiSuperExpression) {

        } else if (element instanceof PsiQualifiedNamedElement) {

        } else if (element instanceof PsiTypeCastExpression) {

        } else if (element instanceof PsiLambdaExpression) {

        } else {
            System.out.println(element.getClass().getName());
        }
        super.visitElement(element);
    }

    public Map<String, List<OperationInvocation>> getMethodInvocationMap() {
        return this.methodInvocationMap;
    }

    public List<VariableDeclaration> getVariableDeclarations() {
        return variableDeclarations;
    }

    public List<String> getTypes() {
        return types;
    }

    public List<AnonymousClassDeclarationObject> getAnonymousClassDeclarations() {
        return anonymousClassDeclarations;
    }

    public List<String> getStringLiterals() {
        return stringLiterals;
    }

    public List<String> getNumberLiterals() {
        return numberLiterals;
    }

    public List<String> getNullLiterals() {
        return nullLiterals;
    }

    public List<String> getBooleanLiterals() {
        return booleanLiterals;
    }

    public List<String> getTypeLiterals() {
        return typeLiterals;
    }

    public Map<String, List<ObjectCreation>> getCreationMap() {
        return creationMap;
    }

    public List<String> getInfixExpressions() {
        return infixExpressions;
    }

    public List<String> getInfixOperators() {
        return infixOperators;
    }

    public List<String> getArrayAccesses() {
        return arrayAccesses;
    }

    public List<String> getPrefixExpressions() {
        return prefixExpressions;
    }

    public List<String> getPostfixExpressions() {
        return postfixExpressions;
    }

    public List<String> getArguments() {
        return this.arguments;
    }

    public List<TernaryOperatorExpression> getTernaryOperatorExpressions() {
        return ternaryOperatorExpressions;
    }

    public List<String> getVariables() {
        return variables;
    }

    public List<LambdaExpressionObject> getLambdas() {
        return lambdas;
    }
}
