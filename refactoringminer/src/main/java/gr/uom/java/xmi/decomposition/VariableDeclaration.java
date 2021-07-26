package gr.uom.java.xmi.decomposition;

import com.intellij.psi.PsiDeclarationStatement;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiEnumConstant;
import com.intellij.psi.PsiExpression;
import com.intellij.psi.PsiField;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiLocalVariable;
import com.intellij.psi.PsiModifier;
import com.intellij.psi.PsiParameter;
import com.intellij.psi.PsiVariable;
import gr.uom.java.xmi.LocationInfo;
import gr.uom.java.xmi.LocationInfo.CodeElementType;
import gr.uom.java.xmi.LocationInfoProvider;
import gr.uom.java.xmi.UMLAnnotation;
import gr.uom.java.xmi.UMLType;
import gr.uom.java.xmi.VariableDeclarationProvider;
import gr.uom.java.xmi.diff.CodeRange;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class VariableDeclaration implements LocationInfoProvider, VariableDeclarationProvider {
    private final String variableName;
    private final UMLType type;
    private final LocationInfo locationInfo;
    private final VariableScope scope;
    private final List<UMLAnnotation> annotations;
    private final boolean isEnumConstant;
    private boolean varargsParameter;
    private boolean isParameter;
    private boolean isAttribute;
    private final boolean isFinal;
    private AbstractExpression initializer;

    public VariableDeclaration(PsiFile file, String filePath, PsiVariable variable, boolean isVararg) {
        this(file, filePath, variable);
        this.varargsParameter = isVararg;
    }

    public VariableDeclaration(PsiFile file, String filePath, PsiVariable variable) {
        this(file, filePath, variable,
            UMLType.extractTypeObject(file, filePath, variable.getTypeElement(), variable.getType()));
    }

    public VariableDeclaration(PsiFile file, String filePath, PsiVariable variable, UMLType type) {
        this.variableName = variable.getName();

        PsiExpression initializer = variable.getInitializer();
        if (initializer != null) {
            this.initializer = new AbstractExpression(file, filePath, initializer, CodeElementType.VARIABLE_DECLARATION_INITIALIZER);
        }

        this.type = type;

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

        this.isFinal = variable.hasModifierProperty(PsiModifier.FINAL);
        this.isEnumConstant = variable instanceof PsiEnumConstant;
        this.annotations = Arrays.stream(variable.getAnnotations())
            .map(annotation -> new UMLAnnotation(file, filePath, annotation))
            .collect(Collectors.toList());
    }

    // TODO:
    private static PsiElement getScopeNode(PsiVariable variableDeclaration) {
        return variableDeclaration.getParent().getParent();
    }

    private static CodeElementType extractVariableDeclarationType(PsiVariable variableDeclaration) {
        if (variableDeclaration instanceof PsiEnumConstant) {
            return CodeElementType.ENUM_CONSTANT_DECLARATION;
        } else if (variableDeclaration instanceof PsiParameter) {
            return CodeElementType.SINGLE_VARIABLE_DECLARATION;
        } else if (variableDeclaration instanceof PsiLocalVariable) {
            return CodeElementType.VARIABLE_DECLARATION_STATEMENT;
        } else if (variableDeclaration instanceof PsiDeclarationStatement) {
            return CodeElementType.VARIABLE_DECLARATION_EXPRESSION;
        } else if (variableDeclaration instanceof PsiField) {
            return CodeElementType.FIELD_DECLARATION;
        }
        throw new IllegalStateException();
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
            return other.variableName == null;
        } else return variableName.equals(other.variableName);
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
