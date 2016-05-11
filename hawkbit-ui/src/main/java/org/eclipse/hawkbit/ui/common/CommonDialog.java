package org.eclipse.hawkbit.ui.common;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.hawkbit.ui.components.SPUIComponentProvider;
import org.eclipse.hawkbit.ui.decorators.SPUIButtonStyleSmallNoBorder;
import org.eclipse.hawkbit.ui.utils.SPUIComponetIdProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.server.FontAwesome;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.FormLayout;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Link;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.Window;

public class CommonDialog extends Window {

    private static final long serialVersionUID = -1321949234316858703L;

    private static final Logger LOGGER = LoggerFactory.getLogger(CommonDialog.class);

    private final VerticalLayout mainLayout = new VerticalLayout();

    private FormLayout formLayout = new FormLayout();

    private Button saveButton;

    private Button cancelButton;

    public CommonDialog() {

        mainLayout.addComponent(formLayout);
        mainLayout.addComponent(createActionButtonsLayout());
        setContent(mainLayout);

        setResizable(false);
        center();
    }

    public CommonDialog(final String title, final FormLayout form, final String helpLink) {

        if (formLayout != null) {
            formLayout = form;
        }
        formLayout.setSpacing(true);
        formLayout.setMargin(true);

        if (StringUtils.isNotEmpty(helpLink)) {
            mainLayout.addComponent(createLinkToHelp(helpLink));
        }
        mainLayout.addComponent(formLayout);
        mainLayout.addComponent(createActionButtonsLayout());

        setCaption(title);
        setContent(mainLayout);
        setResizable(false);
        center();
    }

    private HorizontalLayout createActionButtonsLayout() {

        final HorizontalLayout hlayout = new HorizontalLayout();
        hlayout.setSpacing(true);

        saveButton = SPUIComponentProvider.getButton(SPUIComponetIdProvider.SYSTEM_CONFIGURATION_SAVE, "", "", "", true,
                FontAwesome.SAVE, SPUIButtonStyleSmallNoBorder.class);
        // saveButton.setDescription(i18n.get("configuration.savebutton.tooltip"));
        hlayout.addComponent(saveButton);

        cancelButton = SPUIComponentProvider.getButton(SPUIComponetIdProvider.SYSTEM_CONFIGURATION_CANCEL, "", "", "",
                true, FontAwesome.UNDO, SPUIButtonStyleSmallNoBorder.class);
        // cancelButton.setDescription(i18n.get("configuration.cancellbutton.tooltip"));
        hlayout.addComponent(cancelButton);

        return hlayout;
    }

    public void setSaveButtonClickListener(final ClickListener clickListener) {
        saveButton.addClickListener(clickListener);
    }

    public void setCancelButtonClickListener(final ClickListener clickListener) {
        cancelButton.addClickListener(clickListener);
    }

    private Link createLinkToHelp(final String link) {
        return SPUIComponentProvider.getHelpLink(link);
    }

    public FormLayout getFormLayout() {
        return formLayout;
    }

    public void setFormLayout(final FormLayout formLayout) {
        this.formLayout = formLayout;
    }

}
