/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.mgmt.client.resource.builder;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.hawkbit.rest.resource.model.softwaremoduletype.SoftwareModuleTypeRequestBodyPost;
import org.eclipse.hawkbit.rest.resource.model.target.TargetRequestBody;

import com.google.common.collect.Lists;

/**
 * 
 * Builder pattern for building {@link TargetRequestBody}.
 * 
 * @author Jonathan Knoblauch
 *
 */
public class TargetBuilder {

    private String controllerId;
    private String name;
    private String description;

    /**
     * @param controllerId
     *            the ID of the controller/target
     * @return the builder itself
     */
    public TargetBuilder controllerId(final String controllerId) {
        this.controllerId = controllerId;
        return this;
    }

    /**
     * @param name
     *            the name of the target
     * @return the builder itself
     */
    public TargetBuilder name(final String name) {
        this.name = name;
        return this;
    }

    /**
     * @param description
     *            the description of the target
     * @return the builder itself
     */
    public TargetBuilder description(final String description) {
        this.description = description;
        return this;
    }

    /**
     * Builds a list with a single entry of {@link TargetRequestBody} which can
     * directly be used in the RESTful-API.
     * 
     * @return a single entry list of {@link TargetRequestBody}
     */
    public List<TargetRequestBody> build() {
        return Lists.newArrayList(doBuild(controllerId));
    }

    /**
     * Builds a list of multiple {@link TargetRequestBody} to create multiple
     * targets at once. An increasing number will be added to the controllerId
     * of the target. The name and description will remain.
     * 
     * @param count
     *            the amount of software module type bodies which should be
     *            created
     * @return a list of {@link SoftwareModuleTypeRequestBodyPost}
     */
    public List<TargetRequestBody> buildAsList(final int count) {
        final ArrayList<TargetRequestBody> bodyList = Lists.newArrayList();
        for (int index = 0; index < count; index++) {
            bodyList.add(doBuild(controllerId + index));
        }
        return bodyList;
    }

    private TargetRequestBody doBuild(final String prefixControllerId) {
        final TargetRequestBody body = new TargetRequestBody();
        body.setControllerId(prefixControllerId);
        body.setName(name);
        body.setDescription(description);
        return body;
    }

}
