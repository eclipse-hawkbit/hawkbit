/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.artifacts.smtype;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.eclipse.hawkbit.repository.EntityFactory;
import org.eclipse.hawkbit.repository.SoftwareModuleTypeManagement;
import org.eclipse.hawkbit.repository.model.SoftwareModuleType;
import org.eclipse.hawkbit.ui.SpPermissionChecker;
import org.eclipse.hawkbit.ui.artifacts.event.SoftwareModuleTypeEvent;
import org.eclipse.hawkbit.ui.artifacts.event.SoftwareModuleTypeEvent.SoftwareModuleTypeEnum;
import org.eclipse.hawkbit.ui.colorpicker.ColorPickerConstants;
import org.eclipse.hawkbit.ui.colorpicker.ColorPickerHelper;
import org.eclipse.hawkbit.ui.common.EmptyStringValidator;
import org.eclipse.hawkbit.ui.common.SoftwareModuleTypeBeanQuery;
import org.eclipse.hawkbit.ui.common.builder.LabelBuilder;
import org.eclipse.hawkbit.ui.common.builder.TextAreaBuilder;
import org.eclipse.hawkbit.ui.common.builder.TextFieldBuilder;
import org.eclipse.hawkbit.ui.layouts.CreateUpdateTypeLayout;
import org.eclipse.hawkbit.ui.utils.HawkbitCommonUtil;
import org.eclipse.hawkbit.ui.utils.SPUIDefinitions;
import org.eclipse.hawkbit.ui.utils.SPUILabelDefinitions;
import org.eclipse.hawkbit.ui.utils.UINotification;
import org.eclipse.hawkbit.ui.utils.VaadinMessageSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vaadin.addons.lazyquerycontainer.BeanQueryFactory;
import org.vaadin.spring.events.EventBus.UIEventBus;

import com.vaadin.data.Property.ValueChangeEvent;
import com.vaadin.shared.ui.colorpicker.Color;
import com.vaadin.ui.Label;
import com.vaadin.ui.OptionGroup;
import com.vaadin.ui.TextField;
import com.vaadin.ui.components.colorpicker.ColorChangeListener;
import com.vaadin.ui.themes.ValoTheme;

/**
 * Layout for the create or update software module type.
 *
 */
public class CreateUpdateSoftwareTypeLayout extends CreateUpdateTypeLayout<SoftwareModuleType> {

    private static final long serialVersionUID = 1L;
    private static final Logger LOG = LoggerFactory.getLogger(CreateUpdateSoftwareTypeLayout.class);

    private final transient SoftwareModuleTypeManagement softwareModuleTypeManagement;

    private String singleAssignStr;
    private String multiAssignStr;
    private Label singleAssign;
    private Label multiAssign;
    private OptionGroup assignOptiongroup;

    /**
     * Constructor for CreateUpdateSoftwareTypeLayout
     * 
     * @param i18n
     *            I18N
     * @param entityFactory
     *            EntityFactory
     * @param eventBus
     *            UIEventBus
     * @param permChecker
     *            SpPermissionChecker
     * @param uiNotification
     *            UINotification
     * @param softwareModuleTypeManagement
     *            management for {@link SoftwareModuleType}s
     */
    public CreateUpdateSoftwareTypeLayout(final VaadinMessageSource i18n, final EntityFactory entityFactory,
            final UIEventBus eventBus, final SpPermissionChecker permChecker, final UINotification uiNotification,
            final SoftwareModuleTypeManagement softwareModuleTypeManagement) {
        super(i18n, entityFactory, eventBus, permChecker, uiNotification);
        this.softwareModuleTypeManagement = softwareModuleTypeManagement;
    }

    @Override
    protected void addListeners() {
        super.addListeners();
        optiongroup.addValueChangeListener(this::optionValueChanged);
    }

