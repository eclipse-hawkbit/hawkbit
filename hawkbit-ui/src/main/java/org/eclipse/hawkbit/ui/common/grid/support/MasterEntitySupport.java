/**
 * Copyright (c) 2020 Bosch.IO GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.eclipse.hawkbit.ui.common.grid.support;

import java.util.function.Consumer;
import java.util.function.Function;

import org.eclipse.hawkbit.ui.common.data.proxies.ProxyIdentifiableEntity;
import org.eclipse.hawkbit.ui.common.event.FilterType;
import org.eclipse.hawkbit.ui.common.layout.MasterEntityAwareComponent;

/**
 * Filter support in Master entity
 *
 * @param <M>
 *            Generic type of master entity
 */
public class MasterEntitySupport<M extends ProxyIdentifiableEntity> implements MasterEntityAwareComponent<M> {
    private final FilterSupport<?, ?> filterSupport;
    private final Function<M, ?> masterEntityToFilterMapper;
    private final Consumer<M> postMasterChangeCallback;

    private Long masterId;

    /**
     * Constructor for MasterEntitySupport
     *
     * @param filterSupport
     *            Filter support
     */
    public MasterEntitySupport(final FilterSupport<?, ?> filterSupport) {
        this(filterSupport, null);
    }

    /**
     * Constructor for MasterEntitySupport
     *
     * @param filterSupport
     *            Filter support
     * @param masterEntityToFilterMapper
     *            Master entity to filter mapper
     */
    public MasterEntitySupport(final FilterSupport<?, ?> filterSupport,
            final Function<M, ?> masterEntityToFilterMapper) {
        this(filterSupport, masterEntityToFilterMapper, null);
    }

    /**
     * Constructor for MasterEntitySupport
     *
     * @param filterSupport
     *            Filter support
     * @param masterEntityToFilterMapper
     *            Master entity to filter mapper
     * @param postMasterChangeCallback
     *            Callback called after master entity change
     */
    public MasterEntitySupport(final FilterSupport<?, ?> filterSupport, final Function<M, ?> masterEntityToFilterMapper,
            final Consumer<M> postMasterChangeCallback) {
        this.filterSupport = filterSupport;
        this.masterEntityToFilterMapper = masterEntityToFilterMapper;
        this.postMasterChangeCallback = postMasterChangeCallback;
    }

    @Override
    public void masterEntityChanged(final M masterEntity) {
        if ((masterEntity == null && masterId == null) || !filterSupport.isFilterTypeSupported(FilterType.MASTER)) {
            return;
        }

        filterSupport.updateFilter(FilterType.MASTER, getMasterEntityFilter(masterEntity));

        masterId = masterEntity != null ? masterEntity.getId() : null;

        if (postMasterChangeCallback != null) {
            postMasterChangeCallback.accept(masterEntity);
        }
    }

    private Object getMasterEntityFilter(final M masterEntity) {
        if (masterEntity == null) {
            return null;
        }

        return masterEntityToFilterMapper != null ? masterEntityToFilterMapper.apply(masterEntity)
                : masterEntity.getId();
    }

    /**
     * @return Id of master entity
     */
    public Long getMasterId() {
        return masterId;
    }
}
