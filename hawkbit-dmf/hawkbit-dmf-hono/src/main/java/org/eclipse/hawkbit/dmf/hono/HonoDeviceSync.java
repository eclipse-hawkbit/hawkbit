package org.eclipse.hawkbit.dmf.hono;

import com.esotericsoftware.minlog.Log;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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

    private ObjectMapper objectMapper = new ObjectMapper();

    private Semaphore mutex = new Semaphore(1);
    private boolean syncedInitially = false;

    private String oidcAccessToken = null;
    private Instant oidcAccessTokenExpirationDate;

    private String honoTenantListUri;
    private String honoDeviceListUri;
    private String authorizationMethod;
    private String oidcTokenUri;
    private String oidcClientId;
    private String username;
    private String password;

    HonoDeviceSync(String honoTenantListUri, String honoDevicesEndpoint, String authorizationMethod,
                   String oidcTokenUri, String oidcClientId, String username, String password) {
        this.honoTenantListUri = honoTenantListUri;
        this.honoDeviceListUri = honoDevicesEndpoint;
        this.authorizationMethod = authorizationMethod;
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
            synchronize();
            syncedInitially = true;
        }
    }

    public void synchronize() {
        try {
            mutex.acquire();

            List<IdentifiableHonoTenant> tenants = getAllHonoTenants();
            for (IdentifiableHonoTenant honoTenant : tenants) {
                String tenant = honoTenant.getId();
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
            }

            mutex.release();
        } catch (IOException e) {
            LOG.error("Could not parse hono api response.", e);
        } catch (InterruptedException e) {
            LOG.error("Synchronizing hawkbit with Hono has been interrupted.", e);
        }
    }

    private List<IdentifiableHonoTenant> getAllHonoTenants() throws IOException {
        List<IdentifiableHonoTenant> tenants = new ArrayList<>();
        long offset = 0;
        long total = Long.MAX_VALUE;
        while (tenants.size() < total) {
            URL url = new URL(honoTenantListUri + "?offset=" + offset);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            addAuthorizationHeader(connection);

            HonoTenantListPage page = objectMapper.readValue(connection.getInputStream(), HonoTenantListPage.class);
            tenants.addAll(page.getItems());
            offset += page.getItems().size();
            total = page.getTotal();
        }

        return tenants;
    }

    private Map<String, IdentifiableHonoDevice> getAllHonoDevices(String tenant) throws IOException {
        Map<String, IdentifiableHonoDevice> devices = new HashMap<>();
        long offset = 0;
        long total = Long.MAX_VALUE;
        while (devices.size() < total) {
            URL url = new URL(honoDeviceListUri.replace("$tenantId", tenant) + "?offset=" + offset);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            addAuthorizationHeader(connection);

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

    private void addAuthorizationHeader(HttpURLConnection connection) throws IOException {
        switch (authorizationMethod) {
            case "basic":
                connection.setRequestProperty("Authorization", "Basic " + Base64.getEncoder().encodeToString((username + ":" + password).getBytes()));
                break;

            case "oidc":
                if (oidcAccessToken == null ||
                        (oidcAccessTokenExpirationDate != null && oidcAccessTokenExpirationDate.isBefore(Instant.now()))) {

                    URL url = new URL(oidcTokenUri);
                    HttpURLConnection jwtConnection = (HttpURLConnection) url.openConnection();
                    jwtConnection.setDoOutput(true);
                    DataOutputStream outputStream = new DataOutputStream(jwtConnection.getOutputStream());
                    outputStream.writeBytes("grant_type=password&client_id=" + oidcClientId + "&username=" + username + "&password=" + password);
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
                        throw new IOException("Server returned HTTP response code: " + statusCode + " for URL: " + url.toString());
                    }
                }
                connection.setRequestProperty("Authorization", "Bearer " + oidcAccessToken);
                break;
        }
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
