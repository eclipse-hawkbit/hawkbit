package org.eclipse.hawkbit.rest.resource.model.system;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * A json annotated rest model for SysteConfiguration to RESTful API
 * representation.
 *
 */
@JsonInclude(Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class SystemConfigurationRest {

    @JsonProperty
    private String pollingTime;

    @JsonProperty
    private String pollingOverdueTime;

    @JsonProperty
    private String defaultDistributionSetType;

    @JsonProperty
    private String createdBy;

    @JsonProperty
    private String lastModifiedBy;

    @JsonProperty
    private Long createdAt;

    @JsonProperty
    private Long lastModifiedAt;

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
     * Gets the created by.
     *
     * @return the created by
     */
    public String getCreatedBy() {
        return createdBy;
    }

    /**
     * Sets the created by.
     *
     * @param createdBy
     *            the new created by
     */
    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    /**
     * Gets the last modified by.
     *
     * @return the last modified by
     */
    public String getLastModifiedBy() {
        return lastModifiedBy;
    }

    /**
     * Sets the last modified by.
     *
     * @param lastModifiedBy
     *            the new last modified by
     */
    public void setLastModifiedBy(String lastModifiedBy) {
        this.lastModifiedBy = lastModifiedBy;
    }

    /**
     * Gets the created at.
     *
     * @return the created at
     */
    public Long getCreatedAt() {
        return createdAt;
    }

    /**
     * Sets the created at.
     *
     * @param createdAt
     *            the new created at
     */
    public void setCreatedAt(Long createdAt) {
        this.createdAt = createdAt;
    }

    /**
     * Gets the last modified at.
     *
     * @return the last modified at
     */
    public Long getLastModifiedAt() {
        return lastModifiedAt;
    }

    /**
     * Sets the last modified at.
     *
     * @param lastModifiedAt
     *            the new last modified at
     */
    public void setLastModifiedAt(Long lastModifiedAt) {
        this.lastModifiedAt = lastModifiedAt;
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

    /**
     * Gets the authentication configuration.
     *
     * @return the authentication configuration
     */
    public Map<String, Object> getAuthenticationConfiguration() {
        return this.authenticationConfiguration;
    }
}
