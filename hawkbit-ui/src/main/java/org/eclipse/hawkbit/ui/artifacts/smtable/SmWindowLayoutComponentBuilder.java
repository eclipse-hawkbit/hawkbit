/**
 * Copyright (c) 2020 Bosch.IO GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.artifacts.smtable;

import org.eclipse.hawkbit.repository.model.SoftwareModule;
import org.eclipse.hawkbit.ui.common.builder.FormComponentBuilder;
import org.eclipse.hawkbit.ui.common.builder.TextFieldBuilder;
import org.eclipse.hawkbit.ui.common.data.providers.SoftwareModuleTypeDataProvider;
import org.eclipse.hawkbit.ui.common.data.proxies.ProxySoftwareModule;
import org.eclipse.hawkbit.ui.common.data.proxies.ProxyTypeInfo;
import org.eclipse.hawkbit.ui.utils.UIComponentIdProvider;
import org.eclipse.hawkbit.ui.utils.VaadinMessageSource;

import com.vaadin.data.Binder;
import com.vaadin.ui.CheckBox;
import com.vaadin.ui.ComboBox;
import com.vaadin.ui.TextArea;
import com.vaadin.ui.TextField;

/**
 * Builder for software module window layout
 */
public class SmWindowLayoutComponentBuilder {

    public static final String TEXTFIELD_VENDOR = "textfield.vendor";
    public static final String ARTIFACT_ENCRYPTION = "artifact.encryption";

    private final VaadinMessageSource i18n;
    private final SoftwareModuleTypeDataProvider<ProxyTypeInfo> smTypeDataProvider;

    /**
     * Constructor for SmWindowLayoutComponentBuilder
     *
     * @param i18n
     *            VaadinMessageSource
     * @param smTypeDataProvider
     *            SoftwareModuleTypeDataProvider
     */
    public SmWindowLayoutComponentBuilder(final VaadinMessageSource i18n,
            final SoftwareModuleTypeDataProvider<ProxyTypeInfo> smTypeDataProvider) {
        this.i18n = i18n;
        this.smTypeDataProvider = smTypeDataProvider;
    }

    /**
     * Create combo box options for software module types
     *
     * @param binder
     *            binder the input will be bound to
     *
     * @return input component
     */
    public ComboBox<ProxyTypeInfo> createSoftwareModuleTypeCombo(final Binder<ProxySoftwareModule> binder) {
        return FormComponentBuilder
                .createTypeCombo(binder, smTypeDataProvider, i18n, UIComponentIdProvider.SW_MODULE_TYPE, true)
                .getComponent();
    }

    /**
     * create name field
     * 
     * @param binder
     *            binder the input will be bound to
     * @return input component
     */
    public TextField createNameField(final Binder<ProxySoftwareModule> binder) {
        return FormComponentBuilder.createNameInput(binder, i18n, UIComponentIdProvider.SOFT_MODULE_NAME)
                .getComponent();
    }

    /**
     * create version field
     * 
     * @param binder
     *            binder the input will be bound to
     * @return input component
     */
    public TextField createVersionField(final Binder<ProxySoftwareModule> binder) {
        return FormComponentBuilder.createVersionInput(binder, i18n, UIComponentIdProvider.SOFT_MODULE_VERSION)
                .getComponent();
    }

    /**
     * Create vendor field
     *
     * @param binder
     *            binder the input will be bound to
     *
     * @return input component
     */
    public TextField createVendorField(final Binder<ProxySoftwareModule> binder) {
        final TextField smVendor = new TextFieldBuilder(SoftwareModule.VENDOR_MAX_SIZE)
                .id(UIComponentIdProvider.SOFT_MODULE_VENDOR).caption(i18n.getMessage(TEXTFIELD_VENDOR))
                .prompt(i18n.getMessage(TEXTFIELD_VENDOR)).buildTextComponent();
        smVendor.setSizeUndefined();

        binder.forField(smVendor).bind(ProxySoftwareModule::getVendor, ProxySoftwareModule::setVendor);

        return smVendor;
    }

    /**
     * create description field
     * 
     * @param binder
     *            binder the input will be bound to
     * @return input component
     */
    public TextArea createDescription(final Binder<ProxySoftwareModule> binder) {
        return FormComponentBuilder
                .createDescriptionInput(binder, i18n, UIComponentIdProvider.ADD_SW_MODULE_DESCRIPTION).getComponent();
    }

    /**
     * Create checkbox for artifact encryption
     *
     * @param binder
     *            binder the input will be bound to
     *
     * @return input component
     */
    public CheckBox createArtifactEncryptionCheck(final Binder<ProxySoftwareModule> binder) {
        return FormComponentBuilder.createCheckBox(i18n.getMessage(ARTIFACT_ENCRYPTION),
                UIComponentIdProvider.ARTIFACT_ENCRYPTION_ID, binder, ProxySoftwareModule::isEncrypted,
                ProxySoftwareModule::setEncrypted);
    }
}
