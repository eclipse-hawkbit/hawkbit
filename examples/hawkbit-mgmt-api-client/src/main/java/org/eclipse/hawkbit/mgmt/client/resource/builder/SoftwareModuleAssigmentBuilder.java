/**
 * Copyright (c) 2011-2016 Bosch Software Innovations GmbH, Germany. All rights reserved.
 */
package org.eclipse.hawkbit.mgmt.client.resource.builder;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.hawkbit.rest.resource.model.softwaremodule.SoftwareModuleAssigmentRest;

/**
 * 
 * Builder pattern for building {@link SoftwareModuleAssigmentRest}.
 * 
 * @author Jonathan Knoblauch
 *
 */
public class SoftwareModuleAssigmentBuilder {

    private final List<Long> ids;

    public SoftwareModuleAssigmentBuilder() {
        ids = new ArrayList<Long>();
    }

    /**
     * @param id
     *            the id of the software module
     * @return the builder itself
     */
    public SoftwareModuleAssigmentBuilder id(final Long id) {
        ids.add(id);
        return this;
    }

    /**
     * Builds a list with a single entry of {@link SoftwareModuleAssigmentRest}
     * which can directly be used in the RESTful-API.
     * 
     * @return a single entry list of {@link SoftwareModuleAssigmentRest}
     */
    public List<SoftwareModuleAssigmentRest> build() {
        final List<SoftwareModuleAssigmentRest> softwareModuleAssigmentRestList = new ArrayList<>();
        for (final Long id : ids) {
            final SoftwareModuleAssigmentRest softwareModuleAssigmentRest = new SoftwareModuleAssigmentRest();
            softwareModuleAssigmentRest.setId(id);
            softwareModuleAssigmentRestList.add(softwareModuleAssigmentRest);
        }
        return softwareModuleAssigmentRestList;
    }

}
