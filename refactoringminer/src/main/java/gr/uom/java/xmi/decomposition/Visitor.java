package gr.uom.java.xmi.decomposition;

import com.google.common.base.Strings;
import com.intellij.psi.*;
import com.intellij.util.containers.Stack;
import gr.uom.java.xmi.Formatter;
import gr.uom.java.xmi.TypeUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import static gr.uom.java.xmi.Utils.createOrAppend;

public class Visitor extends PsiRecursiveElementWalkingVisitor {
    public static final Pattern METHOD_SIGNATURE_PATTERN =
        Pattern.compile("(public|protected|private|static|\\s)" +
            " +[\\w<>\\[\\]]+\\s+(\\w+) *\\([^)]*\\) *(\\{?|[^;])");
    public static final Pattern CONST_VARIABLE_PATTERN = Pattern.compile("[\\p{Upper}_]+");
    private final PsiFile file;
    private final String filePath;
    private final List<String> variables = new ArrayList<>();
    private final List<String> types = new ArrayList<>();
    private final Map<String, List<OperationInvocation>> methodInvocationMap = new LinkedHashMap<>();
    private final List<VariableDeclaration> variableDeclarations = new ArrayList<>();
    private final List<AnonymousClassDeclarationObject> anonymousClassDeclarations = new ArrayList<>();
    private final List<String> stringLiterals = new ArrayList<>();
    private final List<String> numberLiterals = new ArrayList<>();
    private final List<String> nullLiterals = new ArrayList<>();
    private final List<String> booleanLiterals = new ArrayList<>();
    private final List<String> typeLiterals = new ArrayList<>();
    private final Map<String, List<ObjectCreation>> creationMap = new LinkedHashMap<>();
    private final List<String> infixExpressions = new ArrayList<>();
    private final List<String> infixOperators = new ArrayList<>();
    private final List<String> arrayAccesses = new ArrayList<>();
    private final List<String> prefixExpressions = new ArrayList<>();
    private final List<String> postfixExpressions = new ArrayList<>();
    private final List<String> arguments = new ArrayList<>();
    private final List<TernaryOperatorExpression> ternaryOperatorExpressions = new ArrayList<>();
    private final List<LambdaExpressionObject> lambdas = new ArrayList<>();
    private final Stack<AnonymousClassDeclarationObject> stackAnonymous = new Stack<>();

    public Visitor(@NotNull PsiFile file, @NotNull String filePath) {
        this.file = file;
        this.filePath = filePath;
    }

    private static boolean isClassName(@NotNull String name) {
        return Character.isUpperCase(name.charAt(0)) && !CONST_VARIABLE_PATTERN.matcher(name).matches();
    }

    private static @Nullable List<String> getReferenceTokens(@NotNull PsiReferenceExpression reference) {
        PsiExpression qualifier = reference.getQualifierExpression();
        List<String> previousTokens = null;
        if (qualifier instanceof PsiThisExpression) {
            previousTokens = new ArrayList<>();
            previousTokens.add("this");
        } else if (qualifier instanceof PsiReferenceExpression) {
            previousTokens = getReferenceTokens((PsiReferenceExpression) qualifier);
        } else if (qualifier == null) {
            previousTokens = new ArrayList<>();
        }
        if (previousTokens == null) {
            return null;
        }
        PsiIdentifier identifier = PsiUtils.findFirstChildOfType(reference, PsiIdentifier.class);
        previousTokens.add(identifier.getText());
        return previousTokens;
    }

