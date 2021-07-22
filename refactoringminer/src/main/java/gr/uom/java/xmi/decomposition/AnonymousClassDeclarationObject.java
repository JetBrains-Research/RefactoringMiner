package gr.uom.java.xmi.decomposition;

import gr.uom.java.xmi.LocationInfo;
import gr.uom.java.xmi.LocationInfo.CodeElementType;
import gr.uom.java.xmi.LocationInfoProvider;
import gr.uom.java.xmi.diff.CodeRange;
import org.eclipse.jdt.core.dom.AnonymousClassDeclaration;
import org.eclipse.jdt.core.dom.CompilationUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class AnonymousClassDeclarationObject implements LocationInfoProvider {
    private final LocationInfo locationInfo;
    private final String astNodeString;
    private final List<String> variables = new ArrayList<String>();
    private final List<String> types = new ArrayList<String>();
    private final Map<String, List<OperationInvocation>> methodInvocationMap = new LinkedHashMap<String, List<OperationInvocation>>();
    private final List<VariableDeclaration> variableDeclarations = new ArrayList<VariableDeclaration>();
    private final List<AnonymousClassDeclarationObject> anonymousClassDeclarations = new ArrayList<AnonymousClassDeclarationObject>();
    private final List<String> stringLiterals = new ArrayList<String>();
    private final List<String> numberLiterals = new ArrayList<String>();
    private final List<String> nullLiterals = new ArrayList<String>();
    private final List<String> booleanLiterals = new ArrayList<String>();
    private final List<String> typeLiterals = new ArrayList<String>();
    private final Map<String, List<ObjectCreation>> creationMap = new LinkedHashMap<String, List<ObjectCreation>>();
    private final List<String> infixExpressions = new ArrayList<String>();
    private final List<String> infixOperators = new ArrayList<String>();
    private final List<String> arrayAccesses = new ArrayList<String>();
    private final List<String> prefixExpressions = new ArrayList<String>();
    private final List<String> postfixExpressions = new ArrayList<String>();
    private final List<String> arguments = new ArrayList<String>();
    private final List<TernaryOperatorExpression> ternaryOperatorExpressions = new ArrayList<TernaryOperatorExpression>();
    private final List<LambdaExpressionObject> lambdas = new ArrayList<LambdaExpressionObject>();
    private AnonymousClassDeclaration astNode;

    public AnonymousClassDeclarationObject(CompilationUnit cu, String filePath, AnonymousClassDeclaration anonymous) {
        this.locationInfo = new LocationInfo(cu, filePath, anonymous, CodeElementType.ANONYMOUS_CLASS_DECLARATION);
        this.astNode = anonymous;
        this.astNodeString = anonymous.toString();
    }

    public LocationInfo getLocationInfo() {
        return locationInfo;
    }

    public AnonymousClassDeclaration getAstNode() {
        return astNode;
    }

    public void setAstNode(AnonymousClassDeclaration node) {
        this.astNode = node;
    }

    public String toString() {
		return astNodeString;
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

	public CodeRange codeRange() {
		return locationInfo.codeRange();
	}
}
