/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.mgmt.rest.resource;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import lombok.extern.slf4j.Slf4j;
import org.eclipse.hawkbit.audit.AuditLog;
import org.eclipse.hawkbit.mgmt.json.model.system.MgmtSystemTenantConfigurationValue;
import org.eclipse.hawkbit.mgmt.json.model.system.MgmtSystemTenantConfigurationValueRequest;
import org.eclipse.hawkbit.mgmt.rest.api.MgmtTenantManagementRestApi;
import org.eclipse.hawkbit.mgmt.rest.resource.mapper.MgmtTenantManagementMapper;
import org.eclipse.hawkbit.repository.SystemManagement;
import org.eclipse.hawkbit.repository.exception.EntityNotFoundException;
import org.eclipse.hawkbit.repository.exception.InsufficientPermissionException;
import org.eclipse.hawkbit.repository.exception.TenantConfigurationValidatorException;
import org.eclipse.hawkbit.repository.helper.TenantConfigHelper;
import org.eclipse.hawkbit.tenancy.configuration.TenantConfigurationProperties;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST Resource for handling tenant specific configuration operations.
 */
@Slf4j
@RestController
public class MgmtTenantManagementResource implements MgmtTenantManagementRestApi {

    private final TenantConfigurationProperties tenantConfigurationProperties;
    private final SystemManagement systemManagement;

    MgmtTenantManagementResource(
            final TenantConfigurationProperties tenantConfigurationProperties,
            final SystemManagement systemManagement) {
        this.tenantConfigurationProperties = tenantConfigurationProperties;
        this.systemManagement = systemManagement;
    }

    @Override
    public ResponseEntity<Map<String, MgmtSystemTenantConfigurationValue>> getTenantConfiguration() {
        // Load and Construct default AccessContext Configuration
        final Map<String, MgmtSystemTenantConfigurationValue> tenantConfigurationValueMap = new HashMap<>();
        tenantConfigurationProperties.getConfigurationKeys().forEach(key -> {
            try {
                tenantConfigurationValueMap.put(key.getKeyName(), loadTenantConfigurationValueBy(key.getKeyName()));
            } catch (final InsufficientPermissionException e) {
                // some values as gateway token may not be accessibly for the caller - just skip them
            }
        });

        // Load and Add Default DistributionSetType
        final MgmtSystemTenantConfigurationValue defaultDsTypeId = loadTenantConfigurationValueBy(
                MgmtTenantManagementMapper.DEFAULT_DISTRIBUTION_SET_TYPE_KEY);
        tenantConfigurationValueMap.put(MgmtTenantManagementMapper.DEFAULT_DISTRIBUTION_SET_TYPE_KEY, defaultDsTypeId);

        // return combined TenantConfiguration and TenantMetadata
        log.debug("getTenantConfiguration, return status {}", HttpStatus.OK);
        return ResponseEntity.ok(tenantConfigurationValueMap);
    }

    @Override
    public ResponseEntity<MgmtSystemTenantConfigurationValue> getTenantConfigurationValue(final String keyName) {
        return ResponseEntity.ok(loadTenantConfigurationValueBy(keyName));
    }

    @Override
    @AuditLog(entity = "TenantConfiguration", type = AuditLog.Type.UPDATE, description = "Update AccessContext Configuration Value")
    public void updateTenantConfigurationValue(final String keyName, final MgmtSystemTenantConfigurationValueRequest configurationValueRest) {
        final Object configurationValue = configurationValueRest.getValue();
        if (isDefaultDistributionSetTypeKey(keyName)) {
            updateDefaultDsType(configurationValue);
        } else {
            TenantConfigHelper
                    .getTenantConfigurationManagement()
                    .addOrUpdateConfiguration(keyName, configurationValueRest.getValue());
        }
    }

    @Override
    @AuditLog(entity = "TenantConfiguration", type = AuditLog.Type.UPDATE, description = "Update AccessContext Configuration")
    public void updateTenantConfiguration(final Map<String, Object> configurationValueMap) {
        final boolean containsNull = configurationValueMap.keySet().stream().anyMatch(Objects::isNull);

        if (containsNull) {
            throw new IllegalArgumentException();
        }

        // try update TenantMetadata first
        final Object defaultDsTypeValueUpdate = configurationValueMap.remove(MgmtTenantManagementMapper.DEFAULT_DISTRIBUTION_SET_TYPE_KEY);
        Long oldDefaultDsType = null;
        MgmtSystemTenantConfigurationValue updatedDefaultDsType = null;
        if (defaultDsTypeValueUpdate != null) {
            oldDefaultDsType = systemManagement.getTenantMetadata().getDefaultDsType().getId();
            updatedDefaultDsType = updateDefaultDsType(defaultDsTypeValueUpdate);
        }
        // try update TenantConfiguration, in case of Error -> rollback TenantMetadata
        try {
            TenantConfigHelper.getTenantConfigurationManagement().addOrUpdateConfiguration(configurationValueMap);
        } catch (Exception ex) {
            // if DefaultDsType was updated, rollback it in case of TenantConfiguration update.
            if (updatedDefaultDsType != null) {
                systemManagement.updateTenantMetadata(oldDefaultDsType);
            }
            throw ex;
        }
    }

    @Override
    @AuditLog(entity = "TenantConfiguration", type = AuditLog.Type.DELETE, description = "Delete AccessContext Configuration Value")
    public void deleteTenantConfigurationValue(final String keyName) {
        // Default DistributionSet Type cannot be deleted as is part of TenantMetadata
        if (isDefaultDistributionSetTypeKey(keyName)) {
            throw new EntityNotFoundException("Configuration key not found", keyName);
        }

        TenantConfigHelper.getTenantConfigurationManagement().deleteConfiguration(keyName);
        log.debug("{} config value deleted", keyName);
    }

    private static boolean isDefaultDistributionSetTypeKey(String keyName) {
        return MgmtTenantManagementMapper.DEFAULT_DISTRIBUTION_SET_TYPE_KEY.equals(keyName);
    }

    private MgmtSystemTenantConfigurationValue loadTenantConfigurationValueBy(final String keyName) {
        // Check if requested key is TenantConfiguration or TenantMetadata, load it and return it as rest response
        final MgmtSystemTenantConfigurationValue response;
        if (isDefaultDistributionSetTypeKey(keyName)) {
            response = MgmtTenantManagementMapper.toResponseDefaultDsType(systemManagement.getTenantMetadata().getDefaultDsType().getId());
        } else {
            response = MgmtTenantManagementMapper.toResponseTenantConfigurationValue(
                    keyName, TenantConfigHelper.getTenantConfigurationManagement().getConfigurationValue(keyName));
        }
        return response;
    }

    private MgmtSystemTenantConfigurationValue updateDefaultDsType(final Object defaultDsType) {
        final long updateDefaultDsType;
        try {
            updateDefaultDsType = ((Number) defaultDsType).longValue();
        } catch (ClassCastException cce) {
            throw new TenantConfigurationValidatorException(String.format(
                    "Default DistributionSetType Value Type is incorrect. Expected Long, received %s", defaultDsType.getClass().getName()));
        }
        systemManagement.updateTenantMetadata(updateDefaultDsType);
        return MgmtTenantManagementMapper.toResponseDefaultDsType(updateDefaultDsType);
    }
}