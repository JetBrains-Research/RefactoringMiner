package gr.uom.java.xmi.decomposition;

import com.intellij.psi.*;
import gr.uom.java.xmi.Formatter;
import gr.uom.java.xmi.LocationInfo;
import gr.uom.java.xmi.LocationInfo.CodeElementType;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static gr.uom.java.xmi.decomposition.PsiUtils.isSuperConstructorInvocation;
import static gr.uom.java.xmi.decomposition.PsiUtils.isThisConstructorInvocation;

public class OperationBody {

    private final CompositeStatementObject compositeStatement;
    private List<String> stringRepresentation;
    private boolean containsAssertion;
    private Set<VariableDeclaration> activeVariableDeclarations;

    public OperationBody(PsiFile file, String filePath, PsiCodeBlock codeBlock) {
        this(file, filePath, codeBlock, Collections.emptyList());
    }

    public OperationBody(PsiFile file, String filePath, PsiCodeBlock codeBlock, List<VariableDeclaration> parameters) {
        this.compositeStatement = new CompositeStatementObject(file, filePath, codeBlock, 0, CodeElementType.BLOCK);
        this.activeVariableDeclarations = new HashSet<>(parameters);
        PsiStatement[] statements = codeBlock.getStatements();
        for (PsiStatement statement : statements) {
            processStatement(file, filePath, compositeStatement, statement);
        }
        for (OperationInvocation invocation : getAllOperationInvocations()) {
            if (invocation.getName().startsWith("assert")) {
                containsAssertion = true;
                break;
            }
        }
        this.activeVariableDeclarations = null;
    }

    public int statementCount() {
        return compositeStatement.statementCount();
    }

    public CompositeStatementObject getCompositeStatement() {
        return compositeStatement;
    }

    public boolean containsAssertion() {
        return containsAssertion;
    }

    public List<OperationInvocation> getAllOperationInvocations() {
        List<OperationInvocation> invocations = new ArrayList<>();
        Map<String, List<OperationInvocation>> invocationMap = compositeStatement.getAllMethodInvocations();
        for (String key : invocationMap.keySet()) {
            invocations.addAll(invocationMap.get(key));
        }
        return invocations;
    }

    public List<AnonymousClassDeclarationObject> getAllAnonymousClassDeclarations() {
        return new ArrayList<>(compositeStatement.getAllAnonymousClassDeclarations());
    }

    public List<LambdaExpressionObject> getAllLambdas() {
        return new ArrayList<>(compositeStatement.getAllLambdas());
    }

    public List<String> getAllVariables() {
        return new ArrayList<>(compositeStatement.getAllVariables());
    }

    public List<VariableDeclaration> getAllVariableDeclarations() {
        return new ArrayList<>(compositeStatement.getAllVariableDeclarations());
    }

    public List<VariableDeclaration> getVariableDeclarationsInScope(LocationInfo location) {
        return new ArrayList<>(compositeStatement.getVariableDeclarationsInScope(location));
    }

    public VariableDeclaration getVariableDeclaration(String variableName) {
        return compositeStatement.getVariableDeclaration(variableName);
    }

    private void processStatement(PsiFile file, String filePath, CompositeStatementObject parent, PsiCodeBlock codeBlock) {
        CompositeStatementObject blockChild = new CompositeStatementObject(file, filePath, codeBlock, parent.getDepth() + 1, CodeElementType.BLOCK);
        parent.addStatement(blockChild);
        addStatementInVariableScopes(blockChild);
        PsiStatement[] blockStatements = codeBlock.getStatements();
        for (PsiStatement blockStatement : blockStatements) {
            processStatement(file, filePath, blockChild, blockStatement);
        }
    }

