/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.controller.model;

import javax.validation.constraints.NotNull;

import org.eclipse.hawkbit.rest.resource.model.doc.ApiModelProperties;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

/**
 * Cancel action to be provided to the target.
 *
 *
 *
 */
@ApiModel("SP Target Cancel Action")
public class Cancel {
    @ApiModelProperty(value = ApiModelProperties.ACTION_ID)
    private final String id;

    @ApiModelProperty(value = ApiModelProperties.CANCEL_ACTION, required = true)
    @NotNull
    private final CancelActionToStop cancelAction;

    /**
     * Parameterized constructor.
     * 
     * @param id
     *            of the {@link CancelAction}
     * @param cancelAction
     *            the action
     */
    public Cancel(final String id, final CancelActionToStop cancelAction) {
        super();
        this.id = id;
        this.cancelAction = cancelAction;
    }

    /**
     * @return the id
     */
    public String getId() {
        return id;
    }

    /**
     * @return the cancelAction
     */
    public CancelActionToStop getCancelAction() {
        return cancelAction;
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return "Cancel [id=" + id + ", cancelAction=" + cancelAction + "]";
    }

}
