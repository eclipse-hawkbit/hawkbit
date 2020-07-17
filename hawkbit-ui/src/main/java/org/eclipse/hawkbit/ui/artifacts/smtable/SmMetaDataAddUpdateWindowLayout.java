/**
 * Copyright (c) 2020 Bosch.IO GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.artifacts.smtable;

import org.eclipse.hawkbit.ui.common.detailslayout.MetaDataAddUpdateWindowLayout;
import org.eclipse.hawkbit.ui.utils.VaadinMessageSource;

import com.vaadin.ui.CheckBox;
import com.vaadin.ui.ComponentContainer;

/**
 * Class for sm metadata add/update window layout.
 */
public class SmMetaDataAddUpdateWindowLayout extends MetaDataAddUpdateWindowLayout {
    protected final CheckBox isVisibleForTarget;

    /**
     * Constructor for AbstractTagWindowLayout
     * 
     * @param i18n
     *            I18N
     */
    public SmMetaDataAddUpdateWindowLayout(final VaadinMessageSource i18n) {
        super(i18n);

        this.isVisibleForTarget = metaDataComponentBuilder.createVisibleForTargetsField(binder);
    }

    /**
     * @return form layout checkbox container for software module
     */
    @Override
    public ComponentContainer getRootComponent() {
        final ComponentContainer formLayout = super.getRootComponent();

        formLayout.addComponent(isVisibleForTarget);

        return formLayout;
    }
}
