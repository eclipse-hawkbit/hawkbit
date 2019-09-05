/**
 * Copyright (c) 2019 Kiwigrid GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.dmf.hono;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.hawkbit.dmf.hono.model.*;
import org.eclipse.hawkbit.repository.EntityFactory;
import org.eclipse.hawkbit.repository.SystemManagement;
import org.eclipse.hawkbit.repository.TargetManagement;
import org.eclipse.hawkbit.repository.exception.EntityNotFoundException;
import org.eclipse.hawkbit.repository.model.Target;
import org.eclipse.hawkbit.security.SystemSecurityContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.cloud.stream.annotation.EnableBinding;
import org.springframework.cloud.stream.annotation.StreamListener;
import org.springframework.context.event.EventListener;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.Semaphore;

@EnableBinding(HonoInputSink.class)
public class HonoDeviceSync {

    private static final Logger LOG = LoggerFactory.getLogger(HonoDeviceSync.class);

    @Autowired
    private EntityFactory entityFactory;

    @Autowired
    private SystemManagement systemManagement;

    @Autowired
    private SystemSecurityContext systemSecurityContext;

    @Autowired
    private TargetManagement targetManagement;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private final Map<String, Semaphore> mutexes = new HashMap<>();
    private boolean syncedInitially = false;

    private String oidcAccessToken = null;
    private Instant oidcAccessTokenExpirationDate;

    private String honoTenantListUri;
    private String honoDeviceListUri;
    private String honoCredentialsListUri;
    private String authenticationMethod;
    private String oidcTokenUri;
    private String oidcClientId;
    private String username;
    private String password;

    HonoDeviceSync(String honoTenantListUri, String honoDevicesEndpoint, String honoCredentialsListUri,
                   String authenticationMethod, String oidcTokenUri, String oidcClientId, String username,
                   String password) {
        this.honoTenantListUri = honoTenantListUri;
        this.honoDeviceListUri = honoDevicesEndpoint;
        this.honoCredentialsListUri = honoCredentialsListUri;
        this.authenticationMethod = authenticationMethod;
        this.oidcTokenUri = oidcTokenUri;
        this.oidcClientId = oidcClientId;
        this.username = username;
        this.password = password;

        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    @EventListener(ApplicationReadyEvent.class)
    private void initialSync() {
        // Since ApplicationReadyEvent is emitted multiple times make sure it is synced at most once during startup.
        if (!syncedInitially) {
            synchronize(false);
            syncedInitially = true;
        }
    }

    public void synchronize(boolean syncOnlyCurrentTenant) {
        try {
            String currentTenant = null;
            if (syncOnlyCurrentTenant) {
                currentTenant = systemManagement.currentTenant();
            }

            List<IdentifiableHonoTenant> tenants = getAllHonoTenants();
            for (IdentifiableHonoTenant honoTenant : tenants) {
                String tenant = honoTenant.getId();

                if (syncOnlyCurrentTenant && !tenant.equals(currentTenant)) {
                    continue;
                }

                synchronizeTenant(tenant);
            }
        } catch (IOException e) {
            LOG.error("Could not parse hono api response.", e);
        } catch (InterruptedException e) {
            LOG.warn("Synchronizing hawkbit with Hono has been interrupted.", e);
        }
    }

    private void synchronizeTenant(String tenant) throws IOException, InterruptedException {
        Semaphore semaphore = mutexes.computeIfAbsent(tenant, t -> new Semaphore(1));
        semaphore.acquire();

        Map<String, IdentifiableHonoDevice> honoDevices = getAllHonoDevices(tenant);
        Slice<Target> targets = systemSecurityContext.runAsSystemAsTenant(
                () -> targetManagement.findAll(Pageable.unpaged()), tenant);

        for (Target target : targets) {
            String controllerId = target.getControllerId();
            if (honoDevices.containsKey(controllerId)) {
                IdentifiableHonoDevice honoDevice = honoDevices.remove(controllerId);
                honoDevice.setTenant(tenant);
                systemSecurityContext.runAsSystemAsTenant(() -> updateTarget(honoDevice), tenant);
            }
            else {
                systemSecurityContext.runAsSystemAsTenant(() -> {
                    targetManagement.deleteByControllerID(target.getControllerId());
                    return true;
                }, tenant);
            }
        }

        // At this point honoTargets only contains objects which were not found in hawkBit's target repository
        for (Map.Entry<String, IdentifiableHonoDevice> entry : honoDevices.entrySet()) {
            systemSecurityContext.runAsSystemAsTenant(() -> createTarget(entry.getValue()), tenant);
        }

        semaphore.release();
    }

    public void checkDeviceIfAbsentSync(String tenant, String deviceID) {
        Optional<Target> target = systemSecurityContext.runAsSystemAsTenant(
                () -> targetManagement.getByControllerID(deviceID), tenant);
        if (!target.isPresent()) {
            try {
                synchronizeTenant(tenant);
            } catch (IOException | InterruptedException e) {
                LOG.error("Could not synchronize with hono for tenant {}.", tenant, e);
            }
        }
    }

    public List<IdentifiableHonoTenant> getAllHonoTenants() throws IOException {
        List<IdentifiableHonoTenant> tenants = new ArrayList<>();
        long offset = 0;
        long total = Long.MAX_VALUE;
        while (tenants.size() < total) {
            HttpURLConnection connection = getHonoData(honoTenantListUri + "?offset=" + offset);

            HonoTenantListPage page = objectMapper.readValue(connection.getInputStream(), HonoTenantListPage.class);
            tenants.addAll(page.getItems());
            offset += page.getItems().size();
            total = page.getTotal();
        }

        return tenants;
    }

    public Map<String, IdentifiableHonoDevice> getAllHonoDevices(String tenant) throws IOException {
        Map<String, IdentifiableHonoDevice> devices = new HashMap<>();
        long offset = 0;
        long total = Long.MAX_VALUE;
        while (devices.size() < total) {
            HttpURLConnection connection = getHonoData(honoDeviceListUri.replace("$tenantId", tenant) + "?offset=" + offset);

            HonoDeviceListPage page = objectMapper.readValue(connection.getInputStream(), HonoDeviceListPage.class);
            if (page.getItems() != null) {
                for (IdentifiableHonoDevice identifiableDevice : page.getItems()) {
                    identifiableDevice.setTenant(tenant);
                    devices.put(identifiableDevice.getId(), identifiableDevice);
                }
                offset += page.getItems().size();
            }
            total = page.getTotal();
        }

        return devices;
    }

    public Collection<HonoCredentials> getAllHonoCredentials(String tenant, String deviceId) {
        try {
            HttpURLConnection connection = getHonoData(honoCredentialsListUri.replace("$tenantId", tenant)
                    .replace("$deviceId", deviceId));
            return objectMapper.readValue(connection.getInputStream(), new TypeReference<Collection<HonoCredentials>>() {});
        }
        catch (IOException e) {
            return null;
        }
    }

    private HttpURLConnection getHonoData(String uri) throws IOException {
        URL url = new URL(uri);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();

        switch (authenticationMethod) {
            case "basic":
                connection.setRequestProperty("Authorization", "Basic " + Base64.getEncoder().encodeToString((username + ":" + password).getBytes()));
                break;

            case "oidc":
                if (oidcAccessToken == null ||
                        (oidcAccessTokenExpirationDate != null && oidcAccessTokenExpirationDate.isBefore(Instant.now()))) {

                    URL oidcTokenUrl = new URL(oidcTokenUri);
                    HttpURLConnection jwtConnection = (HttpURLConnection) oidcTokenUrl.openConnection();
                    jwtConnection.setDoOutput(true);
                    DataOutputStream outputStream = new DataOutputStream(jwtConnection.getOutputStream());
                    outputStream.writeBytes("grant_type=password&client_id=" + URLEncoder.encode(oidcClientId)
                            + "&username=" + URLEncoder.encode(username) + "&password=" + URLEncoder.encode(password));
                    outputStream.flush();
                    outputStream.close();

                    int statusCode = jwtConnection.getResponseCode();
                    if (statusCode >= 200 && statusCode < 300) {
                        JsonNode node = objectMapper.readValue(jwtConnection.getInputStream(), JsonNode.class);
                        oidcAccessToken = node.get("access_token").asText();
                        JsonNode expiresIn = node.get("expires_in");
                        if (expiresIn != null) {
                            oidcAccessTokenExpirationDate = Instant.now().plusSeconds(expiresIn.asLong());
                        }
                    }
                    else {
                        throw new IOException("Server returned HTTP response code: " + statusCode + " for URL: " + oidcTokenUrl.toString());
                    }
                }
                connection.setRequestProperty("Authorization", "Bearer " + oidcAccessToken);
                break;
        }

        return connection;
    }

    @StreamListener(HonoInputSink.DEVICE_CREATED)
    public void onDeviceCreated(IdentifiableHonoDevice honoDevice) {
        final String tenant = honoDevice.getTenant();
        if (tenant == null) {
            throw new RuntimeException("The delivered hono device does not contain information about the tenant");
        }

        systemSecurityContext.runAsSystemAsTenant(() -> createTarget(honoDevice), tenant);
    }

    @StreamListener(HonoInputSink.DEVICE_UPDATED)
    public void onDeviceUpdated(IdentifiableHonoDevice honoDevice) {
        final String tenant = honoDevice.getTenant();
        if (tenant == null) {
            throw new RuntimeException("The delivered hono device does not contain information about the tenant");
        }

        systemSecurityContext.runAsSystemAsTenant(() -> {
            if (targetManagement.getByControllerID(honoDevice.getId()).isPresent()) {
                return updateTarget(honoDevice);
            }
            else {
                return createTarget(honoDevice);
            }
        }, tenant);
    }

    @StreamListener(HonoInputSink.DEVICE_DELETED)
    public void onDeviceDeleted(IdentifiableHonoDevice honoDevice) {
        final String tenant = honoDevice.getTenant();
        if (tenant == null) {
            throw new RuntimeException("The delivered hono device does not contain information about the tenant");
        }

        systemSecurityContext.runAsSystemAsTenant(() -> {
            try {
                targetManagement.deleteByControllerID(honoDevice.getId());
            }
            catch (EntityNotFoundException e) {
                // Do nothing as it is already deleted
            }
            return true;
        }, tenant);
    }

    private Target createTarget(IdentifiableHonoDevice honoDevice) {
        systemManagement.getTenantMetadata(honoDevice.getTenant());
        return targetManagement.create(entityFactory.target().create()
                .controllerId(honoDevice.getId()).description(honoDevice.getDevice().getExt().toString()));
    }

    private Target updateTarget(IdentifiableHonoDevice honoDevice) {
        return targetManagement.update(entityFactory.target()
                .update(honoDevice.getId())
                .description(honoDevice.getDevice().getExt().toString()));
    }
}
