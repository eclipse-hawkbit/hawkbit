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
import java.util.Arrays;
import java.util.List;

import org.eclipse.hawkbit.mgmt.json.model.softwaremoduletype.MgmtSoftwareModuleTypeRequestBodyPost;
import org.eclipse.hawkbit.mgmt.json.model.target.MgmtTargetRequestBody;

/**
 * 
 * Builder pattern for building {@link MgmtTargetRequestBody}.
 *
 */
// Exception squid:S1701 - builder pattern
@SuppressWarnings({ "squid:S1701" })
public class TargetBuilder {

    private String controllerId;
    private String name;
    private String description;
    private String address;

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
     * @param address
     *            the address of the target
     * @return the builder itself
     */
    public TargetBuilder address(final String address) {
        this.address = address;
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
     * Builds a list with a single entry of {@link MgmtTargetRequestBody} which
     * can directly be used in the RESTful-API.
     * 
     * @return a single entry list of {@link MgmtTargetRequestBody}
     */
    public List<MgmtTargetRequestBody> build() {
        return Arrays.asList(doBuild(""));
    }

    /**
     * Builds a list of multiple {@link MgmtTargetRequestBody} to create
     * multiple targets at once. An increasing number will be added to the
     * controllerId and name of the target. The description will remain.
     * 
     * @param count
     *            the amount of target bodies which should be created
     * @return a list of {@link MgmtSoftwareModuleTypeRequestBodyPost}
     */
    public List<MgmtTargetRequestBody> buildAsList(final int count) {

        return buildAsList(0, count);
    }

    /**
     * Builds a list of multiple {@link MgmtTargetRequestBody} to create
     * multiple targets at once. An increasing number will be added to the
     * controllerId and name of the target starting from the provided offset.
     * The description will remain.
     * 
     * @param count
     *            the amount of target bodies which should be created
     * @param offset
     *            for for index start
     * @return a list of {@link MgmtSoftwareModuleTypeRequestBodyPost}
     */
    public List<MgmtTargetRequestBody> buildAsList(final int offset, final int count) {
        final List<MgmtTargetRequestBody> bodyList = new ArrayList<>();
        for (int index = offset; index < count + offset; index++) {
            bodyList.add(doBuild(String.format("%06d", index)));
        }
        return bodyList;
    }

    private MgmtTargetRequestBody doBuild(final String suffix) {
        final MgmtTargetRequestBody body = new MgmtTargetRequestBody();
        body.setControllerId(controllerId + suffix);
        if (name == null) {
            name = controllerId;
        }
        body.setName(name + suffix);
        body.setDescription(description);
        body.setAddress(address);
        return body;
    }

}