    private CodeElementType getCodeElementType(PsiExpression expression) {
        if (expression instanceof PsiMethodCallExpression) {
            PsiMethodCallExpression callExpression = (PsiMethodCallExpression) expression;
            if (isThisConstructorInvocation(callExpression)) {
                return CodeElementType.CONSTRUCTOR_INVOCATION;
            } else if (isSuperConstructorInvocation(callExpression)) {
                return CodeElementType.SUPER_CONSTRUCTOR_INVOCATION;
            }
        }
        return CodeElementType.EXPRESSION_STATEMENT;
    }

    private void processStatement(PsiFile file, String filePath, CompositeStatementObject parent, PsiExpression expression) {
        StatementObject child = new StatementObject(file, filePath, (PsiExpressionStatement) expression.getParent(), parent.getDepth() + 1, getCodeElementType(expression));
        parent.addStatement(child);
        addStatementInVariableScopes(child);
    }

    private void processStatement(PsiFile file, String filePath, CompositeStatementObject parent, PsiStatement statement) {
        if (statement instanceof PsiBlockStatement) {
            PsiCodeBlock codeBlock = ((PsiBlockStatement) statement).getCodeBlock();
            processStatement(file, filePath, parent, codeBlock);
        } else if (statement instanceof PsiIfStatement) {
            PsiIfStatement ifStatement = (PsiIfStatement) statement;
            CompositeStatementObject child = new CompositeStatementObject(file, filePath, ifStatement, parent.getDepth() + 1, CodeElementType.IF_STATEMENT);
            parent.addStatement(child);
            AbstractExpression abstractExpression = new AbstractExpression(file, filePath, ifStatement.getCondition(), CodeElementType.IF_STATEMENT_CONDITION);
            child.addExpression(abstractExpression);
            addStatementInVariableScopes(child);
            processStatement(file, filePath, child, ifStatement.getThenBranch());
            if (ifStatement.getElseBranch() != null) {
                processStatement(file, filePath, child, ifStatement.getElseBranch());
            }
        } else if (statement instanceof PsiForStatement) {
            PsiForStatement forStatement = (PsiForStatement) statement;
            CompositeStatementObject child = new CompositeStatementObject(file, filePath, forStatement, parent.getDepth() + 1, CodeElementType.FOR_STATEMENT);
            parent.addStatement(child);

            PsiStatement initializer = forStatement.getInitialization();
            if (initializer != null && !(initializer instanceof PsiEmptyStatement)) {
                child.addExpression(new AbstractExpression(file, filePath, initializer, CodeElementType.FOR_STATEMENT_INITIALIZER));
            }

            PsiExpression expression = forStatement.getCondition();
            if (expression != null) {
                child.addExpression(new AbstractExpression(file, filePath, expression, CodeElementType.FOR_STATEMENT_CONDITION));
            }

            PsiStatement updater = forStatement.getUpdate();
            if (updater != null) {
                child.addExpression(new AbstractExpression(file, filePath, updater, CodeElementType.FOR_STATEMENT_UPDATER));
            }
            addStatementInVariableScopes(child);
            List<VariableDeclaration> variableDeclarations = child.getVariableDeclarations();
            this.activeVariableDeclarations.addAll(variableDeclarations);
            processStatement(file, filePath, child, forStatement.getBody());
            variableDeclarations.forEach(this.activeVariableDeclarations::remove);
        } else if (statement instanceof PsiForeachStatement) {
            PsiForeachStatement foreachStatement = (PsiForeachStatement) statement;
            CompositeStatementObject child = new CompositeStatementObject(file, filePath, foreachStatement, parent.getDepth() + 1, CodeElementType.ENHANCED_FOR_STATEMENT);
            parent.addStatement(child);
            PsiParameter variableDeclaration = foreachStatement.getIterationParameter();
            VariableDeclaration vd = new VariableDeclaration(file, filePath, variableDeclaration);
            child.addVariableDeclaration(vd);
            AbstractExpression variableDeclarationName = new AbstractExpression(file, filePath, variableDeclaration.getNameIdentifier(), CodeElementType.ENHANCED_FOR_STATEMENT_PARAMETER_NAME);
            child.addExpression(variableDeclarationName);
            AbstractExpression abstractExpression = new AbstractExpression(file, filePath, foreachStatement.getIteratedValue(), CodeElementType.ENHANCED_FOR_STATEMENT_EXPRESSION);
            child.addExpression(abstractExpression);
            addStatementInVariableScopes(child);
            List<VariableDeclaration> variableDeclarations = child.getVariableDeclarations();
            this.activeVariableDeclarations.addAll(variableDeclarations);
            processStatement(file, filePath, child, foreachStatement.getBody());
            variableDeclarations.forEach(this.activeVariableDeclarations::remove);
        } else if (statement instanceof PsiWhileStatement) {
            PsiWhileStatement whileStatement = (PsiWhileStatement) statement;
            CompositeStatementObject child = new CompositeStatementObject(file, filePath, whileStatement, parent.getDepth() + 1, CodeElementType.WHILE_STATEMENT);
            parent.addStatement(child);
            AbstractExpression abstractExpression = new AbstractExpression(file, filePath, whileStatement.getCondition(), CodeElementType.WHILE_STATEMENT_CONDITION);
            child.addExpression(abstractExpression);
            addStatementInVariableScopes(child);
            processStatement(file, filePath, child, whileStatement.getBody());
        } else if (statement instanceof PsiDoWhileStatement) {
            PsiDoWhileStatement doStatement = (PsiDoWhileStatement) statement;
            CompositeStatementObject child = new CompositeStatementObject(file, filePath, doStatement, parent.getDepth() + 1, CodeElementType.DO_STATEMENT);
            parent.addStatement(child);
            AbstractExpression abstractExpression = new AbstractExpression(file, filePath, doStatement.getCondition(), CodeElementType.DO_STATEMENT_CONDITION);
            child.addExpression(abstractExpression);
            addStatementInVariableScopes(child);
            processStatement(file, filePath, child, doStatement.getBody());
        } else if (statement instanceof PsiSwitchStatement) {
            PsiSwitchStatement switchStatement = (PsiSwitchStatement) statement;
            CompositeStatementObject child = new CompositeStatementObject(file, filePath, switchStatement, parent.getDepth() + 1, CodeElementType.SWITCH_STATEMENT);
            parent.addStatement(child);
            AbstractExpression abstractExpression = new AbstractExpression(file, filePath, switchStatement.getExpression(), CodeElementType.SWITCH_STATEMENT_CONDITION);
            child.addExpression(abstractExpression);
            addStatementInVariableScopes(child);
            PsiStatement[] switchStatements = switchStatement.getBody().getStatements();
            for (PsiStatement switchStatement2 : switchStatements)
                processStatement(file, filePath, child, switchStatement2);
        } else if (statement instanceof PsiSwitchLabelStatement) {
            PsiSwitchLabelStatement switchCase = (PsiSwitchLabelStatement) statement;
            StatementObject child = new StatementObject(file, filePath, switchCase, parent.getDepth() + 1, CodeElementType.SWITCH_CASE);
            parent.addStatement(child);
            addStatementInVariableScopes(child);
        } else if (statement instanceof PsiAssertStatement) {
            PsiAssertStatement assertStatement = (PsiAssertStatement) statement;
            StatementObject child = new StatementObject(file, filePath, assertStatement, parent.getDepth() + 1, CodeElementType.ASSERT_STATEMENT);
            parent.addStatement(child);
            addStatementInVariableScopes(child);
        } else if (statement instanceof PsiLabeledStatement) {
            PsiLabeledStatement labeledStatement = (PsiLabeledStatement) statement;
            CodeElementType elementType = CodeElementType.LABELED_STATEMENT.setName(Formatter.format(labeledStatement.getLabelIdentifier()));
            CompositeStatementObject child = new CompositeStatementObject(file, filePath, labeledStatement, parent.getDepth() + 1, elementType);
            parent.addStatement(child);
            addStatementInVariableScopes(child);
            processStatement(file, filePath, child, labeledStatement.getStatement());
        } else if (statement instanceof PsiReturnStatement) {
            PsiReturnStatement returnStatement = (PsiReturnStatement) statement;
            StatementObject child = new StatementObject(file, filePath, returnStatement, parent.getDepth() + 1, CodeElementType.RETURN_STATEMENT);
            parent.addStatement(child);
            addStatementInVariableScopes(child);
        } else if (statement instanceof PsiSynchronizedStatement) {
            PsiSynchronizedStatement synchronizedStatement = (PsiSynchronizedStatement) statement;
            CompositeStatementObject child = new CompositeStatementObject(file, filePath, synchronizedStatement, parent.getDepth() + 1, CodeElementType.SYNCHRONIZED_STATEMENT);
            parent.addStatement(child);
            AbstractExpression abstractExpression = new AbstractExpression(file, filePath, synchronizedStatement.getLockExpression(), CodeElementType.SYNCHRONIZED_STATEMENT_EXPRESSION);
            child.addExpression(abstractExpression);
            addStatementInVariableScopes(child);
            processStatement(file, filePath, child, synchronizedStatement.getBody());
        } else if (statement instanceof PsiThrowStatement) {
            PsiThrowStatement throwStatement = (PsiThrowStatement) statement;
            StatementObject child = new StatementObject(file, filePath, throwStatement, parent.getDepth() + 1, CodeElementType.THROW_STATEMENT);
            parent.addStatement(child);
            addStatementInVariableScopes(child);
        } else if (statement instanceof PsiTryStatement) {
            PsiTryStatement tryStatement = (PsiTryStatement) statement;
            TryStatementObject child = new TryStatementObject(file, filePath, tryStatement, parent.getDepth() + 1);
            parent.addStatement(child);
            PsiResourceList resources = tryStatement.getResourceList();
            if (resources != null) {
                for (PsiResourceListElement resource : resources) {
                    AbstractExpression expression = new AbstractExpression(file, filePath, resource, CodeElementType.TRY_STATEMENT_RESOURCE);
                    child.addExpression(expression);
                }
            }
            addStatementInVariableScopes(child);
            List<VariableDeclaration> variableDeclarations = child.getVariableDeclarations();
            this.activeVariableDeclarations.addAll(variableDeclarations);
            PsiStatement[] tryStatements = tryStatement.getTryBlock().getStatements();
            for (PsiStatement blockStatement : tryStatements) {
                processStatement(file, filePath, child, blockStatement);
            }
            variableDeclarations.forEach(this.activeVariableDeclarations::remove);
            PsiCatchSection[] catchClauses = tryStatement.getCatchSections();
            for (PsiCatchSection catchClause : catchClauses) {
                PsiParameter variableDeclaration = catchClause.getParameter();
                CompositeStatementObject catchClauseStatementObject = new CompositeStatementObject(file, filePath, catchClause.getCatchBlock(), parent.getDepth() + 1, CodeElementType.CATCH_CLAUSE);
                child.addCatchClause(catchClauseStatementObject);
                parent.addStatement(catchClauseStatementObject);
                VariableDeclaration vd = new VariableDeclaration(file, filePath, variableDeclaration);
                catchClauseStatementObject.addVariableDeclaration(vd);
                AbstractExpression variableDeclarationName = new AbstractExpression(file, filePath, variableDeclaration.getNameIdentifier(), CodeElementType.CATCH_CLAUSE_EXCEPTION_NAME);
                catchClauseStatementObject.addExpression(variableDeclarationName);
                if (variableDeclaration.getInitializer() != null) {
                    AbstractExpression variableDeclarationInitializer = new AbstractExpression(file, filePath, variableDeclaration.getInitializer(), CodeElementType.VARIABLE_DECLARATION_INITIALIZER);
                    catchClauseStatementObject.addExpression(variableDeclarationInitializer);
                }
                addStatementInVariableScopes(catchClauseStatementObject);
                List<VariableDeclaration> catchClauseVariableDeclarations = catchClauseStatementObject.getVariableDeclarations();
                this.activeVariableDeclarations.addAll(catchClauseVariableDeclarations);
                PsiStatement[] blockStatements = catchClause.getCatchBlock().getStatements();
                for (PsiStatement blockStatement : blockStatements) {
                    processStatement(file, filePath, catchClauseStatementObject, blockStatement);
                }
                catchClauseVariableDeclarations.forEach(this.activeVariableDeclarations::remove);
            }
            PsiCodeBlock finallyBlock = tryStatement.getFinallyBlock();
            if (finallyBlock != null) {
                CompositeStatementObject finallyClauseStatementObject = new CompositeStatementObject(file, filePath, finallyBlock, parent.getDepth() + 1, CodeElementType.FINALLY_BLOCK);
                child.setFinallyClause(finallyClauseStatementObject);
                parent.addStatement(finallyClauseStatementObject);
                addStatementInVariableScopes(finallyClauseStatementObject);
                PsiStatement[] blockStatements = finallyBlock.getStatements();
                for (PsiStatement blockStatement : blockStatements) {
                    processStatement(file, filePath, finallyClauseStatementObject, blockStatement);
                }
            }
        } else if (statement instanceof PsiDeclarationStatement) {
            PsiDeclarationStatement declarationStatement = (PsiDeclarationStatement) statement;
            // Local classes are not yet supported
            if (declarationStatement.getDeclaredElements()[0] instanceof PsiVariable) {
                StatementObject child = new StatementObject(file, filePath, declarationStatement, parent.getDepth() + 1, CodeElementType.VARIABLE_DECLARATION_STATEMENT);
                parent.addStatement(child);
                addStatementInVariableScopes(child);
                this.activeVariableDeclarations.addAll(child.getVariableDeclarations());
            }
        } else if (statement instanceof PsiBreakStatement) {
            PsiBreakStatement breakStatement = (PsiBreakStatement) statement;
            StatementObject child = new StatementObject(file, filePath, breakStatement, parent.getDepth() + 1, CodeElementType.BREAK_STATEMENT);
            parent.addStatement(child);
            addStatementInVariableScopes(child);
        } else if (statement instanceof PsiContinueStatement) {
            PsiContinueStatement continueStatement = (PsiContinueStatement) statement;
            StatementObject child = new StatementObject(file, filePath, continueStatement, parent.getDepth() + 1, CodeElementType.CONTINUE_STATEMENT);
            parent.addStatement(child);
            addStatementInVariableScopes(child);
        } else if (statement instanceof PsiEmptyStatement) {
            PsiEmptyStatement emptyStatement = (PsiEmptyStatement) statement;
            StatementObject child = new StatementObject(file, filePath, emptyStatement, parent.getDepth() + 1, CodeElementType.EMPTY_STATEMENT);
            parent.addStatement(child);
            addStatementInVariableScopes(child);
        } else if (statement instanceof PsiExpressionStatement) {
            PsiExpressionStatement expressionStatement = (PsiExpressionStatement) statement;
            processStatement(file, filePath, parent, expressionStatement.getExpression());
        }
    }

    private void addStatementInVariableScopes(AbstractStatement statement) {
        for (VariableDeclaration variableDeclaration : activeVariableDeclarations) {
            variableDeclaration.addStatementInScope(statement);
        }
    }

    public Map<String, Set<String>> aliasedAttributes() {
        return compositeStatement.aliasedAttributes();
    }

    public CompositeStatementObject loopWithVariables(String currentElementName, String collectionName) {
        return compositeStatement.loopWithVariables(currentElementName, collectionName);
    }

    public List<String> stringRepresentation() {
        if (stringRepresentation == null) {
            stringRepresentation = compositeStatement.stringRepresentation();
        }
        return stringRepresentation;
    }
}
