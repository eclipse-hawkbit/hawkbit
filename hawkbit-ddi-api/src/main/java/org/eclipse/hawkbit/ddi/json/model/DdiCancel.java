/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ddi.json.model;

import javax.validation.constraints.NotNull;

/**
 * Cancel action to be provided to the target.
 */
public class DdiCancel {

    private final String id;

    @NotNull
    private final DdiCancelActionToStop cancelAction;

    /**
     * Parameterized constructor.
     *
     * @param id
     *            of the cancel action
     * @param cancelAction
     *            the action
     */
    public DdiCancel(final String id, final DdiCancelActionToStop cancelAction) {
        super();
        this.id = id;
        this.cancelAction = cancelAction;
    }

    public String getId() {
        return id;
    }

    public DdiCancelActionToStop getCancelAction() {
        return cancelAction;
    }

    @Override
    public String toString() {
        return "Cancel [id=" + id + ", cancelAction=" + cancelAction + "]";
    }

}
