/**
 * Copyright (c) 2020 Bosch.IO GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.common;

import java.util.Objects;

import javax.validation.ConstraintViolationException;

import org.eclipse.hawkbit.repository.EntityFactory;
import org.eclipse.hawkbit.repository.exception.EntityNotFoundException;
import org.eclipse.hawkbit.repository.exception.EntityReadOnlyException;
import org.eclipse.hawkbit.ui.common.CommonDialogWindow.SaveDialogCloseListener;
import org.eclipse.hawkbit.ui.common.data.proxies.ProxyIdentifiableEntity;
import org.eclipse.hawkbit.ui.common.event.CommandTopics;
import org.eclipse.hawkbit.ui.common.event.EntityModifiedEventPayload;
import org.eclipse.hawkbit.ui.common.event.EventTopics;
import org.eclipse.hawkbit.ui.common.event.SelectionChangedEventPayload;
import org.eclipse.hawkbit.ui.utils.UINotification;
import org.eclipse.hawkbit.ui.utils.VaadinMessageSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vaadin.spring.events.EventBus.UIEventBus;

/**
 * Controller for abstract entity window
 *
 * @param <T>
 *            Type of proxy entity
 * @param <E>
 *            Second type of proxy entity
 * @param <R>
 *            Type of repository entity
 */
public abstract class AbstractEntityWindowController<T, E, R> {

    private static final Logger LOG = LoggerFactory.getLogger(AbstractEntityWindowController.class);

    private final CommonUiDependencies uiDependencies;

    /**
     * Constructor.
     *
     * @param uiDependencies
     *            the {@link CommonUiDependencies}
     */
    protected AbstractEntityWindowController(final CommonUiDependencies uiDependencies) {
        this.uiDependencies = uiDependencies;
    }

    /**
     * Populate entity with data
     *
     * @param proxyEntity
     *            Generic type entity
     */
    public void populateWithData(final T proxyEntity) {
        getLayout().setEntity(buildEntityFromProxy(proxyEntity));

        adaptLayout(proxyEntity);
    }

    /**
     * @return layout
     */
    public abstract EntityWindowLayout<E> getLayout();

    protected abstract E buildEntityFromProxy(final T proxyEntity);

    protected void adaptLayout(final T proxyEntity) {
        // can be overriden to adapt layout components (e.g. disable/enable
        // fields, adapt bindings, etc.)
    }

    /**
     * @return Save dialog close listener
     */
    public SaveDialogCloseListener getSaveDialogCloseListener() {
        return new SaveDialogCloseListener() {
            @Override
            public void saveOrUpdate() {
                persistEntity(getLayout().getEntity());
            }

            @Override
            public boolean canWindowSaveOrUpdate() {
                return isEntityValid(getLayout().getEntity());
            }

            @Override
            public boolean canWindowClose() {
                return closeWindowAfterSave();
            }
        };
    }

    protected void persistEntity(final E entity) {
        try {

            final R createdEntity = persistEntityInRepository(entity);

            handleEntityPersistedSuccessfully(createdEntity);

            selectPersistedEntity(createdEntity);

        } catch (final ConstraintViolationException | EntityNotFoundException | EntityReadOnlyException ex) {

            handleEntityPersistFailed(entity, ex);

        } finally {

            postPersist();
        }
    }

    protected abstract R persistEntityInRepository(E entity);

    protected void handleEntityPersistedSuccessfully(final R persistedEntity) {
        displaySuccess(getPersistSuccessMessageKey(), getDisplayableName(persistedEntity));
        publishModifiedEvent(createModifiedEventPayload(persistedEntity));
    }

    protected void handleEntityPersistFailed(final E entity, final RuntimeException ex) {
        final String name = getDisplayableNameForFailedMessage(entity);
        final String id = Objects.toString(entity);
        final String type = getEntityClass().getSimpleName();
        LOG.warn("Persist of entity name:{}, id:{}, type:{} failed in UI with reason: {}", name, id, type,
                ex.getMessage());
        displayWarning(getPersistFailureMessageKey(), name);
    }

    protected abstract EntityModifiedEventPayload createModifiedEventPayload(final R entity);

    protected abstract Long getId(R entity);

    protected abstract String getDisplayableNameForFailedMessage(E entity);

    protected abstract String getDisplayableName(R entity);

    protected abstract String getPersistSuccessMessageKey();

    protected abstract String getPersistFailureMessageKey();

    protected abstract Class<? extends ProxyIdentifiableEntity> getEntityClass();

    protected Class<? extends ProxyIdentifiableEntity> getParentEntityClass() {
        return null;
    }

    protected void selectPersistedEntity(final R entity) {
        // entity is not selected by default
    }

    protected void postPersist() {
        // empty default implementation
    }

    protected abstract boolean isEntityValid(final E entity);

    protected boolean closeWindowAfterSave() {
        return true;
    }

    protected VaadinMessageSource getI18n() {
        return uiDependencies.getI18n();
    }

    protected EntityFactory getEntityFactory() {
        return uiDependencies.getEntityFactory();
    }

    protected UIEventBus getEventBus() {
        return uiDependencies.getEventBus();
    }

    protected UINotification getUiNotification() {
        return uiDependencies.getUiNotification();
    }

    protected void displaySuccess(final String messageKey, final Object... args) {
        getUiNotification().displaySuccess(getI18n().getMessage(messageKey, args));
    }

    protected void displayValidationError(final String messageKey, final Object... args) {
        getUiNotification().displayValidationError(getI18n().getMessage(messageKey, args));
    }

    protected void displayWarning(final String messageKey, final Object... args) {
        getUiNotification().displayWarning(getI18n().getMessage(messageKey, args));
    }

    protected void publishModifiedEvent(final EntityModifiedEventPayload eventPayload) {
        getEventBus().publish(EventTopics.ENTITY_MODIFIED, this, eventPayload);
    }

    protected void publishSelectionEvent(final SelectionChangedEventPayload<E> eventPayload) {
        getEventBus().publish(CommandTopics.SELECT_GRID_ENTITY, this, eventPayload);
    }

}
