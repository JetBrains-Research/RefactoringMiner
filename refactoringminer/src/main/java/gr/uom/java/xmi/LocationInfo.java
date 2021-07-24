package gr.uom.java.xmi;

import com.intellij.openapi.editor.Document;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiJavaCodeReferenceElement;
import com.intellij.psi.PsiVariable;
import gr.uom.java.xmi.diff.CodeRange;
import org.jetbrains.annotations.NotNull;

/**
 * Provides an information about the element's location in the file.
 */
public class LocationInfo {
    private final String filePath;
    private final int startOffset;
    private final int endOffset;
    private final int startLine;
    private final int endLine;
    private final int startColumn;
    private final int endColumn;
    private final CodeElementType codeElementType;

    public LocationInfo(@NotNull PsiFile file, @NotNull String filePath, @NotNull PsiElement node,
                        @NotNull CodeElementType codeElementType) {
        this.filePath = filePath;
        this.codeElementType = codeElementType;

        TextRange range = getEclipseRange(node);
        this.startOffset = range.getStartOffset();
        this.endOffset = range.getEndOffset();

        Document document = file.getViewProvider().getDocument();
        if (document != null) {
            this.startLine = document.getLineNumber(startOffset) + 1;
            this.endLine = document.getLineNumber(endOffset) + 1;
            this.startColumn = startOffset - document.getLineStartOffset(startLine - 1) + 1;
            this.endColumn = endOffset - document.getLineStartOffset(endLine - 1) + 1;
        } else {
            this.startLine = 0;
            this.endLine = 0;
            this.startColumn = 0;
            this.endColumn = 0;
        }
    }

    public static TextRange getEclipseRange(@NotNull PsiElement node) {
        if (node instanceof PsiVariable) {
            // initializer in range
            return new TextRange(
                ((PsiVariable) node).getNameIdentifier().getTextOffset(),
                node.getLastChild().getTextOffset()
            );
        } else if (node instanceof PsiJavaCodeReferenceElement) {
            // array brackets in range
            return new TextRange(
                node.getTextOffset(),
                node.getParent().getLastChild().getTextRange().getEndOffset()
            );
        }
        return node.getTextRange();
    }

    public int getStartOffset() {
        return startOffset;
    }

    public int getEndOffset() {
        return endOffset;
    }

    public int getLength() {
        return endOffset - startOffset;
    }

    public CodeRange codeRange() {
        return new CodeRange(getFilePath(),
            getStartLine(), getEndLine(),
            getStartColumn(), getEndColumn(), getCodeElementType());
    }

    public String getFilePath() {
        return filePath;
    }

    public int getStartLine() {
        return startLine;
    }

    public int getStartColumn() {
        return startColumn;
    }

    public int getEndLine() {
        return endLine;
    }

    public int getEndColumn() {
        return endColumn;
    }

    public CodeElementType getCodeElementType() {
        return codeElementType;
    }

    public boolean subsumes(LocationInfo other) {
        return this.filePath.equals(other.filePath) &&
            this.startOffset <= other.startOffset &&
            this.endOffset >= other.endOffset;
    }

    public boolean sameLine(LocationInfo other) {
        return this.filePath.equals(other.filePath) &&
            this.startLine == other.startLine &&
            this.endLine == other.endLine;
    }

    public boolean nextLine(LocationInfo other) {
        return this.filePath.equals(other.filePath) &&
            this.startLine == other.endLine + 1;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + endColumn;
        result = prime * result + endLine;
        result = prime * result + endOffset;
        result = prime * result + ((filePath == null) ? 0 : filePath.hashCode());
        result = prime * result + startColumn;
        result = prime * result + startLine;
        result = prime * result + startOffset;
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
        LocationInfo other = (LocationInfo) obj;
        if (endColumn != other.endColumn)
            return false;
        if (endLine != other.endLine)
            return false;
        if (endOffset != other.endOffset)
            return false;
        if (filePath == null) {
            if (other.filePath != null)
                return false;
        } else if (!filePath.equals(other.filePath))
            return false;
        if (startColumn != other.startColumn)
            return false;
        if (startLine != other.startLine)
            return false;
        return startOffset == other.startOffset;
    }

    public enum CodeElementType {
        TYPE_DECLARATION,
        METHOD_DECLARATION,
        FIELD_DECLARATION,
        SINGLE_VARIABLE_DECLARATION,
        VARIABLE_DECLARATION_STATEMENT,
        VARIABLE_DECLARATION_EXPRESSION,
        VARIABLE_DECLARATION_INITIALIZER,
        ANONYMOUS_CLASS_DECLARATION,
        LAMBDA_EXPRESSION,
        LAMBDA_EXPRESSION_BODY,
        CLASS_INSTANCE_CREATION,
        ARRAY_CREATION,
        METHOD_INVOCATION,
        SUPER_METHOD_INVOCATION,
        TERNARY_OPERATOR_CONDITION,
        TERNARY_OPERATOR_THEN_EXPRESSION,
        TERNARY_OPERATOR_ELSE_EXPRESSION,
        LABELED_STATEMENT,
        FOR_STATEMENT("for"),
        FOR_STATEMENT_CONDITION,
        FOR_STATEMENT_INITIALIZER,
        FOR_STATEMENT_UPDATER,
        ENHANCED_FOR_STATEMENT("for"),
        ENHANCED_FOR_STATEMENT_PARAMETER_NAME,
        ENHANCED_FOR_STATEMENT_EXPRESSION,
        WHILE_STATEMENT("while"),
        WHILE_STATEMENT_CONDITION,
        IF_STATEMENT("if"),
        IF_STATEMENT_CONDITION,
        DO_STATEMENT("do"),
        DO_STATEMENT_CONDITION,
        SWITCH_STATEMENT("switch"),
        SWITCH_STATEMENT_CONDITION,
        SYNCHRONIZED_STATEMENT("synchronized"),
        SYNCHRONIZED_STATEMENT_EXPRESSION,
        TRY_STATEMENT("try"),
        TRY_STATEMENT_RESOURCE,
        CATCH_CLAUSE("catch"),
        CATCH_CLAUSE_EXCEPTION_NAME,
        EXPRESSION_STATEMENT,
        SWITCH_CASE,
        ASSERT_STATEMENT,
        RETURN_STATEMENT,
        THROW_STATEMENT,
        CONSTRUCTOR_INVOCATION,
        SUPER_CONSTRUCTOR_INVOCATION,
        BREAK_STATEMENT,
        CONTINUE_STATEMENT,
        EMPTY_STATEMENT,
        BLOCK("{"),
        FINALLY_BLOCK("finally"),
        TYPE,
        LIST_OF_STATEMENTS,
        ANNOTATION,
        SINGLE_MEMBER_ANNOTATION_VALUE,
        NORMAL_ANNOTATION_MEMBER_VALUE_PAIR,
        ENUM_CONSTANT_DECLARATION,
        JAVADOC,
        LINE_COMMENT,
        BLOCK_COMMENT;

        private String name;

        CodeElementType() {

        }

        CodeElementType(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }

        public CodeElementType setName(String name) {
            this.name = name;
            return this;
        }
    }
}
