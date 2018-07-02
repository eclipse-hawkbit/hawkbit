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
import org.eclipse.hawkbit.ui.common.builder.LabelBuilder;
import org.eclipse.hawkbit.ui.layouts.AbstractTypeLayout;
import org.eclipse.hawkbit.ui.utils.UIComponentIdProvider;
import org.eclipse.hawkbit.ui.utils.UINotification;
import org.eclipse.hawkbit.ui.utils.VaadinMessageSource;
import org.vaadin.spring.events.EventBus.UIEventBus;

import com.vaadin.ui.Label;
import com.vaadin.ui.OptionGroup;
import com.vaadin.ui.themes.ValoTheme;

/**
 * General Layout for the software module types pop-up window which is provided
 * on the Distribution and Upload view when creating, updating or deleting a
 * software module type
 *
 */
public abstract class AbstractSoftwareModuleTypeLayout extends AbstractTypeLayout<SoftwareModuleType> {

    private static final long serialVersionUID = 1L;

    private final transient SoftwareModuleTypeManagement softwareModuleTypeManagement;

    private String singleAssignStr;

    private String multiAssignStr;

    private Label singleAssign;

    private Label multiAssign;

    private OptionGroup assignOptiongroup;

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
     * @param softwareModuleTypeManagement
     *            SoftwareModuleTypeManagement
     */
    public AbstractSoftwareModuleTypeLayout(final VaadinMessageSource i18n, final EntityFactory entityFactory,
            final UIEventBus eventBus, final SpPermissionChecker permChecker, final UINotification uiNotification,
            final SoftwareModuleTypeManagement softwareModuleTypeManagement) {
        super(i18n, entityFactory, eventBus, permChecker, uiNotification);
        this.softwareModuleTypeManagement = softwareModuleTypeManagement;
    }

    @Override
    protected int getTagNameSize() {
        return SoftwareModuleType.NAME_MAX_SIZE;
    }

    @Override
    protected int getTagDescSize() {
        return SoftwareModuleType.DESCRIPTION_MAX_SIZE;
    }

    @Override
    protected int getTypeKeySize() {
        return SoftwareModuleType.KEY_MAX_SIZE;
    }

    @Override
    protected String getTagNameId() {
        return UIComponentIdProvider.NEW_SOFTWARE_TYPE_NAME;
    }

    @Override
    protected String getTagDescId() {
        return UIComponentIdProvider.NEW_SOFTWARE_TYPE_DESC;
    }

    @Override
    protected String getTypeKeyId() {
        return UIComponentIdProvider.NEW_SOFTWARE_TYPE_KEY;
    }

    @Override
    protected void createRequiredComponents() {
        super.createRequiredComponents();
        singleAssignStr = getI18n().getMessage("label.singleAssign.type");
        multiAssignStr = getI18n().getMessage("label.multiAssign.type");
        singleAssign = new LabelBuilder().name(singleAssignStr).buildLabel();
        multiAssign = new LabelBuilder().name(multiAssignStr).buildLabel();
        singleMultiOptionGroup();
    }

    @Override
    protected void buildLayout() {
        super.buildLayout();
        getFormLayout().addComponent(assignOptiongroup);
    }

    @Override
    protected Optional<SoftwareModuleType> findEntityByKey() {
        return softwareModuleTypeManagement.getByKey(getTypeKey().getValue());
    }

    @Override
    protected Optional<SoftwareModuleType> findEntityByName() {
        return softwareModuleTypeManagement.getByName(getTagName().getValue());
    }

    @Override
    protected String getDuplicateKeyErrorMessage(final SoftwareModuleType existingType) {
        return getI18n().getMessage("message.type.key.swmodule.duplicate.check", existingType.getKey());
    }

    public SoftwareModuleTypeManagement getSoftwareModuleTypeManagement() {
        return softwareModuleTypeManagement;
    }

    public String getSingleAssignStr() {
        return singleAssignStr;
    }

    public void setSingleAssignStr(final String singleAssignStr) {
        this.singleAssignStr = singleAssignStr;
    }

    public String getMultiAssignStr() {
        return multiAssignStr;
    }

    public void setMultiAssignStr(final String multiAssignStr) {
        this.multiAssignStr = multiAssignStr;
    }

    public Label getSingleAssign() {
        return singleAssign;
    }

    public void setSingleAssign(final Label singleAssign) {
        this.singleAssign = singleAssign;
    }

    public Label getMultiAssign() {
        return multiAssign;
    }

    public void setMultiAssign(final Label multiAssign) {
        this.multiAssign = multiAssign;
    }

    public OptionGroup getAssignOptiongroup() {
        return assignOptiongroup;
    }

    public void setAssignOptiongroup(final OptionGroup assignOptiongroup) {
        this.assignOptiongroup = assignOptiongroup;
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
        assignOptiongroup.setId(UIComponentIdProvider.ASSIGN_OPTION_GROUP_SOFTWARE_MODULE_TYPE_ID);
        assignOptiongroup.select(tagOptions.get(0));
    }

}
