/**
 * Copyright (c) 2021 Bosch.IO GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.management.targettag.targettype;

import com.vaadin.data.Binder;
import org.eclipse.hawkbit.repository.DistributionSetTypeManagement;
import org.eclipse.hawkbit.ui.common.data.proxies.ProxyTargetType;
import org.eclipse.hawkbit.ui.utils.VaadinMessageSource;

/**
 * Builder for target type window layout component
 */
public class TargetTypeWindowLayoutComponentBuilder {

    private final VaadinMessageSource i18n;
    private final DistributionSetTypeManagement distributionSetTypeManagement;

    /**
     * Constructor for TargetTypeWindowLayoutComponentBuilder
     *
     * @param i18n
     *          VaadinMessageSource
     * @param distributionSetTypeManagement
     *          distributionSetTypeManagement
     */
    public TargetTypeWindowLayoutComponentBuilder(final VaadinMessageSource i18n,
                                                  final DistributionSetTypeManagement distributionSetTypeManagement) {
        this.i18n = i18n;
        this.distributionSetTypeManagement = distributionSetTypeManagement;
    }

    /**
     * Create target type layout
     *
     * @param binder
     *          Vaadin binder
     *
     * @return layout of target type distribution set selection
     */
    public TargetTypeDsTypeSelectLayout createTargetTypeDsSelectLayout(final Binder<ProxyTargetType> binder) {

        final TargetTypeDsTypeSelectLayout targetTypeDsTypeSelectLayout = new TargetTypeDsTypeSelectLayout(i18n, distributionSetTypeManagement);
        targetTypeDsTypeSelectLayout.setRequiredIndicatorVisible(false);

        binder.forField(targetTypeDsTypeSelectLayout)
              .bind(ProxyTargetType::getSelectedDsTypes, ProxyTargetType::setSelectedDsTypes);

        return targetTypeDsTypeSelectLayout;
    }

}
