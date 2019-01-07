/**
 * Copyright (c) 2019 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.management.miscs;

import org.eclipse.hawkbit.ui.utils.VaadinMessageSource;
import org.vaadin.hene.flexibleoptiongroup.FlexibleOptionGroup;

/**
 * Action type option group layout for auto assignment.
 */
public class ActionTypeOptionGroupAutoAssignmentLayout extends ActionTypeOptionGroupAbstractLayout {
    private static final long serialVersionUID = 1L;

    public ActionTypeOptionGroupAutoAssignmentLayout(final VaadinMessageSource i18n) {
        super(i18n);
    }

    @Override
    protected void createOptionGroup() {
        actionTypeOptionGroup = new FlexibleOptionGroup();
        actionTypeOptionGroup.addItem(ActionTypeOption.SOFT);
        actionTypeOptionGroup.addItem(ActionTypeOption.FORCED);
        selectDefaultOption();

        addForcedItemWithLabel();
        addSoftItemWithLabel();
    }
}
