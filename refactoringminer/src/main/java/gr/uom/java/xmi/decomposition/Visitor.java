package gr.uom.java.xmi.decomposition;

import com.google.common.base.Strings;
import com.intellij.psi.*;
import com.intellij.util.containers.Stack;
import gr.uom.java.xmi.Formatter;
import gr.uom.java.xmi.TypeUtils;
import org.jetbrains.annotations.NotNull;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.regex.Pattern;

import static gr.uom.java.xmi.Utils.createOrAppend;

public class Visitor extends PsiRecursiveElementWalkingVisitor {
    public static final Pattern METHOD_INVOCATION_PATTERN = Pattern.compile("!(\\w|\\.)*@\\w*");
    public static final Pattern METHOD_SIGNATURE_PATTERN =
        Pattern.compile("(public|protected|private|static|\\s)" +
            " +[\\w\\<\\>\\[\\]]+\\s+(\\w+) *\\([^\\)]*\\) *(\\{?|[^;])");
    public static final Pattern CONST_VARIABLE_PATTERN = Pattern.compile("[\\p{Upper}_]*");
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

    public Visitor(PsiFile file, String filePath) {
        this.file = file;
        this.filePath = filePath;
    }

    public static String processMethodInvocation(PsiMethodCallExpression node) {
        StringBuilder sb = new StringBuilder();
        sb.append(node.getMethodExpression().getReferenceName());
        sb.append("(");
        PsiExpression[] arguments = node.getArgumentList().getExpressions();
        if (arguments.length > 0) {
            for (int i = 0; i < arguments.length - 1; i++)
                sb.append(arguments[i].toString()).append(", ");
            sb.append(arguments[arguments.length - 1].toString());
        }
        sb.append(")");
        return sb.toString();
    }

    private static boolean isClassName(String name) {
        return Character.isUpperCase(name.charAt(0)) && !CONST_VARIABLE_PATTERN.matcher(name).matches();
    }

    private static List<String> getReference(PsiReferenceExpression reference) {
        PsiExpression qualifier = reference.getQualifierExpression();
        if (qualifier instanceof PsiThisExpression) {
            List<String> previousTokens = new ArrayList<>();
            previousTokens.add("this");
            return previousTokens;
        } else if (qualifier instanceof PsiReferenceExpression) {
            List<String> previousTokens = getReference((PsiReferenceExpression) qualifier);
            if (previousTokens == null) {
                return null;
            }
            PsiIdentifier identifier = PsiUtils.findFirstForwardSiblingOfType(qualifier, PsiIdentifier.class);
            previousTokens.add(identifier.getText());
            return previousTokens;
        } else if (qualifier == null) {
            List<String> previousTokens = new ArrayList<>();
            PsiIdentifier identifier =
                PsiUtils.findFirstForwardSiblingOfType(reference.getFirstChild(), PsiIdentifier.class);
            previousTokens.add(identifier.getText());
            return previousTokens;
        } else {
            return null;
        }
    }

    public void onLastAnonymous(Consumer<AnonymousClassDeclarationObject> consumer) {
        if (!stackAnonymous.isEmpty()) {
            consumer.accept(stackAnonymous.peek());
        }
    }

