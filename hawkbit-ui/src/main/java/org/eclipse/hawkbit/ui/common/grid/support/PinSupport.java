/**
 * Copyright (c) 2020 Bosch.IO GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.common.grid.support;

import java.util.Collection;
import java.util.HashSet;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import org.eclipse.hawkbit.ui.common.data.proxies.ProxyIdentifiableEntity;
import org.eclipse.hawkbit.ui.utils.SPUIDefinitions;
import org.eclipse.hawkbit.ui.utils.SPUIStyleDefinitions;
import org.springframework.util.CollectionUtils;

/**
 * Support for pinning the items in grid.
 * 
 * @param <T>
 *            The item-type used by the grid
 * @param <F>
 *            The type of identifier assigned and installed ids are related to
 */
public class PinSupport<T extends ProxyIdentifiableEntity, F> {
    // used to change the pinning style
    private final Consumer<T> refreshItemCallback;

    private final BiConsumer<PinBehaviourType, T> publishPinningChangedCallback;
    private final Consumer<T> updatePinnedUiStateCallback;

    private final Supplier<Optional<F>> getPinFilterCallback;
    private final Consumer<F> updatePinFilterCallback;

    private final Function<F, Collection<Long>> assignedIdsProvider;
    private final Function<F, Collection<Long>> installedIdsProvider;
    private final Collection<Long> assignedIds;
    private final Collection<Long> installedIds;

    private T pinnedItem;

    /**
     * Constructor for PinSupport
     *
     * @param refreshItemCallback
     *            Refresh item call back event
     * @param publishPinningChangedCallback
     *            Publish Pin changed call back event
     * @param updatePinnedUiStateCallback
     *            Update pin callback event
     * @param getPinFilterCallback
     *            Pin filter call back event
     * @param updatePinFilterCallback
     *            Update pin filter callback event
     * @param assignedIdsProvider
     *            Assigned id provider list
     * @param installedIdsProvider
     *            Installed Id provider list
     *
     */
    public PinSupport(final Consumer<T> refreshItemCallback,
            final BiConsumer<PinBehaviourType, T> publishPinningChangedCallback,
            final Consumer<T> updatePinnedUiStateCallback, final Supplier<Optional<F>> getPinFilterCallback,
            final Consumer<F> updatePinFilterCallback, final Function<F, Collection<Long>> assignedIdsProvider,
            final Function<F, Collection<Long>> installedIdsProvider) {
        this.refreshItemCallback = refreshItemCallback;

        this.publishPinningChangedCallback = publishPinningChangedCallback;
        this.updatePinnedUiStateCallback = updatePinnedUiStateCallback;

        this.getPinFilterCallback = getPinFilterCallback;
        this.updatePinFilterCallback = updatePinFilterCallback;

        this.assignedIdsProvider = assignedIdsProvider;
        this.installedIdsProvider = installedIdsProvider;
        this.assignedIds = new HashSet<>();
        this.installedIds = new HashSet<>();

        this.pinnedItem = null;
    }

    /**
     * Updates the view on pinning changed
     *
     * @param item
     *            Generic type of entity
     */
    public void changeItemPinning(final T item) {
        if (isPinned(item.getId())) {
            pinnedItem = null;

            refreshItemCallback.accept(item);
            onPinningChanged(PinBehaviourType.UNPINNED, item);
        } else {
            // used to reset styling of pinned items' grid
            if (isPinFilterActive()) {
                clearAssignedAndInstalled();
            }

            final T previouslyPinnedItem = pinnedItem;
            pinnedItem = item;

            // used to change the icon of previously pinned item to unpinned
            // state
            if (previouslyPinnedItem != null) {
                refreshItemCallback.accept(previouslyPinnedItem);
            }

            refreshItemCallback.accept(item);
            onPinningChanged(PinBehaviourType.PINNED, item);
        }
    }

    private boolean isPinned(final Long itemId) {
        return pinnedItem != null && pinnedItem.getId().equals(itemId);
    }

    private void onPinningChanged(final PinBehaviourType pinType, final T item) {
        // used to remove pin filter when pinning the grids' item
        if (isPinFilterActive()) {
            updatePinFilterCallback.accept(null);
        }

        publishPinningChangedCallback.accept(pinType, item);

        updatePinnedUiStateCallback.accept(pinType == PinBehaviourType.PINNED ? item : null);
    }

