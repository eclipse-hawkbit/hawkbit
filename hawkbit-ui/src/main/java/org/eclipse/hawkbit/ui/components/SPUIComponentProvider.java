/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.components;

import java.util.Arrays;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.hawkbit.repository.model.BaseEntity;
import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.ui.common.CommonDialogWindow;
import org.eclipse.hawkbit.ui.common.UserDetailsFormatter;
import org.eclipse.hawkbit.ui.decorators.SPUIButtonDecorator;
import org.eclipse.hawkbit.ui.decorators.SPUIComboBoxDecorator;
import org.eclipse.hawkbit.ui.decorators.SPUIHeaderLayoutDecorator;
import org.eclipse.hawkbit.ui.decorators.SPUILabelDecorator;
import org.eclipse.hawkbit.ui.decorators.SPUITextAreaDecorator;
import org.eclipse.hawkbit.ui.decorators.SPUITextFieldDecorator;
import org.eclipse.hawkbit.ui.decorators.SPUIWindowDecorator;
import org.eclipse.hawkbit.ui.utils.I18N;
import org.eclipse.hawkbit.ui.utils.SPUIDefinitions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.server.ExternalResource;
import com.vaadin.server.FontAwesome;
import com.vaadin.server.Resource;
import com.vaadin.shared.ui.label.ContentMode;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.CheckBox;
import com.vaadin.ui.ComboBox;
import com.vaadin.ui.Component;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.Link;
import com.vaadin.ui.Panel;
import com.vaadin.ui.TabSheet;
import com.vaadin.ui.TextArea;
import com.vaadin.ui.TextField;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.Window;
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
     * @param className
     * @return
     */
    public static HorizontalLayout getHorizontalLayout(final Class<? extends HorizontalLayout> className) {
        HorizontalLayout hLayout = null;
        try {

            hLayout = className.newInstance();
        } catch (final InstantiationException exception) {
            LOG.error("Error occured while creating HorizontalLayout-" + className, exception);
        } catch (final IllegalAccessException exception) {
            LOG.error("Error occured while acessing HorizontalLayout-" + className, exception);
        }

        return hLayout;
    }

    /**
     * Get HorizontalLayout UI component.
     *
     * @param className
     *            as Layout
     * @return HorizontalLayout as UI
     */
    public static HorizontalLayout getHorizontalLayout() {
        HorizontalLayout hLayout = null;
        try {
            hLayout = HorizontalLayout.class.newInstance();
        } catch (final InstantiationException exception) {
            LOG.error("Error occured while creating HorizontalLayout-", exception);
        } catch (final IllegalAccessException exception) {
            LOG.error("Error occured while acessing HorizontalLayout-", exception);
        }

        return hLayout;
    }

    /**
     * @param tableHeaderLayoutDecorator
     * @return
     */
    public static HorizontalLayout getHeaderLayout(
            final Class<? extends SPUIHeaderLayoutDecorator> tableHeaderLayoutDecorator) {
        // Do we really need this???
        HorizontalLayout hLayout = getHorizontalLayout(new SPUIHorizontalLayout().getUiHorizontalLayout().getClass());

        if (tableHeaderLayoutDecorator == null) {
            return hLayout;
        }

        try {
            final SPUIHeaderLayoutDecorator layoutDecorator = tableHeaderLayoutDecorator.newInstance();
            hLayout = layoutDecorator.decorate(hLayout);

        } catch (final InstantiationException | IllegalAccessException exception) {
            LOG.error("Error occured while creating horizontal decorator " + SPUIHeaderLayoutDecorator.class,
                    exception);
        }

        return hLayout;
    }

    /**
     * Get Label UI component.
     *
     * @param name
     *            label caption
     * @param type
     *            string simple|Confirm|Message
     * @return Label
     */
    public static Label getLabel(final String name, final String type) {
        return SPUILabelDecorator.getDeocratedLabel(name, type);
    }

    /**
     * Get window component.
     * 
     * @param caption
     *            window caption
     * @param id
     *            window id
     * @param type
     *            type of window
     * @return Window
     */
    public static CommonDialogWindow getWindow(final String caption, final String id, final String type,
            final Component content, final ClickListener saveButtonClickListener,
            final ClickListener cancelButtonClickListener) {
        return SPUIWindowDecorator.getDeocratedWindow(caption, id, type, content, saveButtonClickListener,
                cancelButtonClickListener);
    }

    /**
     * Get window component.
     * 
     * @param caption
     *            window caption
     * @param id
     *            window id
     * @param type
     *            type of window
     * @return Window
     */
    public static Window getWindow(final String caption, final String id, final String type) {
        return SPUIWindowDecorator.getDeocratedWindow(caption, id, type);
    }

    /**
     * Get Label UI component.
     * 
     * @param caption
     *            set the caption of the textfield
     * @param style
     *            set style
     * @param styleName
     *            add style
     * @param required
     *            to set field as mandatory
     * @param data
     *            component data
     * @param prompt
     *            prompt user for input
     * @param immediate
     *            set component's immediate mode specified mode
     * @param maxLengthAllowed
     *            maximum characters allowed
     * @return TextField text field
     */
    public static TextField getTextField(final String caption, final String style, final String styleName,
            final boolean required, final String data, final String prompt, final boolean immediate,
            final int maxLengthAllowed) {
        return SPUITextFieldDecorator.decorate(caption, style, styleName, required, data, prompt, immediate,
                maxLengthAllowed);
    }

    /**
     * Get Label UI component. *
     * 
     * @param style
     *            set style
     * @param styleName
     *            add style
     * @param required
     *            to set field as mandatory
     * @param data
     *            component data
     * @param promt
     *            prompt user for input
     * @param maxLength
     *            maximum characters allowed
     * @return TextArea text area
     */
    public static TextArea getTextArea(final String caption, final String style, final String styleName,
            final boolean required, final String data, final String promt, final int maxLength) {
        return SPUITextAreaDecorator.decorate(caption, style, styleName, required, data, promt, maxLength);
    }

    /**
     * Get Label UI component.
     * 
     * @param caption
     *            caption of the combo box
     * @param height
     *            combo box height
     * @param width
     *            combo box width
     * @param style
     *            combo style to add
     * @param styleName
     *            combo style to set
     * @param required
     *            signifies if combo is mandatory
     * @param data
     *            combo box data
     * @param promt
     *            input prompt
     * @return ComboBox
     */
    public static ComboBox getComboBox(final String caption, final String height, final String width,
            final String style, final String styleName, final boolean required, final String data, final String promt) {
        return SPUIComboBoxDecorator.decorate(caption, height, width, style, styleName, required, data, promt);
    }

    /**
     * Get Label UI component.
     *
     * @param caption
     *            as caption
     * @param style
     *            combo style to add
     * @param styleName
     *            combo style to set
     * @param required
     *            signifies if combo is mandatory
     * @param data
     *            combo box data
     * @return ComboBox
     */
    public static CheckBox getCheckBox(final String caption, final String style, final String styleName,
            final boolean required, final String data) {
        return new SPUICheckBox(caption, style, styleName, required, data);
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
     * Get the style required.
     * 
     * @return String
     */
    public static String getPinButtonStyle() {
        final StringBuilder pinStyle = new StringBuilder(ValoTheme.BUTTON_BORDERLESS_COLORED);
        pinStyle.append(' ');
        pinStyle.append(ValoTheme.BUTTON_SMALL);
        pinStyle.append(' ');
        pinStyle.append(ValoTheme.BUTTON_ICON_ONLY);
        pinStyle.append(' ');
        pinStyle.append("pin-icon");
        return pinStyle.toString();
    }

    /**
     * Get DistributionSet Info Panel.
     * 
     * @param distributionSet
     *            as DistributionSet
     * @param caption
     *            as string
     * @param style1
     *            as string
     * @param style2
     *            as string
     * @return Panel
     */
    public static Panel getDistributionSetInfo(final DistributionSet distributionSet, final String caption,
            final String style1, final String style2) {
        return new DistributionSetInfoPanel(distributionSet, caption, style1, style2);
    }

    /**
     * Method to CreateName value labels.
     * 
     * @param label
     *            as string
     * @param values
     *            as string
     * @return Label
     */
    public static Label createNameValueLabel(final String label, final String... values) {
        final String valueStr = StringUtils.join(Arrays.asList(values), " ");
        final Label nameValueLabel = new Label(getBoldHTMLText(label) + valueStr, ContentMode.HTML);
        nameValueLabel.setSizeFull();
        nameValueLabel.addStyleName(SPUIDefinitions.TEXT_STYLE);
        nameValueLabel.addStyleName("label-style");
        return nameValueLabel;
    }

    private static Label createUsernameLabel(final String label, final String username) {
        String loadAndFormatUsername = StringUtils.EMPTY;
        if (!StringUtils.isEmpty(username)) {
            loadAndFormatUsername = UserDetailsFormatter.loadAndFormatUsername(username);
        }

        final Label nameValueLabel = new Label(getBoldHTMLText(label) + loadAndFormatUsername, ContentMode.HTML);
        nameValueLabel.setSizeFull();
        nameValueLabel.addStyleName(SPUIDefinitions.TEXT_STYLE);
        nameValueLabel.addStyleName("label-style");
        nameValueLabel.setDescription(loadAndFormatUsername);
        return nameValueLabel;
    }

    /**
     * Create label which represents the {@link BaseEntity#getCreatedBy()} by
     * user name
     * 
     * @param i18n
     *            the i18n
     * @param baseEntity
     *            the entity
     * @return the label
     */
    public static Label createCreatedByLabel(final I18N i18n, final BaseEntity baseEntity) {
        return createUsernameLabel(i18n.get("label.created.by"),
                baseEntity == null ? StringUtils.EMPTY : baseEntity.getCreatedBy());
    }

    /**
     * Create label which represents the
     * {@link BaseEntity#getLastModifiedBy()()} by user name
     * 
     * @param i18n
     *            the i18n
     * @param baseEntity
     *            the entity
     * @return the label
     */
    public static Label createLastModifiedByLabel(final I18N i18n, final BaseEntity baseEntity) {
        return createUsernameLabel(i18n.get("label.modified.by"),
                baseEntity == null ? StringUtils.EMPTY : baseEntity.getLastModifiedBy());
    }

    /**
     * Get Bold Text.
     * 
     * @param text
     *            as String
     * @return String as bold
     */
    public static String getBoldHTMLText(final String text) {
        return "<b>" + text + "</b>";
    }

    /**
     * Get the layout for Target:Controller Attributes.
     * 
     * @param controllerAttibs
     *            as Map
     * @return VerticalLayout
     */
    public static VerticalLayout getControllerAttributePanel(final Map<String, String> controllerAttibs) {
        return new SPTargetAttributesLayout(controllerAttibs).getTargetAttributesLayout();
    }

    /**
     * Get Tabsheet.
     * 
     * @return SPUITabSheet
     */
    public static TabSheet getDetailsTabSheet() {
        return new SPUITabSheet();
    }

    /**
     * Layout of tabs in detail tabsheet.
     * 
     * @return VerticalLayout
     */
    public static VerticalLayout getDetailTabLayout() {
        final VerticalLayout layout = new VerticalLayout();
        layout.setSpacing(true);
        layout.setMargin(true);
        layout.setImmediate(true);
        return layout;
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
     *            chosen style of the link
     * @param setStyle
     *            set true if the style should be used
     * @return a link UI component
     */
    public static Link getLink(final String id, final String name, final String resource, final FontAwesome icon,
            final String targetOpen, final String style, final boolean setStyle) {

        final Link link = new Link(name, new ExternalResource(resource));
        link.setId(id);
        link.setIcon(icon);

        link.setTargetName(targetOpen);
        if (setStyle) {
            link.setStyleName(style);
        }

        return link;

    }

    /**
     * Generates help/documentation links from within management UI.
     * 
     * @param uri
     *            to documentation site
     * 
     * @return generated link
     */
    public static Link getHelpLink(final String uri) {

        final Link link = new Link("", new ExternalResource(uri));
        link.setTargetName("_blank");
        link.setIcon(FontAwesome.QUESTION_CIRCLE);
        link.setDescription("Documentation");
        return link;

    }
}
