package org.eclipse.hawkbit.repository.jpa.management;

public class T {

    public interface A {
        default int a() {
            return 1;
        }
    }

    public static interface B {
        int a();
    }

    public static class C implements A {
    }

    public static void main(String[] args) {
        System.out.println(new C().a());
    }
}
