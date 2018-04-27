/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.components;

import org.eclipse.hawkbit.ui.common.builder.LabelBuilder;
import org.eclipse.hawkbit.ui.utils.SPUIStyleDefinitions;
import org.eclipse.hawkbit.ui.utils.UIComponentIdProvider;

import com.vaadin.ui.ComboBox;
import com.vaadin.ui.Label;
import com.vaadin.ui.VerticalLayout;

/**
 * Layout for the combobox to select a tag or type for update or delete action.
 *
 */
public class UpdateComboBoxForTagsAndTypes extends VerticalLayout {

    private static final long serialVersionUID = 1L;

    private Label comboLabel;

    private ComboBox tagNameComboBox;

    private final String comboBoxHeaderText;

    private final String comboBoxPrompt;

    /**
     * Constructor
     * 
     * @param comboBoxHeaderText
     *            Text which is displayed above the combobox
     * @param comboBoxPrompt
     *            Text which is displayed as input prompt in the combobox
     */
    public UpdateComboBoxForTagsAndTypes(final String comboBoxHeaderText, final String comboBoxPrompt) {
        this.comboBoxHeaderText = comboBoxHeaderText;
        this.comboBoxPrompt = comboBoxPrompt;
        init();
    }

    private void init() {
        createRequiredComponents();
        buildLayout();
    }

    private void createRequiredComponents() {
        comboLabel = new LabelBuilder().name(comboBoxHeaderText).buildLabel();
        tagNameComboBox = SPUIComponentProvider.getComboBox(null, "", null, null, false, "", comboBoxPrompt);
        tagNameComboBox.addStyleName(SPUIStyleDefinitions.FILTER_TYPE_COMBO_STYLE);
        tagNameComboBox.setImmediate(true);
        tagNameComboBox.setId(UIComponentIdProvider.DIST_TAG_COMBO);
    }

    private void buildLayout() {
        addComponent(comboLabel);
        addComponent(tagNameComboBox);
    }

    public Label getComboLabel() {
        return comboLabel;
    }

    public void setComboLabel(final Label comboLabel) {
        this.comboLabel = comboLabel;
    }

    public ComboBox getTagNameComboBox() {
        return tagNameComboBox;
    }

    public void setTagNameComboBox(final ComboBox tagNameComboBox) {
        this.tagNameComboBox = tagNameComboBox;
    }

}
