/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.mgmt.json.model.distributionset;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import org.eclipse.hawkbit.mgmt.json.model.softwaremodule.MgmtSoftwareModuleAssigment;

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
public class MgmtDistributionSetRequestBodyPost extends MgmtDistributionSetRequestBodyPut {

    // deprecated format from the time where os, application and runtime where
    // statically defined
    @JsonProperty
    @Schema(hidden = true)
    private MgmtSoftwareModuleAssigment os;

    @JsonProperty
    @Schema(hidden = true)
    private MgmtSoftwareModuleAssigment runtime;

    @JsonProperty
    @Schema(hidden = true)
    private MgmtSoftwareModuleAssigment application;
    // deprecated format - END

    @JsonProperty
    private List<MgmtSoftwareModuleAssigment> modules;

    @JsonProperty
    @Schema(example = "test_default_ds_type")
    private String type;

    /**
     * @return the os
     */
    public MgmtSoftwareModuleAssigment getOs() {
        return os;
    }

    /**
     * @param os
     *            the os to set
     *
     * @return updated body
     */
    public MgmtDistributionSetRequestBodyPost setOs(final MgmtSoftwareModuleAssigment os) {
        this.os = os;
        return this;
    }

    /**
     * @return the runtime
     */
    public MgmtSoftwareModuleAssigment getRuntime() {
        return runtime;
    }

    /**
     * @param runtime
     *            the runtime to set
     *
     * @return updated body
     */
    public MgmtDistributionSetRequestBodyPost setRuntime(final MgmtSoftwareModuleAssigment runtime) {
        this.runtime = runtime;

        return this;
    }

    /**
     * @return the application
     */
    public MgmtSoftwareModuleAssigment getApplication() {
        return application;
    }

    /**
     * @param application
     *            the application to set
     *
     * @return updated body
     */
    public MgmtDistributionSetRequestBodyPost setApplication(final MgmtSoftwareModuleAssigment application) {
        this.application = application;

        return this;
    }

    /**
     * @return the modules
     */
    public List<MgmtSoftwareModuleAssigment> getModules() {
        return modules;
    }

    /**
     * @param modules
     *            the modules to set
     *
     * @return updated body
     */
    public MgmtDistributionSetRequestBodyPost setModules(final List<MgmtSoftwareModuleAssigment> modules) {
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
    public MgmtDistributionSetRequestBodyPost setType(final String type) {
        this.type = type;

        return this;
    }

}
