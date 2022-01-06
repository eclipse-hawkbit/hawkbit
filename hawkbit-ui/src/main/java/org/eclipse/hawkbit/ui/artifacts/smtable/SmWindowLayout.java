/**
 * Copyright (c) 2020 Bosch.IO GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.artifacts.smtable;

import org.eclipse.hawkbit.repository.SoftwareModuleTypeManagement;
import org.eclipse.hawkbit.ui.common.AbstractEntityWindowLayout;
import org.eclipse.hawkbit.ui.common.data.mappers.TypeToTypeInfoMapper;
import org.eclipse.hawkbit.ui.common.data.providers.SoftwareModuleTypeDataProvider;
import org.eclipse.hawkbit.ui.common.data.proxies.ProxySoftwareModule;
import org.eclipse.hawkbit.ui.common.data.proxies.ProxyTypeInfo;
import org.eclipse.hawkbit.ui.utils.VaadinMessageSource;

import com.vaadin.ui.CheckBox;
import com.vaadin.ui.ComboBox;
import com.vaadin.ui.ComponentContainer;
import com.vaadin.ui.FormLayout;
import com.vaadin.ui.TextArea;
import com.vaadin.ui.TextField;

/**
 * Target add/update window layout.
 */
public class SmWindowLayout extends AbstractEntityWindowLayout<ProxySoftwareModule> {
    private final SmWindowLayoutComponentBuilder smComponentBuilder;

    private final ComboBox<ProxyTypeInfo> smTypeSelect;
    private final TextField smName;
    private final TextField smVersion;
    private final TextField smVendor;
    private final TextArea smDescription;
    private final CheckBox artifactEncryption;

    /**
     * Constructor for AbstractTagWindowLayout
     * 
     * @param i18n
     *            VaadinMessageSource
     * @param smTypeManagement
     *            SoftwareModuleTypeManagement
     */
    public SmWindowLayout(final VaadinMessageSource i18n, final SoftwareModuleTypeManagement smTypeManagement) {
        super();

        final SoftwareModuleTypeDataProvider<ProxyTypeInfo> smTypeDataProvider = new SoftwareModuleTypeDataProvider<>(
                smTypeManagement, new TypeToTypeInfoMapper<>());
        this.smComponentBuilder = new SmWindowLayoutComponentBuilder(i18n, smTypeDataProvider);

        this.smTypeSelect = smComponentBuilder.createSoftwareModuleTypeCombo(binder);
        this.smName = smComponentBuilder.createNameField(binder);
        this.smVersion = smComponentBuilder.createVersionField(binder);
        this.smVendor = smComponentBuilder.createVendorField(binder);
        this.smDescription = smComponentBuilder.createDescription(binder);
        this.artifactEncryption = smComponentBuilder.createArtifactEncryptionCheck(binder);
    }

    /**
     * @return software module window layout
     */
    @Override
    public ComponentContainer getRootComponent() {
        final FormLayout smWindowLayout = new FormLayout();

        smWindowLayout.setSpacing(true);
        smWindowLayout.setMargin(true);
        smWindowLayout.setSizeUndefined();

        smWindowLayout.addComponent(smTypeSelect);

        smWindowLayout.addComponent(smName);
        smName.focus();

        smWindowLayout.addComponent(smVersion);

        smWindowLayout.addComponent(smVendor);

        smWindowLayout.addComponent(smDescription);

        smWindowLayout.addComponent(artifactEncryption);

        return smWindowLayout;
    }

    /**
     * Disable the software module type
     */
    public void disableSmTypeSelect() {
        smTypeSelect.setEnabled(false);
    }

    /**
     * Disable the software module name
     */
    public void disableNameField() {
        smName.setEnabled(false);
    }

    /**
     * Disable the software module version
     */
    public void disableVersionField() {
        smVersion.setEnabled(false);
    }

    /**
     * Disable the software module artifact encryption
     */
    public void disableEncryptionField() {
        artifactEncryption.setEnabled(false);
    }
}
