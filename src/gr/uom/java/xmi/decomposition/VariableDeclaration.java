package gr.uom.java.xmi.decomposition;

import com.intellij.lang.jvm.JvmModifier;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiEnumConstant;
import com.intellij.psi.PsiExpression;
import com.intellij.psi.PsiField;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiVariable;
import gr.uom.java.xmi.LocationInfo;
import gr.uom.java.xmi.LocationInfo.CodeElementType;
import gr.uom.java.xmi.LocationInfoProvider;
import gr.uom.java.xmi.UMLAnnotation;
import gr.uom.java.xmi.UMLType;
import gr.uom.java.xmi.VariableDeclarationProvider;
import gr.uom.java.xmi.diff.CodeRange;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationExpression;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;
import java.util.List;

public class VariableDeclaration implements LocationInfoProvider, VariableDeclarationProvider {
    private String variableName;
    private AbstractExpression initializer;
    private UMLType type;
    private boolean varargsParameter;
    private LocationInfo locationInfo;
    private boolean isParameter;
    private boolean isAttribute;
    private boolean isEnumConstant;
    private VariableScope scope;
    private boolean isFinal;
    private List<UMLAnnotation> annotations;

    public VariableDeclaration(PsiFile file, String filePath, PsiVariable variable) {
        this.variableName = variable.getName();

        PsiExpression initializer = variable.getInitializer();
        if (initializer != null) {
            this.initializer = new AbstractExpression(file, filePath, initializer, CodeElementType.VARIABLE_DECLARATION_INITIALIZER);
        }

        this.type = UMLType.extractTypeObject(file, filePath, variable.getTypeElement(), 0); //TODO: extra?

        CodeElementType declarationType = extractVariableDeclarationType(variable);
        this.locationInfo = new LocationInfo(file, filePath, variable, declarationType);

        PsiElement scopeNode = getScopeNode(variable);
        int startOffset;
        if (declarationType.equals(CodeElementType.FIELD_DECLARATION)) {
            startOffset = scopeNode.getTextRange().getStartOffset();
        } else {
            startOffset = variable.getTextRange().getStartOffset();
        }
        int endOffset = scopeNode.getTextRange().getEndOffset();
        this.scope = new VariableScope(file, filePath, startOffset, endOffset);

        this.isFinal = variable.hasModifier(JvmModifier.FINAL);
        this.isEnumConstant = variable instanceof PsiEnumConstant;
        // TODO: check
        // TODO: Annotations
    }

    public VariableDeclaration(PsiFile file, String filePath, PsiVariable variable, boolean isVararg) {
        this(file, filePath, variable);
        this.varargsParameter = isVararg;
    }

    public String getVariableName() {
        return variableName;
    }

    public AbstractExpression getInitializer() {
        return initializer;
    }

    public UMLType getType() {
        return type;
    }

    public VariableScope getScope() {
        return scope;
    }

    public boolean isParameter() {
        return isParameter;
    }

    public void setParameter(boolean isParameter) {
        this.isParameter = isParameter;
    }

    public boolean isAttribute() {
        return isAttribute;
    }

    public void setAttribute(boolean isAttribute) {
        this.isAttribute = isAttribute;
    }

    public boolean isEnumConstant() {
        return isEnumConstant;
    }

    public boolean isVarargsParameter() {
        return varargsParameter;
    }

    public boolean isFinal() {
        return isFinal;
    }

    public List<UMLAnnotation> getAnnotations() {
        return annotations;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((scope == null) ? 0 : scope.hashCode());
        result = prime * result + ((variableName == null) ? 0 : variableName.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        VariableDeclaration other = (VariableDeclaration) obj;
        if (scope == null) {
            if (other.scope != null)
                return false;
        } else if (!scope.equals(other.scope))
            return false;
        if (variableName == null) {
            if (other.variableName != null)
                return false;
        } else if (!variableName.equals(other.variableName))
            return false;
        return true;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(variableName).append(" : ").append(type);
        if (varargsParameter) {
            sb.append("...");
        }
        return sb.toString();
    }

    public String toQualifiedString() {
        StringBuilder sb = new StringBuilder();
        sb.append(variableName).append(" : ").append(type.toQualifiedString());
        if (varargsParameter) {
            sb.append("...");
        }
        return sb.toString();
    }

    public LocationInfo getLocationInfo() {
        return locationInfo;
    }

    public CodeRange codeRange() {
        return locationInfo.codeRange();
    }

    // TODO:
    private static PsiElement getScopeNode(PsiVariable variableDeclaration) {
        if (variableDeclaration instanceof SingleVariableDeclaration) {
            return variableDeclaration.getParent();
        } else if (variableDeclaration instanceof VariableDeclarationFragment) {
            return variableDeclaration.getParent().getParent();
        }
        return null;
    }

    // TODO:
    private static CodeElementType extractVariableDeclarationType(PsiVariable variableDeclaration) {
        if (variableDeclaration instanceof SingleVariableDeclaration) {
            return CodeElementType.SINGLE_VARIABLE_DECLARATION;
        } else if (variableDeclaration instanceof VariableDeclarationFragment) {
            VariableDeclarationFragment fragment = (VariableDeclarationFragment) variableDeclaration;
            if (fragment.getParent() instanceof VariableDeclarationStatement) {
                return CodeElementType.VARIABLE_DECLARATION_STATEMENT;
            } else if (fragment.getParent() instanceof VariableDeclarationExpression) {
                return CodeElementType.VARIABLE_DECLARATION_EXPRESSION;
            } else if (fragment.getParent() instanceof PsiField) {
                return CodeElementType.FIELD_DECLARATION;
            }
        }
        return null;
    }

    public boolean equalVariableDeclarationType(VariableDeclaration other) {
        return this.locationInfo.getCodeElementType().equals(other.locationInfo.getCodeElementType());
    }

    public VariableDeclaration getVariableDeclaration() {
        return this;
    }

    public void addStatementInScope(AbstractStatement statement) {
        if (scope.subsumes(statement.getLocationInfo())) {
            scope.addStatement(statement);
            if (statement.getVariables().contains(variableName)) {
                scope.addStatementUsingVariable(statement);
            }
        }
    }

    public List<AbstractCodeFragment> getStatementsInScopeUsingVariable() {
        return scope.getStatementsInScopeUsingVariable();
    }
}