    @Override
    protected void createRequiredComponents() {

        super.createRequiredComponents();

        singleAssignStr = i18n.getMessage("label.singleAssign.type");
        multiAssignStr = i18n.getMessage("label.multiAssign.type");
        singleAssign = new LabelBuilder().name(singleAssignStr).buildLabel();

        multiAssign = new LabelBuilder().name(multiAssignStr).buildLabel();

        tagName = createTextField("textfield.name", SPUIDefinitions.TYPE_NAME, SPUIDefinitions.NEW_SOFTWARE_TYPE_NAME);

        typeKey = createTextField("textfield.key", SPUIDefinitions.TYPE_KEY, SPUIDefinitions.NEW_SOFTWARE_TYPE_KEY);

        tagDesc = new TextAreaBuilder().caption(i18n.getMessage("textfield.description"))
                .styleName(ValoTheme.TEXTFIELD_TINY + " " + SPUIDefinitions.TYPE_DESC)
                .prompt(i18n.getMessage("textfield.description")).immediate(true)
                .id(SPUIDefinitions.NEW_SOFTWARE_TYPE_DESC).buildTextComponent();
        tagDesc.setNullRepresentation("");

        singleMultiOptionGroup();
    }

    @Override
    protected Color getColorForColorPicker() {

        final Optional<SoftwareModuleType> typeSelected = softwareModuleTypeManagement
                .getByName(tagNameComboBox.getValue().toString());
        if (typeSelected.isPresent()) {
            return typeSelected.get().getColour() != null
                    ? ColorPickerHelper.rgbToColorConverter(typeSelected.get().getColour())
                    : ColorPickerHelper.rgbToColorConverter(ColorPickerConstants.DEFAULT_COLOR);
        }
        return ColorPickerHelper.rgbToColorConverter(ColorPickerConstants.DEFAULT_COLOR);
    }

    private TextField createTextField(final String in18Key, final String styleName, final String id) {
        return new TextFieldBuilder().caption(i18n.getMessage(in18Key))
                .styleName(ValoTheme.TEXTFIELD_TINY + " " + styleName).required(true).prompt(i18n.getMessage(in18Key))
                .validator(new EmptyStringValidator(i18n)).immediate(true).id(id).buildTextComponent();
    }

    @Override
    protected void buildLayout() {

        super.buildLayout();
        ColorPickerHelper.setRgbSliderValues(colorPickerLayout);
        getFormLayout().addComponent(typeKey, 4);
        getFormLayout().addComponent(assignOptiongroup);
    }

    @Override
    protected String getWindowCaption() {
        return i18n.getMessage("caption.add.type");
    }

    /**
     * Listener for option group - Create tag/Update.
     * 
     * @param event
     *            ValueChangeEvent
     */
    @Override
    protected void optionValueChanged(final ValueChangeEvent event) {

        super.optionValueChanged(event);

        if (updateTagStr.equals(event.getProperty().getValue())) {
            assignOptiongroup.setEnabled(false);
        } else {
            assignOptiongroup.setEnabled(true);
        }
        assignOptiongroup.select(singleAssignStr);
    }

    /**
     * reset the components.
     */
    @Override
    protected void reset() {

        super.reset();
        assignOptiongroup.select(singleAssignStr);
    }

    @Override
    protected void resetTagNameField() {

        super.resetTagNameField();
        typeKey.clear();
        tagDesc.clear();
        assignOptiongroup.select(singleAssignStr);
    }

    /**
     * Select tag & set tag name & tag desc values corresponding to selected
     * tag.
     * 
     * @param targetTagSelected
     *            as the selected tag from combo
     */
    @Override
    protected void setTagDetails(final String targetTagSelected) {
        tagName.setValue(targetTagSelected);
        softwareModuleTypeManagement.getByName(targetTagSelected).ifPresent(selectedTypeTag -> {
            tagDesc.setValue(selectedTypeTag.getDescription());
            typeKey.setValue(selectedTypeTag.getKey());
            if (selectedTypeTag.getMaxAssignments() == 1) {
                assignOptiongroup.setValue(singleAssignStr);
            } else {
                assignOptiongroup.setValue(multiAssignStr);
            }
            setColorPickerComponentsColor(selectedTypeTag.getColour());
        });
    }

    private void singleMultiOptionGroup() {
        final List<String> optionValues = new ArrayList<>();
        optionValues.add(singleAssign.getValue());
        optionValues.add(multiAssign.getValue());
        assignOptionGroupByValues(optionValues);
    }

