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

import org.eclipse.hawkbit.mgmt.json.model.distributionset.MgmtDistributionSetRequestBodyPost;
import org.eclipse.hawkbit.mgmt.json.model.softwaremodule.MgmtSoftwareModuleAssigment;

/**
 * Builder pattern for building {@link MgmtDistributionSetRequestBodyPost}.
 */
// Exception squid:S1701 - builder pattern
@SuppressWarnings({ "squid:S1701" })
public class DistributionSetBuilder {

    private String name;
    private String version;
    private String type;
    private String description;
    private final List<MgmtSoftwareModuleAssigment> modules = new ArrayList<>();

    /**
     * @param name
     *            the name of the distribution set
     * @return the builder itself
     */
    public DistributionSetBuilder name(final String name) {
        this.name = name;
        return this;
    }

    public DistributionSetBuilder moduleByID(final Long id) {
        final MgmtSoftwareModuleAssigment softwareModuleAssigmentRest = new MgmtSoftwareModuleAssigment();
        softwareModuleAssigmentRest.setId(id);
        modules.add(softwareModuleAssigmentRest);
        return this;
    }

    /**
     * @param version
     *            the version of the distribution set
     * @return the builder itself
     */
    public DistributionSetBuilder version(final String version) {
        this.version = version;
        return this;
    }

    /**
     * @param type
     *            the distribution set type name for this distribution set
     * @return the builder itself
     */
    public DistributionSetBuilder type(final String type) {
        this.type = type;
        return this;
    }

    /**
     * @param description
     *            the description
     * @return the builder itself
     */
    public DistributionSetBuilder description(final String description) {
        this.description = description;
        return this;
    }

    /**
     * Builds a list with a single entry of
     * {@link MgmtDistributionSetRequestBodyPost} which can directly be used to
     * post on the RESTful-API.
     * 
     * @return a single entry list of {@link MgmtDistributionSetRequestBodyPost}
     */
    public List<MgmtDistributionSetRequestBodyPost> build() {
        return Arrays.asList(doBuild(""));
    }

    /**
     * Builds a list of multiple {@link MgmtDistributionSetRequestBodyPost} to
     * create multiple distribution sets at once. An increasing number will be
     * used for version of the distribution set. The name and type will remain
     * the same.
     * 
     * @param count
     *            the amount of distribution sets body which should be created
     * @return a list of {@link MgmtDistributionSetRequestBodyPost}
     */
    public List<MgmtDistributionSetRequestBodyPost> buildAsList(final int count) {
        return buildAsList(0, count);
    }

    /**
     * Builds a list of multiple {@link MgmtDistributionSetRequestBodyPost} to
     * create multiple distribution sets at once. An increasing number will be
     * used for version of the distribution set starting from given offset. The
     * name and type will remain the same.
     * 
     * @param count
     *            the amount of distribution sets body which should be created
     * @param offset
     *            for for index start
     * @return a list of {@link MgmtDistributionSetRequestBodyPost}
     */
    public List<MgmtDistributionSetRequestBodyPost> buildAsList(final int offset, final int count) {
        final List<MgmtDistributionSetRequestBodyPost> bodyList = new ArrayList<>();
        for (int index = offset; index < count + offset; index++) {
            bodyList.add(doBuild(String.valueOf(index)));
        }

        return bodyList;
    }

    private MgmtDistributionSetRequestBodyPost doBuild(final String suffix) {
        final MgmtDistributionSetRequestBodyPost body = new MgmtDistributionSetRequestBodyPost();
        body.setName(name);
        body.setVersion(version + suffix);
        body.setType(type);
        body.setDescription(description);
        body.setModules(modules);
        return body;
    }

}