    @Override
    public void visitElement(@NotNull PsiElement element) {
        boolean goInSubtree = true;
        if (element instanceof PsiArrayAccessExpression) {
            String source = Formatter.format(element);
            addArrayAccess(source);
        } else if (element instanceof PsiPrefixExpression) {
            String source = Formatter.format(element);
            addPrefixExpression(source);
        } else if (element instanceof PsiPostfixExpression) {
            String source = Formatter.format(element);
            addPostfixExpression(source);
        } else if (element instanceof PsiConditionalExpression) {
            TernaryOperatorExpression ternaryOperator =
                new TernaryOperatorExpression(file, filePath, (PsiConditionalExpression) element);
            addTernaryOperator(ternaryOperator);
        } else if (element instanceof PsiPolyadicExpression) {
            PsiPolyadicExpression polyadic = (PsiPolyadicExpression) element;
            String polyadicStr = Formatter.format(polyadic);
            String operator = Formatter.format(polyadic.getTokenBeforeOperand(polyadic.getOperands()[1]));
            addInfixExpression(polyadicStr);
            addInfixOperator(operator);
        } else if (element instanceof PsiNewExpression) {
            goInSubtree = processNewExpression((PsiNewExpression) element);
        } else if (element instanceof PsiDeclarationStatement) {
            goInSubtree = processDeclaration((PsiDeclarationStatement) element);
        } else if (element instanceof PsiResourceVariable) {
            VariableDeclaration variableDeclaration = new VariableDeclaration(file, filePath, (PsiResourceVariable) element);
            addVariableDeclaration(variableDeclaration);
        } else if (element instanceof PsiAnonymousClass) {
            AnonymousClassDeclarationObject anonymousObject =
                new AnonymousClassDeclarationObject(file, filePath, (PsiAnonymousClass) element);
            addAnonymousClassDeclaration(anonymousObject);
            stackAnonymous.add(anonymousObject);
        } else if (element instanceof PsiLiteralExpression) {
            processLiteral((PsiLiteral) element);
        } else if (element instanceof PsiClassObjectAccessExpression) {
            String source = Formatter.format(element);
            addTypeLiteral(source);
        } else if (element instanceof PsiThisExpression) {
            if (!(element.getParent() instanceof PsiReference)) {
                String source = Formatter.format(element);
                addVariable(source);
            }
        } else if (element instanceof PsiIdentifier) {
            processIdentifier((PsiIdentifier) element);
        } else if (element instanceof PsiReferenceExpression) {
            processReference((PsiReferenceExpression) element);
        } else if (element instanceof PsiJavaCodeReferenceElement) {
            goInSubtree = false;
            if (!(element.getParent() instanceof PsiAnnotation)) {
                processTypeIdentifier(element);
            }
        } else if (element instanceof PsiTypeElement) {
            goInSubtree = false;
            String source = Formatter.format(element);
            addType(source);
        } else if (element instanceof PsiKeyword) {
            goInSubtree = false;
            if (PsiUtils.isTypeKeyword((PsiKeyword) element)) {
                processTypeIdentifier(element);
            }
        } else if (element instanceof PsiMethodCallExpression) {
            processMethodCall((PsiMethodCallExpression) element);
        } else if (element instanceof PsiTypeCastExpression) {
            String source = Formatter.format(element);
            addVariable(source);
        } else if (element instanceof PsiLambdaExpression) {
            LambdaExpressionObject lambda = new LambdaExpressionObject(file, filePath, (PsiLambdaExpression) element);
            addLambda(lambda);
        }
        if (goInSubtree) {
            super.visitElement(element);
        }
    }

    private void addLambda(LambdaExpressionObject lambda) {
        if (stackAnonymous.isEmpty()) {
            lambdas.add(lambda);
        } else {
            stackAnonymous.peek().getLambdas().add(lambda);
        }
    }

    private void addType(String source) {
        if (stackAnonymous.isEmpty()) {
            types.add(source);
        } else {
            stackAnonymous.peek().getTypes().add(source);
        }
    }

    private void addVariable(String source) {
        if (stackAnonymous.isEmpty()) {
            variables.add(source);
        } else {
            stackAnonymous.peek().getVariables().add(source);
        }
    }

    private void addTypeLiteral(String source) {
        if (stackAnonymous.isEmpty()) {
            typeLiterals.add(source);
        } else {
            stackAnonymous.peek().getTypeLiterals().add(source);
        }
    }

    private void addAnonymousClassDeclaration(AnonymousClassDeclarationObject anonymousObject) {
        if (stackAnonymous.isEmpty()) {
            anonymousClassDeclarations.add(anonymousObject);
        } else {
            stackAnonymous.peek().getAnonymousClassDeclarations().add(anonymousObject);
        }
    }

    private void addVariableDeclaration(VariableDeclaration variableDeclaration) {
        if (stackAnonymous.isEmpty()) {
            variableDeclarations.add(variableDeclaration);
        } else {
            stackAnonymous.peek().getVariableDeclarations().add(variableDeclaration);
        }
    }

    private void addInfixExpression(String polyadicStr) {
        if (stackAnonymous.isEmpty()) {
            infixExpressions.add(polyadicStr);
        } else {
            stackAnonymous.peek().getInfixExpressions().add(polyadicStr);
        }
    }

