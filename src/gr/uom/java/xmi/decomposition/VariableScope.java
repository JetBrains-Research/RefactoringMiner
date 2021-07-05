package gr.uom.java.xmi.decomposition;

import com.intellij.openapi.editor.Document;
import com.intellij.psi.PsiFile;
import gr.uom.java.xmi.LocationInfo;
import java.util.ArrayList;
import java.util.List;

//TODO: Big common part with LocationInfo?
public class VariableScope {
    private final String filePath;
    private final int startOffset;
    private final int endOffset;
    private final int startLine;
    private final int startColumn;
    private final int endLine;
    private final int endColumn;
    private final List<AbstractCodeFragment> statementsInScope = new ArrayList<>();
    private final List<AbstractCodeFragment> statementsInScopeUsingVariable = new ArrayList<>();

    public VariableScope(PsiFile file, String filePath, int startOffset, int endOffset) {
        this.filePath = filePath;
        this.startOffset = startOffset;
        this.endOffset = endOffset;

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
        VariableScope other = (VariableScope) obj;
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
        if (startOffset != other.startOffset)
            return false;
        return true;
    }

    public String toString() {
        return startLine + ":" + startColumn
            + "-"
            + endLine + ":" + endColumn;
    }

    public void addStatement(AbstractCodeFragment statement) {
        this.statementsInScope.add(statement);
    }

    public void addStatementUsingVariable(AbstractCodeFragment statement) {
        this.statementsInScopeUsingVariable.add(statement);
    }

    public List<AbstractCodeFragment> getStatementsInScope() {
        return statementsInScope;
    }

    public List<AbstractCodeFragment> getStatementsInScopeUsingVariable() {
        return statementsInScopeUsingVariable;
    }

    public boolean subsumes(LocationInfo other) {
        return this.filePath.equals(other.getFilePath()) &&
            this.startOffset <= other.getStartOffset() &&
            this.endOffset >= other.getEndOffset();
    }
}
