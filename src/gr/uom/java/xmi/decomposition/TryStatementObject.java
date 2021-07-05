package gr.uom.java.xmi.decomposition;

import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiTryStatement;
import gr.uom.java.xmi.LocationInfo.CodeElementType;
import java.util.ArrayList;
import java.util.List;

public class TryStatementObject extends CompositeStatementObject {
    private final List<CompositeStatementObject> catchClauses;
    private CompositeStatementObject finallyClause;

    public TryStatementObject(PsiFile file, String filePath, PsiTryStatement statement, int depth) {
        super(file, filePath, statement, depth, CodeElementType.TRY_STATEMENT);
        this.catchClauses = new ArrayList<>();
    }

    public void addCatchClause(CompositeStatementObject catchClause) {
        catchClauses.add(catchClause);
    }

    public List<CompositeStatementObject> getCatchClauses() {
        return catchClauses;
    }

    public void setFinallyClause(CompositeStatementObject finallyClause) {
        this.finallyClause = finallyClause;
    }

    public CompositeStatementObject getFinallyClause() {
        return finallyClause;
    }

    public boolean isTryWithResources() {
        return getExpressions().size() > 0;
    }
}
