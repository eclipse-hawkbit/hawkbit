/** 
 * Copyright (c) 2020 Bosch.IO GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.common.layout.listener.support;

import java.util.Collection;
import java.util.Optional;
import java.util.function.LongConsumer;
import java.util.function.LongFunction;
import java.util.function.Predicate;

import org.eclipse.hawkbit.ui.common.data.proxies.ProxyIdentifiableEntity;
import org.eclipse.hawkbit.ui.common.event.SelectionChangedEventPayload.SelectionChangedEventType;
import org.eclipse.hawkbit.ui.common.grid.support.SelectionSupport;
import org.eclipse.hawkbit.ui.common.layout.listener.EntityModifiedListener.EntityModifiedAwareSupport;

/**
 * Support for Entity modified with selection aware
 *
 * @param <T>
 *            Generic type of ProxyIdentifiableEntity
 */
public class EntityModifiedSelectionAwareSupport<T extends ProxyIdentifiableEntity>
        implements EntityModifiedAwareSupport {
    private final SelectionSupport<T> selectionSupport;
    private final LongFunction<Optional<T>> getFromBackendCallback;
    private final Predicate<T> shouldDeselectCallback;
    private final LongConsumer selectedEntityDeletedCallback;

    /**
     * Constructor for EntityModifiedSelectionAwareSupport
     *
     * @param selectionSupport
     *            Selection support
     * @param getFromBackendCallback
     *            Backend callback
     * @param shouldDeselectCallback
     *            Deselect callback
     * @param selectedEntityDeletedCallback
     *            Delete callback for selected entity
     */
    public EntityModifiedSelectionAwareSupport(final SelectionSupport<T> selectionSupport,
            final LongFunction<Optional<T>> getFromBackendCallback, final Predicate<T> shouldDeselectCallback,
            final LongConsumer selectedEntityDeletedCallback) {
        this.selectionSupport = selectionSupport;
        this.getFromBackendCallback = getFromBackendCallback;
        this.shouldDeselectCallback = shouldDeselectCallback;
        this.selectedEntityDeletedCallback = selectedEntityDeletedCallback;
    }

    /**
     * Static method for constructor EntityModifiedSelectionAwareSupport
     *
     * @param selectionSupport
     *            Selection support
     * @param getFromBackendCallback
     *            Backend callback
     * @param <E>
     *            Generic type support
     *
     * @return Support for Entity modified with selection aware
     */
    public static <E extends ProxyIdentifiableEntity> EntityModifiedSelectionAwareSupport<E> of(
            final SelectionSupport<E> selectionSupport, final LongFunction<Optional<E>> getFromBackendCallback) {
        return of(selectionSupport, getFromBackendCallback, null, null);
    }

    /**
     * Static method for constructor EntityModifiedSelectionAwareSupport
     *
     * @param selectionSupport
     *            Selection support
     * @param getFromBackendCallback
     *            Backend callback
     * @param selectedEntityDeletedCallback
     *            Delete callback for selected entity
     * @param <E>
     *            Generic type support
     *
     * @return Support for Entity modified with selection aware
     */
    public static <E extends ProxyIdentifiableEntity> EntityModifiedSelectionAwareSupport<E> of(
            final SelectionSupport<E> selectionSupport, final LongFunction<Optional<E>> getFromBackendCallback,
            final LongConsumer selectedEntityDeletedCallback) {
        return of(selectionSupport, getFromBackendCallback, null, selectedEntityDeletedCallback);
    }

    /**
     * Static method for constructor EntityModifiedSelectionAwareSupport
     *
     * @param selectionSupport
     *            Selection support
     * @param getFromBackendCallback
     *            Backend callback
     * @param shouldDeselectCallback
     *            Deselect callback
     * @param <E>
     *            Generic type support
     *
     * @return Support for Entity modified with selection aware
     */
    public static <E extends ProxyIdentifiableEntity> EntityModifiedSelectionAwareSupport<E> of(
            final SelectionSupport<E> selectionSupport, final LongFunction<Optional<E>> getFromBackendCallback,
            final Predicate<E> shouldDeselectCallback) {
        return of(selectionSupport, getFromBackendCallback, shouldDeselectCallback, null);
    }

    /**
     * Static method for constructor EntityModifiedSelectionAwareSupport
     *
     * @param selectionSupport
     *            Selection support
     * @param getFromBackendCallback
     *            Backend callback
     * @param shouldDeselectCallback
     *            Deselect callback
     * @param selectedEntityDeletedCallback
     *            Delete callback for selected entity
     * @param <E>
     *            Generic type support
     *
     * @return Support for Entity modified with selection aware
     */
    public static <E extends ProxyIdentifiableEntity> EntityModifiedSelectionAwareSupport<E> of(
            final SelectionSupport<E> selectionSupport, final LongFunction<Optional<E>> getFromBackendCallback,
            final Predicate<E> shouldDeselectCallback, final LongConsumer selectedEntityDeletedCallback) {
        return new EntityModifiedSelectionAwareSupport<>(selectionSupport, getFromBackendCallback,
                shouldDeselectCallback, selectedEntityDeletedCallback);
    }

    @Override
    public void onEntitiesUpdated(final Collection<Long> entityIds) {
        if (getFromBackendCallback == null) {
            return;
        }

        getModifiedEntityId(entityIds).ifPresent(selectedEntityId ->
        // we load the up-to-date version of selected entity from
        // database and reselect it, so that master-aware components
        // could update itself
        getFromBackendCallback.apply(selectedEntityId).ifPresent(updatedItem -> selectionSupport
                .sendSelectionChangedEvent(getSelectionEventType(updatedItem), updatedItem)));
    }

    private Optional<Long> getModifiedEntityId(final Collection<Long> modifiedEntityIds) {
        if (selectionSupport == null) {
            return Optional.empty();
        }

        return selectionSupport.getSelectedEntityId().filter(modifiedEntityIds::contains);
    }

    private SelectionChangedEventType getSelectionEventType(final T updatedItem) {
        return shouldDeselectCallback != null && shouldDeselectCallback.test(updatedItem)
                ? SelectionChangedEventType.ENTITY_DESELECTED
                : SelectionChangedEventType.ENTITY_SELECTED;
    }

    @Override
    public void onEntitiesDeleted(final Collection<Long> entityIds) {
        getModifiedEntityId(entityIds).ifPresent(selectedEntityId -> {
            if (selectedEntityDeletedCallback != null) {
                selectedEntityDeletedCallback.accept(selectedEntityId);
            }
            // we need to update the master-aware components, that the
            // master entity was deselected after deletion
            selectionSupport.sendSelectionChangedEvent(SelectionChangedEventType.ENTITY_DESELECTED,
                    selectionSupport.getSelectedEntity().orElse(null));
        });
    }
}
