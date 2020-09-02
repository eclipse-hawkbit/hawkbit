/**
 * Copyright (c) 2020 Bosch.IO GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.artifacts.smtype;

import org.eclipse.hawkbit.ui.common.data.proxies.ProxyType;
import org.eclipse.hawkbit.ui.common.data.proxies.ProxyType.SmTypeAssign;
import org.eclipse.hawkbit.ui.utils.UIComponentIdProvider;
import org.eclipse.hawkbit.ui.utils.VaadinMessageSource;

import com.vaadin.data.Binder;
import com.vaadin.ui.RadioButtonGroup;

/**
 * Builder for software module type window layout component
 */
public class SmTypeWindowLayoutComponentBuilder {

    public static final String TEXTFIELD_KEY = "textfield.key";

    private final VaadinMessageSource i18n;

    /**
     * Constructor for SmTypeWindowLayoutComponentBuilder
     *
     * @param i18n
     *            VaadinMessageSource
     */
    public SmTypeWindowLayoutComponentBuilder(final VaadinMessageSource i18n) {
        this.i18n = i18n;
    }

    /**
     * Create software module type assignment group
     *
     * @param binder
     *            Vaadin binder of ProxyType
     *
     * @return RadioButtonGroup of software module type assignment
     */
    public RadioButtonGroup<SmTypeAssign> createSmTypeAssignOptionGroup(final Binder<ProxyType> binder) {
        final RadioButtonGroup<SmTypeAssign> smTypeAssignOptionGroup = new RadioButtonGroup<>();
        smTypeAssignOptionGroup.setId(UIComponentIdProvider.ASSIGN_OPTION_GROUP_SOFTWARE_MODULE_TYPE_ID);

        smTypeAssignOptionGroup.setItemCaptionGenerator(item -> {
            switch (item) {
            case SINGLE:
                return i18n.getMessage("label.singleAssign.type");
            case MULTI:
                return i18n.getMessage("label.multiAssign.type");
            default:
                return null;
            }
        });
        smTypeAssignOptionGroup.setItemDescriptionGenerator(item -> {
            switch (item) {
            case SINGLE:
                return i18n.getMessage("label.singleAssign.type.desc");
            case MULTI:
                return i18n.getMessage("label.multiAssign.type.desc");
            default:
                return null;
            }
        });

        binder.forField(smTypeAssignOptionGroup).bind(ProxyType::getSmTypeAssign, ProxyType::setSmTypeAssign);
        smTypeAssignOptionGroup.setItems(SmTypeAssign.values());

        return smTypeAssignOptionGroup;
    }
}
