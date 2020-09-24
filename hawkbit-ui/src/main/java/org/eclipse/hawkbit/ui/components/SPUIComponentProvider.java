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
import org.eclipse.hawkbit.ui.decorators.SPUIButtonDecorator;
import org.eclipse.hawkbit.ui.rollout.ProxyFontIcon;
import org.eclipse.hawkbit.ui.utils.VaadinMessageSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.data.provider.DataProvider;
import com.vaadin.data.provider.ListDataProvider;
import com.vaadin.icons.VaadinIcons;
import com.vaadin.server.ExternalResource;
import com.vaadin.server.Resource;
import com.vaadin.shared.ui.ContentMode;
import com.vaadin.ui.Button;
import com.vaadin.ui.ComboBox;
import com.vaadin.ui.ItemCaptionGenerator;
import com.vaadin.ui.Label;
import com.vaadin.ui.Link;
import com.vaadin.ui.themes.ValoTheme;

/**
 * Util class to get the Vaadin UI components for the SP-OS main UI. Factory
 * Approach to create necessary UI component which are decorated Aspect of fine
 * tuning the component or extending the component is separated.
 *
 */
public final class SPUIComponentProvider {
    private static final Logger LOG = LoggerFactory.getLogger(SPUIComponentProvider.class);

    /**
     * Prevent Instance creation as utility class.
     */
    private SPUIComponentProvider() {

    }

    /**
     * Get Button - Factory Approach for decoration.
     *
     * @param id
     *            as string
     * @param buttonName
     *            as string
     * @param buttonDesc
     *            as string
     * @param style
     *            string as string
     * @param setStyle
     *            string as boolean
     * @param icon
     *            as image
     * @param buttonDecoratorclassName
     *            as decorator
     * @return Button as UI
     */
    public static Button getButton(final String id, final String buttonName, final String buttonDesc,
            final String style, final boolean setStyle, final Resource icon,
            final Class<? extends SPUIButtonDecorator> buttonDecoratorclassName) {
        Button button = null;
        SPUIButtonDecorator buttonDecorator = null;
        try {
            // Create instance
            buttonDecorator = buttonDecoratorclassName.newInstance();
            // Decorate button
            button = buttonDecorator.decorate(new SPUIButton(id, buttonName, buttonDesc), style, setStyle, icon);
        } catch (final InstantiationException exception) {
            LOG.error("Error occured while creating Button decorator-" + buttonName, exception);
        } catch (final IllegalAccessException exception) {
            LOG.error("Error occured while acessing Button decorator-" + buttonName, exception);
        }
        return button;
    }

    /**
     * Method to create a link.
     *
     * @param id
     *            of the link
     * @param name
     *            of the link
     * @param resource
     *            path of the link
     * @param icon
     *            of the link
     * @param targetOpen
     *            specify how the link should be open (f. e. new windows =
     *            _blank)
     * @param style
     *            chosen style of the link. Might be {@code null} if no style
     *            should be used
     * @return a link UI component
     */
    public static Link getLink(final String id, final String name, final String resource, final Resource icon,
            final String targetOpen, final String style) {

        final Link link = new Link(name, new ExternalResource(resource));
        link.setId(id);
        link.setIcon(icon);
        link.setDescription(name);

        link.setTargetName(targetOpen);
        if (style != null) {
            link.setStyleName(style);
        }

        return link;

    }

    /**
     * Generates help/documentation links from within management UI.
     *
     * @param i18n
     *            the i18n
     * @param uri
     *            to documentation site
     *
     * @return generated link
     */
    public static Link getHelpLink(final VaadinMessageSource i18n, final String uri) {

        final Link link = new Link("", new ExternalResource(uri));
        link.setTargetName("_blank");
        link.setIcon(VaadinIcons.QUESTION_CIRCLE);
        link.setDescription(i18n.getMessage("tooltip.documentation.link"));
        return link;

    }

