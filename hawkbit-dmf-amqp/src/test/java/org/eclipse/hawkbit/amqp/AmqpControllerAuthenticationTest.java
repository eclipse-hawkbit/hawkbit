/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.amqp;

import static org.fest.assertions.api.Assertions.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.eclipse.hawkbit.dmf.amqp.api.MessageHeaderKey;
import org.eclipse.hawkbit.dmf.amqp.api.MessageType;
import org.eclipse.hawkbit.dmf.json.model.DownloadResponse;
import org.eclipse.hawkbit.dmf.json.model.TenantSecurityToken;
import org.eclipse.hawkbit.dmf.json.model.TenantSecurityToken.FileResource;
import org.eclipse.hawkbit.repository.ArtifactManagement;
import org.eclipse.hawkbit.repository.ControllerManagement;
import org.eclipse.hawkbit.repository.SystemManagement;
import org.eclipse.hawkbit.repository.TenantConfigurationManagement;
import org.eclipse.hawkbit.repository.model.TenantConfigurationValue;
import org.eclipse.hawkbit.security.DdiSecurityProperties;
import org.eclipse.hawkbit.security.DdiSecurityProperties.Authentication.Anonymous;
import org.eclipse.hawkbit.security.DdiSecurityProperties.Rp;
import org.eclipse.hawkbit.security.SecurityContextTenantAware;
import org.eclipse.hawkbit.security.SystemSecurityContext;
import org.eclipse.hawkbit.tenancy.configuration.TenantConfigurationKey;
import org.junit.Before;
import org.junit.Test;
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

import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

/**
 *
 * Test Amqp controller authentication.
 */
@Features("Component Tests - Device Management Federation API")
@Stories("AmqpController Authentication Test")
public class AmqpControllerAuthenticationTest {

    private static final String TENANT = "DEFAULT";
    private static final Long TENANT_ID = 123L;
    private static String CONTROLLLER_ID = "123";
    private static final Long TARGET_ID = 123L;
    private AmqpMessageHandlerService amqpMessageHandlerService;
    private MessageConverter messageConverter;
    private TenantConfigurationManagement tenantConfigurationManagement;
    private AmqpControllerAuthentication authenticationManager;
    private SystemManagement systemManagement;

    private static final TenantConfigurationValue<Boolean> CONFIG_VALUE_FALSE = TenantConfigurationValue
            .<Boolean> builder().value(Boolean.FALSE).build();

    private static final TenantConfigurationValue<Boolean> CONFIG_VALUE_TRUE = TenantConfigurationValue
            .<Boolean> builder().value(Boolean.TRUE).build();

    @Before
    public void before() throws Exception {
        messageConverter = new Jackson2JsonMessageConverter();
        final RabbitTemplate rabbitTemplate = mock(RabbitTemplate.class);
        when(rabbitTemplate.getMessageConverter()).thenReturn(messageConverter);
        amqpMessageHandlerService = new AmqpMessageHandlerService(rabbitTemplate,
                mock(AmqpMessageDispatcherService.class));

        final DdiSecurityProperties secruityProperties = mock(DdiSecurityProperties.class);
        final Rp rp = mock(Rp.class);
        final org.eclipse.hawkbit.security.DdiSecurityProperties.Authentication ddiAuthentication = mock(
                org.eclipse.hawkbit.security.DdiSecurityProperties.Authentication.class);
        final Anonymous anonymous = mock(Anonymous.class);
        when(secruityProperties.getRp()).thenReturn(rp);
        when(rp.getSslIssuerHashHeader()).thenReturn("X-Ssl-Issuer-Hash-%d");
        when(secruityProperties.getAuthentication()).thenReturn(ddiAuthentication);
        when(ddiAuthentication.getAnonymous()).thenReturn(anonymous);
        when(anonymous.isEnabled()).thenReturn(false);

        tenantConfigurationManagement = mock(TenantConfigurationManagement.class);

        when(tenantConfigurationManagement.getConfigurationValue(any(), eq(Boolean.class)))
                .thenReturn(CONFIG_VALUE_FALSE);

        final ControllerManagement controllerManagement = mock(ControllerManagement.class);
        when(controllerManagement.getSecurityTokenByControllerId(anyString())).thenReturn(CONTROLLLER_ID);
        amqpMessageHandlerService.setArtifactManagement(mock(ArtifactManagement.class));

        final SecurityContextTenantAware tenantAware = new SecurityContextTenantAware();
        final SystemSecurityContext systemSecurityContext = new SystemSecurityContext(tenantAware);

        systemManagement = mock(SystemManagement.class);

        authenticationManager = new AmqpControllerAuthentication(systemManagement, controllerManagement,
                tenantConfigurationManagement, tenantAware, secruityProperties, systemSecurityContext);

        authenticationManager.postConstruct();
        amqpMessageHandlerService.setAuthenticationManager(authenticationManager);
    }

    @Test
    @Description("Tests authentication manager without principal")
    public void testAuthenticationeBadCredantialsWithoutPricipal() {
        final TenantSecurityToken securityToken = new TenantSecurityToken(TENANT, TENANT_ID, CONTROLLLER_ID, TARGET_ID,
                FileResource.createFileResourceBySha1("12345"));
        try {
            authenticationManager.doAuthenticate(securityToken);
            fail("BadCredentialsException was excepeted since principal was missing");
        } catch (final BadCredentialsException exception) {
            // test ok - exception was excepted
        }

    }

