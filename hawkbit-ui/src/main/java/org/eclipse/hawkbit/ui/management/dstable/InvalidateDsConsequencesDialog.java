/**
 * Copyright (c) 2021 Bosch.IO GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.management.dstable;

import java.util.List;
import java.util.function.Consumer;

import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Link;
import com.vaadin.ui.RadioButtonGroup;
import com.vaadin.ui.themes.ValoTheme;
import org.eclipse.hawkbit.repository.model.DistributionSetInvalidation.CancelationType;
import org.eclipse.hawkbit.ui.UiProperties;
import org.eclipse.hawkbit.ui.common.CommonDialogWindow;
import org.eclipse.hawkbit.ui.common.CommonDialogWindow.ConfirmStyle;
import org.eclipse.hawkbit.ui.common.CommonDialogWindow.SaveDialogCloseListener;
import org.eclipse.hawkbit.ui.common.builder.WindowBuilder;
import org.eclipse.hawkbit.ui.common.data.proxies.ProxyDistributionSet;
import org.eclipse.hawkbit.ui.components.SPUIComponentProvider;
import org.eclipse.hawkbit.ui.decorators.SPUIButtonStyleTiny;
import org.eclipse.hawkbit.ui.utils.SPUIDefinitions;
import org.eclipse.hawkbit.ui.utils.SPUIStyleDefinitions;
import org.eclipse.hawkbit.ui.utils.UIComponentIdProvider;
import org.eclipse.hawkbit.ui.utils.UIMessageIdProvider;
import org.eclipse.hawkbit.ui.utils.VaadinMessageSource;

import com.vaadin.ui.CheckBox;
import com.vaadin.ui.Label;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.Window;

/**
 * Showing the consequences for invalidating distribution set.
 */
public class InvalidateDsConsequencesDialog {

    private final Consumer<Boolean> callback;

    private final VaadinMessageSource i18n;

    private final UiProperties uiProperties;

    private final CommonDialogWindow window;

    private final CheckBox stopRolloutsCheckBox;

    private final RadioButtonGroup<CancelationType> cancelationTypeGroup;

    /**
     * Constructor for {@link InvalidateDsConsequencesDialog}
     *
     * @param allDistributionSetsForInvalidation
     *            {@link List} of {@link ProxyDistributionSet} that are selected for
     *            invalidation
     * @param i18n
     *            {@link VaadinMessageSource}
     * @param callback
     *            callback for dialog result
     */
    public InvalidateDsConsequencesDialog(final List<ProxyDistributionSet> allDistributionSetsForInvalidation,
            final VaadinMessageSource i18n, final UiProperties uiProperties, final Consumer<Boolean> callback) {

        this.i18n = i18n;
        this.uiProperties = uiProperties;
        this.cancelationTypeGroup = createCancelationTypeOptionGroup();
        this.stopRolloutsCheckBox = createStopRolloutsCheckbox();

        final VerticalLayout content = new VerticalLayout();
        content.setSpacing(true);
        content.setMargin(true);

        final Label consequencesLabel = new Label(createConsequencesText(allDistributionSetsForInvalidation));
        consequencesLabel.setWidthFull();
        content.addComponent(consequencesLabel);

        content.addComponent(createCancelationTypeInfo());
        content.addComponent(cancelationTypeGroup);
        content.addComponent(stopRolloutsCheckBox);
        content.addComponent(createConsequencesHint());
        addValueChangeListeners();

        final WindowBuilder windowBuilder = new WindowBuilder(SPUIDefinitions.CREATE_UPDATE_WINDOW)
                .id(UIComponentIdProvider.INVALIDATE_DS_CONSEQUENCES)
                .caption(createCaption(allDistributionSetsForInvalidation)).content(content)
                .cancelButtonClickListener(e -> callback.accept(false))
                .saveDialogCloseListener(getSaveDialogCloseListener()).hideMandatoryExplanation()
                .buttonDecorator(SPUIButtonStyleTiny.class).confirmStyle(ConfirmStyle.NEXT).i18n(i18n);

        this.window = windowBuilder.buildCommonDialogWindow();
        window.setSaveButtonEnabled(true);
        window.addStyleName(SPUIStyleDefinitions.CONFIRMBOX_WINDOW_STYLE);
        this.callback = callback;

    }

    private SaveDialogCloseListener getSaveDialogCloseListener() {
        return new SaveDialogCloseListener() {
            @Override
            public void saveOrUpdate() {
                callback.accept(true);
            }

            @Override
            public boolean canWindowSaveOrUpdate() {
                return true;
            }
        };
    }

    private String createCaption(final List<ProxyDistributionSet> allDistributionSetsForInvalidation) {
        String caption = "";
        if (allDistributionSetsForInvalidation.size() == 1) {
            caption = i18n.getMessage(UIMessageIdProvider.CAPTION_INVALIDATE_DISTRIBUTIONSET_CONSEQUENCES_SINGULAR,
                    allDistributionSetsForInvalidation.get(0).getNameVersion());
        } else {
            caption = i18n.getMessage(UIMessageIdProvider.CAPTION_INVALIDATE_DISTRIBUTIONSET_CONSEQUENCES_PLURAL,
                    allDistributionSetsForInvalidation.size());
        }
        return caption;
    }

