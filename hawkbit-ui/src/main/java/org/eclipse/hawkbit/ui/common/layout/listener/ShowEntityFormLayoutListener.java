/** 
 * Copyright (c) 2020 Bosch.IO GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.common.layout.listener;

import java.util.function.Consumer;

import org.eclipse.hawkbit.ui.common.data.proxies.ProxyIdentifiableEntity;
import org.eclipse.hawkbit.ui.common.event.CommandTopics;
import org.eclipse.hawkbit.ui.common.event.EventLayoutViewAware;
import org.eclipse.hawkbit.ui.common.event.LayoutVisibilityEventPayload;
import org.eclipse.hawkbit.ui.common.event.LayoutVisibilityEventPayload.VisibilityType;
import org.eclipse.hawkbit.ui.common.event.ShowFormEventPayload;
import org.eclipse.hawkbit.ui.common.event.ShowFormEventPayload.FormType;
import org.vaadin.spring.events.EventBus.UIEventBus;
import org.vaadin.spring.events.EventScope;
import org.vaadin.spring.events.annotation.EventBusListenerMethod;

/**
 * Event listener for show entity form layout
 *
 * @param <T>
 *            Generic type of ProxyIdentifiableEntity
 */
public class ShowEntityFormLayoutListener<T extends ProxyIdentifiableEntity> extends LayoutViewAwareListener {
    private final Class<T> entityType;
    private final Class<? extends ProxyIdentifiableEntity> parentEntityType;
    private final Runnable addFormCallback;
    private final Consumer<T> updateFormCallback;

    /**
     * Constructor for ShowEntityFormLayoutListener
     *
     * @param eventBus
     *            UIEventBus
     * @param entityType
     *            Generic entity type
     * @param layoutViewAware
     *            EventLayoutViewAware
     * @param addFormCallback
     *            Runnable
     * @param updateFormCallback
     *            Update form callback event
     */
    public ShowEntityFormLayoutListener(final UIEventBus eventBus, final Class<T> entityType,
            final EventLayoutViewAware layoutViewAware, final Runnable addFormCallback,
            final Consumer<T> updateFormCallback) {
        this(eventBus, entityType, null, layoutViewAware, addFormCallback, updateFormCallback);
    }

    /**
     * Constructor for ShowEntityFormLayoutListener
     *
     * @param eventBus
     *            UIEventBus
     * @param entityType
     *            Generic entity type
     * @param parentEntityType
     *            Identifiable Entity type
     * @param layoutViewAware
     *            EventLayoutViewAware
     * @param addFormCallback
     *            Runnable
     * @param updateFormCallback
     *            Update form callback event
     */
    public ShowEntityFormLayoutListener(final UIEventBus eventBus, final Class<T> entityType,
            final Class<? extends ProxyIdentifiableEntity> parentEntityType, final EventLayoutViewAware layoutViewAware,
            final Runnable addFormCallback, final Consumer<T> updateFormCallback) {
        super(eventBus, CommandTopics.SHOW_ENTITY_FORM_LAYOUT, layoutViewAware);

        this.entityType = entityType;
        this.parentEntityType = parentEntityType;
        this.addFormCallback = addFormCallback;
        this.updateFormCallback = updateFormCallback;
    }

    @EventBusListenerMethod(scope = EventScope.UI)
    private void onShowFormEvent(final ShowFormEventPayload<T> eventPayload) {
        if (!suitableEntityType(eventPayload.getEntityType())
                || !suitableParentEntityType(eventPayload.getParentEntityType())
                || !getLayoutViewAware().suitableView(eventPayload)) {
            return;
        }

        if (FormType.ADD == eventPayload.getFormType()) {
            addFormCallback.run();
        } else {
            updateFormCallback.accept(eventPayload.getEntity());
        }

        getEventBus().publish(CommandTopics.CHANGE_LAYOUT_VISIBILITY, this,
                new LayoutVisibilityEventPayload(VisibilityType.SHOW, getLayout(), getView()));
    }

    private boolean suitableEntityType(final Class<? extends ProxyIdentifiableEntity> type) {
        return entityType != null && entityType.equals(type);
    }

    private boolean suitableParentEntityType(final Class<? extends ProxyIdentifiableEntity> parentType) {
        // parent type is optional
        return parentEntityType == null || parentEntityType.equals(parentType);
    }
}
