package gr.uom.java.xmi;

import org.jetbrains.annotations.NotNull;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Does not support wildcards and annotations
 */
public class UMLTypeStringParser {
    private UMLTypeStringParser() {}

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
                    .map(UMLTypeStringParser::extractArrayDimensions)
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
}
