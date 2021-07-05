package gr.uom.java.xmi;

import com.intellij.psi.PsiAnonymousClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementVisitor;
import org.jetbrains.annotations.NotNull;
import java.util.LinkedHashSet;
import java.util.Set;

// TODO: Build a tree
// TODO: extends recursive
public class AnonymousClassDeclarationVisitor extends PsiElementVisitor {
    private final Set<PsiAnonymousClass> anonymousClassDeclarations = new LinkedHashSet<>();

    @Override
    public void visitElement(@NotNull PsiElement element) {
        if (element instanceof PsiAnonymousClass) {
            PsiAnonymousClass anonymousClass = (PsiAnonymousClass) element;
            anonymousClassDeclarations.add(anonymousClass);
        }
        element.accept(this);
    }

    public Set<PsiAnonymousClass> getAnonymousClassDeclarations() {
        return anonymousClassDeclarations;
    }
}
