package org.eclipse.hawkbit.ui.management;

@FunctionalInterface
public interface SaveButtonEnabler {
    void setButtonEnabled(boolean enabled);
}