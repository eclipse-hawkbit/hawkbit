/**
 * Copyright (c) 2020 Bosch.IO GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.management.tag;

import org.eclipse.hawkbit.ui.common.builder.FormComponentBuilder;
import org.eclipse.hawkbit.ui.common.data.proxies.ProxyFilterButton;
import org.eclipse.hawkbit.ui.components.ColorPickerComponent;
import org.eclipse.hawkbit.ui.utils.UIComponentIdProvider;
import org.eclipse.hawkbit.ui.utils.UINotification;
import org.eclipse.hawkbit.ui.utils.VaadinMessageSource;

import com.cronutils.utils.StringUtils;
import com.vaadin.data.Binder;
import com.vaadin.shared.ui.colorpicker.Color;
import com.vaadin.ui.TextArea;
import com.vaadin.ui.TextField;
import com.vaadin.ui.components.colorpicker.ColorUtil;

/**
 * Builder for tag window layout component
 */
public class TagWindowLayoutComponentBuilder {

    private final VaadinMessageSource i18n;
    private final UINotification uiNotification;

    /**
     * Constructor for TagWindowLayoutComponentBuilder
     *
     * @param i18n
     *            VaadinMessageSource
     * @param uiNotification
     *            UINotification
     */
    public TagWindowLayoutComponentBuilder(final VaadinMessageSource i18n, final UINotification uiNotification) {
        this.i18n = i18n;
        this.uiNotification = uiNotification;
    }

    /**
     * Create name field
     * 
     * @param binder
     *            binder the input will be bound to
     * @return input component
     */
    public TextField createNameField(final Binder<? extends ProxyFilterButton> binder) {
        return FormComponentBuilder.createNameInput(binder, i18n, UIComponentIdProvider.TAG_POPUP_NAME).getComponent();
    }

    /**
     * Create description field
     * 
     * @param binder
     *            binder the input will be bound to
     * @return input component
     */
    public TextArea createDescription(final Binder<? extends ProxyFilterButton> binder) {
        return FormComponentBuilder.createDescriptionInput(binder, i18n, UIComponentIdProvider.TAG_POPUP_DESCRIPTION)
                .getComponent();
    }

    /**
     * Create color picker component
     *
     * @param binder
     *            Filter button binder
     *
     * @return Colour picker component for tags
     */
    public ColorPickerComponent createColorPickerComponent(final Binder<? extends ProxyFilterButton> binder) {
        final ColorPickerComponent colorPickerComponent = new ColorPickerComponent(
                UIComponentIdProvider.TAG_COLOR_PREVIEW_ID, i18n.getMessage("label.choose.tag.color"));

        binder.forField(colorPickerComponent)
                .withConverter(color -> "rgb(" + color.getRed() + "," + color.getGreen() + "," + color.getBlue() + ")",
                        colorString -> {
                            try {
                                return StringUtils.isEmpty(colorString)
                                        ? ColorUtil.stringToColor(ProxyFilterButton.DEFAULT_COLOR)
                                        : ColorUtil.stringToColor(colorString);
                            } catch (final NumberFormatException e) {
                                try {
                                    return BasicColor.valueOf(colorString.trim().toUpperCase()).getColor();
                                } catch (final IllegalArgumentException ex) {
                                    uiNotification
                                            .displayValidationError(i18n.getMessage("color.not.exists", colorString));
                                    return ColorUtil.stringToColor(ProxyFilterButton.DEFAULT_COLOR);
                                }
                            }
                        })
                .bind(ProxyFilterButton::getColour, ProxyFilterButton::setColour);

        return colorPickerComponent;
    }

    private enum BasicColor {
        BLACK(Color.BLACK), BLUE(Color.BLUE), CYAN(Color.CYAN), GREEN(Color.GREEN), MAGENTA(Color.MAGENTA), RED(
                Color.RED), WHITE(Color.WHITE), YELLOW(Color.YELLOW);

        private final Color color;

        BasicColor(final Color color) {
            this.color = color;
        }

        /**
         * @return Tag color
         */
        public Color getColor() {
            return color;
        }
    }
}