    private void addInfixOperator(String operator) {
        if (stackAnonymous.isEmpty()) {
            infixOperators.add(operator);
        } else {
            stackAnonymous.peek().getInfixOperators().add(operator);
        }
    }

    private void addTernaryOperator(TernaryOperatorExpression ternaryOperator) {
        if (stackAnonymous.isEmpty()) {
            ternaryOperatorExpressions.add(ternaryOperator);
        } else {
            stackAnonymous.peek().getTernaryOperatorExpressions().add(ternaryOperator);
        }
    }

    private void addPostfixExpression(String source) {
        if (stackAnonymous.isEmpty()) {
            postfixExpressions.add(source);
        } else {
            stackAnonymous.peek().getPostfixExpressions().add(source);
        }
    }

    private void addPrefixExpression(String source) {
        if (stackAnonymous.isEmpty()) {
            prefixExpressions.add(source);
        } else {
            stackAnonymous.peek().getPrefixExpressions().add(source);
        }
    }

    private void addArrayAccess(String source) {
        if (stackAnonymous.isEmpty()) {
            arrayAccesses.add(source);
        } else {
            stackAnonymous.peek().getArrayAccesses().add(source);
        }
    }

    private void addArgument(String source) {
        if (stackAnonymous.isEmpty()) {
            arguments.add(source);
        } else {
            stackAnonymous.peek().getArguments().add(source);
        }
    }

    private void addMethodInvocation(String source, OperationInvocation invocation) {
        if (stackAnonymous.isEmpty()) {
            methodInvocationMap.compute(source, createOrAppend(invocation));
        } else {
            stackAnonymous.peek().getMethodInvocationMap().compute(source, createOrAppend(invocation));
        }
    }

    private void addBooleanLiteral(String source) {
        if (stackAnonymous.isEmpty()) {
            booleanLiterals.add(source);
        } else {
            stackAnonymous.peek().getBooleanLiterals().add(source);
        }
    }

    private void addNumberLiteral(String source) {
        if (stackAnonymous.isEmpty()) {
            numberLiterals.add(source);
        } else {
            stackAnonymous.peek().getNumberLiterals().add(source);
        }
    }

    private void addStringLiteral(String source) {
        if (stackAnonymous.isEmpty()) {
            stringLiterals.add(source);
        } else {
            stackAnonymous.peek().getStringLiterals().add(source);
        }
    }

    private void addNullLiteral(String source) {
        if (stackAnonymous.isEmpty()) {
            nullLiterals.add(source);
        } else {
            stackAnonymous.peek().getNullLiterals().add(source);
        }
    }

    private void addCreation(ObjectCreation creation, String source) {
        if (stackAnonymous.isEmpty()) {
            creationMap.compute(source, createOrAppend(creation));
        } else {
            stackAnonymous.peek().getCreationMap().compute(source, createOrAppend(creation));
        }
    }

    @Override
    protected void elementFinished(PsiElement element) {
        if (element instanceof PsiAnonymousClass) {
            stackAnonymous.pop();
        }
    }

    private void processMethodCall(@NotNull PsiMethodCallExpression methodCall) {
        PsiExpression[] arguments = methodCall.getArgumentList().getExpressions();
        for (PsiExpression argument : arguments) {
            processArgument(argument);
        }
        String source = Formatter.format(methodCall);
        OperationInvocation invocation = new OperationInvocation(file, filePath, methodCall);
        addMethodInvocation(source, invocation);
    }

    private void processLiteral(@NotNull PsiLiteral literal) {
        Object value = literal.getValue();
        String source = Formatter.format(literal);
        if (value == null) {
            addNullLiteral(source);
        } else if (value instanceof String) {
            addStringLiteral(source);
        } else if (value instanceof Number) {
            addNumberLiteral(source);
        } else if (value instanceof Boolean) {
            addBooleanLiteral(source);
        } else {
            // Characters not processed
            assert value instanceof Character;
        }
    }

    /**
     * @param element TypeKeyword or PsiJavaCodeReferenceElement
     */
    private void processTypeIdentifier(@NotNull PsiElement element) {
        int arrayDimensions = TypeUtils.arrayDimensions(element);
        String typeStr = Formatter.format(element) + Strings.repeat("[]", arrayDimensions);
        addType(typeStr);
    }

