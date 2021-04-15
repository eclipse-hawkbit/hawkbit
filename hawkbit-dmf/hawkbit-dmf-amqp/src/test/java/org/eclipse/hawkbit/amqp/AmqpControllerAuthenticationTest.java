/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.amqp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Optional;

import org.eclipse.hawkbit.api.HostnameResolver;
import org.eclipse.hawkbit.cache.DownloadIdCache;
import org.eclipse.hawkbit.dmf.amqp.api.MessageHeaderKey;
import org.eclipse.hawkbit.dmf.amqp.api.MessageType;
import org.eclipse.hawkbit.dmf.json.model.DmfDownloadResponse;
import org.eclipse.hawkbit.repository.ArtifactManagement;
import org.eclipse.hawkbit.repository.ControllerManagement;
import org.eclipse.hawkbit.repository.SystemManagement;
import org.eclipse.hawkbit.repository.TenantConfigurationManagement;
import org.eclipse.hawkbit.repository.jpa.JpaEntityFactory;
import org.eclipse.hawkbit.repository.jpa.model.JpaArtifact;
import org.eclipse.hawkbit.repository.jpa.model.JpaSoftwareModule;
import org.eclipse.hawkbit.repository.jpa.model.JpaSoftwareModuleType;
import org.eclipse.hawkbit.repository.model.Target;
import org.eclipse.hawkbit.repository.model.TenantConfigurationValue;
import org.eclipse.hawkbit.repository.model.TenantMetaData;
import org.eclipse.hawkbit.security.DdiSecurityProperties;
import org.eclipse.hawkbit.security.DdiSecurityProperties.Authentication.Anonymous;
import org.eclipse.hawkbit.security.DdiSecurityProperties.Rp;
import org.eclipse.hawkbit.security.DmfTenantSecurityToken;
import org.eclipse.hawkbit.security.DmfTenantSecurityToken.FileResource;
import org.eclipse.hawkbit.security.SecurityContextTenantAware;
import org.eclipse.hawkbit.security.SystemSecurityContext;
import org.eclipse.hawkbit.tenancy.UserAuthoritiesResolver;
import org.eclipse.hawkbit.tenancy.configuration.TenantConfigurationProperties.TenantConfigurationKey;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken;

import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;

/**
 *
 * Test Amqp controller authentication.
 */
@Feature("Component Tests - Device Management Federation API")
@Story("AmqpController Authentication Test")
@ExtendWith(MockitoExtension.class)
public class AmqpControllerAuthenticationTest {

    private static final String SHA1 = "12345";
    private static final Long ARTIFACT_ID = 1123L;
    private static final String TENANT = "DEFAULT";
    private static final Long TENANT_ID = 123L;
    private static final String CONTROLLER_ID = "123";
    private static final Long TARGET_ID = 123L;
    private AmqpMessageHandlerService amqpMessageHandlerService;
    private AmqpAuthenticationMessageHandler amqpAuthenticationMessageHandlerService;

    private final MessageConverter messageConverter = new Jackson2JsonMessageConverter();

    private AmqpControllerAuthentication authenticationManager;

    private JpaArtifact testArtifact;

    @Mock
    private TenantConfigurationManagement tenantConfigurationManagementMock;

    @Mock
    private SystemManagement systemManagement;

    @Mock
    private DownloadIdCache cacheMock;

    @Mock
    private HostnameResolver hostnameResolverMock;

    @Mock
    private ArtifactManagement artifactManagementMock;

    @Mock
    private Target targetMock;

    @Mock
    private UserAuthoritiesResolver authoritiesResolver;

    @Mock
    private RabbitTemplate rabbitTemplate;

    @Mock
    private ControllerManagement controllerManagement;

    @Mock
    private DdiSecurityProperties securityProperties;

    @Mock
    private Rp rp;

    @Mock
    private DdiSecurityProperties.Authentication ddiAuthentication;

    @Mock
    private Anonymous anonymous;

    private static final TenantConfigurationValue<Boolean> CONFIG_VALUE_FALSE = TenantConfigurationValue
            .<Boolean> builder().value(Boolean.FALSE).build();

    private static final TenantConfigurationValue<Boolean> CONFIG_VALUE_TRUE = TenantConfigurationValue
            .<Boolean> builder().value(Boolean.TRUE).build();

