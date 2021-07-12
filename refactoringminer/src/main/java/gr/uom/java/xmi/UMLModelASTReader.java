package gr.uom.java.xmi;

import com.intellij.lang.java.JavaLanguage;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.psi.*;
import com.intellij.psi.javadoc.PsiDocComment;
import com.intellij.psi.javadoc.PsiDocTag;
import com.intellij.psi.util.PsiTreeUtil;
import gr.uom.java.xmi.decomposition.OperationBody;
import gr.uom.java.xmi.decomposition.VariableDeclaration;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;

public class UMLModelASTReader {
    private final UMLModel umlModel;

    public UMLModelASTReader(Map<String, String> javaFileContents, Set<String> repositoryDirectories) {
        this.umlModel = new UMLModel(repositoryDirectories);
        processJavaFileContents(javaFileContents, ProjectManager.getInstance().getDefaultProject());
    }

    public UMLModel getUmlModel() {
        return this.umlModel;
    }

    private void processJavaFileContents(Map<String, String> javaFileContents, Project project) {
        for (Map.Entry<String, String> file : javaFileContents.entrySet()) {
            PsiFile psiFile = buildPsiFile(project, file.getValue());
            try {
                processCompilationUnit(file.getKey(), psiFile, file.getValue());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public PsiFile buildPsiFile(Project project, String content) {
        PsiFileFactory factory = PsiFileFactory.getInstance(project);
        return factory.createFileFromText(JavaLanguage.INSTANCE, content);
    }

    private void processCompilationUnit(String sourceFilePath, PsiFile file, String javaFileContent) {
        List<UMLComment> comments = extractInternalComments(file, sourceFilePath, javaFileContent);
        String packageName = getPackageName(file);
        Collection<PsiImportStatement> imports = PsiTreeUtil.findChildrenOfType(file, PsiImportStatement.class);
        List<String> importedTypes = new ArrayList<>();
        for (PsiImportStatement importDeclaration : imports) {
            importedTypes.add(importDeclaration.getQualifiedName());
        }

        PsiElement[] topLevelTypeDeclarations = file.getChildren();
        for (PsiElement element : topLevelTypeDeclarations) {
            if (element instanceof PsiClass) {
                PsiClass topLevelClass = (PsiClass) element;
                if (topLevelClass.isEnum()) {
                    processEnumDeclaration(file, topLevelClass, packageName, sourceFilePath, importedTypes, comments);
                } else {
                    processTypeDeclaration(file, topLevelClass, packageName, sourceFilePath, importedTypes, comments);
                }
            }
        }
    }

    @NotNull
    private String getPackageName(@NotNull PsiFile file) {
        PsiPackageStatement packageDeclaration = PsiTreeUtil.findChildOfType(file, PsiPackageStatement.class);
        String packageName;
        if (packageDeclaration != null) {
            packageName = packageDeclaration.getPackageName();
        } else {
            packageName = "";
        }
        return packageName;
    }

    @NotNull
    private List<UMLComment> extractInternalComments(@NotNull PsiFile file,
                                                     @NotNull String sourceFile,
                                                     @NotNull String javaFileContent) {
        Collection<PsiComment> astComments = PsiTreeUtil.findChildrenOfType(file, PsiComment.class);
        List<UMLComment> comments = new ArrayList<>();
        for (PsiComment comment : astComments) {
            LocationInfo locationInfo = null;
            if (comment.getTokenType() == JavaTokenType.END_OF_LINE_COMMENT) {
                locationInfo = new LocationInfo(file, sourceFile, comment, LocationInfo.CodeElementType.LINE_COMMENT);
            } else if (comment.getTokenType() == JavaTokenType.C_STYLE_COMMENT) {
                locationInfo = new LocationInfo(file, sourceFile, comment, LocationInfo.CodeElementType.BLOCK_COMMENT);
            }
            if (locationInfo != null) {
                int start = comment.getTextRange().getStartOffset();
                int end = comment.getTextRange().getEndOffset();
                String text = javaFileContent.substring(start, end);
                UMLComment umlComment = new UMLComment(text, locationInfo);
                comments.add(umlComment);
            }
        }
        return comments;
    }

    private void distributeComments(List<UMLComment> compilationUnitComments, LocationInfo codeElementLocationInfo, List<UMLComment> codeElementComments) {
        ListIterator<UMLComment> listIterator = compilationUnitComments.listIterator(compilationUnitComments.size());
        while (listIterator.hasPrevious()) {
            UMLComment comment = listIterator.previous();
            LocationInfo commentLocationInfo = comment.getLocationInfo();
            if (codeElementLocationInfo.subsumes(commentLocationInfo) ||
                codeElementLocationInfo.sameLine(commentLocationInfo) ||
                (codeElementLocationInfo.nextLine(commentLocationInfo) && !codeElementLocationInfo.getCodeElementType().equals(LocationInfo.CodeElementType.ANONYMOUS_CLASS_DECLARATION)) ||
                (codeElementComments.size() > 0 && codeElementComments.get(0).getLocationInfo().nextLine(commentLocationInfo))) {
                codeElementComments.add(0, comment);
            }
        }
        compilationUnitComments.removeAll(codeElementComments);
    }

    private UMLJavadoc generateJavadoc(PsiFile file, PsiJavaDocumentedElement element, String sourceFile) {
        UMLJavadoc doc = null;
        PsiDocComment javaDoc = element.getDocComment();
        if (javaDoc != null) {
            LocationInfo locationInfo = new LocationInfo(file, sourceFile, javaDoc, LocationInfo.CodeElementType.JAVADOC);
            doc = new UMLJavadoc(locationInfo);
            PsiDocTag[] tags = javaDoc.getTags();
            for (PsiDocTag tag : tags) {
                UMLTagElement tagElement = new UMLTagElement(tag.getName());
                PsiElement[] fragments = tag.getDataElements();
                for (PsiElement docElement : fragments) {
                    tagElement.addFragment(sourceFile.substring(docElement.getTextRange().getStartOffset(),
                        docElement.getTextRange().getEndOffset()));
                }
                doc.addTag(tagElement);
            }
        }
        return doc;
    }

    private void processEnumDeclaration(PsiFile file, PsiClass psiEnumClass, String packageName, String sourceFile,
                                        List<String> importedTypes, List<UMLComment> comments) {
        String className = psiEnumClass.getName();
        LocationInfo locationInfo = new LocationInfo(file, sourceFile, psiEnumClass, LocationInfo.CodeElementType.TYPE_DECLARATION);
        boolean isTopLevel = psiEnumClass.getParent() instanceof PsiFile;
        UMLClass umlClass = new UMLClass(packageName, className, locationInfo, isTopLevel, importedTypes);
        umlClass.setJavadoc(generateJavadoc(file, psiEnumClass, sourceFile));
        umlClass.setEnum(true);

        for (UMLType umlType : getUMLTypes(file, sourceFile, psiEnumClass.getImplementsList())) {
            UMLRealization umlRealization = new UMLRealization(umlClass, umlType.getClassType());
            umlClass.addImplementedInterface(umlType);
            this.umlModel.addRealization(umlRealization);
        }

        for (PsiElement enumElement : psiEnumClass.getChildren()) {
            if (enumElement instanceof PsiEnumConstant) {
                processEnumConstantDeclaration(file, (PsiEnumConstant) enumElement, sourceFile, umlClass, comments);
            }
        }

        processModifiers(file, sourceFile, psiEnumClass, umlClass);

        processBodyDeclarations(file, psiEnumClass, packageName, sourceFile, importedTypes, umlClass, comments);

        processAnonymousClassDeclarations(file, psiEnumClass, packageName, sourceFile, className, umlClass);

        this.umlModel.addClass(umlClass);
        distributeComments(comments, locationInfo, umlClass.getComments());
    }

    private List<UMLType> getUMLTypes(PsiFile file, String sourceFile, @Nullable PsiReferenceList list) {
        if (list == null) {
            return Collections.emptyList();
        }
        PsiJavaCodeReferenceElement[] superInterfaceElements = list.getReferenceElements();
        PsiClassType[] superInterfaceTypes = list.getReferencedTypes();
        assert superInterfaceTypes.length == superInterfaceElements.length;
        List<UMLType> types = new ArrayList<>(superInterfaceTypes.length);
        for (int i = 0; i < superInterfaceTypes.length; i++) {
            types.set(i, UMLType.extractTypeObject(file, sourceFile, superInterfaceElements[i], superInterfaceTypes[i]));
        }
        return types;
    }

    private void processBodyDeclarations(PsiFile file, PsiClass psiClass, String packageName,
                                         String sourceFile, List<String> importedTypes,
                                         UMLClass umlClass, List<UMLComment> comments) {
        PsiElement[] children = psiClass.getChildren();
        for (PsiElement psiElement : children) {
            if (psiElement instanceof PsiField) {
                PsiField psiField = (PsiField) psiElement;
                List<UMLAttribute> attributes = processFieldDeclaration(file, psiField, sourceFile, comments);
                for (UMLAttribute attribute : attributes) {
                    attribute.setClassName(umlClass.getName());
                    umlClass.addAttribute(attribute);
                }
            } else if (psiElement instanceof PsiMethod) {
                PsiMethod psiMethod = (PsiMethod) psiElement;
                UMLOperation operation = processMethodDeclaration(file, psiMethod, sourceFile, comments);
                operation.setClassName(umlClass.getName());
                umlClass.addOperation(operation);
            } else if (psiElement instanceof PsiClass) {
                PsiClass psiSubClass = (PsiClass) psiElement;
                if (psiSubClass.isEnum()) {
                    processEnumDeclaration(file, psiSubClass, umlClass.getName(), sourceFile, importedTypes, comments);
                } else {
                    processTypeDeclaration(file, psiSubClass, umlClass.getName(), sourceFile, importedTypes, comments);
                }
            }
        }
    }

    private void processTypeDeclaration(PsiFile file, PsiClass psiClass,
                                        String packageName, String sourceFile,
                                        List<String> importedTypes, List<UMLComment> comments) {
        String className = psiClass.getName();
        LocationInfo locationInfo = new LocationInfo(file, sourceFile, psiClass, LocationInfo.CodeElementType.TYPE_DECLARATION);
        boolean isTopLevel = psiClass.getParent() instanceof PsiFile;
        UMLClass umlClass = new UMLClass(packageName, className, locationInfo, isTopLevel, importedTypes);
        umlClass.setJavadoc(generateJavadoc(file, psiClass, sourceFile));

        if (psiClass.isInterface()) {
            umlClass.setInterface(true);
        } else {
            List<UMLType> extendsList = getUMLTypes(file, sourceFile, psiClass.getExtendsList());
            if (!extendsList.isEmpty()) {
                assert extendsList.size() == 1;
                UMLType umlType = extendsList.get(0);
                UMLGeneralization umlGeneralization = new UMLGeneralization(umlClass, umlType.getClassType());
                umlClass.setSuperclass(umlType);
                this.umlModel.addGeneralization(umlGeneralization);
            }

            for (UMLType umlType : getUMLTypes(file, sourceFile, psiClass.getImplementsList())) {
                UMLRealization umlRealization = new UMLRealization(umlClass, umlType.getClassType());
                umlClass.addImplementedInterface(umlType);
                this.umlModel.addRealization(umlRealization);
            }
        }

        processModifiers(file, sourceFile, psiClass, umlClass);

        PsiTypeParameter[] typeParameters = psiClass.getTypeParameters();
        for (PsiTypeParameter typeParameter : typeParameters) {
            umlClass.addTypeParameter(processTypeParameters(file, sourceFile, typeParameter));
        }

        PsiField[] fieldDeclarations = psiClass.getFields();
        for (PsiField fieldDeclaration : fieldDeclarations) {
            List<UMLAttribute> attributes = processFieldDeclaration(file, fieldDeclaration, sourceFile, comments);
            for (UMLAttribute attribute : attributes) {
                attribute.setClassName(umlClass.getName());
                umlClass.addAttribute(attribute);
            }
        }

        PsiMethod[] methodDeclarations = psiClass.getMethods();
        for (PsiMethod methodDeclaration : methodDeclarations) {
            UMLOperation operation = processMethodDeclaration(file, methodDeclaration, sourceFile, comments);
            operation.setClassName(umlClass.getName());
            umlClass.addOperation(operation);
        }

        processAnonymousClassDeclarations(file, psiClass, packageName, sourceFile, className, umlClass);

        this.umlModel.addClass(umlClass);

        PsiClass[] types = psiClass.getInnerClasses();
        for (PsiClass type : types) {
            if (type.isEnum()) {
                processEnumDeclaration(file, type, umlClass.getName(), sourceFile, importedTypes, comments);
            } else {
                processTypeDeclaration(file, type, umlClass.getName(), sourceFile, importedTypes, comments);
            }
        }
        distributeComments(comments, locationInfo, umlClass.getComments());
    }

    @NotNull
    private UMLTypeParameter processTypeParameters(PsiFile file, String sourceFile, PsiTypeParameter typeParameter) {
        UMLTypeParameter umlTypeParameter = new UMLTypeParameter(typeParameter.getQualifiedName());
        List<UMLType> extendsList = getUMLTypes(file, sourceFile, typeParameter.getExtendsList());
        if (!extendsList.isEmpty()) {
            assert extendsList.size() == 1;
            umlTypeParameter.addTypeBound(extendsList.get(0));
        }
        PsiAnnotation[] typeParameterAnnotations = typeParameter.getAnnotations();
        for (PsiAnnotation psiAnnotation : typeParameterAnnotations) {
            umlTypeParameter.addAnnotation(new UMLAnnotation(file, sourceFile, psiAnnotation));
        }
        return umlTypeParameter;
    }

    private void processAnonymousClassDeclarations(PsiFile file, PsiClass psiClass,
                                                   String packageName, String sourceFile,
                                                   String className, UMLClass umlClass) {
        AnonymousClassDeclarationVisitor visitor = new AnonymousClassDeclarationVisitor();
        psiClass.accept(visitor);
        List<UMLAnonymousClass> createdAnonymousClasses = new ArrayList<>();
        for (AnonymousClassDeclarationVisitor.AnonymousClassDeclaration anonymousClassDeclaration
            : visitor.getPostOrderedAnonymousClassDeclarations()) {
            PsiAnonymousClass anonymous = anonymousClassDeclaration.psiAnonymousClass;
            UMLOperation matchingOperation = null;
            UMLAttribute matchingAttribute = null;
            List<UMLComment> comments = null;
            for (UMLOperation operation : umlClass.getOperations()) {
                if (operation.getLocationInfo().getStartOffset() <= anonymous.getTextRange().getStartOffset() &&
                    operation.getLocationInfo().getEndOffset() >= anonymous.getTextRange().getEndOffset()) {
                    comments = operation.getComments();
                    matchingOperation = operation;
                    break;
                }
            }
            if (matchingOperation == null) {
                for (UMLAttribute attribute : umlClass.getAttributes()) {
                    if (attribute.getLocationInfo().getStartOffset() <= anonymous.getTextRange().getStartOffset() &&
                        attribute.getLocationInfo().getEndOffset() >= anonymous.getTextRange().getEndOffset()) {
                        comments = attribute.getComments();
                        matchingAttribute = attribute;
                        break;
                    }
                }
            }
            if (matchingOperation != null || matchingAttribute != null) {
                String anonymousBinaryName = anonymousClassDeclaration.binaryName;
                String anonymousCodePath = anonymous.getName();
                UMLAnonymousClass anonymousClass = processAnonymousClassDeclaration(file, anonymous, packageName + "." + className, anonymousBinaryName, anonymousCodePath, sourceFile, comments);
                umlClass.addAnonymousClass(anonymousClass);
                if (matchingOperation != null) {
                    matchingOperation.addAnonymousClass(anonymousClass);
                }
                if (matchingAttribute != null) {
                    matchingAttribute.addAnonymousClass(anonymousClass);
                }
                for (UMLOperation operation : anonymousClass.getOperations()) {
                    for (UMLAnonymousClass createdAnonymousClass : createdAnonymousClasses) {
                        if (operation.getLocationInfo().subsumes(createdAnonymousClass.getLocationInfo())) {
                            operation.addAnonymousClass(createdAnonymousClass);
                        }
                    }
                }
                createdAnonymousClasses.add(anonymousClass);
            }
        }
    }

    private void processModifiers(PsiFile file, String sourceFile, PsiClass psiClass, UMLClass umlClass) {
        PsiModifierList modifiers = psiClass.getModifierList();
        assert modifiers != null;
        if (modifiers.hasExplicitModifier(PsiModifier.ABSTRACT)) {
            umlClass.setAbstract(true);
        }
        if (modifiers.hasExplicitModifier(PsiModifier.STATIC)) {
            umlClass.setStatic(true);
        }
        if (modifiers.hasExplicitModifier(PsiModifier.FINAL)) {
            umlClass.setFinal(true);
        }

        if (modifiers.hasExplicitModifier(PsiModifier.PUBLIC)) {
            umlClass.setVisibility("public");
        } else if (modifiers.hasExplicitModifier(PsiModifier.PROTECTED)) {
            umlClass.setVisibility("protected");
        } else if (modifiers.hasExplicitModifier(PsiModifier.PRIVATE)) {
            umlClass.setVisibility("private");
        } else {
            umlClass.setVisibility("package");
        }

        try {
            for (PsiAnnotation annotation : psiClass.getAnnotations()) {
                umlClass.addAnnotation(new UMLAnnotation(file, sourceFile, annotation));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void processModifiers(PsiFile file, String sourceFile, PsiModifierList modifiers, UMLOperation umlOperation) {
        if (modifiers.hasExplicitModifier(PsiModifier.ABSTRACT)) {
            umlOperation.setAbstract(true);
        }
        if (modifiers.hasExplicitModifier(PsiModifier.STATIC)) {
            umlOperation.setStatic(true);
        }
        if (modifiers.hasExplicitModifier(PsiModifier.FINAL)) {
            umlOperation.setFinal(true);
        }
        if (modifiers.hasExplicitModifier(PsiModifier.SYNCHRONIZED)) {
            umlOperation.setSynchronized(true);
        }

        if (modifiers.hasModifierProperty(PsiModifier.PUBLIC)) {
            umlOperation.setVisibility("public");
        } else if (modifiers.hasExplicitModifier(PsiModifier.PROTECTED)) {
            umlOperation.setVisibility("protected");
        } else if (modifiers.hasExplicitModifier(PsiModifier.PRIVATE)) {
            umlOperation.setVisibility("private");
        } else {
            umlOperation.setVisibility("package");
        }

        for (PsiAnnotation annotation : modifiers.getAnnotations()) {
            umlOperation.addAnnotation(new UMLAnnotation(file, sourceFile, annotation));
        }
    }

    private void processModifiers(@Nullable PsiModifierList modifiers, UMLAttribute umlAttribute) {
        assert modifiers != null;
        if (modifiers.hasExplicitModifier(PsiModifier.STATIC)) {
            umlAttribute.setStatic(true);
        }
        if (modifiers.hasExplicitModifier(PsiModifier.FINAL)) {
            umlAttribute.setFinal(true);
        }
        if (modifiers.hasExplicitModifier(PsiModifier.VOLATILE)) {
            umlAttribute.setVolatile(true);
        }
        if (modifiers.hasExplicitModifier(PsiModifier.TRANSIENT)) {
            umlAttribute.setTransient(true);
        }

        if (modifiers.hasModifierProperty(PsiModifier.PUBLIC)) {
            umlAttribute.setVisibility("public");
        } else if (modifiers.hasExplicitModifier(PsiModifier.PROTECTED)) {
            umlAttribute.setVisibility("protected");
        } else if (modifiers.hasExplicitModifier(PsiModifier.PRIVATE)) {
            umlAttribute.setVisibility("private");
        } else {
            umlAttribute.setVisibility("package");
        }

        //TODO: Annotations?
    }

    private UMLOperation processMethodDeclaration(PsiFile file, PsiMethod psiMethod,
                                                  String sourceFile, List<UMLComment> comments) {
        String methodName = psiMethod.getName(); // TODO: Qualified
        LocationInfo locationInfo = new LocationInfo(file, sourceFile, psiMethod, LocationInfo.CodeElementType.METHOD_DECLARATION);
        UMLOperation umlOperation = new UMLOperation(methodName, locationInfo);
        umlOperation.setJavadoc(generateJavadoc(file, psiMethod, sourceFile));
        distributeComments(comments, locationInfo, umlOperation.getComments());
        umlOperation.setConstructor(psiMethod.isConstructor());

        processModifiers(file, sourceFile, psiMethod.getModifierList(), umlOperation);

        PsiTypeParameter[] typeParameters = psiMethod.getTypeParameters();
        for (PsiTypeParameter typeParameter : typeParameters) {
            umlOperation.addTypeParameter(processTypeParameters(file, sourceFile, typeParameter));
        }

        PsiCodeBlock block = psiMethod.getBody();
        if (block != null) {
            OperationBody body = new OperationBody(file, sourceFile, block);
            umlOperation.setBody(body);
            umlOperation.setEmptyBody(block.isEmpty());
        } else {
            umlOperation.setBody(null);
        }


        if (!psiMethod.isConstructor()) {
            UMLType type = UMLType.extractTypeObject(file, sourceFile, psiMethod.getReturnTypeElement(), psiMethod.getReturnType());
            UMLParameter returnParameter = new UMLParameter("return", type, "return", false);
            umlOperation.addParameter(returnParameter);
        }
        PsiParameter[] parameters = psiMethod.getParameterList().getParameters();
        for (PsiParameter parameter : parameters) {
            UMLType type = UMLType.extractTypeObject(file, sourceFile, parameter.getTypeElement(), parameter.getType());
            String parameterName = parameter.getName();
            UMLParameter umlParameter = new UMLParameter(parameterName, type, "in", parameter.isVarArgs());
            VariableDeclaration variableDeclaration = new VariableDeclaration(file, sourceFile, parameter, parameter.isVarArgs());
            variableDeclaration.setParameter(true);
            umlParameter.setVariableDeclaration(variableDeclaration);
            umlOperation.addParameter(umlParameter);
        }
        for (UMLType umlType : getUMLTypes(file, sourceFile, psiMethod.getThrowsList())) {
            umlOperation.addThrownExceptionType(umlType);
        }
        return umlOperation;
    }

    private void processEnumConstantDeclaration(PsiFile file, PsiEnumConstant psiEnumConstant, String sourceFile,
                                                UMLClass umlClass, List<UMLComment> comments) {
        UMLJavadoc javadoc = generateJavadoc(file, psiEnumConstant, sourceFile);
        LocationInfo locationInfo = new LocationInfo(file, sourceFile, psiEnumConstant, LocationInfo.CodeElementType.ENUM_CONSTANT_DECLARATION);
        UMLEnumConstant enumConstant = new UMLEnumConstant(psiEnumConstant.getName(), UMLType.extractTypeObject(umlClass.getName()), locationInfo);
        gr.uom.java.xmi.decomposition.VariableDeclaration variableDeclaration = new gr.uom.java.xmi.decomposition.VariableDeclaration(file, sourceFile, psiEnumConstant);
        enumConstant.setVariableDeclaration(variableDeclaration);
        enumConstant.setJavadoc(javadoc);
        distributeComments(comments, locationInfo, enumConstant.getComments());
        enumConstant.setFinal(true);
        enumConstant.setStatic(true);
        enumConstant.setVisibility("public");
        PsiExpressionList argumentList = psiEnumConstant.getArgumentList();
        if (argumentList != null) {
            for (PsiExpression argument : argumentList.getExpressions()) {
                enumConstant.addArgument(argument.toString());
            }
        }
        enumConstant.setClassName(umlClass.getName());
        umlClass.addEnumConstant(enumConstant);
    }

    //TODO: Some fields together?
    private List<UMLAttribute> processFieldDeclaration(PsiFile file, PsiField psiField,
                                                       String sourceFile, List<UMLComment> comments) {
        UMLJavadoc javadoc = generateJavadoc(file, psiField, sourceFile);
        List<UMLAttribute> attributes = new ArrayList<>();
        UMLType type = UMLType.extractTypeObject(file, sourceFile, psiField.getTypeElement(), psiField.getType());
        String fieldName = psiField.getName();
        LocationInfo locationInfo = new LocationInfo(file, sourceFile, psiField, LocationInfo.CodeElementType.FIELD_DECLARATION);
        VariableDeclaration variableDeclaration = new VariableDeclaration(file, sourceFile, psiField);
        variableDeclaration.setAttribute(true);
        UMLAttribute umlAttribute = new UMLAttribute(fieldName, type, locationInfo);
        umlAttribute.setVariableDeclaration(variableDeclaration);
        umlAttribute.setJavadoc(javadoc);
        distributeComments(comments, locationInfo, umlAttribute.getComments());//TODO: Empty comments??
        processModifiers(psiField.getModifierList(), umlAttribute);
        attributes.add(umlAttribute);
        return attributes;
    }

    private UMLAnonymousClass processAnonymousClassDeclaration(PsiFile file, PsiAnonymousClass psiAnonymousClass,
                                                               String packageName, String binaryName, String codePath,
                                                               String sourceFile, List<UMLComment> comments) {
        LocationInfo locationInfo = new LocationInfo(file, sourceFile, psiAnonymousClass, LocationInfo.CodeElementType.ANONYMOUS_CLASS_DECLARATION);
        UMLAnonymousClass anonymousClass = new UMLAnonymousClass(packageName, binaryName, codePath, locationInfo);

        for (PsiField field : psiAnonymousClass.getFields()) {
            List<UMLAttribute> attributes = processFieldDeclaration(file, field, sourceFile, comments);
            for (UMLAttribute attribute : attributes) {
                attribute.setClassName(anonymousClass.getCodePath());
                anonymousClass.addAttribute(attribute);
            }
        }

        for (PsiMethod method : psiAnonymousClass.getMethods()) {
            UMLOperation operation = processMethodDeclaration(file, method, sourceFile, comments);
            operation.setClassName(anonymousClass.getCodePath());
            anonymousClass.addOperation(operation);
        }

        distributeComments(comments, locationInfo, anonymousClass.getComments());
        return anonymousClass;
    }
}