    private String createConsequencesText(final List<ProxyDistributionSet> allDistributionSetsForInvalidation) {
        if (allDistributionSetsForInvalidation.size() == 1) {
            return i18n.getMessage(UIMessageIdProvider.MESSAGE_INVALIDATE_DISTRIBUTIONSET_CONSEQUENCES_SINGULAR);
        } else {
            return i18n.getMessage(UIMessageIdProvider.MESSAGE_INVALIDATE_DISTRIBUTIONSET_CONSEQUENCES_PLURAL,
                    allDistributionSetsForInvalidation.size());
        }
    }

    private CheckBox createStopRolloutsCheckbox() {
        final CheckBox checkBox = new CheckBox();
        checkBox.setId(UIComponentIdProvider.INVALIDATE_DS_STOP_ROLLOUTS);
        checkBox.setCaption(i18n.getMessage(UIMessageIdProvider.LABEL_INVALIDATE_DS_STOP_ROLLOUTS));
        return checkBox;
    }

    private HorizontalLayout createCancelationTypeInfo() {
        final HorizontalLayout horizontalLabelInfo = new HorizontalLayout();
        final Label typeLabel = new Label(
                i18n.getMessage(UIMessageIdProvider.LABEL_INVALIDATE_DS_TYPE_OF_CANCELLATION));
        final Link linkToInvalidationHelp = SPUIComponentProvider.getHelpLink(i18n,
                uiProperties.getLinks().getDocumentation().getDistributionSetInvalidation());
        horizontalLabelInfo.addComponent(typeLabel);
        horizontalLabelInfo.addComponent(linkToInvalidationHelp);
        return horizontalLabelInfo;
    }

    private RadioButtonGroup<CancelationType> createCancelationTypeOptionGroup() {
        final RadioButtonGroup<CancelationType> cancellationTypeOptions = new RadioButtonGroup<>();
        cancellationTypeOptions.setId(UIComponentIdProvider.INVALIDATE_DS_CANCELATION_TYPE);
        cancellationTypeOptions.addStyleName(ValoTheme.OPTIONGROUP_HORIZONTAL);
        cancellationTypeOptions.setSizeUndefined();

        cancellationTypeOptions.setItems(CancelationType.values());
        cancellationTypeOptions.setItemCaptionGenerator(this::getCancelationTypeCaptionMessageId);
        cancellationTypeOptions.setItemDescriptionGenerator(this::getCancelationTypeToolTipMessageId);

        // default shall be "None"
        cancellationTypeOptions.setSelectedItem(CancelationType.NONE);

        return cancellationTypeOptions;
    }

    private Label createConsequencesHint() {
        String hint = i18n.getMessage(UIMessageIdProvider.MESSAGE_INVALIDATE_DISTRIBUTIONSET_UNREPEATABLE_HINT);
        final Label hintLabel = new Label(hint);
        hintLabel.setWidthFull();
        hintLabel.setStyleName(ValoTheme.LABEL_TINY);
        return hintLabel;
    }

    /**
     * Returns the user selection stop rollouts
     *
     * @return boolean value of checkbox stop rollouts
     */
    boolean isStopRolloutsSelected() {
        return stopRolloutsCheckBox.getValue();
    }

    CancelationType getCancelationType() {
        return cancelationTypeGroup.getValue();
    }

    private void addValueChangeListeners() {
        cancelationTypeGroup.addValueChangeListener(event -> {
            if (event.getValue() == CancelationType.NONE) {
                stopRolloutsCheckBox.setValue(false);
                stopRolloutsCheckBox.setEnabled(true);
            } else {
                stopRolloutsCheckBox.setValue(true);
                stopRolloutsCheckBox.setEnabled(false);
            }
        });
    }

    /**
     * @return confirmation window
     */
    public Window getWindow() {
        return window;
    }

    private String getCancelationTypeCaptionMessageId(CancelationType item) {
        switch (item) {
        case FORCE:
            return i18n.getMessage(UIMessageIdProvider.LABEL_CANCEL_ACTION_FORCE);
        case SOFT:
            return i18n.getMessage(UIMessageIdProvider.LABEL_CANCEL_ACTION_SOFT);
        case NONE:
            return i18n.getMessage(UIMessageIdProvider.LABEL_CANCEL_ACTION_NONE);
        default:
            return null;
        }
    }

    private String getCancelationTypeToolTipMessageId(CancelationType item) {
        switch (item) {
        case FORCE:
            return i18n.getMessage(UIMessageIdProvider.TOOLTIP_DISTRIBUTIONSET_INVALIDATE_FORCED);
        case SOFT:
            return i18n.getMessage(UIMessageIdProvider.TOOLTIP_DISTRIBUTIONSET_INVALIDATE_SOFT);
        case NONE:
            return i18n.getMessage(UIMessageIdProvider.TOOLTIP_DISTRIBUTIONSET_INVALIDATE_NONE);
        default:
            return null;
        }
    }
}