    @BeforeEach
    public void before() {
        when(securityProperties.getRp()).thenReturn(rp);
        when(rp.getSslIssuerHashHeader()).thenReturn("X-Ssl-Issuer-Hash-%d");
        when(tenantConfigurationManagementMock.getConfigurationValue(any(), eq(Boolean.class)))
                .thenReturn(CONFIG_VALUE_FALSE);

        final SecurityContextTenantAware tenantAware = new SecurityContextTenantAware(authoritiesResolver);
        final SystemSecurityContext systemSecurityContext = new SystemSecurityContext(tenantAware);

        authenticationManager = new AmqpControllerAuthentication(systemManagement, controllerManagement,
                tenantConfigurationManagementMock, tenantAware, securityProperties, systemSecurityContext);

        authenticationManager.postConstruct();

        testArtifact = new JpaArtifact(SHA1, "afilename", new JpaSoftwareModule(
                new JpaSoftwareModuleType("a key", "a name", null, 1), "a name", null, null, null));
        testArtifact.setId(1L);

        amqpMessageHandlerService = new AmqpMessageHandlerService(rabbitTemplate,
                mock(AmqpMessageDispatcherService.class), controllerManagement, new JpaEntityFactory(),
                systemSecurityContext, tenantConfigurationManagementMock);

        amqpAuthenticationMessageHandlerService = new AmqpAuthenticationMessageHandler(rabbitTemplate,
                authenticationManager, artifactManagementMock, cacheMock, hostnameResolverMock, controllerManagement,
                tenantAware);
    }

    private void mockAuthenticationWithoutPrincipal() {
        lenient().when(securityProperties.getAuthentication()).thenReturn(ddiAuthentication);
        lenient().when(ddiAuthentication.getAnonymous()).thenReturn(anonymous);
        lenient().when(anonymous.isEnabled()).thenReturn(false);
    }

    private void mockSuccessfulAuthentication() throws MalformedURLException {
        when(rabbitTemplate.getMessageConverter()).thenReturn(messageConverter);
        when(hostnameResolverMock.resolveHostname()).thenReturn(new URL("http://localhost"));
        when(tenantConfigurationManagementMock.getConfigurationValue(
                eq(TenantConfigurationKey.AUTHENTICATION_MODE_TARGET_SECURITY_TOKEN_ENABLED), eq(Boolean.class)))
                        .thenReturn(CONFIG_VALUE_TRUE);
        when(targetMock.getSecurityToken()).thenReturn(CONTROLLER_ID);
        when(targetMock.getControllerId()).thenReturn(CONTROLLER_ID);
    }

    @Test
    @Description("Tests authentication manager without principal")
    public void testAuthenticationBadCredentialsWithoutPrincipal() {
        final DmfTenantSecurityToken securityToken = new DmfTenantSecurityToken(TENANT, TENANT_ID, CONTROLLER_ID,
                TARGET_ID, FileResource.createFileResourceBySha1(SHA1));

        mockAuthenticationWithoutPrincipal();

        assertThatExceptionOfType(BadCredentialsException.class)
                .as("BadCredentialsException was expected since principal was missing")
                .isThrownBy(() -> authenticationManager.doAuthenticate(securityToken));
    }

    @Test
    @Description("Tests authentication manager without wrong credential")
    public void testAuthenticationBadCredentialsWithWrongCredential() {
        final DmfTenantSecurityToken securityToken = new DmfTenantSecurityToken(TENANT, TENANT_ID, CONTROLLER_ID,
                TARGET_ID, FileResource.createFileResourceBySha1(SHA1));

        when(tenantConfigurationManagementMock.getConfigurationValue(
                eq(TenantConfigurationKey.AUTHENTICATION_MODE_TARGET_SECURITY_TOKEN_ENABLED), eq(Boolean.class)))
                        .thenReturn(CONFIG_VALUE_TRUE);

        securityToken.putHeader(DmfTenantSecurityToken.AUTHORIZATION_HEADER, "TargetToken 12" + CONTROLLER_ID);

        assertThatExceptionOfType(BadCredentialsException.class)
                .as("BadCredentialsException was expected due to wrong credential")
                .isThrownBy(() -> authenticationManager.doAuthenticate(securityToken));
    }

    @Test
    @Description("Tests authentication successful")
    public void testSuccessfulAuthentication() {

        when(controllerManagement.get(any(Long.class))).thenReturn(Optional.of(targetMock));

        final DmfTenantSecurityToken securityToken = new DmfTenantSecurityToken(TENANT, TENANT_ID, CONTROLLER_ID,
                TARGET_ID, FileResource.createFileResourceBySha1(SHA1));

        when(tenantConfigurationManagementMock.getConfigurationValue(
                eq(TenantConfigurationKey.AUTHENTICATION_MODE_TARGET_SECURITY_TOKEN_ENABLED), eq(Boolean.class)))
                        .thenReturn(CONFIG_VALUE_TRUE);
        when(targetMock.getSecurityToken()).thenReturn(CONTROLLER_ID);
        when(targetMock.getControllerId()).thenReturn(CONTROLLER_ID);

        securityToken.putHeader(DmfTenantSecurityToken.AUTHORIZATION_HEADER, "TargetToken " + CONTROLLER_ID);
        final Authentication authentication = authenticationManager.doAuthenticate(securityToken);
        assertThat(authentication).isNotNull();
    }

