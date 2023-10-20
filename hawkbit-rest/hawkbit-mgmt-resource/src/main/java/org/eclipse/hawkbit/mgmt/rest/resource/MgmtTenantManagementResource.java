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

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.eclipse.hawkbit.mgmt.json.model.system.MgmtSystemTenantConfigurationValue;
import org.eclipse.hawkbit.mgmt.json.model.system.MgmtSystemTenantConfigurationValueRequest;
import org.eclipse.hawkbit.mgmt.rest.api.MgmtTenantManagementRestApi;
import org.eclipse.hawkbit.repository.SystemManagement;
import org.eclipse.hawkbit.repository.TenantConfigurationManagement;
import org.eclipse.hawkbit.repository.model.TenantConfigurationValue;
import org.eclipse.hawkbit.tenancy.configuration.TenantConfigurationProperties;
import org.eclipse.hawkbit.tenancy.configuration.validator.TenantConfigurationValidatorException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST Resource for handling tenant specific configuration operations.
 */
@RestController
public class MgmtTenantManagementResource implements MgmtTenantManagementRestApi {

    private static final Logger LOG = LoggerFactory.getLogger(MgmtTenantManagementResource.class);

    private final TenantConfigurationManagement tenantConfigurationManagement;
    private final TenantConfigurationProperties tenantConfigurationProperties;

    /**
     * Default {@link org.eclipse.hawkbit.repository.model.DistributionSetType} as part of supported configs.
     * */

    private final SystemManagement systemManagement;

    MgmtTenantManagementResource(final TenantConfigurationManagement tenantConfigurationManagement,
            final TenantConfigurationProperties tenantConfigurationProperties,
        final SystemManagement systemManagement) {
        this.tenantConfigurationManagement = tenantConfigurationManagement;
        this.tenantConfigurationProperties = tenantConfigurationProperties;
        this.systemManagement = systemManagement;
    }

    @Override
    public ResponseEntity<Map<String, MgmtSystemTenantConfigurationValue>> getTenantConfiguration() {
        //Load and Construct default Tenant Configuration
        Map<String, MgmtSystemTenantConfigurationValue> tenantConfigurationValueMap = tenantConfigurationProperties.getConfigurationKeys().stream().collect(
            Collectors.toMap(TenantConfigurationProperties.TenantConfigurationKey::getKeyName,
            key -> loadTenantConfigurationValueBy(key.getKeyName())));
        //Load and Add Default DistributionSetType
        MgmtSystemTenantConfigurationValue defaultDsTypeId = loadTenantConfigurationValueBy(MgmtTenantManagementMapper.DEFAULT_DISTRIBUTION_SET_TYPE_KEY);
        tenantConfigurationValueMap.put(MgmtTenantManagementMapper.DEFAULT_DISTRIBUTION_SET_TYPE_KEY, defaultDsTypeId);
        //return combined TenantConfiguration and TenantMetadata
        LOG.debug("getTenantConfiguration, return status {}", HttpStatus.OK);
        return ResponseEntity.ok(tenantConfigurationValueMap);
    }

    @Override
    public ResponseEntity<MgmtSystemTenantConfigurationValue> getTenantConfigurationValue(
        @PathVariable("keyName") final String keyName) {
        return ResponseEntity.ok(loadTenantConfigurationValueBy(keyName));
    }

    private MgmtSystemTenantConfigurationValue loadTenantConfigurationValueBy(String keyName) {

        //Check if requested key is TenantConfiguration or TenantMetadata, load it and return it as rest response
        MgmtSystemTenantConfigurationValue response;
        if (isDefaultDistributionSetTypeKey(keyName)) {
            response = MgmtTenantManagementMapper.toResponseDefaultDsType(String.valueOf(systemManagement.getTenantMetadata().getDefaultDsType().getId()));
        } else {
            response = MgmtTenantManagementMapper.toResponseTenantConfigurationValue(keyName, tenantConfigurationManagement.getConfigurationValue(keyName));
        }
        return response;
    }

    @Override
    public ResponseEntity<Void> deleteTenantConfigurationValue(@PathVariable("keyName") final String keyName) {

        //Default DistributionSet Type cannot be deleted as is part of TenantMetadata
        if (isDefaultDistributionSetTypeKey(keyName)) {
            return ResponseEntity.badRequest().build();
        }

        tenantConfigurationManagement.deleteConfiguration(keyName);

        LOG.debug("{} config value deleted, return status {}", keyName, HttpStatus.OK);
        return ResponseEntity.ok().build();
    }

