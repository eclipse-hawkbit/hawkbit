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

import org.eclipse.hawkbit.rest.resource.model.doc.ApiModelProperties;
import org.eclipse.hawkbit.rest.resource.model.softwaremodule.SoftwareModuleAssigmentRest;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

/**
 * A json annotated rest model for DistributionSet for POST.
 *
 *
 *
 *
 */
@JsonInclude(Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@ApiModel("Distribution set")
public class DistributionSetRequestBodyCreate extends DistributionSetRequestBodyUpdate {

    // deprecated format from the time where os, application and runtime where
    // statically defined
    @ApiModelProperty(hidden = true)
    @JsonProperty
    private SoftwareModuleAssigmentRest os;

    @ApiModelProperty(hidden = true)
    @JsonProperty
    private SoftwareModuleAssigmentRest runtime;

    @ApiModelProperty(hidden = true)
    @JsonProperty
    private SoftwareModuleAssigmentRest application;
    // deprecated format - END

    @ApiModelProperty(value = ApiModelProperties.DS_MODULES)
    @JsonProperty
    private List<SoftwareModuleAssigmentRest> modules;

    @ApiModelProperty(value = ApiModelProperties.DS_REQUIRED_STEP)
    @JsonProperty
    private boolean requiredMigrationStep;

    @ApiModelProperty(value = ApiModelProperties.DS_TYPE)
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
     */
    public void setOs(final SoftwareModuleAssigmentRest os) {
        this.os = os;
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
     */
    public void setRuntime(final SoftwareModuleAssigmentRest runtime) {
        this.runtime = runtime;
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
     */
    public void setApplication(final SoftwareModuleAssigmentRest application) {
        this.application = application;
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
     */
    public void setRequiredMigrationStep(final boolean requiredMigrationStep) {
        this.requiredMigrationStep = requiredMigrationStep;
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
     */
    public void setModules(final List<SoftwareModuleAssigmentRest> modules) {
        this.modules = modules;
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
     */
    public void setType(final String type) {
        this.type = type;
    }

}