    @Test
    @Description("Tests authentication message without principal")
    public void testAuthenticationMessageBadCredentialsWithoutPrincipal() {
        final MessageProperties messageProperties = createMessageProperties(null);
        final DmfTenantSecurityToken securityToken = new DmfTenantSecurityToken(TENANT, TENANT_ID, CONTROLLER_ID,
                TARGET_ID, FileResource.createFileResourceBySha1(SHA1));

        mockAuthenticationWithoutPrincipal();
        when(rabbitTemplate.getMessageConverter()).thenReturn(messageConverter);

        final Message message = amqpMessageHandlerService.getMessageConverter().toMessage(securityToken,
                messageProperties);

        // test
        final Message onMessage = amqpAuthenticationMessageHandlerService.onAuthenticationRequest(message);

        // verify
        final DmfDownloadResponse downloadResponse = (DmfDownloadResponse) messageConverter.fromMessage(onMessage);
        assertThat(downloadResponse).isNotNull();
        assertThat(downloadResponse.getResponseCode()).isEqualTo(HttpStatus.FORBIDDEN.value());
    }

    @Test
    @Description("Tests authentication message without wrong credential")
    public void testAuthenticationMessageBadCredentialsWithWrongCredential() {
        final MessageProperties messageProperties = createMessageProperties(null);
        final DmfTenantSecurityToken securityToken = new DmfTenantSecurityToken(TENANT, TENANT_ID, CONTROLLER_ID,
                TARGET_ID, FileResource.createFileResourceBySha1(SHA1));

        when(tenantConfigurationManagementMock.getConfigurationValue(
                eq(TenantConfigurationKey.AUTHENTICATION_MODE_TARGET_SECURITY_TOKEN_ENABLED), eq(Boolean.class)))
                .thenReturn(CONFIG_VALUE_TRUE);

        when(rabbitTemplate.getMessageConverter()).thenReturn(messageConverter);

        securityToken.putHeader(DmfTenantSecurityToken.AUTHORIZATION_HEADER, "TargetToken 12" + CONTROLLER_ID);
        final Message message = amqpMessageHandlerService.getMessageConverter().toMessage(securityToken,
                messageProperties);

        // test
        final Message onMessage = amqpAuthenticationMessageHandlerService.onAuthenticationRequest(message);

        // verify
        final DmfDownloadResponse downloadResponse = (DmfDownloadResponse) messageConverter.fromMessage(onMessage);
        assertThat(downloadResponse).isNotNull();
        assertThat(downloadResponse.getResponseCode()).isEqualTo(HttpStatus.FORBIDDEN.value());
    }

    @Test
    @Description("Tests authentication message successful")
    public void successfulMessageAuthentication() throws Exception {

        final MessageProperties messageProperties = createMessageProperties(null);
        final DmfTenantSecurityToken securityToken = new DmfTenantSecurityToken(TENANT, null, CONTROLLER_ID, null,
                FileResource.createFileResourceBySha1(SHA1));

        mockSuccessfulAuthentication();
        when(controllerManagement.getByControllerId(anyString())).thenReturn(Optional.of(targetMock));
        when(controllerManagement.hasTargetArtifactAssigned(CONTROLLER_ID, SHA1)).thenReturn(true);
        when(artifactManagementMock.findFirstBySHA1(SHA1)).thenReturn(Optional.of(testArtifact));

        securityToken.putHeader(DmfTenantSecurityToken.AUTHORIZATION_HEADER, "TargetToken " + CONTROLLER_ID);
        final Message message = amqpMessageHandlerService.getMessageConverter().toMessage(securityToken,
                messageProperties);

        // test
        final Message onMessage = amqpAuthenticationMessageHandlerService.onAuthenticationRequest(message);

        // verify
        final DmfDownloadResponse downloadResponse = (DmfDownloadResponse) messageConverter.fromMessage(onMessage);
        assertThat(downloadResponse).isNotNull();
        assertThat(downloadResponse.getDownloadUrl()).isNotNull();
        assertThat(downloadResponse.getResponseCode()).isEqualTo(HttpStatus.OK.value());
        assertThat(SecurityContextHolder.getContext()).isNotNull();
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
        assertThat(SecurityContextHolder.getContext().getAuthentication().getClass().getName())
                .isEqualTo(PreAuthenticatedAuthenticationToken.class.getName());

    }

