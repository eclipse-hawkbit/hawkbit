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
import org.eclipse.hawkbit.mgmt.json.model.softwaremoduletype.MgmtSoftwareModuleTypeAssigment;

/**
 * 
 * Builder pattern for building {@link MgmtDistributionSetTypeRequestBodyPost}.
 *
 */
// Exception squid:S1701 - builder pattern
@SuppressWarnings({ "squid:S1701" })
public class DistributionSetTypeBuilder {

    private String key;
    private String name;
    private String description;
    private final List<MgmtSoftwareModuleTypeAssigment> mandatorymodules = new ArrayList<>();
    private final List<MgmtSoftwareModuleTypeAssigment> optionalmodules = new ArrayList<>();

    /**
     * @param key
     *            the key of the distribution set type
     * @return the builder itself
     */
    public DistributionSetTypeBuilder key(final String key) {
        this.key = key;
        return this;
    }

    /**
     * @param name
     *            the name of the distribution set type
     * @return the builder itself
     */
    public DistributionSetTypeBuilder name(final String name) {
        this.name = name;
        return this;
    }

    /**
     * @param description
     *            the description
     * @return the builder itself
     */
    public DistributionSetTypeBuilder description(final String description) {
        this.description = description;
        return this;
    }

    /**
     * @param softwareModuleTypeIds
     *            the IDs of the software module types which should be mandatory
     *            for the distribution set type
     * @return the builder itself
     */
    public DistributionSetTypeBuilder mandatorymodules(final Long... softwareModuleTypeIds) {
        for (final Long id : softwareModuleTypeIds) {
            final MgmtSoftwareModuleTypeAssigment softwareModuleTypeAssigmentRest = new MgmtSoftwareModuleTypeAssigment();
            softwareModuleTypeAssigmentRest.setId(id);
            this.mandatorymodules.add(softwareModuleTypeAssigmentRest);
        }
        return this;
    }

    /**
     * 
     * @param softwareModuleTypeIds
     *            the IDs of the software module types which should be optional
     *            for the distribution set type
     * @return the builder itself
     */
    public DistributionSetTypeBuilder optionalmodules(final Long... softwareModuleTypeIds) {
        for (final Long id : softwareModuleTypeIds) {
            final MgmtSoftwareModuleTypeAssigment softwareModuleTypeAssigmentRest = new MgmtSoftwareModuleTypeAssigment();
            softwareModuleTypeAssigmentRest.setId(id);
            this.optionalmodules.add(softwareModuleTypeAssigmentRest);
        }
        return this;
    }

    /**
     * Builds a list with a single entry of
     * {@link MgmtDistributionSetTypeRequestBodyPost} which can directly be used
     * in the RESTful-API.
     * 
     * @return a single entry list of
     *         {@link MgmtDistributionSetTypeRequestBodyPost}
     */
    public List<MgmtDistributionSetTypeRequestBodyPost> build() {
        return Arrays.asList(doBuild(""));
    }

    /**
     * Builds a list of multiple {@link MgmtDistributionSetTypeRequestBodyPost}
     * to create multiple distribution set types at once. An increasing number
     * will be added to the name and key of the distribution set type. The
     * optional and mandatory software module types will remain the same.
     * 
     * @param count
     *            the amount of distribution sets type body which should be
     *            created
     * @return a list of {@link MgmtDistributionSetTypeRequestBodyPost}
     */
    public List<MgmtDistributionSetTypeRequestBodyPost> buildAsList(final int count) {
        final List<MgmtDistributionSetTypeRequestBodyPost> bodyList = new ArrayList<>();
        for (int index = 0; index < count; index++) {
            bodyList.add(doBuild(String.valueOf(index)));
        }
        return bodyList;

    }

    private MgmtDistributionSetTypeRequestBodyPost doBuild(final String suffix) {
        final MgmtDistributionSetTypeRequestBodyPost body = new MgmtDistributionSetTypeRequestBodyPost();
        body.setKey(key + suffix);
        body.setName(name + suffix);
        body.setDescription(description);
        body.setMandatorymodules(mandatorymodules);
        body.setOptionalmodules(optionalmodules);
        return body;
    }

}
