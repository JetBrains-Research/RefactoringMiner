package gr.uom.java.xmi;

import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiArrayType;
import com.intellij.psi.PsiDisjunctionType;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiJavaCodeReferenceElement;
import com.intellij.psi.PsiKeyword;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiModifierList;
import com.intellij.psi.PsiType;
import com.intellij.psi.PsiTypeElement;
import com.intellij.psi.PsiWhiteSpace;
import com.intellij.psi.PsiWildcardType;
import gr.uom.java.xmi.ListCompositeType.Kind;
import gr.uom.java.xmi.LocationInfo.CodeElementType;
import gr.uom.java.xmi.diff.CodeRange;
import gr.uom.java.xmi.diff.StringDistance;
import org.jetbrains.annotations.NotNull;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public abstract class UMLType implements Serializable, LocationInfoProvider {
    protected LocationInfo locationInfo;
    protected int arrayDimension;
    protected List<UMLAnnotation> annotations = new ArrayList<>();
    protected List<UMLType> typeArguments = Collections.emptyList();

    /**
     * Parse type from qualified type string
     */
    public static UMLType extractTypeObject(String qualifiedName) {
        qualifiedName = qualifiedName.replaceAll("\\s", "");
        return extractArrayDimensions(qualifiedName);
    }

    @NotNull
    private static UMLType extractArrayDimensions(String qualifiedName) {
        int arrayDimension = 0;
        if (qualifiedName.endsWith("...")) {
            qualifiedName = qualifiedName.substring(0, qualifiedName.length() - 3);
            arrayDimension++;
        }
        while (qualifiedName.endsWith("[]")) {
            qualifiedName = qualifiedName.substring(0, qualifiedName.length() - 2);
            arrayDimension++;
        }
        UMLType typeObject = extractQualifiedName(qualifiedName);
        typeObject.arrayDimension = arrayDimension;
        return typeObject;
    }

    @NotNull
    private static UMLType extractQualifiedName(String qualifiedName) {
        List<String> elements = splitCapturing(qualifiedName, '.');
        UMLType left = null;
        for (int i = 0; i < elements.size(); i++) {
            // All names before first with generics marks as package names (Eclipse compatibility)
            if (left == null && !elements.get(i).contains("<")) {
                continue;
            }
            if (left == null) {
                left = extractGenerics(String.join(".", elements.subList(0, i + 1)));
            } else {
                left = new CompositeType(left, extractGenerics(elements.get(i)));
            }
        }
        if (left == null) {
            left = extractGenerics(qualifiedName);
        }
        return left;
    }

    /**
     * Split string by separator chars not captured by <>
     */
    @NotNull
    private static List<String> splitCapturing(String qualifiedName, char separator) {
        List<String> parts = new ArrayList<>();
        int sum = 0;
        int last = -1;
        for (int i = 0; i < qualifiedName.length(); i++) {
            char character = qualifiedName.charAt(i);
            switch (character) {
                case '>': {
                    sum++;
                    break;
                }
                case '<': {
                    sum--;
                    break;
                }
            }
            if (character == separator && sum == 0) {
                parts.add(qualifiedName.substring(last + 1, i));
                last = i;
            }
        }
        parts.add(qualifiedName.substring(last + 1));
        return parts;
    }

    @NotNull
    private static LeafType extractGenerics(String qualifiedName) {
        if (qualifiedName.contains("<")) {
            String typeArguments = qualifiedName.substring(qualifiedName.indexOf('<') + 1, qualifiedName.lastIndexOf('>'));
            List<UMLType> typeArgumentsList;
            if (!typeArguments.isEmpty()) {
                typeArgumentsList = splitCapturing(typeArguments, ',').stream()
                    .map(UMLType::extractArrayDimensions)
                    .collect(Collectors.toList());
            } else {
                typeArgumentsList = Collections.emptyList();
            }
            LeafType typeObject = extractSimpleType(qualifiedName.substring(0, qualifiedName.indexOf('<')));
            typeObject.typeArguments = typeArgumentsList;
            return typeObject;
        }
        return extractSimpleType(qualifiedName);
    }

    @NotNull
    private static LeafType extractSimpleType(String qualifiedName) {
        return new LeafType(qualifiedName);
    }

    private static UMLType extractType(PsiFile file, String filePath, PsiTypeElement typeElement, PsiType type) {
        if (type instanceof PsiDisjunctionType) {
            List<UMLType> umlTypes = Arrays.stream(typeElement.getChildren())
                .filter(element -> element instanceof PsiTypeElement)
                .map(element -> (PsiTypeElement) element)
                .map(dTypeElement -> extractTypeObject(file, filePath, dTypeElement))
                .collect(Collectors.toList());
            return new ListCompositeType(umlTypes, Kind.UNION);
        } else if (type instanceof PsiWildcardType) {
            PsiWildcardType wildcardType = (PsiWildcardType) type;
            if (wildcardType.isBounded()) {
                PsiTypeElement bound = (PsiTypeElement) typeElement.getLastChild();
                return new WildcardType(extractTypeObject(file, filePath, bound, wildcardType.getBound()), wildcardType.isSuper());
            } else {
                return new WildcardType(null, false);
            }
        } else {
            String typeString = Arrays.stream(typeElement.getChildren())
                .filter(child -> !(child instanceof PsiAnnotation) && !(child instanceof PsiWhiteSpace))
                .map(Formatter::format)
                .collect(Collectors.joining());
            if (type instanceof PsiArrayType) {
                UMLType commonType = extractArrayDimensions(typeString);
                commonType.arrayDimension = type.getArrayDimensions();
                return commonType;
            } else {
                return extractQualifiedName(typeString);
            }
        }
    }

    /**
     * Construct UMLType from Psi type and typeElement
     *
     * @param typeElement Element associated with type declaration position
     * @param type        Real type (differs from typeElement.getType() on C-style arrays)
     */
    public static UMLType extractTypeObject(PsiFile file, String filePath, PsiTypeElement typeElement, PsiType type) {
        UMLType umlType = extractType(file, filePath, typeElement, type);
        umlType.locationInfo = new LocationInfo(file, filePath, typeElement, CodeElementType.TYPE);
        addAnnotations(file, filePath, typeElement, umlType);
        return umlType;
    }

    private static void addAnnotations(PsiFile file, String filePath, PsiTypeElement typeElement, UMLType umlType) {
        if (typeElement.getParent() instanceof PsiMethod) {
            // TODO: return type annotations attached only to method?
        } else {
            PsiModifierList modifierList = getPrecedingModifiersList(typeElement);
            if (modifierList != null) {
                Arrays.stream(modifierList.getChildren())
                    .filter(element -> element instanceof PsiAnnotation)
                    .map(annotation -> new UMLAnnotation(file, filePath, (PsiAnnotation) annotation))
                    .forEach(umlType.annotations::add);
            } else {
                // Only parts of DisjunctionType have not preventing modifiersList
                assert typeElement.getParent() instanceof PsiTypeElement;
            }
        }
    }

    private static PsiModifierList getPrecedingModifiersList(PsiTypeElement typeElement) {
        PsiElement prev = typeElement;
        while (prev != null) {
            prev = prev.getPrevSibling();
            if (prev instanceof PsiModifierList) {
                return (PsiModifierList) prev;
            }
        }
        return null;
    }

    public static UMLType extractTypeObject(PsiFile file, String filePath, PsiTypeElement typeElement) {
        return extractTypeObject(file, filePath, typeElement, typeElement.getType());
    }

    /**
     * Extract Class type with optional array dimensions
     */
    public static UMLType extractTypeObject(PsiFile file, String filePath, PsiJavaCodeReferenceElement typeElement, PsiType type) {
        UMLType umlType = extractTypeObject(file, filePath, typeElement);
        if (type instanceof PsiArrayType) {
            umlType.arrayDimension = type.getArrayDimensions();
        }
        return umlType;
    }

    /**
     * Extract Class type without array dimensions
     */
    public static UMLType extractTypeObject(PsiFile file, String filePath, PsiJavaCodeReferenceElement typeElement) {
        UMLType umlType = extractQualifiedName(Formatter.format(typeElement));
        umlType.locationInfo = new LocationInfo(file, filePath, typeElement, CodeElementType.TYPE);
        Arrays.stream(typeElement.getChildren())
            .filter(element -> element instanceof PsiAnnotation)
            .map(annotation -> new UMLAnnotation(file, filePath, (PsiAnnotation) annotation))
            .forEach(umlType.annotations::add);
        return umlType;
    }

    /**
     * Extract type for primitives and array of primitives
     */
    public static LeafType extractTypeObject(PsiFile file, String filePath, PsiKeyword typeKeyword) {
        LeafType umlType = extractSimpleType(Formatter.format(typeKeyword));
        umlType.locationInfo = new LocationInfo(file, filePath, typeKeyword, CodeElementType.TYPE);
        umlType.arrayDimension = TypeUtils.arrayDimensions(typeKeyword);
        Arrays.stream(typeKeyword.getParent().getChildren())
            .filter(element -> element instanceof PsiAnnotation)
            .map(annotation -> new UMLAnnotation(file, filePath, (PsiAnnotation) annotation))
            .forEach(umlType.annotations::add);
        return umlType;
    }

    public static LeafType extractVarType(PsiFile file, String filePath, PsiTypeElement varElement) {
        LeafType umlType = new LeafType("var");
        umlType.locationInfo = new LocationInfo(file, filePath, varElement, CodeElementType.TYPE);
        addAnnotations(file, filePath, varElement, umlType);
        return umlType;
    }

    public LocationInfo getLocationInfo() {
        return locationInfo;
    }

    public List<UMLType> getTypeArguments() {
        return typeArguments;
    }

    public List<UMLAnnotation> getAnnotations() {
        return annotations;
    }

    public CodeRange codeRange() {
        return locationInfo.codeRange();
    }

    protected boolean equalTypeArgumentsAndArrayDimension(UMLType typeObject) {
        if (!this.isParameterized() && !typeObject.isParameterized())
            return this.arrayDimension == typeObject.arrayDimension;
        else if (this.isParameterized() && typeObject.isParameterized())
            return equalTypeArguments(typeObject) && this.arrayDimension == typeObject.arrayDimension;
        return false;
    }

    private boolean equalTypeArguments(UMLType type) {
        String thisTypeArguments = this.typeArgumentsToString();
        String otherTypeArguments = type.typeArgumentsToString();
        if ((thisTypeArguments.equals("<?>") && otherTypeArguments.startsWith("<? ")) ||
            (thisTypeArguments.startsWith("<? ") && otherTypeArguments.equals("<?>"))) {
            return true;
        }
        if ((thisTypeArguments.equals("<Object>") && otherTypeArguments.contains("<Object>")) ||
            (otherTypeArguments.equals("<Object>") && thisTypeArguments.contains("<Object>"))) {
            return true;
        }
        if (this.typeArguments.size() != type.typeArguments.size()) {
            return false;
        }
        for (int i = 0; i < this.typeArguments.size(); i++) {
            UMLType thisComponent = this.typeArguments.get(i);
            UMLType otherComponent = type.typeArguments.get(i);
            if (!thisComponent.equals(otherComponent)) {
                return false;
            }
        }
        return true;
    }

    public abstract boolean equals(Object o);

    protected String typeArgumentsToString() {
        StringBuilder sb = new StringBuilder();
        if (typeArguments.isEmpty()) {
        } else {
            sb.append("<");
            for (int i = 0; i < typeArguments.size(); i++) {
                sb.append(typeArguments.get(i).toQualifiedString());
                if (i < typeArguments.size() - 1)
                    sb.append(",");
            }
            sb.append(">");
        }
        return sb.toString();
    }

    public abstract String toQualifiedString();

    public boolean isParameterized() {
        return typeArguments.size() > 0;
    }

    protected boolean equalTypeArgumentsAndArrayDimensionForSubType(UMLType typeObject) {
        if (!this.isParameterized() && !typeObject.isParameterized())
            return this.arrayDimension == typeObject.arrayDimension;
        else if (this.isParameterized() && typeObject.isParameterized())
            return equalTypeArguments(typeObject) && this.arrayDimension == typeObject.arrayDimension;
        else if (this.isParameterized() && this.typeArgumentsToString().equals("<?>") && !typeObject.isParameterized())
            return this.arrayDimension == typeObject.arrayDimension;
        else if (!this.isParameterized() && typeObject.isParameterized() && typeObject.typeArgumentsToString().equals("<?>"))
            return this.arrayDimension == typeObject.arrayDimension;
        return false;
    }

    public boolean containsTypeArgument(String type) {
        for (UMLType typeArgument : typeArguments) {
            if (typeArgument.toString().equals(type)) {
                return true;
            }
        }
        return false;
    }

    public abstract String toString();

    public abstract int hashCode();

    public abstract String getClassType();

    public boolean equalsQualified(UMLType type) {
        if (this.getClass() == type.getClass()) {
            return this.equals(type);
        }
        return false;
    }

    public boolean equalsWithSubType(UMLType type) {
        if (this.getClass() == type.getClass()) {
            return this.equals(type);
        }
        return false;
    }

    public boolean equalClassType(UMLType type) {
        if (this.getClass() == type.getClass()) {
            return this.equals(type);
        }
        return false;
    }

    public boolean compatibleTypes(UMLType type) {
        if (this.getClass() == type.getClass()) {
            return this.equals(type);
        }
        return false;
    }

    protected String typeArgumentsAndArrayDimensionToString() {
        StringBuilder sb = new StringBuilder();
        if (isParameterized())
            sb.append(typeArgumentsToString());
        sb.append("[]".repeat(Math.max(0, getArrayDimension())));
        return sb.toString();
    }

    public int getArrayDimension() {
        return this.arrayDimension;
    }

    public double normalizedNameDistance(UMLType type) {
        String s1 = this.toString();
        String s2 = type.toString();
        int distance = StringDistance.editDistance(s1, s2);
        return (double) distance / (double) Math.max(s1.length(), s2.length());
    }
}
