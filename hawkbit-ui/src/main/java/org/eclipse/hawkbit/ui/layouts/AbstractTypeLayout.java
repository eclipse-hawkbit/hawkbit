/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.layouts;

import java.util.Optional;

import org.eclipse.hawkbit.repository.EntityFactory;
import org.eclipse.hawkbit.repository.model.NamedEntity;
import org.eclipse.hawkbit.ui.SpPermissionChecker;
import org.eclipse.hawkbit.ui.colorpicker.ColorPickerConstants;
import org.eclipse.hawkbit.ui.colorpicker.ColorPickerHelper;
import org.eclipse.hawkbit.ui.common.builder.TextFieldBuilder;
import org.eclipse.hawkbit.ui.utils.HawkbitCommonUtil;
import org.eclipse.hawkbit.ui.utils.SPUIDefinitions;
import org.eclipse.hawkbit.ui.utils.UINotification;
import org.eclipse.hawkbit.ui.utils.VaadinMessageSource;
import org.vaadin.spring.events.EventBus.UIEventBus;

import com.vaadin.server.Page;
import com.vaadin.shared.ui.colorpicker.Color;
import com.vaadin.ui.TextArea;
import com.vaadin.ui.TextField;
import com.vaadin.ui.components.colorpicker.ColorChangeEvent;
import com.vaadin.ui.components.colorpicker.ColorSelector;
import com.vaadin.ui.themes.ValoTheme;

/**
 * Superclass defining common properties and methods for creating types.
 * 
 * @param <E>
 *            entity
 */
public abstract class AbstractTypeLayout<E extends NamedEntity> extends AbstractTagLayout<E> {

    private static final long serialVersionUID = 1L;

    private TextField typeKey;

    private static final String TYPE_NAME_DYNAMIC_STYLE = "new-tag-name";

    private static final String TYPE_DESC_DYNAMIC_STYLE = "new-tag-desc";

    /**
     * Constructor
     * 
     * @param i18n
     *            VaadinMessageSource
     * @param entityFactory
     *            EntityFactory
     * @param eventBus
     *            UIEventBus
     * @param permChecker
     *            SpPermissionChecker
     * @param uiNotification
     *            UINotification
     */
    public AbstractTypeLayout(final VaadinMessageSource i18n, final EntityFactory entityFactory,
            final UIEventBus eventBus, final SpPermissionChecker permChecker, final UINotification uiNotification) {
        super(i18n, entityFactory, eventBus, permChecker, uiNotification);
    }

    @Override
    public void setColor(final Color color) {
        if (color == null) {
            return;
        }
        getColorPickerLayout().setSelectedColor(color);
        getColorPickerLayout().getSelPreview().setColor(getColorPickerLayout().getSelectedColor());
        final String colorPickedPreview = getColorPickerLayout().getSelPreview().getColor().getCSS();
        if (getTagName().isEnabled() && null != getColorPickerLayout().getColorSelect()) {
            createDynamicStyleForComponents(getTagName(), typeKey, getTagDesc(), colorPickedPreview);
            getColorPickerLayout().getColorSelect().setColor(getColorPickerLayout().getSelPreview().getColor());
        }
    }

    @Override
    public void colorChanged(final ColorChangeEvent event) {
        setColor(event.getColor());
        for (final ColorSelector select : getColorPickerLayout().getSelectors()) {
            if (!event.getSource().equals(select) && select.equals(this)
                    && !select.getColor().equals(getColorPickerLayout().getSelectedColor())) {
                select.setColor(getColorPickerLayout().getSelectedColor());
            }
        }
        ColorPickerHelper.setRgbSliderValues(getColorPickerLayout());
        getPreviewButtonColor(event.getColor().getCSS());
        createDynamicStyleForComponents(getTagName(), typeKey, getTagDesc(), event.getColor().getCSS());
    }

    @Override
    protected void buildLayout() {
        super.buildLayout();
        ColorPickerHelper.setRgbSliderValues(getColorPickerLayout());
        getFormLayout().addComponent(typeKey);
    }

