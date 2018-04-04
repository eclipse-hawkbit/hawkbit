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

    private String caption;

    public ComboBoxBuilder setValueChangeListener(final Property.ValueChangeListener valueChangeListener) {
        this.valueChangeListener = valueChangeListener;
        return this;
    }

    public ComboBoxBuilder setId(final String id) {
        this.id = id;
        return this;
    }

    public ComboBoxBuilder setPrompt(final String prompt) {
        this.prompt = prompt;
        return this;
    }

    public ComboBoxBuilder setCaption(final String caption) {
        this.caption = caption;
        return this;
    }

    /**
     * @return a new ComboBox
     */
    public ComboBox buildCombBox() {
        final ComboBox comboBox = SPUIComponentProvider.getComboBox(null, "", null, ValoTheme.COMBOBOX_SMALL, false, "",
                prompt);
        comboBox.setImmediate(true);
        comboBox.setPageLength(7);
        comboBox.setItemCaptionPropertyId(SPUILabelDefinitions.VAR_NAME);
        comboBox.setSizeUndefined();
        if (caption != null) {
            comboBox.setCaption(caption);
        }
        if (id != null) {
            comboBox.setId(id);
        }
        if (valueChangeListener != null) {
            comboBox.addValueChangeListener(valueChangeListener);
        }
        return comboBox;
    }
}