    @Test
    @Description("Tests authentication message successful with targetId intead of controllerId provided and artifactId instead of SHA1.")
    public void successfulMessageAuthenticationWithTargetIdAndArtifactId() throws Exception {
        final MessageProperties messageProperties = createMessageProperties(null);
        final DmfTenantSecurityToken securityToken = new DmfTenantSecurityToken(TENANT, null, null, TARGET_ID,
                FileResource.createFileResourceByArtifactId(ARTIFACT_ID));

        mockSuccessfulAuthentication();
        when(controllerManagement.get(any(Long.class))).thenReturn(Optional.of(targetMock));
        when(controllerManagement.hasTargetArtifactAssigned(TARGET_ID, SHA1)).thenReturn(true);
        when(artifactManagementMock.get(ARTIFACT_ID)).thenReturn(Optional.of(testArtifact));

        securityToken.putHeader(DmfTenantSecurityToken.AUTHORIZATION_HEADER, "TargetToken " + CONTROLLER_ID);
        final Message message = amqpMessageHandlerService.getMessageConverter().toMessage(securityToken,
                messageProperties);

        // test
        final Message onMessage = amqpAuthenticationMessageHandlerService.onAuthenticationRequest(message);

        // verify
        final DmfDownloadResponse downloadResponse = (DmfDownloadResponse) messageConverter.fromMessage(onMessage);
        assertThat(downloadResponse).isNotNull();
        assertThat(downloadResponse.getDownloadUrl()).isNotNull();
        assertThat(downloadResponse.getResponseCode()).isEqualTo(HttpStatus.OK.value());
        assertThat(SecurityContextHolder.getContext()).isNotNull();
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
        assertThat(SecurityContextHolder.getContext().getAuthentication().getClass().getName())
                .isEqualTo(PreAuthenticatedAuthenticationToken.class.getName());

    }

    @Test
    @Description("Tests authentication message successful")
    public void successfulMessageAuthenticationWithTenantId() throws Exception {

        final MessageProperties messageProperties = createMessageProperties(null);
        final DmfTenantSecurityToken securityToken = new DmfTenantSecurityToken(null, TENANT_ID, CONTROLLER_ID,
                TARGET_ID, FileResource.createFileResourceBySha1(SHA1));
        final TenantMetaData tenantMetaData = mock(TenantMetaData.class);

        mockSuccessfulAuthentication();
        when(controllerManagement.get(any(Long.class))).thenReturn(Optional.of(targetMock));
        when(controllerManagement.hasTargetArtifactAssigned(CONTROLLER_ID, SHA1)).thenReturn(true);
        when(artifactManagementMock.findFirstBySHA1(SHA1)).thenReturn(Optional.of(testArtifact));
        when(tenantMetaData.getTenant()).thenReturn(TENANT);
        when(systemManagement.getTenantMetadata(TENANT_ID)).thenReturn(tenantMetaData);

        securityToken.putHeader(DmfTenantSecurityToken.AUTHORIZATION_HEADER, "TargetToken " + CONTROLLER_ID);
        final Message message = amqpMessageHandlerService.getMessageConverter().toMessage(securityToken,
                messageProperties);

        // test
        final Message onMessage = amqpAuthenticationMessageHandlerService.onAuthenticationRequest(message);

        // verify
        final DmfDownloadResponse downloadResponse = (DmfDownloadResponse) messageConverter.fromMessage(onMessage);
        assertThat(downloadResponse).isNotNull();
        assertThat(downloadResponse.getDownloadUrl()).isNotNull();
        assertThat(downloadResponse.getResponseCode()).isEqualTo(HttpStatus.OK.value());
        assertThat(SecurityContextHolder.getContext()).isNotNull();
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
        assertThat(SecurityContextHolder.getContext().getAuthentication().getClass().getName())
                .isEqualTo(PreAuthenticatedAuthenticationToken.class.getName());

    }

    private MessageProperties createMessageProperties(final MessageType type) {
        return createMessageProperties(type, "MyTest");
    }

    private MessageProperties createMessageProperties(final MessageType type, final String replyTo) {
        final MessageProperties messageProperties = new MessageProperties();
        if (type != null) {
            messageProperties.setHeader(MessageHeaderKey.TYPE, type.name());
        }
        messageProperties.setHeader(MessageHeaderKey.TENANT, TENANT);
        messageProperties.setContentType(MessageProperties.CONTENT_TYPE_JSON);
        messageProperties.setReplyTo(replyTo);
        return messageProperties;
    }

}