    @Override
    public void visitElement(@NotNull PsiElement element) {
        boolean goInSubtree = true;
        if (element instanceof PsiArrayAccessExpression) {
            String source = Formatter.format(element);
            arrayAccesses.add(source);
            onLastAnonymous(anonymous -> anonymous.getArrayAccesses().add(source));
        } else if (element instanceof PsiPrefixExpression) {
            String source = Formatter.format(element);
            prefixExpressions.add(source);
            onLastAnonymous(anonymous -> anonymous.getPrefixExpressions().add(source));
        } else if (element instanceof PsiPostfixExpression) {
            String source = Formatter.format(element);
            postfixExpressions.add(source);
            onLastAnonymous(anonymous -> anonymous.getPostfixExpressions().add(source));
        } else if (element instanceof PsiConditionalExpression) {
            TernaryOperatorExpression ternaryOperator =
                new TernaryOperatorExpression(file, filePath, (PsiConditionalExpression) element);
            ternaryOperatorExpressions.add(ternaryOperator);
            onLastAnonymous(anonymous -> anonymous.getTernaryOperatorExpressions().add(ternaryOperator));
        } else if (element instanceof PsiPolyadicExpression) {
            PsiPolyadicExpression polyadic = (PsiPolyadicExpression) element;
            String polyadicStr = Formatter.format(polyadic);
            String operator = Formatter.format(polyadic.getTokenBeforeOperand(polyadic.getOperands()[1]));
            infixExpressions.add(polyadicStr);
            infixOperators.add(operator);
            onLastAnonymous(anonymous -> anonymous.getInfixExpressions().add(polyadicStr));
            onLastAnonymous(anonymous -> anonymous.getInfixOperators().add(operator));
        } else if (element instanceof PsiNewExpression) {
            PsiNewExpression newExpression = (PsiNewExpression) element;
            ObjectCreation creation = new ObjectCreation(file, filePath, newExpression);
            String source = Formatter.format(element);
            creationMap.compute(source, createOrAppend(creation));
            onLastAnonymous(anonymous -> anonymous.getCreationMap().compute(source, createOrAppend(creation)));
            if (newExpression.isArrayCreation()) {
                PsiArrayInitializerExpression initializer = newExpression.getArrayInitializer();
                if (initializer != null && initializer.getInitializers().length > 10) {
                    goInSubtree = false;
                }
            } else {
                PsiExpressionList argList = newExpression.getArgumentList();
                if (argList != null) {
                    for (PsiExpression expression : argList.getExpressions()) {
                        processArgument(expression);
                    }
                }
            }
        } else if (element instanceof PsiDeclarationStatement) {
            PsiDeclarationStatement declaration = (PsiDeclarationStatement) element;
            for (PsiElement declaredElement : declaration.getDeclaredElements()) {
                if (declaredElement instanceof PsiVariable) {
                    if (!(declaredElement.getParent() instanceof PsiLambdaExpression)) {
                        VariableDeclaration variableDeclaration =
                            new VariableDeclaration(file, filePath, (PsiVariable) declaredElement);
                        variableDeclarations.add(variableDeclaration);
                        onLastAnonymous(anonymous -> anonymous.getVariableDeclarations().add(variableDeclaration));
                    }
                } else if (declaredElement instanceof PsiClass) {
                    goInSubtree = false;
                    // Local classes are not yet supported
                } else {
                    throw new AssertionError("Unqualified declared element");
                }
            }
        } else if (element instanceof PsiResourceVariable) {
            VariableDeclaration variableDeclaration = new VariableDeclaration(file, filePath, (PsiResourceVariable) element);
            variableDeclarations.add(variableDeclaration);
            onLastAnonymous(anonymous -> anonymous.getVariableDeclarations().add(variableDeclaration));
        } else if (element instanceof PsiAnonymousClass) {
            AnonymousClassDeclarationObject anonymousObject =
                new AnonymousClassDeclarationObject(file, filePath, (PsiAnonymousClass) element);
            anonymousClassDeclarations.add(anonymousObject);
            onLastAnonymous(anonymous -> anonymous.getAnonymousClassDeclarations().add(anonymousObject));
            stackAnonymous.add(anonymousObject);
        } else if (element instanceof PsiLiteralExpression) {
            Object value = ((PsiLiteral) element).getValue();
            String source = Formatter.format(element);
            if (value == null) {
                nullLiterals.add(source);
                onLastAnonymous(anonymous -> anonymous.getNullLiterals().add(source));
            } else if (value instanceof String) {
                stringLiterals.add(source);
                onLastAnonymous(anonymous -> anonymous.getStringLiterals().add(source));
            } else if (value instanceof Number) {
                numberLiterals.add(source);
                onLastAnonymous(anonymous -> anonymous.getNumberLiterals().add(source));
            } else if (value instanceof Boolean) {
                booleanLiterals.add(source);
                onLastAnonymous(anonymous -> anonymous.getBooleanLiterals().add(source));
            }
        } else if (element instanceof PsiClassObjectAccessExpression) {
            String source = Formatter.format(element);
            typeLiterals.add(source);
            onLastAnonymous(anonymous -> anonymous.getTypeLiterals().add(source));
        } else if (element instanceof PsiThisExpression) {
            String source = Formatter.format(element);
            if (!(element.getParent() instanceof PsiReference)) {
                variables.add(source);
                onLastAnonymous(anonymous -> anonymous.getVariables().add(source));
            }
        } else if (element instanceof PsiIdentifier) {
            processIdentifier((PsiIdentifier) element);
        } else if (element instanceof PsiReferenceExpression && !(element instanceof PsiMethodReferenceExpression)) {
            if (!(element.getParent() instanceof PsiMethodCallExpression)) {
                List<String> reference = getReference((PsiReferenceExpression) element);
                if (reference != null) {
                    int firstNotTypeIndex = 0;
                    for (int i = 0; i < reference.size(); i++) {
                        if (isClassName(reference.get(i))) {
                            firstNotTypeIndex = i + 1;
                        }
                    }
                    if (firstNotTypeIndex != 0) {
                        String type = String.join(".", reference.subList(0, firstNotTypeIndex));
                        types.add(type);
                        onLastAnonymous(anonymous -> anonymous.getTypes().add(type));
                    }
                    if (firstNotTypeIndex != reference.size()) {
                        String variable = String.join(".", reference);
                        variables.add(variable);
                        onLastAnonymous(anonymous -> anonymous.getVariables().add(variable));
                    }
                }
            }
        } else if (element instanceof PsiJavaCodeReferenceElement) {
            PsiJavaCodeReferenceElement reference = (PsiJavaCodeReferenceElement) element;
            goInSubtree = false;
            if (!(element.getParent() instanceof PsiAnnotation)) {
                String typeStr =
                    Formatter.format(reference) + Strings.repeat("[]", TypeUtils.arrayDimensions(reference));
                types.add(typeStr);
                onLastAnonymous(anonymous -> anonymous.getTypes().add(typeStr));
            }
        } else if (element instanceof PsiTypeElement) {
            String source = Formatter.format(element);
            goInSubtree = false;
            types.add(source);
            onLastAnonymous(anonymous -> anonymous.getTypes().add(source));
        } else if (element instanceof PsiKeyword) {
            if (PsiUtils.isTypeKeyword((PsiKeyword) element)) {
                String typeStr =
                    Formatter.format(element) + Strings.repeat("[]", TypeUtils.arrayDimensions(element));
                types.add(typeStr);
                onLastAnonymous(anonymous -> anonymous.getTypes().add(typeStr));
            }
        } else if (element instanceof PsiMethodCallExpression) {
            PsiMethodCallExpression methodCall = (PsiMethodCallExpression) element;
            PsiExpression[] arguments = methodCall.getArgumentList().getExpressions();
            for (PsiExpression argument : arguments) {
                processArgument(argument);
            }
            String source = Formatter.format(element);
            OperationInvocation invocation = new OperationInvocation(file, filePath, methodCall);
            methodInvocationMap.compute(source, createOrAppend(invocation));
            onLastAnonymous(anonymous -> anonymous.getMethodInvocationMap().compute(source, createOrAppend(invocation)));
        } else if (element instanceof PsiTypeCastExpression) {
            String source = Formatter.format(element);
            variables.add(source);
            onLastAnonymous(anonymous -> anonymous.getVariables().add(source));
        } else if (element instanceof PsiLambdaExpression) {
            LambdaExpressionObject lambda = new LambdaExpressionObject(file, filePath, (PsiLambdaExpression) element);
            lambdas.add(lambda);
            onLastAnonymous(anonymous -> anonymous.getLambdas().add(lambda));
        }
        if (goInSubtree) {
            super.visitElement(element);
        }
    }

    private void processIdentifier(@NotNull PsiIdentifier identifier) {
        String source = Formatter.format(identifier);
        PsiElement parent = identifier.getParent();
        if (!(parent instanceof PsiMethod || parent instanceof PsiParameter || parent instanceof PsiReference)) {
            variables.add(source);
            onLastAnonymous(anonymous -> anonymous.getVariables().add(source));
        }
    }

    private void processArgument(PsiExpression argument) {
        if (!(argument instanceof PsiLiteral && ((PsiLiteral) argument).getValue() != null
            || argument instanceof PsiReference
            || argument instanceof PsiThisExpression)) {
            String source = Formatter.format(argument);
            arguments.add(source);
            onLastAnonymous(anonymous -> anonymous.getArguments().add(source));
        }
    }

    @Override
    protected void elementFinished(PsiElement element) {
        if (element instanceof PsiAnonymousClass) {
            stackAnonymous.pop();
            // TODO: remove if strange condition
        }
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
