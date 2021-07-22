package gr.uom.java.xmi;

import com.intellij.psi.PsiAnonymousClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiRecursiveElementWalkingVisitor;
import com.intellij.util.containers.Stack;
import org.jetbrains.annotations.NotNull;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class AnonymousClassDeclarationVisitor extends PsiRecursiveElementWalkingVisitor {
    private final Stack<PsiAnonymousClass> stackDeclarations = new Stack<>();
    private final Stack<Integer> childCount = new Stack<>();
    private final List<AnonymousClassDeclaration> declarationsPostOrdered = new ArrayList<>();

    @Override
    public void visitElement(@NotNull PsiElement element) {
        boolean isAnonymous = element instanceof PsiAnonymousClass;
        if (isAnonymous) {
            PsiAnonymousClass anonymousClass = (PsiAnonymousClass) element;
            stackDeclarations.add(anonymousClass);
            if (!childCount.empty()) {
                childCount.add(childCount.pop() + 1);
            }
            childCount.add(1);
        }
        super.visitElement(element);
        if (isAnonymous) {
            declarationsPostOrdered.add(new AnonymousClassDeclaration(stackDeclarations.pop(), childCount));
            childCount.pop();
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
