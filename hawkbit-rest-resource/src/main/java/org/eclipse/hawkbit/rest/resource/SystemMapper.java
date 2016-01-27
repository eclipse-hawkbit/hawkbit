package org.eclipse.hawkbit.rest.resource;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.hawkbit.repository.TenantConfigurationManagement;
import org.eclipse.hawkbit.repository.model.TenantConfigurationValue;
import org.eclipse.hawkbit.rest.resource.model.system.SystemConfigurationRest;
import org.eclipse.hawkbit.rest.resource.model.system.TenantConfigurationValueRest;
import org.eclipse.hawkbit.tenancy.configuration.DurationHelper;
import org.eclipse.hawkbit.tenancy.configuration.TenantConfigurationKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final public class SystemMapper {

    private static final Logger LOG = LoggerFactory.getLogger(SystemMapper.class);

    private SystemMapper() {
        // Utility class
    }

    private static DurationHelper dh = new DurationHelper();

    public static SystemConfigurationRest toResponse(
            final TenantConfigurationManagement tenantConfigurationManagement) {

        final SystemConfigurationRest sysconf = new SystemConfigurationRest();

        final Map<String, TenantConfigurationValueRest> authconf = new HashMap<String, TenantConfigurationValueRest>();

        for (final TenantConfigurationKey key : TenantConfigurationKey.values()) {
            final TenantConfigurationValueRest value = toResponse(
                    tenantConfigurationManagement.getConfigurationValue(key, key.getDataType()));
            authconf.put(key.getKeyName(), value);
        }
        sysconf.setConfiguration(authconf);

        return sysconf;
    }

    public static TenantConfigurationValueRest toResponse(final TenantConfigurationValue<?> confValue) {
        final TenantConfigurationValueRest response = new TenantConfigurationValueRest();

        response.setValue(confValue.getValue());
        response.setGlobal(confValue.isGlobal());
        response.setCreatedAt(confValue.getCreatedAt());
        response.setCreatedBy(confValue.getCreatedBy());
        response.setLastModifiedAt(confValue.getLastModifiedAt());
        response.setLastModifiedBy(confValue.getLastModifiedBy());

        return response;
    }
}
