package org.eclipse.hawkbit.rest.resource.model.system;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * A json annotated rest model for System Configuration for PUT.
 */
@JsonInclude(Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class SystemConfigurationRequestBodyPut {

    @JsonProperty
    private String pollingTime;

    @JsonProperty
    private String pollingOverdueTime;

    @JsonProperty
    private String defaultDistributionSetType;

    @JsonProperty
    private Map<String, Object> authenticationConfiguration;

    /**
     * Gets the polling time.
     *
     * @return the polling time
     */
    public String getPollingTime() {
        return pollingTime;
    }

    /**
     * Sets the polling time.
     *
     * @param pollingTime
     *            the new polling time
     */
    public void setPollingTime(String pollingTime) {
        this.pollingTime = pollingTime;
    }

    /**
     * Gets the polling overdue time.
     *
     * @return the polling overdue time
     */
    public String getPollingOverdueTime() {
        return pollingOverdueTime;
    }

    /**
     * Sets the polling overdue time.
     *
     * @param pollingOverdueTime
     *            the new polling overdue time
     */
    public void setPollingOverdueTime(String pollingOverdueTime) {
        this.pollingOverdueTime = pollingOverdueTime;
    }

    /**
     * Gets the default distribution set type.
     *
     * @return the default distribution set type
     */
    public String getDefaultDistributionSetType() {
        return defaultDistributionSetType;
    }

    /**
     * Sets the default distribution set type.
     *
     * @param defaultDistributionSetType
     *            the new default distribution set type
     */
    public void setDefaultDistributionSetType(String defaultDistributionSetType) {
        this.defaultDistributionSetType = defaultDistributionSetType;
    }

    /**
     * Gets the authentication configuration.
     *
     * @return the authentication configuration
     */
    public Map<String, Object> getAuthenticationConfiguration() {
        return authenticationConfiguration;
    }

    /**
     * Sets the authentication configuration.
     *
     * @param authenticationConfiguration
     *            the authentication configuration
     */
    public void setAuthenticationConfiguration(Map<String, Object> authenticationConfiguration) {
        this.authenticationConfiguration = authenticationConfiguration;
    }

}
