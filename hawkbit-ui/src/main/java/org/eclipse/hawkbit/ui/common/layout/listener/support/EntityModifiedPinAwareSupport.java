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
import java.util.function.LongFunction;
import java.util.function.Predicate;

import org.eclipse.hawkbit.ui.common.data.proxies.ProxyIdentifiableEntity;
import org.eclipse.hawkbit.ui.common.grid.support.PinSupport;
import org.eclipse.hawkbit.ui.common.layout.listener.EntityModifiedListener.EntityModifiedAwareSupport;

/**
 * Support for Entity modified with pinned aware
 *
 * @param <T>
 *            Generic type of ProxyIdentifiableEntity
 */
public class EntityModifiedPinAwareSupport<T extends ProxyIdentifiableEntity> implements EntityModifiedAwareSupport {
    private final PinSupport<T, ?> pinSupport;

    private final boolean shouldUpdatePinStylingOnUpdate;
    private final boolean shouldReApplyPinningOnUpdate;

    private final LongFunction<Optional<T>> getFromBackendCallback;
    private final Predicate<T> shouldUnpinnOnUpdateCallback;

    /**
     * Constructor for EntityModifiedPinAwareSupport
     *
     * @param pinSupport
     *            Pin support
     * @param shouldUpdatePinStylingOnUpdate
     *            Configure pin styling on update
     * @param shouldReApplyPinningOnUpdate
     *            Configure pin reapply on update
     * @param getFromBackendCallback
     *            Backend callback
     * @param shouldUnpinnOnUpdateCallback
     *            Unpin on update callback
     */
    public EntityModifiedPinAwareSupport(final PinSupport<T, ?> pinSupport,
            final boolean shouldUpdatePinStylingOnUpdate, final boolean shouldReApplyPinningOnUpdate,
            final LongFunction<Optional<T>> getFromBackendCallback, final Predicate<T> shouldUnpinnOnUpdateCallback) {
        this.pinSupport = pinSupport;

        this.shouldUpdatePinStylingOnUpdate = shouldUpdatePinStylingOnUpdate;
        this.shouldReApplyPinningOnUpdate = shouldReApplyPinningOnUpdate;

        this.getFromBackendCallback = getFromBackendCallback;
        this.shouldUnpinnOnUpdateCallback = shouldUnpinnOnUpdateCallback;
    }

    /**
     * Static method for constructor EntityModifiedPinAwareSupport
     *
     * @param pinSupport
     *            Pin support
     * @param shouldUpdatePinStylingOnUpdate
     *            Configure pin styling on update
     * @param shouldReApplyPinningOnUpdate
     *            Configure pin reapply on update
     * @param <E>
     *            Generic type support
     *
     * @return Support for Entity modified with pinned aware
     */
    public static <E extends ProxyIdentifiableEntity> EntityModifiedPinAwareSupport<E> of(
            final PinSupport<E, ?> pinSupport, final boolean shouldUpdatePinStylingOnUpdate,
            final boolean shouldReApplyPinningOnUpdate) {
        return new EntityModifiedPinAwareSupport<>(pinSupport, shouldUpdatePinStylingOnUpdate,
                shouldReApplyPinningOnUpdate, null, null);
    }

    /**
     * Static method for constructor EntityModifiedPinAwareSupport
     *
     * @param pinSupport
     *            Pin support
     * @param getFromBackendCallback
     *            Backend callback
     * @param shouldUnpinnOnUpdateCallback
     *            Unpin on update callback
     * @param <E>
     *            Generic type support
     *
     * @return Support for Entity modified with pinned aware
     */
    public static <E extends ProxyIdentifiableEntity> EntityModifiedPinAwareSupport<E> of(
            final PinSupport<E, ?> pinSupport, final LongFunction<Optional<E>> getFromBackendCallback,
            final Predicate<E> shouldUnpinnOnUpdateCallback) {
        return new EntityModifiedPinAwareSupport<>(pinSupport, false, false, getFromBackendCallback,
                shouldUnpinnOnUpdateCallback);
    }

    @Override
    public void onEntitiesUpdated(final Collection<Long> entityIds) {
        if (pinSupport == null) {
            return;
        }

        if (shouldUpdatePinStylingOnUpdate) {
            pinSupport.repopulateAssignedAndInstalled();
        }

        if (pinSupport.isPinItemInIds(entityIds)) {
            if (shouldReApplyPinningOnUpdate) {
                pinSupport.reApplyPinning();
            }

            if (getFromBackendCallback != null && shouldUnpinnOnUpdateCallback != null) {
                unpinInvalidItem();
            }
        }
    }

    private void unpinInvalidItem() {
        pinSupport.getPinnedItemId().flatMap(getFromBackendCallback::apply).ifPresent(updatedPinnedItem -> {
            if (shouldUnpinnOnUpdateCallback.test(updatedPinnedItem)) {
                pinSupport.removePinning();
            }
        });
    }

    @Override
    public void onEntitiesDeleted(final Collection<Long> entityIds) {
        if (pinSupport == null || !pinSupport.isPinItemInIds(entityIds)) {
            return;
        }

        if (pinSupport.isPinItemInIds(entityIds)) {
            pinSupport.removePinning();
        }
    }
}