package org.jetbrains.research.refactoringminer.data;

import java.util.Objects;

public class TestClass {
    protected int field = 1;

    public TestClass(int field) {
        this.field = field;
    }

    public int getField() {
        return field;
    }

    public void setField(int field) {
        this.field = field;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TestClass testClass = (TestClass) o;
        return field == testClass.field;
    }

    @Override
    public int hashCode() {
        return Objects.hash(field);
    }
}
