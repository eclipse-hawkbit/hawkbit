/**
 * Copyright (c) 2020 Bosch.IO GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.common.event;

import org.eclipse.hawkbit.ui.common.data.proxies.ProxyIdentifiableEntity;

/**
 * Payload event to show form
 *
 * @param <T>
 *            Generic type
 */
public class ShowFormEventPayload<T extends ProxyIdentifiableEntity> extends EventViewAware {
    private final FormType formType;
    private final Class<? extends ProxyIdentifiableEntity> entityType;
    private final Class<? extends ProxyIdentifiableEntity> parentEntityType;
    private final T entity;

    /**
     * Constructor for ShowFormEventPayload
     *
     * @param formType
     *            FormType
     * @param entityType
     *            Type of entity
     * @param view
     *            EventView
     */
    public ShowFormEventPayload(final FormType formType, final Class<? extends ProxyIdentifiableEntity> entityType,
            final EventView view) {
        this(formType, entityType, null, null, view);
    }

    /**
     * Constructor for ShowFormEventPayload
     *
     * @param formType
     *            FormType
     * @param entity
     *            Generic type entity payload
     * @param view
     *            EventView
     */
    public ShowFormEventPayload(final FormType formType, final T entity, final EventView view) {
        this(formType, entity.getClass(), null, entity, view);
    }

    /**
     * Constructor for ShowFormEventPayload
     *
     * @param formType
     *            FormType
     * @param entityType
     *            Type of entity
     * @param parentEntityType
     *            Type of parent entity
     * @param view
     *            EventView
     */
    public ShowFormEventPayload(final FormType formType, final Class<? extends ProxyIdentifiableEntity> entityType,
            final Class<? extends ProxyIdentifiableEntity> parentEntityType, final EventView view) {
        this(formType, entityType, parentEntityType, null, view);
    }

    /**
     * Constructor for ShowFormEventPayload
     *
     * @param formType
     *            FormType
     * @param parentEntityType
     *            Type of parent entity
     * @param entity
     *            Generic type entity payload
     * @param view
     *            EventView
     */
    public ShowFormEventPayload(final FormType formType,
            final Class<? extends ProxyIdentifiableEntity> parentEntityType, final T entity, final EventView view) {
        this(formType, entity.getClass(), parentEntityType, entity, view);
    }

    /**
     * Constructor for ShowFormEventPayload
     *
     * @param formType
     *            FormType
     * @param entityType
     *            Type of entity
     * @param parentEntityType
     *            Type of parent entity
     * @param entity
     *            Generic type entity payload
     * @param view
     *            EventView
     */
    private ShowFormEventPayload(final FormType formType, final Class<? extends ProxyIdentifiableEntity> entityType,
            final Class<? extends ProxyIdentifiableEntity> parentEntityType, final T entity, final EventView view) {
        super(view);

        this.formType = formType;
        this.entityType = entityType;
        this.parentEntityType = parentEntityType;
        this.entity = entity;
    }

    /**
     * @return Form type
     */
    public FormType getFormType() {
        return formType;
    }

    /**
     * @return Event payload of identifiable entity type
     */
    public Class<? extends ProxyIdentifiableEntity> getEntityType() {
        return entityType;
    }

    /**
     * @return Event payload of identifiable parent entity type
     */
    public Class<? extends ProxyIdentifiableEntity> getParentEntityType() {
        return parentEntityType;
    }

    /**
     * @return entity
     */
    public T getEntity() {
        return entity;
    }

    /**
     * Form action type
     */
    public enum FormType {
        ADD, EDIT;
    }
}
