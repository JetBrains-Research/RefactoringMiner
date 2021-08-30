package org.refactoringminer.api;

import java.util.List;

public interface CodeRangeProvider {
    List<? extends CodeRange> leftSide();

    List<? extends CodeRange> rightSide();
}
