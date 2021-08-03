package gr.uom.java.xmi;

import com.intellij.psi.PsiAnonymousClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiRecursiveElementWalkingVisitor;
import com.intellij.util.containers.Stack;
import org.jetbrains.annotations.NotNull;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Finds all of anonymous classes in element subtree and add them to list in the postorder
 */
public class AnonymousClassDeclarationVisitor extends PsiRecursiveElementWalkingVisitor {
    private final Stack<PsiAnonymousClass> stackDeclarations = new Stack<>();
    private final Stack<Integer> childCount = new Stack<>();
    private final List<AnonymousClassDeclaration> declarationsPostOrdered = new ArrayList<>();

    {
        childCount.add(1);
    }

    @Override
    public void visitElement(@NotNull PsiElement element) {
        if (element instanceof PsiAnonymousClass) {
            PsiAnonymousClass anonymousClass = (PsiAnonymousClass) element;
            stackDeclarations.add(anonymousClass);
            childCount.add(1);
        }
        super.visitElement(element);
    }

    @Override
    protected void elementFinished(PsiElement element) {
        if (element instanceof PsiAnonymousClass) {
            childCount.pop();
            declarationsPostOrdered.add(new AnonymousClassDeclaration(stackDeclarations.pop(), childCount));
            childCount.add(childCount.pop() + 1);
        }
    }

    public List<AnonymousClassDeclaration> getPostOrderedAnonymousClassDeclarations() {
        return declarationsPostOrdered;
    }

    static class AnonymousClassDeclaration {
        public final PsiAnonymousClass psiAnonymousClass;
        public final String binaryName;

        AnonymousClassDeclaration(PsiAnonymousClass psiAnonymousClass, List<Integer> stackAnonymousChildCount) {
            this.psiAnonymousClass = psiAnonymousClass;
            this.binaryName = stackAnonymousChildCount.stream()
                .map(Object::toString)
                .collect(Collectors.joining("."));
        }
    }
}
