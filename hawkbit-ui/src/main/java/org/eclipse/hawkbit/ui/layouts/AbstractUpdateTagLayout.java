/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.layouts;

import org.eclipse.hawkbit.repository.EntityFactory;
import org.eclipse.hawkbit.repository.model.NamedEntity;
import org.eclipse.hawkbit.ui.SpPermissionChecker;
import org.eclipse.hawkbit.ui.components.UpdateComboBoxForTagsAndTypes;
import org.eclipse.hawkbit.ui.utils.UIComponentIdProvider;
import org.eclipse.hawkbit.ui.utils.UINotification;
import org.eclipse.hawkbit.ui.utils.VaadinMessageSource;
import org.vaadin.spring.events.EventBus.UIEventBus;

import com.vaadin.data.Property.ValueChangeEvent;

/**
 * General Layout for pop-up window for Tags which is created when updating or
 * deleting a tag. The layout includes the combobox for selecting the tag to
 * manage.
 *
 * @param <E>
 */
public abstract class AbstractUpdateTagLayout<E extends NamedEntity> extends AbstractTagLayout<E> {

    private static final long serialVersionUID = 1L;

    private UpdateComboBoxForTagsAndTypes updateCombobox;

    public AbstractUpdateTagLayout(final VaadinMessageSource i18n, final EntityFactory entityFactory,
            final UIEventBus eventBus, final SpPermissionChecker permChecker, final UINotification uiNotification) {
        super(i18n, entityFactory, eventBus, permChecker, uiNotification);
    }

    @Override
    public void init() {
        super.init();
        populateTagNameCombo();
    }

    protected abstract void populateTagNameCombo();

    @Override
    protected void createRequiredComponents() {
        super.createRequiredComponents();
        updateCombobox = new UpdateComboBoxForTagsAndTypes(
                getI18n().getMessage("label.choose.tag", getI18n().getMessage("label.choose.tag.update")),
                getI18n().getMessage("label.combobox.tag"));
        updateCombobox.getTagNameComboBox().setId(UIComponentIdProvider.DIST_TAG_COMBO);
    }

    @Override
    protected void buildLayout() {
        super.buildLayout();
        getFormLayout().addComponent(updateCombobox, 0);
    }

    @Override
    protected void addListeners() {
        super.addListeners();
        updateCombobox.getTagNameComboBox().addValueChangeListener(this::tagNameChosen);
    }

    protected void tagNameChosen(final ValueChangeEvent event) {
        final String tagSelected = (String) event.getProperty().getValue();
        if (tagSelected != null) {
            setTagDetails(tagSelected);
        } else {
            resetFields();
        }
        if (isUpdateAction()) {
            getWindow().setOrginaleValues();
        }
    }

    @Override
    protected boolean isUpdateAction() {
        return true;
    }

    /**
     * Select tag & set tag name & tag desc values corresponding to selected
     * tag.
     *
     * @param distTagSelected
     *            as the selected tag from combo
     */
    protected abstract void setTagDetails(final String tagSelected);

    public UpdateComboBoxForTagsAndTypes getUpdateCombobox() {
        return updateCombobox;
    }

}
