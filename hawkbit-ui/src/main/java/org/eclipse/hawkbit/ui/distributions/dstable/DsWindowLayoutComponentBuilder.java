/**
 * Copyright (c) 2020 Bosch.IO GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.distributions.dstable;

import org.eclipse.hawkbit.ui.common.builder.FormComponentBuilder;
import org.eclipse.hawkbit.ui.common.data.providers.DistributionSetTypeDataProvider;
import org.eclipse.hawkbit.ui.common.data.proxies.ProxyDistributionSet;
import org.eclipse.hawkbit.ui.common.data.proxies.ProxyTypeInfo;
import org.eclipse.hawkbit.ui.utils.UIComponentIdProvider;
import org.eclipse.hawkbit.ui.utils.VaadinMessageSource;

import com.vaadin.data.Binder;
import com.vaadin.ui.CheckBox;
import com.vaadin.ui.ComboBox;
import com.vaadin.ui.TextArea;
import com.vaadin.ui.TextField;
import com.vaadin.ui.themes.ValoTheme;

import static org.eclipse.hawkbit.ui.utils.SPUIStyleDefinitions.DIST_CHECKBOX_STYLE;

/**
 * Builder for Distribution set window layout component
 */
public class DsWindowLayoutComponentBuilder {

    public static final String MIGRATION_STEP = "label.dist.required.migration.step";

    private final VaadinMessageSource i18n;
    private final DistributionSetTypeDataProvider<ProxyTypeInfo> dsTypeDataProvider;

    /**
     * Constructor for DsWindowLayoutComponentBuilder
     *
     * @param i18n
     *            VaadinMessageSource
     * @param dsTypeDataProvider
     *            DistributionSetTypeDataProvider
     */
    public DsWindowLayoutComponentBuilder(final VaadinMessageSource i18n,
            final DistributionSetTypeDataProvider<ProxyTypeInfo> dsTypeDataProvider) {
        this.i18n = i18n;
        this.dsTypeDataProvider = dsTypeDataProvider;
    }

    /**
     * Create combobox of distribution set type
     *
     * @param binder
     *            Vaddin binder
     *
     * @return Distribution set type combobox
     */
    public ComboBox<ProxyTypeInfo> createDistributionSetTypeCombo(final Binder<ProxyDistributionSet> binder) {
        return FormComponentBuilder
                .createTypeCombo(binder, dsTypeDataProvider, i18n, UIComponentIdProvider.DIST_ADD_DISTSETTYPE, true)
                .getComponent();
    }

    /**
     * create name field
     * 
     * @param binder
     *            binder the input will be bound to
     * @return input component
     */
    public TextField createNameField(final Binder<ProxyDistributionSet> binder) {
        return FormComponentBuilder.createNameInput(binder, i18n, UIComponentIdProvider.DIST_ADD_NAME).getComponent();
    }

    /**
     * create version field
     * 
     * @param binder
     *            binder the input will be bound to
     * @return input component
     */
    public TextField createVersionField(final Binder<ProxyDistributionSet> binder) {
        return FormComponentBuilder.createVersionInput(binder, i18n, UIComponentIdProvider.DIST_ADD_VERSION)
                .getComponent();
    }

    /**
     * create description field
     * 
     * @param binder
     *            binder the input will be bound to
     * @return input component
     */
    public TextArea createDescription(final Binder<ProxyDistributionSet> binder) {
        return FormComponentBuilder.createDescriptionInput(binder, i18n, UIComponentIdProvider.DIST_ADD_DESC)
                .getComponent();
    }

    /**
     * Create migration step field
     *
     * @param binder
     *            Vaadin binder
     *
     * @return Migration step required checkbox
     */
    public CheckBox createMigrationStepField(final Binder<ProxyDistributionSet> binder) {
        final CheckBox migrationRequired = FormComponentBuilder.createCheckBox(i18n.getMessage(MIGRATION_STEP),
                UIComponentIdProvider.DIST_ADD_MIGRATION_CHECK, binder, ProxyDistributionSet::isRequiredMigrationStep,
                ProxyDistributionSet::setRequiredMigrationStep);

        migrationRequired.setDescription(i18n.getMessage(MIGRATION_STEP));
        migrationRequired.addStyleName(DIST_CHECKBOX_STYLE);
        migrationRequired.addStyleName(ValoTheme.CHECKBOX_SMALL);

        return migrationRequired;
    }
}
