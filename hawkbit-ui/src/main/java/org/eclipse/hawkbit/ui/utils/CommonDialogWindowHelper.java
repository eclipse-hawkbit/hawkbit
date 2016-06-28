package org.eclipse.hawkbit.ui.utils;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import com.vaadin.ui.AbstractField;
import com.vaadin.ui.Component;
import com.vaadin.ui.FormLayout;

public class CommonDialogWindowHelper {

    /**
     * Run through a FormLayout and collect all required Component's IDs in a
     * Map
     * 
     * @param formLayout
     *            Form to be analysed
     * @return Map with all required component's IDs
     */
    public static Map<String, Boolean> getMandatoryFields(final FormLayout formLayout) {
        final Map<String, Boolean> requiredFields = new HashMap<>();
        final Iterator<Component> iterate = formLayout.iterator();
        while (iterate.hasNext()) {
            final Component c = iterate.next();
            if (c instanceof AbstractField && ((AbstractField) c).isRequired()) {
                requiredFields.put(c.getId(), Boolean.FALSE);
            }
        }
        return requiredFields;
    }
}
