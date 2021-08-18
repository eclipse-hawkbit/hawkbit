/**
 * Copyright (c) 2021 Bosch.IO GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.model;

import java.util.Collection;

public class DistributionSetInvalidation {

    private Collection<Long> setIds;
    private CancelationType cancelationType;
    private boolean cancelRollouts;

    public DistributionSetInvalidation(final Collection<Long> setIds, final CancelationType cancelationType,
            final boolean cancelRollouts) {
        this.setIds = setIds;
        this.cancelationType = cancelationType;
        this.cancelRollouts = cancelRollouts;
    }

    public Collection<Long> getSetIds() {
        return setIds;
    }

    public void setSetIds(final Collection<Long> setIds) {
        this.setIds = setIds;
    }

    public CancelationType getCancelationType() {
        return cancelationType;
    }

    public void setCancelationType(final CancelationType cancelationType) {
        this.cancelationType = cancelationType;
    }

    public boolean isCancelRollouts() {
        return cancelRollouts;
    }

    public void setCancelRollouts(final boolean cancelRollouts) {
        this.cancelRollouts = cancelRollouts;
    }

    public enum CancelationType {
        FORCE, SOFT, NONE;
    }

}
