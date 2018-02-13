/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.management.dstag;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.eclipse.hawkbit.repository.DistributionSetTagManagement;
import org.eclipse.hawkbit.repository.EntityFactory;
import org.eclipse.hawkbit.repository.builder.TagUpdate;
import org.eclipse.hawkbit.repository.exception.EntityNotFoundException;
import org.eclipse.hawkbit.repository.model.DistributionSetTag;
import org.eclipse.hawkbit.repository.model.Tag;
import org.eclipse.hawkbit.ui.SpPermissionChecker;
import org.eclipse.hawkbit.ui.colorpicker.ColorPickerConstants;
import org.eclipse.hawkbit.ui.colorpicker.ColorPickerHelper;
import org.eclipse.hawkbit.ui.common.table.BaseEntityEventType;
import org.eclipse.hawkbit.ui.components.RefreshableContainer;
import org.eclipse.hawkbit.ui.layouts.AbstractCreateUpdateTagLayout;
import org.eclipse.hawkbit.ui.management.event.DistributionSetTagTableEvent;
import org.eclipse.hawkbit.ui.utils.SPUIDefinitions;
import org.eclipse.hawkbit.ui.utils.UINotification;
import org.eclipse.hawkbit.ui.utils.VaadinMessageSource;
import org.springframework.data.domain.PageRequest;
import org.springframework.util.StringUtils;
import org.vaadin.spring.events.EventBus.UIEventBus;

import com.vaadin.shared.ui.colorpicker.Color;
import com.vaadin.ui.UI;

/**
 * Class for Create/Update Tag Layout of distribution set
 */
public class CreateUpdateDistributionTagLayoutWindow extends AbstractCreateUpdateTagLayout<DistributionSetTag>
        implements RefreshableContainer {

    private static final long serialVersionUID = 1L;

    private static final String TARGET_TAG_NAME_DYNAMIC_STYLE = "new-target-tag-name";
    private static final String MSG_TEXTFIELD_NAME = "textfield.name";

    private final transient DistributionSetTagManagement distributionSetTagManagement;

    CreateUpdateDistributionTagLayoutWindow(final VaadinMessageSource i18n,
            final DistributionSetTagManagement distributionSetTagManagement, final EntityFactory entityFactory,
            final UIEventBus eventBus, final SpPermissionChecker permChecker, final UINotification uiNotification) {
        super(i18n, entityFactory, eventBus, permChecker, uiNotification);
        this.distributionSetTagManagement = distributionSetTagManagement;
    }

    /**
     * @return the color which should be selected in the color-picker component.
     */
    @Override
    protected Color getColorForColorPicker() {
        return ColorPickerHelper.rgbToColorConverter(distributionSetTagManagement
                .getByName(tagNameComboBox.getValue().toString()).map(DistributionSetTag::getColour)
                .filter(Objects::nonNull).orElse(ColorPickerConstants.DEFAULT_COLOR));

    }

    /**
     * update tag.
     */
    protected void updateExistingTag(final Tag targetObj) {
        final TagUpdate update = entityFactory.tag().update(targetObj.getId()).name(tagName.getValue())
                .description(tagDesc.getValue());

        distributionSetTagManagement.update(update);
        eventBus.publish(this,
                new DistributionSetTagTableEvent(BaseEntityEventType.UPDATED_ENTITY, (DistributionSetTag) targetObj));
        uiNotification.displaySuccess(i18n.getMessage("message.update.success", new Object[] { targetObj.getName() }));

    }

    @Override
    protected void populateTagNameCombo() {
        if (tagNameComboBox == null) {
            return;
        }

        tagNameComboBox.removeAllItems();
        final List<DistributionSetTag> distTagNameList = distributionSetTagManagement
                .findAll(new PageRequest(0, MAX_TAGS)).getContent();
        distTagNameList.forEach(value -> tagNameComboBox.addItem(value.getName()));
    }

    @Override
    protected void addListeners() {
        super.addListeners();
        optiongroup.addValueChangeListener(this::optionValueChanged);
    }

    @Override
    protected void createEntity() {
        createNewTag();

    }

    @Override
    protected void updateEntity(final DistributionSetTag entity) {
        updateExistingTag(findEntityByName()
                .orElseThrow(() -> new EntityNotFoundException(DistributionSetTag.class, tagName.getValue())));
    }

    @Override
    protected Optional<DistributionSetTag> findEntityByName() {
        return distributionSetTagManagement.getByName(tagName.getValue());
    }

    /**
     * Create new tag.
     */
    @Override
    protected void createNewTag() {
        super.createNewTag();
        String colour = ColorPickerConstants.START_COLOR.getCSS();
        if (!StringUtils.isEmpty(getColorPicked())) {
            colour = getColorPicked();
        }

        final DistributionSetTag newDistTag = distributionSetTagManagement
                .create(entityFactory.tag().create().name(tagNameValue).description(tagDescValue).colour(colour));
        eventBus.publish(this, new DistributionSetTagTableEvent(BaseEntityEventType.ADD_ENTITY, newDistTag));
        displaySuccess(newDistTag.getName());
        resetDistTagValues();
    }

    /**
     * RESET.
     */
    @Override
    public void discard() {
        super.discard();
        resetDistTagValues();
    }

    /**
     * RESET.
     */
    private void resetDistTagValues() {
        tagName.removeStyleName(TARGET_TAG_NAME_DYNAMIC_STYLE);
        tagName.addStyleName(SPUIDefinitions.NEW_TARGET_TAG_NAME);
        tagName.setValue("");
        tagName.setInputPrompt(i18n.getMessage(MSG_TEXTFIELD_NAME));
        setColor(ColorPickerConstants.START_COLOR);
        getWindow().setVisible(false);
        tagPreviewBtnClicked = false;
        UI.getCurrent().removeWindow(getWindow());
    }

    /**
     * Select tag & set tag name & tag desc values corresponding to selected
     * tag.
     *
     * @param distTagSelected
     *            as the selected tag from combo
     */
    @Override
    public void setTagDetails(final String distTagSelected) {
        tagName.setValue(distTagSelected);
        final Optional<DistributionSetTag> selectedDistTag = distributionSetTagManagement.getByName(distTagSelected);
        if (selectedDistTag.isPresent()) {
            tagDesc.setValue(selectedDistTag.get().getDescription());
            if (null == selectedDistTag.get().getColour()) {
                setTagColor(getColorPickerLayout().getDefaultColor(), ColorPickerConstants.DEFAULT_COLOR);
            } else {
                setTagColor(ColorPickerHelper.rgbToColorConverter(selectedDistTag.get().getColour()),
                        selectedDistTag.get().getColour());
            }
        }
    }

    @Override
    protected void createRequiredComponents() {
        super.createRequiredComponents();
        createOptionGroup(permChecker.hasCreateRepositoryPermission(), permChecker.hasUpdateRepositoryPermission());
    }

    @Override
    protected void reset() {

        super.reset();
        setOptionGroupDefaultValue(permChecker.hasCreateRepositoryPermission(),
                permChecker.hasUpdateRepositoryPermission());
    }

    @Override
    protected String getWindowCaption() {
        return i18n.getMessage("caption.add.tag");
    }

    @Override
    public void refreshContainer() {
        populateTagNameCombo();
    }
}
