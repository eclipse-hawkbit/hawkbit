package org.eclipse.hawkbit.rest.resource;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.hawkbit.repository.DistributionSetManagement;
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

    public static SystemConfigurationRest toResponse(SystemManagement systemManagement) {

        TenantMetaData tenantMetaData = systemManagement.getTenantMetadata();

        SystemConfigurationRest sysconf = new SystemConfigurationRest();

        sysconf.setDefaultDistributionSetType(tenantMetaData.getDefaultDsType().getKey());
        sysconf.setCreatedAt(tenantMetaData.getCreatedAt());
        sysconf.setCreatedBy(tenantMetaData.getCreatedBy());
        sysconf.setLastModifiedAt(tenantMetaData.getLastModifiedAt());
        sysconf.setLastModifiedBy(tenantMetaData.getLastModifiedBy());

        sysconf.setPollingOverdueTime(dh.durationToFormattedString(tenantMetaData.getPollingOverdueTime()));
        sysconf.setPollingTime(dh.durationToFormattedString(tenantMetaData.getPollingTime()));

        Map<String, Object> authconf = new HashMap<String, Object>();

        for (TenantConfigurationKey key : TenantConfigurationKey.values()) {
            Object value;

            switch (key) {
            case AUTHENTICATION_MODE_HEADER_ENABLED:
            case AUTHENTICATION_MODE_TARGET_SECURITY_TOKEN_ENABLED:
            case AUTHENTICATION_MODE_GATEWAY_SECURITY_TOKEN_ENABLED:
                value = systemManagement.getConfigurationValue(key, Boolean.class);
                break;
            case AUTHENTICATION_MODE_GATEWAY_SECURITY_TOKEN_NAME:
            case AUTHENTICATION_MODE_GATEWAY_SECURITY_TOKEN_KEY:
            case AUTHENTICATION_MODE_HEADER_AUTHORITY_NAME:
                value = systemManagement.getConfigurationValue(key, String.class);
                break;
            default:
                LOG.warn("There is no data type specified for TenantConfigurationKey {}.", key.getKeyName());
                value = systemManagement.getConfigurationValue(key, String.class);
            }
            authconf.put(key.getKeyName(), value);
        }

        sysconf.setAuthenticationConfiguration(authconf);

        return sysconf;
    }

    public static TenantMetaData fromRequest(SystemManagement systemManagement,
            SystemConfigurationRequestBodyPut systemConReq, DistributionSetManagement distributionSetManagement) {

        TenantMetaData tenantMetaData = systemManagement.getTenantMetadata();

        String ddstypeKey = systemConReq.getDefaultDistributionSetType();

        if (distributionSetManagement.findDistributionSetTypeByKey(ddstypeKey) == null) {
            throw new IllegalArgumentException(
                    String.format("The specified default distribution set type %s doe not exist.", ddstypeKey));
        }

        tenantMetaData.setPollingOverdueTime(dh.formattedStringToDuration(systemConReq.getPollingOverdueTime()));
        tenantMetaData.setPollingTime(dh.formattedStringToDuration(systemConReq.getPollingTime()));

        return null;

    }
}
