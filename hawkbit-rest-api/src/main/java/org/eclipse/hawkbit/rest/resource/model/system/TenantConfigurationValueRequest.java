package org.eclipse.hawkbit.rest.resource.model.system;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * A json annotated rest model for System Configuration for PUT.
 */
@JsonInclude(Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class TenantConfigurationValueRequest {

    @JsonProperty
    private Object value;

    /**
     * 
     * @return the value of the TenantConfigurationValueRequest
     */
    public Object getValue() {
        return value;
    }

    /**
     * Sets teh TenantConfigurationValueRequest
     * 
     * @param value
     */
    public void setValue(final Object value) {
        this.value = value;
    }

}
