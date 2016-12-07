/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.common.builder;

import org.eclipse.hawkbit.ui.components.SPUIComponentProvider;
import org.eclipse.hawkbit.ui.utils.SPUILabelDefinitions;

import com.vaadin.data.Property;
import com.vaadin.ui.ComboBox;
import com.vaadin.ui.themes.ValoTheme;

/**
 * Builds ComboBox Elements with a commonly used properties.
 */
public class ComboBoxBuilder {

    private Property.ValueChangeListener valueChangeListener;

    private String id;

    private String prompt;

    public ComboBoxBuilder setValueChangeListener(final Property.ValueChangeListener valueChangeListener) {
        this.valueChangeListener = valueChangeListener;
        return this;
    }

    public ComboBoxBuilder setId(final String id) {
        this.id = id;
        return this;
    }

    public ComboBoxBuilder setPrompt(String prompt) {
        this.prompt = prompt;
        return this;
    }

    /**
     * @return a new ComboBox
     */
    public ComboBox buildCombBox() {
        final ComboBox targetFilter = SPUIComponentProvider.getComboBox(null, "", null, ValoTheme.COMBOBOX_SMALL, false,
                "", prompt);
        targetFilter.setImmediate(true);
        targetFilter.setPageLength(7);
        targetFilter.setItemCaptionPropertyId(SPUILabelDefinitions.VAR_NAME);
        targetFilter.setSizeUndefined();
        if (id != null) {
            targetFilter.setId(id);
        }
        if (valueChangeListener != null) {
            targetFilter.addValueChangeListener(valueChangeListener);
        }
        return targetFilter;
    }
}
