package gr.uom.java.xmi.decomposition;

import gr.uom.java.xmi.LocationInfo;
import gr.uom.java.xmi.LocationInfoProvider;
import gr.uom.java.xmi.decomposition.replacement.MergeVariableReplacement;
import gr.uom.java.xmi.decomposition.replacement.Replacement;
import gr.uom.java.xmi.decomposition.replacement.Replacement.ReplacementType;
import gr.uom.java.xmi.diff.CodeRange;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public abstract class AbstractCall implements LocationInfoProvider {
    /**
     * Count of arguments
     */
    protected int typeArguments;
    protected String expression;
    protected List<String> arguments;
    protected LocationInfo locationInfo;
    protected StatementCoverageType coverage = StatementCoverageType.NONE;

    public StatementCoverageType getCoverage() {
        return coverage;
    }

    public abstract AbstractCall update(String oldExpression, String newExpression);

    public String actualString() {
        StringBuilder sb = new StringBuilder();
        if (expression != null) {
            sb.append(expression).append(".");
        }
        sb.append(getName());
        sb.append("(");
        int size = arguments.size();
        if (size > 0) {
            for (int i = 0; i < size - 1; i++)
                sb.append(arguments.get(i)).append(",");
            sb.append(arguments.get(size - 1));
        }
        sb.append(")");
        return sb.toString();
    }

    public abstract String getName();

    public boolean expressionIsNullOrThis() {
        if (expression == null) {
            return true;
        } else return expression.equals("this");
    }

    public boolean identicalOrConcatenatedArguments(AbstractCall call) {
        List<String> arguments1 = getArguments();
        List<String> arguments2 = call.getArguments();
        if (arguments1.size() != arguments2.size())
            return false;
        for (int i = 0; i < arguments1.size(); i++) {
            String argument1 = arguments1.get(i);
            String argument2 = arguments2.get(i);
            boolean argumentConcatenated = false;
            if ((argument1.contains("+") || argument2.contains("+")) && !argument1.contains("++") && !argument2.contains("++")) {
                Set<String> tokens1 = new LinkedHashSet<>(Arrays.asList(UMLOperationBodyMapper.SPLIT_CONCAT_STRING_PATTERN.split(argument1)));
                Set<String> tokens2 = new LinkedHashSet<>(Arrays.asList(UMLOperationBodyMapper.SPLIT_CONCAT_STRING_PATTERN.split(argument2)));
                Set<String> intersection = new LinkedHashSet<>(tokens1);
                intersection.retainAll(tokens2);
                int size = intersection.size();
                int threshold = Math.max(tokens1.size(), tokens2.size()) - size;
                if (size > 0 && size >= threshold) {
                    argumentConcatenated = true;
                }
            }
            if (!argument1.equals(argument2) && !argumentConcatenated)
                return false;
        }
        return true;
    }

    public List<String> getArguments() {
        return arguments;
    }

    public boolean identicalWithMergedArguments(AbstractCall call, Set<Replacement> replacements) {
        if (onlyArgumentsChanged(call, replacements)) {
            List<String> updatedArguments1 = new ArrayList<>(this.arguments);
            Map<String, Set<Replacement>> commonVariableReplacementMap = new LinkedHashMap<>();
            for (Replacement replacement : replacements) {
                if (replacement.getType().equals(ReplacementType.VARIABLE_NAME)) {
                    String key = replacement.getAfter();
                    if (commonVariableReplacementMap.containsKey(key)) {
                        commonVariableReplacementMap.get(key).add(replacement);
                        int index = updatedArguments1.indexOf(replacement.getBefore());
                        if (index != -1) {
                            updatedArguments1.remove(index);
                        }
                    } else {
                        Set<Replacement> r = new LinkedHashSet<>();
                        r.add(replacement);
                        commonVariableReplacementMap.put(key, r);
                        int index = updatedArguments1.indexOf(replacement.getBefore());
                        if (index != -1) {
                            updatedArguments1.remove(index);
                            updatedArguments1.add(index, key);
                        }
                    }
                }
            }
            if (updatedArguments1.equals(call.arguments)) {
                for (String key : commonVariableReplacementMap.keySet()) {
                    Set<Replacement> r = commonVariableReplacementMap.get(key);
                    if (r.size() > 1) {
                        replacements.removeAll(r);
                        Set<String> mergedVariables = new LinkedHashSet<>();
                        for (Replacement replacement : r) {
                            mergedVariables.add(replacement.getBefore());
                        }
                        MergeVariableReplacement merge = new MergeVariableReplacement(mergedVariables, key);
                        replacements.add(merge);
                    }
                }
                return true;
            }
        }
        return false;
    }

    private boolean onlyArgumentsChanged(AbstractCall call, Set<Replacement> replacements) {
        return identicalExpression(call, replacements) &&
            identicalName(call) &&
            !equalArguments(call) &&
            getArguments().size() != call.getArguments().size();
    }

    public abstract boolean identicalName(AbstractCall call);

    public boolean identicalExpression(AbstractCall call, Set<Replacement> replacements) {
        return identicalExpression(call) ||
            identicalExpressionAfterTypeReplacements(call, replacements);
    }

    public boolean identicalExpression(AbstractCall call) {
        return (getExpression() != null && call.getExpression() != null &&
            getExpression().equals(call.getExpression())) ||
            (getExpression() == null && call.getExpression() == null);
    }

    public String getExpression() {
        return expression;
    }

    private boolean identicalExpressionAfterTypeReplacements(AbstractCall call, Set<Replacement> replacements) {
        if (getExpression() != null && call.getExpression() != null) {
            String expression1 = getExpression();
            String expression2 = call.getExpression();
            String expression1AfterReplacements = expression1;
            for (Replacement replacement : replacements) {
                if (replacement.getType().equals(ReplacementType.TYPE) ||
                    //allow only class names corresponding to static calls
                    (replacement.getType().equals(ReplacementType.VARIABLE_NAME) &&
                        Character.isUpperCase(expression1.charAt(0)) && Character.isUpperCase(expression2.charAt(0)))) {

                    if (replacement.getBefore().equals(expression1) && replacement.getAfter().equals(expression2)) {
                        return true;
                    }

                    expression1AfterReplacements = ReplacementUtil.performReplacement(expression1AfterReplacements, expression2, replacement.getBefore(), replacement.getAfter());
                }
            }
            return expression1AfterReplacements.equals(expression2);
        }
        return false;
    }

    public boolean equalArguments(AbstractCall call) {
        return getArguments().equals(call.getArguments());
    }

    public boolean identicalOrWrappedArguments(AbstractCall call) {
        List<String> arguments1 = getArguments();
        List<String> arguments2 = call.getArguments();
        if (arguments1.size() != arguments2.size())
            return false;
        for (int i = 0; i < arguments1.size(); i++) {
            String argument1 = arguments1.get(i);
            String argument2 = arguments2.get(i);
            boolean argumentWrapped = argument1.contains("(" + argument2 + ")") ||
                argument2.contains("(" + argument1 + ")");
            if (!argument1.equals(argument2) && !argumentWrapped)
                return false;
        }
        return true;
    }

    public boolean identical(AbstractCall call, Set<Replacement> replacements, List<UMLOperationBodyMapper> lambdaMappers) {
        return identicalExpression(call, replacements) &&
            identicalName(call) &&
            (equalArguments(call) || onlyLambdaArgumentsDiffer(call, lambdaMappers));
    }

    private boolean onlyLambdaArgumentsDiffer(AbstractCall call, List<UMLOperationBodyMapper> lambdaMappers) {
        if (lambdaMappers.size() > 0) {
            List<String> arguments1 = getArguments();
            List<String> arguments2 = call.getArguments();
            if (arguments1.size() == arguments2.size()) {
                for (int i = 0; i < arguments1.size(); i++) {
                    String argument1 = arguments1.get(i);
                    String argument2 = arguments2.get(i);
                    if (argument1.contains("->") && argument2.contains("->")) {
                        for (UMLOperationBodyMapper lambdaMapper : lambdaMappers) {
                            if (lambdaMapper.nonMappedElementsT1() > 0 && lambdaMapper.nonMappedElementsT2() > 0) {
                                return false;
                            }
                        }
                    } else if (!argument1.equals(argument2)) {
                        return false;
                    }
                }
                return true;
            }
        }
        return false;
    }

    public boolean allArgumentsReplaced(AbstractCall call, Set<Replacement> replacements, Map<String, String> parameterToArgumentMap) {
        int replacedArguments = 0;
        List<String> arguments1 = getArguments();
        List<String> arguments2 = call.getArguments();
        if (arguments1.size() == arguments2.size()) {
            for (int i = 0; i < arguments1.size(); i++) {
                String argument1 = arguments1.get(i);
                String argument2 = arguments2.get(i);
                for (Replacement replacement : replacements) {
                    if ((replacement.getBefore().equals(argument1) || replacement.getBefore().equals(parameterToArgumentMap.get(argument1))) &&
                        (replacement.getAfter().equals(argument2) || replacement.getAfter().equals(parameterToArgumentMap.get(argument2)))) {
                        replacedArguments++;
                        break;
                    }
                }
            }
        }
        return replacedArguments > 0 && replacedArguments == arguments1.size();
    }

    public boolean renamedWithIdenticalExpressionAndArguments(AbstractCall call, Set<Replacement> replacements, double distance, List<UMLOperationBodyMapper> lambdaMappers) {
        boolean identicalOrReplacedArguments = identicalOrReplacedArguments(call, replacements, lambdaMappers);
        boolean allArgumentsReplaced = allArgumentsReplaced(call, replacements);
        return getExpression() != null && call.getExpression() != null &&
            identicalExpression(call, replacements) &&
            !identicalName(call) &&
            (equalArguments(call) || (allArgumentsReplaced && normalizedNameDistance(call) <= distance) || (identicalOrReplacedArguments && !allArgumentsReplaced));
    }

    public abstract double normalizedNameDistance(AbstractCall call);

    public boolean identicalOrReplacedArguments(AbstractCall call, Set<Replacement> replacements, List<UMLOperationBodyMapper> lambdaMappers) {
        List<String> arguments1 = getArguments();
        List<String> arguments2 = call.getArguments();
        if (arguments1.size() != arguments2.size())
            return false;
        for (int i = 0; i < arguments1.size(); i++) {
            String argument1 = arguments1.get(i);
            String argument2 = arguments2.get(i);
            boolean argumentReplacement = false;
            for (Replacement replacement : replacements) {
                if (replacement.getBefore().equals(argument1) && replacement.getAfter().equals(argument2)) {
                    argumentReplacement = true;
                    break;
                }
            }
            boolean lambdaReplacement = false;
            if (argument1.contains("->") && argument2.contains("->")) {
                for (UMLOperationBodyMapper lambdaMapper : lambdaMappers) {
                    if (lambdaMapper.nonMappedElementsT1() == 0 && lambdaMapper.nonMappedElementsT2() == 0) {
                        lambdaReplacement = true;
                        break;
                    }
                }
            }
            if (!argument1.equals(argument2) && !argumentReplacement && !lambdaReplacement)
                return false;
        }
        return true;
    }

    public boolean allArgumentsReplaced(AbstractCall call, Set<Replacement> replacements) {
        int replacedArguments = 0;
        List<String> arguments1 = getArguments();
        List<String> arguments2 = call.getArguments();
        if (arguments1.size() == arguments2.size()) {
            for (int i = 0; i < arguments1.size(); i++) {
                String argument1 = arguments1.get(i);
                String argument2 = arguments2.get(i);
                for (Replacement replacement : replacements) {
                    if (replacement.getBefore().equals(argument1) && replacement.getAfter().equals(argument2)) {
                        replacedArguments++;
                        break;
                    }
                }
            }
        }
        return replacedArguments > 0 && replacedArguments == arguments1.size();
    }

    public boolean renamedWithDifferentExpressionAndIdenticalArguments(AbstractCall call) {
        return (this.getName().contains(call.getName()) || call.getName().contains(this.getName())) &&
            equalArguments(call) && this.arguments.size() > 0 &&
            ((this.getExpression() == null && call.getExpression() != null) || (call.getExpression() == null && this.getExpression() != null));
    }

    public boolean renamedWithIdenticalArgumentsAndNoExpression(AbstractCall call, double distance, List<UMLOperationBodyMapper> lambdaMappers) {
        boolean allExactLambdaMappers = lambdaMappers.size() > 0;
        for (UMLOperationBodyMapper lambdaMapper : lambdaMappers) {
            if (!lambdaMapper.allMappingsAreExactMatches()) {
                allExactLambdaMappers = false;
                break;
            }
        }
        return getExpression() == null && call.getExpression() == null &&
            !identicalName(call) &&
            (normalizedNameDistance(call) <= distance || allExactLambdaMappers) &&
            equalArguments(call);
    }

    public boolean renamedWithIdenticalExpressionAndDifferentNumberOfArguments(AbstractCall call, Set<Replacement> replacements, double distance, List<UMLOperationBodyMapper> lambdaMappers) {
        boolean allExactLambdaMappers = lambdaMappers.size() > 0;
        for (UMLOperationBodyMapper lambdaMapper : lambdaMappers) {
            if (!lambdaMapper.allMappingsAreExactMatches()) {
                allExactLambdaMappers = false;
                break;
            }
        }
        return getExpression() != null && call.getExpression() != null &&
            identicalExpression(call, replacements) &&
            (normalizedNameDistance(call) <= distance || allExactLambdaMappers) &&
            !equalArguments(call) &&
            getArguments().size() != call.getArguments().size();
    }

    public Replacement makeReplacementForAssignedArgument(String statement) {
        if (argumentIsAssigned(statement)) {
            return new Replacement(statement.substring(statement.indexOf("=") + 1, statement.length() - 2),
                getArguments().get(0), ReplacementType.ARGUMENT_REPLACED_WITH_RIGHT_HAND_SIDE_OF_ASSIGNMENT_EXPRESSION);
        }
        return null;
    }

    private boolean argumentIsAssigned(String statement) {
        return getArguments().size() == 1 && statement.contains("=") && statement.endsWith(";\n") &&
            //length()-2 to remove ";\n" from the end of the assignment statement, indexOf("=")+1 to remove the left hand side of the assignment
            equalsIgnoringExtraParenthesis(getArguments().get(0), statement.substring(statement.indexOf("=") + 1, statement.length() - 2));
    }

    private static boolean equalsIgnoringExtraParenthesis(String s1, String s2) {
        if (s1.equals(s2))
            return true;
        String parenthesizedS1 = "(" + s1 + ")";
        if (parenthesizedS1.equals(s2))
            return true;
        String parenthesizedS2 = "(" + s2 + ")";
        return parenthesizedS2.equals(s1);
    }

    public Replacement makeReplacementForReturnedArgument(String statement) {
        if (argumentIsReturned(statement)) {
            return new Replacement(getArguments().get(0), statement.substring(7, statement.length() - 2),
                ReplacementType.ARGUMENT_REPLACED_WITH_RETURN_EXPRESSION);
        } else if (argumentIsEqual(statement)) {
            return new Replacement(getArguments().get(0), statement.substring(0, statement.length() - 2),
                ReplacementType.ARGUMENT_REPLACED_WITH_STATEMENT);
        }
        return null;
    }

    private boolean argumentIsEqual(String statement) {
        if (statement.endsWith(";\n")) {
            for (String argument : getArguments()) {
                //length()-2 to remove ";\n" from the end of the statement
                if (equalsIgnoringExtraParenthesis(argument, statement.substring(0, statement.length() - 2))) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean argumentIsReturned(String statement) {
        if (statement.startsWith("return ")) {
            for (String argument : getArguments()) {
                //length()-2 to remove ";\n" from the end of the return statement, 7 to remove the prefix "return "
                if (equalsIgnoringExtraParenthesis(argument, statement.substring(7, statement.length() - 2))) {
                    return true;
                }
            }
        }
        return false;
    }

    public Replacement makeReplacementForWrappedCall(String statement) {
        if (argumentIsReturned(statement)) {
            return new Replacement(statement.substring(7, statement.length() - 2), getArguments().get(0),
                ReplacementType.ARGUMENT_REPLACED_WITH_RETURN_EXPRESSION);
        } else if (argumentIsEqual(statement)) {
            return new Replacement(statement.substring(0, statement.length() - 2), getArguments().get(0),
                ReplacementType.ARGUMENT_REPLACED_WITH_STATEMENT);
        }
        return null;
    }

    public boolean identicalWithDifferentNumberOfArguments(AbstractCall call, Set<Replacement> replacements, Map<String, String> parameterToArgumentMap) {
        if (onlyArgumentsChanged(call, replacements)) {
            int argumentIntersectionSize = argumentIntersectionSize(call, replacements, parameterToArgumentMap);
            return argumentIntersectionSize > 0 || getArguments().size() == 0 || call.getArguments().size() == 0;
        }
        return false;
    }

    private int argumentIntersectionSize(AbstractCall call, Set<Replacement> replacements, Map<String, String> parameterToArgumentMap) {
        Set<String> argumentIntersection = argumentIntersection(call);
        int argumentIntersectionSize = argumentIntersection.size();
        for (String parameter : parameterToArgumentMap.keySet()) {
            String argument = parameterToArgumentMap.get(parameter);
            if (getArguments().contains(argument) &&
                call.getArguments().contains(parameter)) {
                argumentIntersectionSize++;
            }
        }
        for (Replacement r : replacements) {
            if (r.isLiteral() && getArguments().contains(r.getBefore()) && call.getArguments().contains(r.getAfter())) {
                argumentIntersectionSize++;
            }
        }
        return argumentIntersectionSize;
    }

    public Set<String> argumentIntersection(AbstractCall call) {
        List<String> args1 = preprocessArguments(getArguments());
        List<String> args2 = preprocessArguments(call.getArguments());
        Set<String> argumentIntersection = new LinkedHashSet<>(args1);
        argumentIntersection.retainAll(args2);
        return argumentIntersection;
    }

    private List<String> preprocessArguments(List<String> arguments) {
        List<String> args = new ArrayList<>();
        for (String arg : arguments) {
            if (arg.contains("\n")) {
                args.add(arg.substring(0, arg.indexOf("\n")));
            } else {
                args.add(arg);
            }
        }
        return args;
    }

    protected void update(AbstractCall newCall, String oldExpression, String newExpression) {
        newCall.typeArguments = this.typeArguments;
        if (this.expression != null && this.expression.equals(oldExpression)) {
            newCall.expression = newExpression;
        } else {
            newCall.expression = this.expression;
        }
        newCall.arguments = new ArrayList<>();
        for (String argument : this.arguments) {
            newCall.arguments.add(
                ReplacementUtil.performReplacement(argument, oldExpression, newExpression));
        }
    }

    public CodeRange codeRange() {
        LocationInfo info = getLocationInfo();
        return info.codeRange();
    }

    public LocationInfo getLocationInfo() {
        return locationInfo;
    }

    public enum StatementCoverageType {
        NONE, ONLY_CALL, RETURN_CALL, THROW_CALL, CAST_CALL, VARIABLE_DECLARATION_INITIALIZER_CALL
    }
}