    private void assignOptionGroupByValues(final List<String> tagOptions) {
        assignOptiongroup = new OptionGroup("", tagOptions);
        assignOptiongroup.setStyleName(ValoTheme.OPTIONGROUP_SMALL);
        assignOptiongroup.addStyleName("custom-option-group");
        assignOptiongroup.setNullSelectionAllowed(false);
        assignOptiongroup.setId(SPUIDefinitions.ASSIGN_OPTION_GROUP_SOFTWARE_MODULE_TYPE_ID);
        assignOptiongroup.select(tagOptions.get(0));
    }

    @Override
    protected void createEntity() {
        createNewSWModuleType();
    }

    @Override
    protected void updateEntity(final SoftwareModuleType entity) {
        updateSWModuleType(entity);
    }

    @Override
    protected Optional<SoftwareModuleType> findEntityByKey() {
        return softwareModuleTypeManagement.getByKey(typeKey.getValue());
    }

    @Override
    protected Optional<SoftwareModuleType> findEntityByName() {
        return softwareModuleTypeManagement.getByName(tagName.getValue());
    }

    @Override
    protected String getDuplicateKeyErrorMessage(final SoftwareModuleType existingType) {
        return i18n.getMessage("message.type.key.swmodule.duplicate.check", new Object[] { existingType.getKey() });
    }

    private void createNewSWModuleType() {
        int assignNumber = 0;
        final String colorPicked = ColorPickerHelper.getColorPickedString(getColorPickerLayout().getSelPreview());
        final String typeNameValue = tagName.getValue();
        final String typeKeyValue = typeKey.getValue();
        final String typeDescValue = tagDesc.getValue();
        final String assignValue = (String) assignOptiongroup.getValue();
        if (assignValue != null && assignValue.equalsIgnoreCase(singleAssignStr)) {
            assignNumber = 1;
        } else if (assignValue != null && assignValue.equalsIgnoreCase(multiAssignStr)) {
            assignNumber = Integer.MAX_VALUE;
        }

        if (typeNameValue != null && typeKeyValue != null) {
            final SoftwareModuleType newSWType = softwareModuleTypeManagement
                    .create(entityFactory.softwareModuleType().create().key(typeKeyValue).name(typeNameValue)
                            .description(typeDescValue).colour(colorPicked).maxAssignments(assignNumber));
            uiNotification
                    .displaySuccess(i18n.getMessage("message.save.success", new Object[] { newSWType.getName() }));
            eventBus.publish(this,
                    new SoftwareModuleTypeEvent(SoftwareModuleTypeEnum.ADD_SOFTWARE_MODULE_TYPE, newSWType));
        } else {
            uiNotification.displayValidationError(i18n.getMessage("message.error.missing.typenameorkey"));
        }
    }

    private void updateSWModuleType(final SoftwareModuleType existingType) {
        softwareModuleTypeManagement
                .update(entityFactory.softwareModuleType().update(existingType.getId()).description(tagDesc.getValue())
                        .colour(ColorPickerHelper.getColorPickedString(getColorPickerLayout().getSelPreview())));
        uiNotification
                .displaySuccess(i18n.getMessage("message.update.success", new Object[] { existingType.getName() }));
        eventBus.publish(this,
                new SoftwareModuleTypeEvent(SoftwareModuleTypeEnum.UPDATE_SOFTWARE_MODULE_TYPE, existingType));
    }

    @Override
    public void addColorChangeListener(final ColorChangeListener listener) {
        LOG.debug("inside addColorChangeListener");
    }

    @Override
    public void removeColorChangeListener(final ColorChangeListener listener) {
        LOG.debug("inside removeColorChangeListener");

    }

    @Override
    public Color getColor() {
        return null;
    }

    @Override
    protected void populateTagNameCombo() {
        tagNameComboBox.setContainerDataSource(
                HawkbitCommonUtil.createLazyQueryContainer(new BeanQueryFactory<>(SoftwareModuleTypeBeanQuery.class)));
        tagNameComboBox.setItemCaptionPropertyId(SPUILabelDefinitions.VAR_NAME);
    }

}
