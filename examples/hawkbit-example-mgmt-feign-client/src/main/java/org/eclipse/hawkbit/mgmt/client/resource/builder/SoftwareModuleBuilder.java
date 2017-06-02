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

import org.eclipse.hawkbit.mgmt.json.model.distributionsettype.MgmtDistributionSetTypeRequestBodyPost;
import org.eclipse.hawkbit.mgmt.json.model.softwaremodule.MgmtSoftwareModuleRequestBodyPost;

/**
 * 
 * Builder pattern for building {@link MgmtSoftwareModuleRequestBodyPost}.
 *
 */
// Exception squid:S1701 - builder pattern
@SuppressWarnings({ "squid:S1701" })
public class SoftwareModuleBuilder {

    private String name;
    private String version;
    private String type;
    private String vendor;
    private String description;

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
     * @param vendor
     *            the vendor
     * @return the builder itself
     */
    public SoftwareModuleBuilder vendor(final String vendor) {
        this.vendor = vendor;
        return this;
    }

    /**
     * @param description
     *            the description
     * @return the builder itself
     */
    public SoftwareModuleBuilder description(final String description) {
        this.description = description;
        return this;
    }

    /**
     * Builds a list with a single entry of
     * {@link MgmtSoftwareModuleRequestBodyPost} which can directly be used in
     * the RESTful-API.
     * 
     * @return a single entry list of {@link MgmtSoftwareModuleRequestBodyPost}
     */
    public List<MgmtSoftwareModuleRequestBodyPost> build() {
        return Arrays.asList(doBuild(""));
    }

    /**
     * Builds a list of multiple {@link MgmtSoftwareModuleRequestBodyPost} to
     * create multiple software module at once. An increasing number will be
     * added to the version of the software module. The name and type will
     * remain the same.
     * 
     * @param count
     *            the amount of software module body which should be created
     * @return a list of {@link MgmtDistributionSetTypeRequestBodyPost}
     */
    public List<MgmtSoftwareModuleRequestBodyPost> buildAsList(final int count) {
        final List<MgmtSoftwareModuleRequestBodyPost> bodyList = new ArrayList<>();
        for (int index = 0; index < count; index++) {
            bodyList.add(doBuild(String.valueOf(index)));
        }

        return bodyList;
    }

    private MgmtSoftwareModuleRequestBodyPost doBuild(final String suffix) {
        final MgmtSoftwareModuleRequestBodyPost body = new MgmtSoftwareModuleRequestBodyPost();
        body.setName(name);
        body.setVersion(version + suffix);
        body.setType(type);
        body.setVendor(vendor);
        body.setDescription(description);
        return body;
    }

}
