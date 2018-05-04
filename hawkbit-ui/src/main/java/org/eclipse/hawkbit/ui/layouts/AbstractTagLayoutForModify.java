/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.layouts;

import org.eclipse.hawkbit.repository.EntityFactory;
import org.eclipse.hawkbit.repository.model.NamedEntity;
import org.eclipse.hawkbit.ui.SpPermissionChecker;
import org.eclipse.hawkbit.ui.utils.UINotification;
import org.eclipse.hawkbit.ui.utils.VaadinMessageSource;
import org.vaadin.spring.events.EventBus.UIEventBus;

/**
 * General Layout for pop-up window for Tags which is created when updating or
 * deleting a tag. The layout includes the combobox for selecting the tag to
 * manage.
 *
 * @param <E>
 */
public abstract class AbstractTagLayoutForModify<E extends NamedEntity> extends AbstractTagLayout<E> {

    private static final long serialVersionUID = 1L;

    private final String selectedTagName;

    public AbstractTagLayoutForModify(final VaadinMessageSource i18n, final EntityFactory entityFactory,
            final UIEventBus eventBus, final SpPermissionChecker permChecker, final UINotification uiNotification,
            final String selectedTagName) {
        super(i18n, entityFactory, eventBus, permChecker, uiNotification);
        this.selectedTagName = selectedTagName;
        init();
    }

    @Override
    public void init() {
        super.init();
        setTagDetails(selectedTagName);
    }

    @Override
    protected boolean isUpdateAction() {
        return true;
    }

    /**
     * Select tag & set tag name & tag desc values corresponding to selected
     * tag.
     *
     * @param distTagSelected
     *            as the selected tag from combo
     */
    protected abstract void setTagDetails(final String selectedTagName);

}
