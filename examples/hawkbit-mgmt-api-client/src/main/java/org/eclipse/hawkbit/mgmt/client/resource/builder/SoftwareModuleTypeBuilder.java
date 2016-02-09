/**
 * Copyright (c) 2011-2016 Bosch Software Innovations GmbH, Germany. All rights reserved.
 */
package org.eclipse.hawkbit.mgmt.client.resource.builder;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.hawkbit.rest.resource.model.softwaremodule.SoftwareModuleRequestBodyPost;
import org.eclipse.hawkbit.rest.resource.model.softwaremoduletype.SoftwareModuleTypeRequestBodyPost;

import com.google.common.collect.Lists;

/**
 * 
 * Builder pattern for building {@link SoftwareModuleRequestBodyPost}.
 * 
 * @author Jonathan Knoblauch
 *
 */
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

    public SoftwareModuleTypeBuilder description(final String description) {
        this.description = description;
        return this;
    }

    public SoftwareModuleTypeBuilder maxAssignments(final int maxAssignments) {
        this.maxAssignments = maxAssignments;
        return this;
    }

    /**
     * Builds a list with a single entry of
     * {@link SoftwareModuleTypeRequestBodyPost} which can directly be used in
     * the RESTful-API.
     * 
     * @return a single entry list of {@link SoftwareModuleTypeRequestBodyPost}
     */
    public List<SoftwareModuleTypeRequestBodyPost> build() {
        return Lists.newArrayList(doBuild(key, name));
    }

    /**
     * Builds a list of multiple {@link SoftwareModuleTypeRequestBodyPost} to
     * create multiple software module types at once. An increasing number will
     * be added to the name and key of the software module type.
     * 
     * @param count
     *            the amount of software module type bodies which should be
     *            created
     * @return a list of {@link SoftwareModuleTypeRequestBodyPost}
     */
    public List<SoftwareModuleTypeRequestBodyPost> buildAsList(final int count) {
        final ArrayList<SoftwareModuleTypeRequestBodyPost> bodyList = Lists.newArrayList();
        for (int index = 0; index < count; index++) {
            bodyList.add(doBuild(key + index, name + index));
        }
        return bodyList;
    }

    private SoftwareModuleTypeRequestBodyPost doBuild(final String prefixKey, final String prefixName) {
        final SoftwareModuleTypeRequestBodyPost body = new SoftwareModuleTypeRequestBodyPost();
        body.setKey(prefixKey);
        body.setName(prefixName);
        body.setDescription(description);
        body.setMaxAssignments(maxAssignments);
        return body;
    }

}