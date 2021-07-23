package gr.uom.java.xmi.decomposition;

import com.intellij.psi.*;
import gr.uom.java.xmi.Formatter;
import org.jetbrains.annotations.NotNull;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import static gr.uom.java.xmi.Utils.createOrAppend;

public class Visitor extends PsiRecursiveElementWalkingVisitor {
    public static final Pattern METHOD_INVOCATION_PATTERN = Pattern.compile("!(\\w|\\.)*@\\w*");
    public static final Pattern METHOD_SIGNATURE_PATTERN =
        Pattern.compile("(public|protected|private|static|\\s)" +
            " +[\\w\\<\\>\\[\\]]+\\s+(\\w+) *\\([^\\)]*\\) *(\\{?|[^;])");
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
    private final Set<PsiElement> builderPatternChains = new LinkedHashSet<>();
    private final Deque<AnonymousClassDeclarationObject> stackAnonymous = new ArrayDeque<>();

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

    @Override
    public void visitElement(@NotNull PsiElement element) {
        boolean goInSubtree = true;
        if (element instanceof PsiArrayAccessExpression) {
            String source = Formatter.format(element);
            arrayAccesses.add(source);
            if (!stackAnonymous.isEmpty()) {
                stackAnonymous.getLast().getArrayAccesses().add(source);
            }
        } else if (element instanceof PsiPrefixExpression) {
            String source = Formatter.format(element);
            prefixExpressions.add(source);
            if (!stackAnonymous.isEmpty()) {
                stackAnonymous.getLast().getPrefixExpressions().add(source);
            }
        } else if (element instanceof PsiPostfixExpression) {
            String source = Formatter.format(element);
            postfixExpressions.add(source);
            if (!stackAnonymous.isEmpty()) {
                stackAnonymous.getLast().getPostfixExpressions().add(source);
            }
        } else if (element instanceof PsiConditionalExpression) {
            TernaryOperatorExpression ternaryOperator =
                new TernaryOperatorExpression(file, filePath, (PsiConditionalExpression) element);
            ternaryOperatorExpressions.add(ternaryOperator);
            if (!stackAnonymous.isEmpty()) {
                stackAnonymous.getLast().getTernaryOperatorExpressions().add(ternaryOperator);
            }
        } else if (element instanceof PsiBinaryExpression) {
            String source = Formatter.format(element);
            String operation =
                ((PsiBinaryExpression) element).getOperationSign().getTokenType().toString();
            infixExpressions.add(source);
            infixOperators.add(operation);
            if (!stackAnonymous.isEmpty()) {
                stackAnonymous.getLast().getInfixExpressions().add(source);
                stackAnonymous.getLast().getInfixOperators().add(operation);
            }
        } else if (element instanceof PsiPolyadicExpression) {
            PsiPolyadicExpression polyadic = (PsiPolyadicExpression) element;
            infixExpressions.add(Formatter.format(polyadic));
            infixOperators.add(polyadic.getOperationTokenType().toString());
        } else if (element instanceof PsiNewExpression) {
            PsiNewExpression newExpression = (PsiNewExpression) element;
            ObjectCreation creation = new ObjectCreation(file, filePath, newExpression);
            String source = Formatter.format(element);
            creationMap.compute(source, createOrAppend(creation));
            if (!stackAnonymous.isEmpty()) {
                stackAnonymous.getLast().getCreationMap().compute(source, createOrAppend(creation));
            }
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
                        if (!stackAnonymous.isEmpty()) {
                            stackAnonymous.getLast().getVariableDeclarations().add(variableDeclaration);
                        }
                    }
                } else if (declaredElement instanceof PsiClass) {
                    // Local classes
                    AnonymousClassDeclarationObject anonymousObject =
                        new AnonymousClassDeclarationObject(file, filePath, (PsiClass) declaredElement);
                    anonymousClassDeclarations.add(anonymousObject);
                } else {
                    throw new IllegalStateException();
                }
            }
        } else if (element instanceof PsiAnonymousClass) {
            AnonymousClassDeclarationObject anonymousObject =
                new AnonymousClassDeclarationObject(file, filePath, (PsiAnonymousClass) element);
            anonymousClassDeclarations.add(anonymousObject);
        } else if (element instanceof PsiLiteralExpression) {
            Object value = ((PsiLiteral) element).getValue();
            String source = Formatter.format(element);
            if (value == null) {
                nullLiterals.add(source);
                if (!stackAnonymous.isEmpty()) {
                    stackAnonymous.getLast().getNullLiterals().add(source);
                }
            } else if (value instanceof String) {
                stringLiterals.add(source);
                if (!stackAnonymous.isEmpty()) {
                    stackAnonymous.getLast().getStringLiterals().add(source);
                }
            } else if (value instanceof Number) {
                numberLiterals.add(source);
                if (!stackAnonymous.isEmpty()) {
                    stackAnonymous.getLast().getNumberLiterals().add(source);
                }
            } else if (value instanceof Boolean) {
                booleanLiterals.add(source);
                if (!stackAnonymous.isEmpty()) {
                    stackAnonymous.getLast().getBooleanLiterals().add(source);
                }
            }
        } else if (element instanceof PsiClassObjectAccessExpression) {
            String source = Formatter.format(element);
            typeLiterals.add(source);
            if (!stackAnonymous.isEmpty()) {
                stackAnonymous.getLast().getTypeLiterals().add(source);
            }
        } else if (element instanceof PsiThisExpression) {
            String source = Formatter.format(element);
            if (!(element.getParent() instanceof PsiReference)) {
                variables.add(source);
                if (!stackAnonymous.isEmpty()) {
                    stackAnonymous.getLast().getVariables().add(source);
                }
            }
        } else if (element instanceof PsiIdentifier) {
            processIdentifier((PsiIdentifier) element);
        } else if (element instanceof PsiReferenceExpression) {
            PsiElement firstChild = element.getFirstChild();
            if (firstChild instanceof PsiThisExpression) {
                variables.add(Formatter.format(element));
            } else if (element.getChildren().length == 2) {
                // find identifier and add to variables
                if (firstChild instanceof PsiIdentifier) {
                    variables.add(Formatter.format(firstChild));
                } else {
                    PsiElement lastChild = element.getLastChild();
                    if (lastChild instanceof PsiIdentifier) {
                        variables.add(Formatter.format(lastChild));
                    }
                }
            }
        } else if (element instanceof PsiJavaCodeReferenceElement) {
            PsiJavaCodeReferenceElement reference = (PsiJavaCodeReferenceElement) element;
            goInSubtree = false;
            assert !(reference.getParent() instanceof PsiReference);
            if (!(reference.getParent() instanceof PsiTypeElement)) {
                types.add(Formatter.format(reference));
            }
        } else if (element instanceof PsiTypeElement) {
            String source = Formatter.format(element);
            goInSubtree = false;
            types.add(source);
        } else if (element instanceof PsiMethodCallExpression) {
            PsiMethodCallExpression methodCall = (PsiMethodCallExpression) element;
            PsiExpression[] arguments = methodCall.getArgumentList().getExpressions();
            for (PsiExpression argument : arguments) {
                processArgument(argument);
            }
            String source = Formatter.format(element);
            // TODO: adding to builder pattern chains & stopping
            OperationInvocation invocation = new OperationInvocation(file, filePath, methodCall);
            methodInvocationMap.compute(source, createOrAppend(invocation));
            // TODO: super, this constructor??
        } else if (element instanceof PsiTypeCastExpression) {
            variables.add(Formatter.format(element));
        } else if (element instanceof PsiLambdaExpression) {
            LambdaExpressionObject lambda = new LambdaExpressionObject(file, filePath, (PsiLambdaExpression) element);
            lambdas.add(lambda);
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
        }
    }

    private void processArgument(PsiExpression argument) {
        if (!(argument instanceof PsiLiteral
            || argument instanceof PsiReference
            || argument instanceof PsiThisExpression)) {
            arguments.add(Formatter.format(argument));
        }
    }

    @Override
    protected void elementFinished(PsiElement element) {
        if (element instanceof PsiAnonymousClass) {
            AnonymousClassDeclarationObject removedAnonymous = stackAnonymous.pollLast();
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