    private boolean isPinFilterActive() {
        return getPinFilterCallback.get().isPresent();
    }

    private void clearAssignedAndInstalled() {
        assignedIds.clear();
        installedIds.clear();
    }

    /**
     * @return Pinned item
     */
    public Optional<T> getPinnedItem() {
        return Optional.ofNullable(pinnedItem);
    }

    /**
     * @return Id of pinned item
     */
    public Optional<Long> getPinnedItemId() {
        return getPinnedItem().map(ProxyIdentifiableEntity::getId);
    }

    /**
     * Gets the pin style
     *
     * @param item
     *            Pinned item
     *
     * @return Pin style
     */
    public String getPinningStyle(final T item) {
        if (isPinned(item.getId())) {
            return null;
        } else {
            return SPUIStyleDefinitions.UN_PINNED_STYLE;
        }
    }

    /**
     * Gets the style of assigned or installed row
     *
     * @param itemId
     *            Id of item
     *
     * @return Assigned or installed row style
     */
    public String getAssignedOrInstalledRowStyle(final Long itemId) {
        if (!isPinFilterActive()) {
            return null;
        }

        if (!CollectionUtils.isEmpty(installedIds) && installedIds.contains(itemId)) {
            return SPUIDefinitions.HIGHLIGHT_GREEN;
        }

        if (!CollectionUtils.isEmpty(assignedIds) && assignedIds.contains(itemId)) {
            return SPUIDefinitions.HIGHLIGHT_ORANGE;
        }

        return null;
    }

    /**
     * Restore the pinning
     *
     * @param itemToRestore
     *            Pin item to restore
     */
    public void restorePinning(final T itemToRestore) {
        pinnedItem = itemToRestore;
    }

    /**
     * Update the pin filter
     *
     * @param pinFilter
     *            Pin filter item
     */
    public void updatePinFilter(final F pinFilter) {
        // used to remove grids' pinned item when applying the pin filter
        if (clearPinning()) {
            updatePinnedUiStateCallback.accept(null);
        }

        if (pinFilter == null && !isPinFilterActive()) {
            return;
        }

        repopulateAssignedAndInstalled(pinFilter);
        updatePinFilterCallback.accept(pinFilter);
    }

    private boolean clearPinning() {
        if (pinnedItem != null) {
            final T previouslyPinnedItem = pinnedItem;
            pinnedItem = null;

            refreshItemCallback.accept(previouslyPinnedItem);

            return true;
        }

        return false;
    }

    /**
     * Apply installed and assigned pin filter and refresh the view
     *
     * @param pinFilter
     *            Pin filter
     */
    public void repopulateAssignedAndInstalled(final F pinFilter) {
        clearAssignedAndInstalled();

        if (pinFilter != null) {
            assignedIds.addAll(assignedIdsProvider.apply(pinFilter));
            installedIds.addAll(installedIdsProvider.apply(pinFilter));
        }
    }

    /**
     * Repopulate installed and assigned pin filter
     */
    public void repopulateAssignedAndInstalled() {
        getPinFilterCallback.get().ifPresent(this::repopulateAssignedAndInstalled);
    }

    /**
     * Match pin item in the collection of pin id
     *
     * @param itemIds
     *            List of Pinned Id
     *
     * @return True if pinned item is found in list of pinned Ids else false
     */
    public boolean isPinItemInIds(final Collection<Long> itemIds) {
        return pinnedItem != null && !CollectionUtils.isEmpty(itemIds) && itemIds.contains(pinnedItem.getId());
    }

    /**
     * Remove pinned item
     */
    public void removePinning() {
        onPinningChanged(PinBehaviourType.UNPINNED, pinnedItem);
        pinnedItem = null;
    }

    /**
     * Add the pinned item
     */
    public void reApplyPinning() {
        onPinningChanged(PinBehaviourType.PINNED, pinnedItem);
    }

    /**
     * Enum constants for pin behaviour type
     */
    public enum PinBehaviourType {
        PINNED, UNPINNED;
    }
}