    private boolean processDeclaration(@NotNull PsiDeclarationStatement declaration) {
        for (PsiElement declaredElement : declaration.getDeclaredElements()) {
            if (declaredElement instanceof PsiVariable) {
                VariableDeclaration variableDeclaration =
                    new VariableDeclaration(file, filePath, (PsiVariable) declaredElement);
                addVariableDeclaration(variableDeclaration);
            } else if (declaredElement instanceof PsiClass) {
                // Local classes are not yet supported
                return false;
            } else {
                throw new AssertionError("Unqualified declared element");
            }
        }
        return true;
    }

    private void processReference(@NotNull PsiReferenceExpression element) {
        if (element.getParent() instanceof PsiMethodCallExpression
            || element instanceof PsiMethodReferenceExpression) {
            return;
        }
        List<String> reference = getReferenceTokens(element);
        if (reference != null) {
            int firstNotTypeIndex = 0;
            for (int i = 0; i < reference.size(); i++) {
                if (isClassName(reference.get(i))) {
                    firstNotTypeIndex = i + 1;
                }
            }
            if (firstNotTypeIndex != 0) {
                String type = String.join(".", reference.subList(0, firstNotTypeIndex));
                addType(type);
            }
            if (firstNotTypeIndex != reference.size()) {
                String variable = String.join(".", reference);
                addVariable(variable);
            }
        }
    }

    private boolean processNewExpression(@NotNull PsiNewExpression newExpression) {
        ObjectCreation creation = new ObjectCreation(file, filePath, newExpression);
        String source = Formatter.format(newExpression);
        addCreation(creation, source);
        if (newExpression.isArrayCreation()) {
            PsiArrayInitializerExpression initializer = newExpression.getArrayInitializer();
            return initializer == null || initializer.getInitializers().length <= 10;
        } else {
            PsiExpressionList argList = newExpression.getArgumentList();
            if (argList != null) {
                for (PsiExpression expression : argList.getExpressions()) {
                    processArgument(expression);
                }
            }
        }
        return true;
    }

    private void processIdentifier(@NotNull PsiIdentifier identifier) {
        String source = Formatter.format(identifier);
        PsiElement parent = identifier.getParent();
        if (!(parent instanceof PsiMethod || parent instanceof PsiReference)) {
            addVariable(source);
        }
    }

    private void processArgument(@NotNull PsiExpression argument) {
        if (argument instanceof PsiLiteral && ((PsiLiteral) argument).getValue() != null
            || argument instanceof PsiReference
            || argument instanceof PsiThisExpression) {
            return;
        }
        String source = Formatter.format(argument);
        addArgument(source);
    }

    public @NotNull Map<String, List<OperationInvocation>> getMethodInvocationMap() {
        return this.methodInvocationMap;
    }

    public @NotNull List<VariableDeclaration> getVariableDeclarations() {
        return variableDeclarations;
    }

    public @NotNull List<String> getTypes() {
        return types;
    }

    public @NotNull List<AnonymousClassDeclarationObject> getAnonymousClassDeclarations() {
        return anonymousClassDeclarations;
    }

    public @NotNull List<String> getStringLiterals() {
        return stringLiterals;
    }

    public @NotNull List<String> getNumberLiterals() {
        return numberLiterals;
    }

    public @NotNull List<String> getNullLiterals() {
        return nullLiterals;
    }

    public @NotNull List<String> getBooleanLiterals() {
        return booleanLiterals;
    }

    public @NotNull List<String> getTypeLiterals() {
        return typeLiterals;
    }

    public @NotNull Map<String, List<ObjectCreation>> getCreationMap() {
        return creationMap;
    }

    public @NotNull List<String> getInfixExpressions() {
        return infixExpressions;
    }

    public @NotNull List<String> getInfixOperators() {
        return infixOperators;
    }

    public @NotNull List<String> getArrayAccesses() {
        return arrayAccesses;
    }

    public @NotNull List<String> getPrefixExpressions() {
        return prefixExpressions;
    }

    public @NotNull List<String> getPostfixExpressions() {
        return postfixExpressions;
    }

    public @NotNull List<String> getArguments() {
        return this.arguments;
    }

    public @NotNull List<TernaryOperatorExpression> getTernaryOperatorExpressions() {
        return ternaryOperatorExpressions;
    }

    public @NotNull List<String> getVariables() {
        return variables;
    }

    public @NotNull List<LambdaExpressionObject> getLambdas() {
        return lambdas;
    }
}
