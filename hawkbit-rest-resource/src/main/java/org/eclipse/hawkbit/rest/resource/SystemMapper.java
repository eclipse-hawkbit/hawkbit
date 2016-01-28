package org.eclipse.hawkbit.rest.resource;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.hawkbit.repository.TenantConfigurationManagement;
import org.eclipse.hawkbit.repository.model.TenantConfigurationValue;
import org.eclipse.hawkbit.rest.resource.model.system.TenantConfigurationValueRest;
import org.eclipse.hawkbit.tenancy.configuration.TenantConfigurationKey;

/**
 * A mapper which maps repository model to RESTful model representation and
 * back.
 */
final public class SystemMapper {

    private SystemMapper() {
        // Utility class
    }

    /**
     * @param tenantConfigurationManagement
     *            instance of TenantConfigurationManagement
     * @return a map of all existing configuration values
     */
    public static Map<String, TenantConfigurationValueRest> toResponse(
            final TenantConfigurationManagement tenantConfigurationManagement) {

        final Map<String, TenantConfigurationValueRest> configurationMap = new HashMap<String, TenantConfigurationValueRest>();

        for (final TenantConfigurationKey key : TenantConfigurationKey.values()) {
            configurationMap.put(key.getKeyName(),
                    toResponse(tenantConfigurationManagement.getConfigurationValue(key)));
        }

        return configurationMap;
    }

    /**
     * maps a TenantConfigurationValue from the repository model to a
     * TenantConfigurationValueRest, the RESTful model.
     * 
     * @param repoConfValue
     *            configuration value as repository model
     * @return configuration value as RESTful model
     */
    public static TenantConfigurationValueRest toResponse(final TenantConfigurationValue<?> repoConfValue) {
        final TenantConfigurationValueRest restConfValue = new TenantConfigurationValueRest();

        restConfValue.setValue(repoConfValue.getValue());
        restConfValue.setGlobal(repoConfValue.isGlobal());
        restConfValue.setCreatedAt(repoConfValue.getCreatedAt());
        restConfValue.setCreatedBy(repoConfValue.getCreatedBy());
        restConfValue.setLastModifiedAt(repoConfValue.getLastModifiedAt());
        restConfValue.setLastModifiedBy(repoConfValue.getLastModifiedBy());

        return restConfValue;
    }
}
