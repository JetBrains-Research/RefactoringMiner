package gr.uom.java.xmi;

import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiArrayType;
import com.intellij.psi.PsiClassType;
import com.intellij.psi.PsiDisjunctionType;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiJavaCodeReferenceElement;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiModifierList;
import com.intellij.psi.PsiPrimitiveType;
import com.intellij.psi.PsiType;
import com.intellij.psi.PsiTypeElement;
import com.intellij.psi.PsiWildcardType;
import gr.uom.java.xmi.ListCompositeType.Kind;
import gr.uom.java.xmi.LocationInfo.CodeElementType;
import gr.uom.java.xmi.diff.CodeRange;
import gr.uom.java.xmi.diff.StringDistance;
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
        if (qualifiedName.endsWith("...")) {
            // TODO: remove setVararg and do arrayDimension++
            qualifiedName = qualifiedName.substring(0, qualifiedName.length() - 3);
        }

        if (qualifiedName.contains(".")) {
            // TODO: parse as Composite type
            qualifiedName = qualifiedName.substring(qualifiedName.lastIndexOf('.'));
        }

        int arrayDimension = 0;
        while (qualifiedName.endsWith("[]")) {
            qualifiedName = qualifiedName.substring(0, qualifiedName.length() - 2);
            arrayDimension++;
        }

        List<UMLType> typeArgumentDecomposition = new ArrayList<>();
        if (qualifiedName.contains("<")) {
            String typeArguments = qualifiedName.substring(qualifiedName.indexOf("<") + 1, qualifiedName.lastIndexOf(">"));
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < typeArguments.length(); i++) {
                char charAt = typeArguments.charAt(i);
                if (charAt != ',') {
                    sb.append(charAt);
                } else {
                    if (sb.length() > 0 && equalOpeningClosingTags(sb.toString())) {
                        typeArgumentDecomposition.add(extractTypeObject(sb.toString()));
                        sb = new StringBuilder();
                    } else {
                        sb.append(charAt);
                    }
                }
            }
            if (sb.length() > 0) {
                typeArgumentDecomposition.add(extractTypeObject(sb.toString()));
            }
            qualifiedName = qualifiedName.substring(0, qualifiedName.indexOf("<"));
        }

        LeafType typeObject = new LeafType(qualifiedName);
        typeObject.arrayDimension = arrayDimension;
        typeObject.typeArguments = typeArgumentDecomposition;
        return typeObject;
    }

    private static boolean equalOpeningClosingTags(String typeArguments) {
        int openingTags = 0;
        int closingTags = 0;
        for (int i = 0; i < typeArguments.length(); i++) {
            if (typeArguments.charAt(i) == '>') {
                openingTags++;
            } else if (typeArguments.charAt(i) == '<') {
                closingTags++;
            }
        }
        return openingTags == closingTags;
    }

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
            PsiModifierList modifierList = getPreventingModifiersList(typeElement);
            if (modifierList != null) {
                Arrays.stream(modifierList.getChildren())
                    .filter(element -> element instanceof PsiAnnotation)
                    .map(annotation -> new UMLAnnotation(file, filePath, (PsiAnnotation) annotation))
                    .forEach(umlType.annotations::add);
            } else {
                if (typeElement.getParent() instanceof PsiTypeElement) {
                    // parts of DisjunctionType
                } else {
                    throw new IllegalStateException();
                }
            }
        }
    }

    private static PsiModifierList getPreventingModifiersList(PsiTypeElement typeElement) {
        PsiElement prev = typeElement;
        while (prev != null) {
            prev = prev.getPrevSibling();
            if (prev instanceof PsiModifierList) {
                return (PsiModifierList) prev;
            }
        }
        return null;
    }

    /**
     * Construct UMLType from Psi parameters
     *
     * @param typeElement Element associated with type declaration position
     * @param type        Real type (differs from typeElement.getType() on C-style arrays)
     */
    private static UMLType extractType(PsiFile file, String filePath, PsiTypeElement typeElement, PsiType type) {
        if (type instanceof PsiDisjunctionType) {
            List<UMLType> umlTypes = Arrays.stream(typeElement.getChildren())
                .filter(element -> element instanceof PsiTypeElement)
                .map(element -> (PsiTypeElement) element)
                .map(dTypeElement -> extractTypeObject(file, filePath, dTypeElement, dTypeElement.getType()))
                .collect(Collectors.toList());
            return new ListCompositeType(umlTypes, Kind.UNION);
        } else {
            PsiJavaCodeReferenceElement innermostType = typeElement.getInnermostComponentReferenceElement();
            if (innermostType != null) {
                return extractTypeObject(file, filePath, innermostType, type);
            } else {
                return extractTypeObject(typeElement.getText());
            }
        }
    }

    public static UMLType extractTypeObject(PsiFile file, String filePath, PsiJavaCodeReferenceElement typeElement, PsiType type) {
        UMLType umlType = extractTypeObject(type, typeElement);
        umlType.locationInfo = new LocationInfo(file, filePath, typeElement, CodeElementType.TYPE);
        Arrays.stream(typeElement.getChildren())
            .filter(element -> element instanceof PsiAnnotation)
            .map(annotation -> new UMLAnnotation(file, filePath, (PsiAnnotation) annotation))
            .forEach(umlType.annotations::add);
        return umlType;
    }

    private static UMLType extractTypeObject(PsiType type, PsiTypeElement typeElement) {
        return extractTypeObject(type, (PsiJavaCodeReferenceElement) typeElement.getFirstChild());
    }

    /**
     * @param type        Exact type. Not all functionality available because of index requirement.
     * @param typeElement Element of type. Ignoring array dimensions
     */
    private static UMLType extractTypeObject(PsiType type, PsiJavaCodeReferenceElement typeElement) {
        if (type instanceof PsiPrimitiveType) {
            PsiPrimitiveType primitiveType = (PsiPrimitiveType) type;
            return extractTypeObject(primitiveType.getName());
        } else if (type instanceof PsiWildcardType) {
            PsiWildcardType wildcardType = (PsiWildcardType) type;
            if (wildcardType.isBounded()) {
                PsiTypeElement bound = (PsiTypeElement) typeElement.getLastChild();
                return new WildcardType(extractTypeObject(wildcardType.getBound(), bound), wildcardType.isSuper());
            } else {
                return new WildcardType(null, false);
            }
        } else if (type instanceof PsiArrayType) {
            PsiArrayType arrayType = (PsiArrayType) type;
            UMLType myArrayType = extractTypeObject(arrayType.getDeepComponentType(), typeElement);
            myArrayType.arrayDimension = arrayType.getArrayDimensions();
            return myArrayType;
        } else if (type instanceof PsiClassType) {
            return extractTypeObject(typeElement.getText());
        } else {
            System.out.println(type.getClass().getName());
            throw new IllegalStateException();
        }
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

    public void setVarargs() {
        arrayDimension++;
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
