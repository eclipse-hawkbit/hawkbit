/**
 * Copyright (c) 2020 Red Hat Inc and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.eclipse.hawkbit.ui.utils;

import org.springframework.data.domain.Sort;

public final class Sorts {

    private Sorts() {
    }

    public static Sort from(final boolean[] sortStates, final Object[] sortIds) {
        Sort sort = from(sortStates[0], sortIds[0]);

        for (int targetId = 1; targetId < sortIds.length; targetId++) {
            sort.and(from(sortStates[targetId], sortIds[targetId]));
        }

        return sort;
    }

    public static Sort from(final boolean state, final Object id) {
        final Sort sort = Sort.by((String)id);
        if (state) {
            return sort.ascending();
        } else {
            return sort.descending();
        }
    }


}
