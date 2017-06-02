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

import org.eclipse.hawkbit.mgmt.json.model.softwaremodule.MgmtSoftwareModuleRequestBodyPost;
import org.eclipse.hawkbit.mgmt.json.model.softwaremoduletype.MgmtSoftwareModuleTypeRequestBodyPost;

/**
 * 
 * Builder pattern for building {@link MgmtSoftwareModuleRequestBodyPost}.
 *
 */
// Exception squid:S1701 - builder pattern
@SuppressWarnings({ "squid:S1701" })
public class SoftwareModuleTypeBuilder {

    private String key;
    private String name;
    private String description;
    private int maxAssignments;

    /**
     * @param key
     *            the key of the software module type
     * @return the builder itself
     */
    public SoftwareModuleTypeBuilder key(final String key) {
        this.key = key;
        return this;
    }

    /**
     * @param name
     *            the name of the software module type
     * @return the builder itself
     */
    public SoftwareModuleTypeBuilder name(final String name) {
        this.name = name;
        return this;
    }

    /**
     * @param description
     *            of the module
     * @return the builder itself
     */
    public SoftwareModuleTypeBuilder description(final String description) {
        this.description = description;
        return this;
    }

    /**
     * @param maxAssignments
     *            of a module of that type to the same distribution set
     * @return the builder itself
     */
    public SoftwareModuleTypeBuilder maxAssignments(final int maxAssignments) {
        this.maxAssignments = maxAssignments;
        return this;
    }

    /**
     * Builds a list with a single entry of
     * {@link MgmtSoftwareModuleTypeRequestBodyPost} which can directly be used
     * in the RESTful-API.
     * 
     * @return a single entry list of
     *         {@link MgmtSoftwareModuleTypeRequestBodyPost}
     */
    public List<MgmtSoftwareModuleTypeRequestBodyPost> build() {
        return Arrays.asList(doBuild(""));
    }

    /**
     * Builds a list of multiple {@link MgmtSoftwareModuleTypeRequestBodyPost}
     * to create multiple software module types at once. An increasing number
     * will be added to the name and key of the software module type.
     * 
     * @param count
     *            the amount of software module type bodies which should be
     *            created
     * @return a list of {@link MgmtSoftwareModuleTypeRequestBodyPost}
     */
    public List<MgmtSoftwareModuleTypeRequestBodyPost> buildAsList(final int count) {
        final List<MgmtSoftwareModuleTypeRequestBodyPost> bodyList = new ArrayList<>();
        for (int index = 0; index < count; index++) {
            bodyList.add(doBuild(String.valueOf(index)));
        }
        return bodyList;
    }

    private MgmtSoftwareModuleTypeRequestBodyPost doBuild(final String suffix) {
        final MgmtSoftwareModuleTypeRequestBodyPost body = new MgmtSoftwareModuleTypeRequestBodyPost();
        body.setKey(key + suffix);
        body.setName(name + suffix);
        body.setDescription(description);
        body.setMaxAssignments(maxAssignments);
        return body;
    }

}
