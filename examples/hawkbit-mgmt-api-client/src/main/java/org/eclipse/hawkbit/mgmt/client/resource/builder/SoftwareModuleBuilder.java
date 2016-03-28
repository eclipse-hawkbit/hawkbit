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

import org.eclipse.hawkbit.rest.resource.model.distributionsettype.DistributionSetTypeRequestBodyPost;
import org.eclipse.hawkbit.rest.resource.model.softwaremodule.SoftwareModuleRequestBodyPost;

import com.google.common.collect.Lists;

/**
 * 
 * Builder pattern for building {@link SoftwareModuleRequestBodyPost}.
 * 
 * @author Jonathan Knoblauch
 *
 */
public class SoftwareModuleBuilder {

    private String name;
    private String version;
    private String type;

    /**
     * @param name
     *            the name of the software module
     * @return the builder itself
     */
    public SoftwareModuleBuilder name(final String name) {
        this.name = name;
        return this;
    }

    /**
     * @param version
     *            the version of the software module
     * @return the builder itsefl
     */
    public SoftwareModuleBuilder version(final String version) {
        this.version = version;
        return this;
    }

    /**
     * @param type
     *            the key of the software module type to be used for this
     *            software module
     * @return the builder itself
     */
    public SoftwareModuleBuilder type(final String type) {
        this.type = type;
        return this;
    }

    /**
     * Builds a list with a single entry of
     * {@link SoftwareModuleRequestBodyPost} which can directly be used in the
     * RESTful-API.
     * 
     * @return a single entry list of {@link SoftwareModuleRequestBodyPost}
     */
    public List<SoftwareModuleRequestBodyPost> build() {
        return Lists.newArrayList(doBuild(name));
    }

    /**
     * Builds a list of multiple {@link SoftwareModuleRequestBodyPost} to create
     * multiple software module at once. An increasing number will be added to
     * the name of the software module. The version and type will remain the
     * same.
     * 
     * @param count
     *            the amount of software module body which should be created
     * @return a list of {@link DistributionSetTypeRequestBodyPost}
     */
    public List<SoftwareModuleRequestBodyPost> buildAsList(final int count) {
        final ArrayList<SoftwareModuleRequestBodyPost> bodyList = Lists.newArrayList();
        for (int index = 0; index < count; index++) {
            bodyList.add(doBuild(name + index));
        }

        return bodyList;
    }

    private SoftwareModuleRequestBodyPost doBuild(final String prefixName) {
        final SoftwareModuleRequestBodyPost body = new SoftwareModuleRequestBodyPost();
        body.setName(prefixName);
        body.setVersion(version);
        body.setType(type);
        return body;
    }

}