    @Test
    @Description("Tests authentication manager without wrong credential")
    public void testAuthenticationBadCredantialsWithWrongCredential() {
        final TenantSecurityToken securityToken = new TenantSecurityToken(TENANT, TENANT_ID, CONTROLLLER_ID, TARGET_ID,
                FileResource.createFileResourceBySha1("12345"));
        when(tenantConfigurationManagement.getConfigurationValue(
                eq(TenantConfigurationKey.AUTHENTICATION_MODE_TARGET_SECURITY_TOKEN_ENABLED), eq(Boolean.class)))
                        .thenReturn(CONFIG_VALUE_TRUE);
        securityToken.getHeaders().put(TenantSecurityToken.AUTHORIZATION_HEADER, "TargetToken 12" + CONTROLLLER_ID);
        try {
            authenticationManager.doAuthenticate(securityToken);
            fail("BadCredentialsException was excepeted due to wrong credential");
        } catch (final BadCredentialsException exception) {
            // test ok - exception was excepted
        }

    }

    @Test
    @Description("Tests authentication successfull")
    public void testSuccessfullAuthentication() {
        final TenantSecurityToken securityToken = new TenantSecurityToken(TENANT, TENANT_ID, CONTROLLLER_ID, TARGET_ID,
                FileResource.createFileResourceBySha1("12345"));
        when(tenantConfigurationManagement.getConfigurationValue(
                eq(TenantConfigurationKey.AUTHENTICATION_MODE_TARGET_SECURITY_TOKEN_ENABLED), eq(Boolean.class)))
                        .thenReturn(CONFIG_VALUE_TRUE);
        securityToken.getHeaders().put(TenantSecurityToken.AUTHORIZATION_HEADER, "TargetToken " + CONTROLLLER_ID);
        final Authentication authentication = authenticationManager.doAuthenticate(securityToken);
        assertThat(authentication).isNotNull();
    }

    @Test
    @Description("Tests authentication message without principal")
    public void testAuthenticationMessageBadCredantialsWithoutPricipal() {
        final MessageProperties messageProperties = createMessageProperties(null);

        final TenantSecurityToken securityToken = new TenantSecurityToken(TENANT, TENANT_ID, CONTROLLLER_ID, TARGET_ID,
                FileResource.createFileResourceBySha1("12345"));
        final Message message = amqpMessageHandlerService.getMessageConverter().toMessage(securityToken,
                messageProperties);

        // test
        final Message onMessage = amqpMessageHandlerService.onAuthenticationRequest(message);

        // verify
        final DownloadResponse downloadResponse = (DownloadResponse) messageConverter.fromMessage(onMessage);
        assertThat(downloadResponse).isNotNull();
        assertThat(downloadResponse.getResponseCode()).isEqualTo(HttpStatus.FORBIDDEN.value());
    }

    @Test
    @Description("Tests authentication message without wrong credential")
    public void testAuthenticationMessageBadCredantialsWithWrongCredential() {
        final MessageProperties messageProperties = createMessageProperties(null);
        final TenantSecurityToken securityToken = new TenantSecurityToken(TENANT, TENANT_ID, CONTROLLLER_ID, TARGET_ID,
                FileResource.createFileResourceBySha1("12345"));
        when(tenantConfigurationManagement.getConfigurationValue(
                eq(TenantConfigurationKey.AUTHENTICATION_MODE_TARGET_SECURITY_TOKEN_ENABLED), eq(Boolean.class)))
                        .thenReturn(CONFIG_VALUE_TRUE);
        securityToken.getHeaders().put(TenantSecurityToken.AUTHORIZATION_HEADER, "TargetToken 12" + CONTROLLLER_ID);
        final Message message = amqpMessageHandlerService.getMessageConverter().toMessage(securityToken,
                messageProperties);

        // test
        final Message onMessage = amqpMessageHandlerService.onAuthenticationRequest(message);

        // verify
        final DownloadResponse downloadResponse = (DownloadResponse) messageConverter.fromMessage(onMessage);
        assertThat(downloadResponse).isNotNull();
        assertThat(downloadResponse.getResponseCode()).isEqualTo(HttpStatus.FORBIDDEN.value());
    }

    @Test
    @Description("Tests authentication message successfull")
    public void testSuccessfullMessageAuthentication() {
        final MessageProperties messageProperties = createMessageProperties(null);
        final TenantSecurityToken securityToken = new TenantSecurityToken(TENANT, TENANT_ID, CONTROLLLER_ID, TARGET_ID,
                FileResource.createFileResourceBySha1("12345"));
        when(tenantConfigurationManagement.getConfigurationValue(
                eq(TenantConfigurationKey.AUTHENTICATION_MODE_TARGET_SECURITY_TOKEN_ENABLED), eq(Boolean.class)))
                        .thenReturn(CONFIG_VALUE_TRUE);
        securityToken.getHeaders().put(TenantSecurityToken.AUTHORIZATION_HEADER, "TargetToken " + CONTROLLLER_ID);
        final Message message = amqpMessageHandlerService.getMessageConverter().toMessage(securityToken,
                messageProperties);

        // test
        final Message onMessage = amqpMessageHandlerService.onAuthenticationRequest(message);

        // verify
        final DownloadResponse downloadResponse = (DownloadResponse) messageConverter.fromMessage(onMessage);
        assertThat(downloadResponse).isNotNull();
        assertThat(downloadResponse.getResponseCode()).isEqualTo(HttpStatus.NOT_FOUND.value());
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
