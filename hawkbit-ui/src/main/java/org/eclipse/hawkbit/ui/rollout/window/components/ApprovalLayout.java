/**
 * Copyright (c) 2020 Bosch.IO GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.rollout.window.components;

import org.eclipse.hawkbit.repository.model.Rollout;
import org.eclipse.hawkbit.repository.model.Rollout.ApprovalDecision;
import org.eclipse.hawkbit.ui.common.builder.TextFieldBuilder;
import org.eclipse.hawkbit.ui.common.data.proxies.ProxyRolloutApproval;
import org.eclipse.hawkbit.ui.components.SPUIComponentProvider;
import org.eclipse.hawkbit.ui.utils.UIComponentIdProvider;
import org.eclipse.hawkbit.ui.utils.VaadinMessageSource;

import com.vaadin.data.Binder;
import com.vaadin.data.ValidationException;
import com.vaadin.icons.VaadinIcons;
import com.vaadin.ui.GridLayout;
import com.vaadin.ui.RadioButtonGroup;
import com.vaadin.ui.TextField;
import com.vaadin.ui.themes.ValoTheme;

/**
 * Rollout approval layout
 */
public class ApprovalLayout extends ValidatableLayout {
    private static final String APPROVAL_CAPTION = "label.approval.decision";
    private static final String APPROVAL_BUTTON_LABEL = "button.approve";
    private static final String DENY_BUTTON_LABEL = "button.deny";

    private final VaadinMessageSource i18n;

    private final Binder<ProxyRolloutApproval> binder;

    private final RadioButtonGroup<Rollout.ApprovalDecision> approveButtonsGroup;
    private final TextField approvalRemark;

    /**
     * Constructor for ApprovalLayout
     *
     * @param i18n
     *          VaadinMessageSource
     */
    public ApprovalLayout(final VaadinMessageSource i18n) {
        this.i18n = i18n;

        this.binder = new Binder<>();

        this.approveButtonsGroup = createApproveButtonsGroupField();
        this.approvalRemark = createApprovalRemarkField();

        setValidationStatusByBinder(binder);
    }

    private RadioButtonGroup<ApprovalDecision> createApproveButtonsGroupField() {
        final RadioButtonGroup<Rollout.ApprovalDecision> approveButtonsGroupField = new RadioButtonGroup<>();
        approveButtonsGroupField.setId(UIComponentIdProvider.ROLLOUT_APPROVAL_OPTIONGROUP_ID);
        approveButtonsGroupField.addStyleName(ValoTheme.OPTIONGROUP_SMALL);
        approveButtonsGroupField.addStyleName(ValoTheme.OPTIONGROUP_HORIZONTAL);
        approveButtonsGroupField.addStyleName("custom-option-group");

        approveButtonsGroupField.setItems(Rollout.ApprovalDecision.values());

        approveButtonsGroupField.setItemCaptionGenerator(item -> {
            if (Rollout.ApprovalDecision.APPROVED == item) {
                return i18n.getMessage(APPROVAL_BUTTON_LABEL);
            } else {
                return i18n.getMessage(DENY_BUTTON_LABEL);
            }
        });
        approveButtonsGroupField.setItemIconGenerator(item -> {
            if (Rollout.ApprovalDecision.APPROVED == item) {
                return VaadinIcons.CHECK;
            } else {
                return VaadinIcons.CLOSE;
            }
        });

        binder.forField(approveButtonsGroupField).asRequired().bind(ProxyRolloutApproval::getApprovalDecision,
                ProxyRolloutApproval::setApprovalDecision);

        return approveButtonsGroupField;
    }

    private TextField createApprovalRemarkField() {
        final TextField approvalRemarkField = new TextFieldBuilder(Rollout.APPROVAL_REMARK_MAX_SIZE)
                .id(UIComponentIdProvider.ROLLOUT_APPROVAL_REMARK_FIELD_ID)
                .prompt(i18n.getMessage("label.approval.remark")).buildTextComponent();
        approvalRemarkField.setWidthFull();

        binder.forField(approvalRemarkField).bind(ProxyRolloutApproval::getApprovalRemark,
                ProxyRolloutApproval::setApprovalRemark);

        return approvalRemarkField;
    }

    /**
     * Add approval option to layout
     *
     * @param layout
     *          Rollout grid layout
     * @param lastColumnIdx
     *          Approval button position
     * @param lastRowIdx
     *          Approval remark position
     */
    public void addApprovalToLayout(final GridLayout layout, final int lastColumnIdx, final int lastRowIdx) {
        layout.addComponent(SPUIComponentProvider.generateLabel(i18n, APPROVAL_CAPTION), 0, lastRowIdx);

        layout.addComponent(approveButtonsGroup, 1, lastRowIdx);
        layout.addComponent(approvalRemark, 2, lastRowIdx, lastColumnIdx, lastRowIdx);
    }

    /**
     * Sets the rollout approval bean in binder
     *
     * @param bean
     *          Approval bean
     */
    public void setBean(final ProxyRolloutApproval bean) {
        binder.readBean(bean);
    }

    /**
     * @return Rollout approval bean
     *
     * @throws ValidationException
     *          ValidationException
     */
    public ProxyRolloutApproval getBean() throws ValidationException {
        final ProxyRolloutApproval bean = new ProxyRolloutApproval();
        binder.writeBean(bean);

        return bean;
    }
}
