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
import org.eclipse.hawkbit.ui.utils.I18N;
import org.eclipse.hawkbit.ui.utils.SPUILabelDefinitions;

import com.vaadin.data.Property;
import com.vaadin.ui.ComboBox;
import com.vaadin.ui.themes.ValoTheme;

public class ComboBoxBuilder {

    private I18N i18n;

    private Property.ValueChangeListener valueChangeListener;

    private String id;

    public ComboBoxBuilder setI18n(final I18N i18n) {
        this.i18n = i18n;
        return this;
    }

    public ComboBoxBuilder setValueChangeListener(final Property.ValueChangeListener valueChangeListener) {
        this.valueChangeListener = valueChangeListener;
        return this;
    }

    public ComboBoxBuilder setId(final String id) {
        this.id = id;
        return this;
    }

    public ComboBox buildTargetFilterQueryCombo() {
        final ComboBox targetFilter = SPUIComponentProvider.getComboBox(null, "", null, ValoTheme.COMBOBOX_SMALL, false,
                "", i18n.get("prompt.target.filter"));
        targetFilter.setImmediate(true);
        targetFilter.setPageLength(7);
        targetFilter.setItemCaptionPropertyId(SPUILabelDefinitions.VAR_NAME);
        targetFilter.setId(id);
        targetFilter.setSizeUndefined();
        targetFilter.addValueChangeListener(valueChangeListener);
        return targetFilter;
    }
}