    @Override
    protected void createRequiredComponents() {
        super.createRequiredComponents();
        typeKey = new TextFieldBuilder(getTypeKeySize()).id(getTypeKeyId())
                .caption(getI18n().getMessage("textfield.key"))
                .styleName(ValoTheme.TEXTFIELD_TINY + " " + SPUIDefinitions.DIST_SET_TYPE_KEY).required(true, getI18n())
                .prompt(getI18n().getMessage("textfield.key")).immediate(true).buildTextComponent();
        getColorLabel().setValue(getI18n().getMessage("label.choose.type.color"));
    }

    protected abstract String getTypeKeyId();

    protected abstract int getTypeKeySize();

    @Override
    protected void setColorToComponents(final Color newColor) {
        super.setColorToComponents(newColor);
        createDynamicStyleForComponents(getTagName(), typeKey, getTagDesc(), newColor.getCSS());
    }

    @Override
    protected void restoreComponentStyles() {
        super.restoreComponentStyles();
        typeKey.removeStyleName(TYPE_NAME_DYNAMIC_STYLE);
        typeKey.addStyleName(SPUIDefinitions.TYPE_KEY);
    }

    protected void setColorPickerComponentsColor(final String color) {
        if (color == null) {
            getColorPickerLayout()
                    .setSelectedColor(ColorPickerHelper.rgbToColorConverter(ColorPickerConstants.DEFAULT_COLOR));
            getColorPickerLayout().getSelPreview().setColor(getColorPickerLayout().getSelectedColor());
            getColorPickerLayout().getColorSelect().setColor(getColorPickerLayout().getSelectedColor());
            createDynamicStyleForComponents(getTagName(), typeKey, getTagDesc(), ColorPickerConstants.DEFAULT_COLOR);
            getPreviewButtonColor(ColorPickerConstants.DEFAULT_COLOR);
        } else {
            getColorPickerLayout().setSelectedColor(ColorPickerHelper.rgbToColorConverter(color));
            getColorPickerLayout().getSelPreview().setColor(getColorPickerLayout().getSelectedColor());
            getColorPickerLayout().getColorSelect().setColor(getColorPickerLayout().getSelectedColor());
            createDynamicStyleForComponents(getTagName(), typeKey, getTagDesc(), color);
            getPreviewButtonColor(color);
        }
    }

    @Override
    protected boolean isDuplicate() {
        return isDuplicateByKey() || super.isDuplicate();
    }

    @Override
    protected void resetFields() {
        super.resetFields();
        typeKey.clear();
        typeKey.setEnabled(true);
    }

    protected abstract Optional<E> findEntityByKey();

    protected abstract String getDuplicateKeyErrorMessage(E existingType);

    private static void createDynamicStyleForComponents(final TextField tagName, final TextField typeKey,
            final TextArea typeDesc, final String typeTagColor) {
        tagName.removeStyleName(SPUIDefinitions.TYPE_NAME);
        typeKey.removeStyleName(SPUIDefinitions.TYPE_KEY);
        typeDesc.removeStyleName(SPUIDefinitions.TYPE_DESC);
        getDynamicStyles(typeTagColor);
        tagName.addStyleName(TYPE_NAME_DYNAMIC_STYLE);
        typeKey.addStyleName(TYPE_NAME_DYNAMIC_STYLE);
        typeDesc.addStyleName(TYPE_DESC_DYNAMIC_STYLE);
    }

    private static void getDynamicStyles(final String colorPickedPreview) {
        Page.getCurrent().getJavaScript()
                .execute(HawkbitCommonUtil.changeToNewSelectedPreviewColor(colorPickedPreview));
    }

    private boolean isDuplicateByKey() {
        final Optional<E> existingType = findEntityByKey();
        existingType.ifPresent(type -> getUiNotification().displayValidationError(getDuplicateKeyErrorMessage(type)));
        return existingType.isPresent();
    }

    public TextField getTypeKey() {
        return typeKey;
    }

}
