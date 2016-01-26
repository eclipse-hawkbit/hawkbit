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
    private Map<String, Object> configuration;

    /**
     * Sets the authentication configuration.
     *
     * @param authenticationConfiguration
     *            the authentication configuration
     */
    public void setAuthenticationConfiguration(Map<String, Object> authenticationConfiguration) {
        this.configuration = authenticationConfiguration;
    }

    /**
     * Gets the authentication configuration.
     *
     * @return the authentication configuration
     */
    public Map<String, Object> getAuthenticationConfiguration() {
        return this.configuration;
    }
}