    @Override
    public ResponseEntity<MgmtSystemTenantConfigurationValue> updateTenantConfigurationValue(
            @PathVariable("keyName") final String keyName,
            @RequestBody final MgmtSystemTenantConfigurationValueRequest configurationValueRest) {
        Serializable                  configurationValue            = configurationValueRest.getValue();
        final MgmtSystemTenantConfigurationValue responseUpdatedValue;
        if (isDefaultDistributionSetTypeKey(keyName)) {
            responseUpdatedValue = updateDefaultDsType(configurationValue);
        } else {
            final TenantConfigurationValue<? extends Serializable> updatedTenantConfigurationValue = tenantConfigurationManagement
                .addOrUpdateConfiguration(keyName, configurationValueRest.getValue());
            responseUpdatedValue = MgmtTenantManagementMapper.toResponseTenantConfigurationValue(keyName, updatedTenantConfigurationValue);
        }

        return ResponseEntity.ok(responseUpdatedValue);
    }

    @Override
    public ResponseEntity<List<MgmtSystemTenantConfigurationValue>> updateTenantConfiguration(
            Map<String, Serializable> configurationValueMap) {

        boolean containsNull = configurationValueMap.keySet().stream()
                        .anyMatch(Objects::isNull);

        if (containsNull) {
            return ResponseEntity.badRequest().build();
        }

        //Try update TenantMetadata first
        Serializable defaultDsTypeValueUpdate = configurationValueMap.remove(MgmtTenantManagementMapper.DEFAULT_DISTRIBUTION_SET_TYPE_KEY);
        Long oldDefaultDsType = null;
        MgmtSystemTenantConfigurationValue updatedDefaultDsType = null;
        if (defaultDsTypeValueUpdate != null) {
            oldDefaultDsType = systemManagement.getTenantMetadata().getDefaultDsType().getId();
            updatedDefaultDsType = updateDefaultDsType(defaultDsTypeValueUpdate);
        }
        //try update TenantConfiguration, in case of Error -> rollback TenantMetadata
        Map<String, TenantConfigurationValue<Serializable>> tenantConfigurationValues;
        try {
            tenantConfigurationValues = tenantConfigurationManagement.addOrUpdateConfiguration(configurationValueMap);
        } catch (Exception ex) {
            //if DefaultDsType was updated, rollback it in case of TenantConfiguration update.
            if (updatedDefaultDsType != null) {
                systemManagement.updateTenantMetadata(oldDefaultDsType);
            }
            throw ex;
        }

        List<MgmtSystemTenantConfigurationValue> tenantConfigurationListUpdated = new java.util.ArrayList<>(tenantConfigurationValues.entrySet().stream()
            .map(entry -> MgmtTenantManagementMapper.toResponseTenantConfigurationValue(entry.getKey(), entry.getValue())).toList());
        if (updatedDefaultDsType != null) {
            tenantConfigurationListUpdated.add(updatedDefaultDsType);
        }


        return ResponseEntity.ok(tenantConfigurationListUpdated);
    }
    private MgmtSystemTenantConfigurationValue updateDefaultDsType(Serializable defaultDsType) {
        if (!defaultDsType.getClass().isAssignableFrom(String.class)) {
            throw new TenantConfigurationValidatorException(String.format(
                "Default DistributionSetType Value Type is incorrect. Expected Long, received %s", defaultDsType.getClass().getName()));
        }
        long defaultDistributionSetTypeValue;
        try {
            defaultDistributionSetTypeValue = Long.parseLong((String)defaultDsType);
        } catch (Exception ex) {
            throw new TenantConfigurationValidatorException(String.format(
                "Default DistributionSetType Value Type is incorrect. Expected input to be a Number, received %s", defaultDsType.getClass().getName()));
        }
        systemManagement.updateTenantMetadata(defaultDistributionSetTypeValue);
        return MgmtTenantManagementMapper.toResponseDefaultDsType((String)defaultDsType);
    }

    private static boolean isDefaultDistributionSetTypeKey(String keyName) {
        return MgmtTenantManagementMapper.DEFAULT_DISTRIBUTION_SET_TYPE_KEY.equals(keyName);
    }


}
