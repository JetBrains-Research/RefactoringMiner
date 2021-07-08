package gr.uom.java.xmi.decomposition;

import com.intellij.psi.PsiConditionalExpression;
import com.intellij.psi.PsiFile;
import gr.uom.java.xmi.LocationInfo.CodeElementType;
import gr.uom.java.xmi.decomposition.replacement.Replacement;
import gr.uom.java.xmi.decomposition.replacement.Replacement.ReplacementType;

public class TernaryOperatorExpression {
    private final AbstractExpression condition;
    private final AbstractExpression thenExpression;
    private final AbstractExpression elseExpression;
    private final String expression;

    public TernaryOperatorExpression(PsiFile file, String filePath, PsiConditionalExpression expression) {
        this.condition = new AbstractExpression(file, filePath, expression.getCondition(), CodeElementType.TERNARY_OPERATOR_CONDITION);
        this.thenExpression = new AbstractExpression(file, filePath, expression.getThenExpression(), CodeElementType.TERNARY_OPERATOR_THEN_EXPRESSION);
        this.elseExpression = new AbstractExpression(file, filePath, expression.getElseExpression(), CodeElementType.TERNARY_OPERATOR_ELSE_EXPRESSION);
        this.expression = expression.toString();
    }

    public AbstractExpression getCondition() {
        return condition;
    }

    public AbstractExpression getThenExpression() {
        return thenExpression;
    }

    public AbstractExpression getElseExpression() {
        return elseExpression;
    }

    public String getExpression() {
        return expression;
    }

    public Replacement makeReplacementWithTernaryOnTheRight(String statement) {
        if (getElseExpression().getString().equals(statement)) {
            return new Replacement(statement, getExpression(), ReplacementType.EXPRESSION_REPLACED_WITH_TERNARY_ELSE);
        }
        if (getThenExpression().getString().equals(statement)) {
            return new Replacement(statement, getExpression(), ReplacementType.EXPRESSION_REPLACED_WITH_TERNARY_THEN);
        }
        return null;
    }

    public Replacement makeReplacementWithTernaryOnTheLeft(String statement) {
        if (getElseExpression().getString().equals(statement)) {
            return new Replacement(getExpression(), statement, ReplacementType.EXPRESSION_REPLACED_WITH_TERNARY_ELSE);
        }
        if (getThenExpression().getString().equals(statement)) {
            return new Replacement(getExpression(), statement, ReplacementType.EXPRESSION_REPLACED_WITH_TERNARY_THEN);
        }
        return null;
    }
}
