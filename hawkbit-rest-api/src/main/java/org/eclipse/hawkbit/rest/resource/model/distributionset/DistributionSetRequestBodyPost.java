/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.rest.resource.model.distributionset;

import java.util.List;

import org.eclipse.hawkbit.rest.resource.model.softwaremodule.SoftwareModuleAssigmentRest;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * A json annotated rest model for DistributionSet for POST.
 *
 */
@JsonInclude(Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class DistributionSetRequestBodyPost extends DistributionSetRequestBodyPut {

    // deprecated format from the time where os, application and runtime where
    // statically defined
    @JsonProperty
    private SoftwareModuleAssigmentRest os;

    @JsonProperty
    private SoftwareModuleAssigmentRest runtime;

    @JsonProperty
    private SoftwareModuleAssigmentRest application;
    // deprecated format - END

    @JsonProperty
    private List<SoftwareModuleAssigmentRest> modules;

    @JsonProperty
    private boolean requiredMigrationStep;

    @JsonProperty
    private String type;

    /**
     * @return the os
     */
    public SoftwareModuleAssigmentRest getOs() {
        return os;
    }

    /**
     * @param os
     *            the os to set
     *
     * @return updated body
     */
    public DistributionSetRequestBodyPost setOs(final SoftwareModuleAssigmentRest os) {
        this.os = os;
        return this;
    }

    /**
     * @return the runtime
     */
    public SoftwareModuleAssigmentRest getRuntime() {
        return runtime;
    }

    /**
     * @param runtime
     *            the runtime to set
     *
     * @return updated body
     */
    public DistributionSetRequestBodyPost setRuntime(final SoftwareModuleAssigmentRest runtime) {
        this.runtime = runtime;

        return this;
    }

    /**
     * @return the application
     */
    public SoftwareModuleAssigmentRest getApplication() {
        return application;
    }

    /**
     * @param application
     *            the application to set
     *
     * @return updated body
     */
    public DistributionSetRequestBodyPost setApplication(final SoftwareModuleAssigmentRest application) {
        this.application = application;

        return this;
    }

    /**
     * @return the requiredMigrationStep
     */
    public boolean isRequiredMigrationStep() {
        return requiredMigrationStep;
    }

    /**
     * @param requiredMigrationStep
     *            the requiredMigrationStep to set
     *
     * @return updated body
     */
    public DistributionSetRequestBodyPost setRequiredMigrationStep(final boolean requiredMigrationStep) {
        this.requiredMigrationStep = requiredMigrationStep;

        return this;
    }

    /**
     * @return the modules
     */
    public List<SoftwareModuleAssigmentRest> getModules() {
        return modules;
    }

    /**
     * @param modules
     *            the modules to set
     *
     * @return updated body
     */
    public DistributionSetRequestBodyPost setModules(final List<SoftwareModuleAssigmentRest> modules) {
        this.modules = modules;

        return this;
    }

    /**
     * @return the type
     */
    public String getType() {
        return type;
    }

    /**
     * @param type
     *            the type to set
     *
     * @return updated body
     */
    public DistributionSetRequestBodyPost setType(final String type) {
        this.type = type;

        return this;
    }

}
