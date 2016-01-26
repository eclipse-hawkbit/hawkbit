package org.eclipse.hawkbit.rest.resource;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.hawkbit.repository.SystemManagement;
import org.eclipse.hawkbit.repository.model.TenantMetaData;
import org.eclipse.hawkbit.repository.model.helper.DurationHelper;
import org.eclipse.hawkbit.rest.resource.model.system.SystemConfigurationRequestBodyPut;
import org.eclipse.hawkbit.rest.resource.model.system.SystemConfigurationRest;
import org.eclipse.hawkbit.tenancy.configuration.TenantConfigurationKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final public class SystemMapper {

    private static final Logger LOG = LoggerFactory.getLogger(SystemMapper.class);

    private SystemMapper() {
        // Utility class
    }

    private static DurationHelper dh = new DurationHelper();

    public static SystemConfigurationRest toResponse(final SystemManagement systemManagement) {

        final SystemConfigurationRest sysconf = new SystemConfigurationRest();

        final Map<String, Object> authconf = new HashMap<String, Object>();

        for (final TenantConfigurationKey key : TenantConfigurationKey.values()) {
            final Object value = systemManagement.getConfigurationValue(key);
            authconf.put(key.getKeyName(), value);
        }
        sysconf.setAuthenticationConfiguration(authconf);

        return sysconf;
    }

    public static TenantMetaData fromRequest(final SystemManagement systemManagement,
            final SystemConfigurationRequestBodyPut systemConReq) {

        // TODO
        return null;
    }
}