    /**
     * Generate a label containing an icon
     * 
     * @param fontIcon
     *            icon to display in the label
     * @param id
     *            label ID
     * @return the label
     */
    public static Label getLabelIcon(final ProxyFontIcon fontIcon, final String id) {
        if (fontIcon == null) {
            return new Label("");
        }

        final Label labelIcon = new Label(fontIcon.getHtml(), ContentMode.HTML);
        labelIcon.setId(id);
        labelIcon.setDescription(fontIcon.getDescription());
        labelIcon.addStyleName("small");
        labelIcon.addStyleName("font-icon");
        labelIcon.addStyleName(fontIcon.getStyle());

        return labelIcon;
    }

    /**
     * Generate a label
     * 
     * @param i18n
     *            message source for internationalization
     * @param key
     *            to generate the internationalized name from
     * @return the label
     */
    public static Label generateLabel(final VaadinMessageSource i18n, final String key) {
        return new LabelBuilder().name(i18n.getMessage(key)).buildLabel();
    }

    /**
     * Generate a caption label
     * 
     * @param i18n
     *            message source for internationalization
     * @param key
     *            to generate the internationalized name from
     * @return the caption label
     */
    public static Label generateCaptionLabel(final VaadinMessageSource i18n, final String key) {
        return new LabelBuilder().name(i18n.getMessage(key)).buildCaptionLabel();
    }

    /**
     * Create generic combobox with backend data provider.
     * 
     * @param id
     *            id of the combobox
     * @param caption
     *            caption of the combobox
     * @param description
     *            description of the combobox
     * @param placeholder
     *            placeholder of the combobox
     * @param isEmptySelectionAllowed
     *            flag for empty selection enabled
     * @param itemCaptionGenerator
     *            generator fot item captions
     * @param dataProvider
     *            provides data/items
     * 
     * @return generic ComboBox
     */
    public static <T> ComboBox<T> getComboBox(final String id, final String caption, final String description,
            final String placeholder, final boolean isEmptySelectionAllowed,
            final ItemCaptionGenerator<T> itemCaptionGenerator, final DataProvider<T, String> dataProvider) {
        final ComboBox<T> comboBox = getComboBox(id, caption, description, placeholder, isEmptySelectionAllowed,
                itemCaptionGenerator);
        comboBox.setDataProvider(dataProvider);

        return comboBox;
    }

    private static <T> ComboBox<T> getComboBox(final String id, final String caption, final String description,
            final String placeholder, final boolean isEmptySelectionAllowed,
            final ItemCaptionGenerator<T> itemCaptionGenerator) {
        final ComboBox<T> comboBox = new ComboBox<>();
        comboBox.setId(id);
        comboBox.setCaption(caption);
        comboBox.setDescription(description);
        comboBox.setPlaceholder(placeholder);
        comboBox.addStyleName(ValoTheme.COMBOBOX_SMALL);
        comboBox.setEmptySelectionAllowed(isEmptySelectionAllowed);

        comboBox.setItemCaptionGenerator(itemCaptionGenerator);

        return comboBox;
    }

    /**
     * Create generic combobox with list in-memory data provider.
     * 
     * @param id
     *            id of the combobox
     * @param caption
     *            caption of the combobox
     * @param description
     *            description of the combobox
     * @param placeholder
     *            placeholder of the combobox
     * @param isEmptySelectionAllowed
     *            flag for empty selection enabled
     * @param itemCaptionGenerator
     *            generator fot item captions
     * @param dataProvider
     *            provides data/items
     * 
     * @return generic ComboBox
     */
    public static <T> ComboBox<T> getComboBox(final String id, final String caption, final String description,
            final String placeholder, final boolean isEmptySelectionAllowed,
            final ItemCaptionGenerator<T> itemCaptionGenerator, final ListDataProvider<T> dataProvider) {
        final ComboBox<T> comboBox = getComboBox(id, caption, description, placeholder, isEmptySelectionAllowed,
                itemCaptionGenerator);
        comboBox.setDataProvider(dataProvider);

        return comboBox;
    }
}
