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
import org.eclipse.hawkbit.rest.resource.model.softwaremoduletype.SoftwareModuleTypeAssigmentRest;

import com.google.common.collect.Lists;

/**
 * 
 * Builder pattern for building {@link DistributionSetTypeRequestBodyPost}.
 * 
 * @author Jonathan Knoblauch
 *
 */
public class DistributionSetTypeBuilder {

    private String key;
    private String name;
    private final List<SoftwareModuleTypeAssigmentRest> mandatorymodules = Lists.newArrayList();
    private final List<SoftwareModuleTypeAssigmentRest> optionalmodules = Lists.newArrayList();

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
     * @param softwareModuleTypeIds
     *            the IDs of the software module types which should be mandatory
     *            for the distribution set type
     * @return the builder itself
     */
    public DistributionSetTypeBuilder mandatorymodules(final Long... softwareModuleTypeIds) {
        for (final Long id : softwareModuleTypeIds) {
            final SoftwareModuleTypeAssigmentRest softwareModuleTypeAssigmentRest = new SoftwareModuleTypeAssigmentRest();
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
            final SoftwareModuleTypeAssigmentRest softwareModuleTypeAssigmentRest = new SoftwareModuleTypeAssigmentRest();
            softwareModuleTypeAssigmentRest.setId(id);
            this.optionalmodules.add(softwareModuleTypeAssigmentRest);
        }
        return this;
    }

    /**
     * Builds a list with a single entry of
     * {@link DistributionSetTypeRequestBodyPost} which can directly be used in
     * the RESTful-API.
     * 
     * @return a single entry list of {@link DistributionSetTypeRequestBodyPost}
     */
    public List<DistributionSetTypeRequestBodyPost> build() {
        return Lists.newArrayList(doBuild(name, key));
    }

    /**
     * Builds a list of multiple {@link DistributionSetTypeRequestBodyPost} to
     * create multiple distribution set types at once. An increasing number will
     * be added to the name and key of the distribution set type. The optional
     * and mandatory software module types will remain the same.
     * 
     * @param count
     *            the amount of distribution sets type body which should be
     *            created
     * @return a list of {@link DistributionSetTypeRequestBodyPost}
     */
    public List<DistributionSetTypeRequestBodyPost> buildAsList(final int count) {
        final ArrayList<DistributionSetTypeRequestBodyPost> bodyList = Lists.newArrayList();
        for (int index = 0; index < count; index++) {
            bodyList.add(doBuild(name + index, key + index));
        }
        return bodyList;

    }

    private DistributionSetTypeRequestBodyPost doBuild(final String prefixName, final String prefixKey) {
        final DistributionSetTypeRequestBodyPost body = new DistributionSetTypeRequestBodyPost();
        body.setKey(prefixKey);
        body.setName(prefixName);
        body.setMandatorymodules(mandatorymodules);
        body.setOptionalmodules(optionalmodules);
        return body;
    }

}
