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
import org.eclipse.hawkbit.dmf.json.model.TenantSecruityToken;
import org.eclipse.hawkbit.repository.ArtifactManagement;
import org.eclipse.hawkbit.repository.ControllerManagement;
import org.eclipse.hawkbit.repository.SystemManagement;
import org.eclipse.hawkbit.security.SecurityContextTenantAware;
import org.eclipse.hawkbit.security.SecurityProperties;
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
 *
 */
@Features("AMQP Authenfication Test")
@Stories("Tests the authenfication")
public class AmqpControllerAuthentficationTest {

    private static final String TENANT = "DEFAULT";
    private static String CONTROLLLER_ID = "123";
    private AmqpMessageHandlerService amqpMessageHandlerService;
    private MessageConverter messageConverter;
    private SystemManagement systemManagement;
    private AmqpControllerAuthentfication authenticationManager;

    @Before
    public void before() throws Exception {
        messageConverter = new Jackson2JsonMessageConverter();
        amqpMessageHandlerService = new AmqpMessageHandlerService(messageConverter, mock(RabbitTemplate.class));

        authenticationManager = new AmqpControllerAuthentfication();
        authenticationManager.setControllerManagement(mock(ControllerManagement.class));
        final SecurityProperties secruityProperties = mock(SecurityProperties.class);
        when(secruityProperties.getRpSslIssuerHashHeader()).thenReturn("X-Ssl-Issuer-Hash-%d");
        authenticationManager.setSecruityProperties(secruityProperties);
        systemManagement = mock(SystemManagement.class);
        authenticationManager.setSystemManagement(systemManagement);
        when(systemManagement.getConfigurationValue(any(), any())).thenReturn(Boolean.FALSE);

        final ControllerManagement controllerManagement = mock(ControllerManagement.class);
        when(controllerManagement.getSecurityTokenByControllerId(anyString())).thenReturn(CONTROLLLER_ID);
        authenticationManager.setControllerManagement(controllerManagement);
        amqpMessageHandlerService.setArtifactManagement(mock(ArtifactManagement.class));

        authenticationManager.setTenantAware(new SecurityContextTenantAware());
        authenticationManager.postConstruct();
        amqpMessageHandlerService.setAuthenticationManager(authenticationManager);
    }

    @Test(expected = BadCredentialsException.class)
    @Description("Tests authentication manager without principal")
    public void testAuthenticationeBadCredantialsWithoutPricipal() {
        final TenantSecruityToken securityToken = new TenantSecruityToken(TENANT, CONTROLLLER_ID, "12345");
        authenticationManager.doAuthenticate(securityToken);
        fail();
    }

    @Test(expected = BadCredentialsException.class)
    @Description("Tests authentication manager  without wrong credential")
    public void testAuthenticationBadCredantialsWithWrongCredential() {
        final TenantSecruityToken securityToken = new TenantSecruityToken(TENANT, CONTROLLLER_ID, "12345");
        when(systemManagement.getConfigurationValue(
                eq(TenantConfigurationKey.AUTHENTICATION_MODE_TARGET_SECURITY_TOKEN_ENABLED), any()))
                        .thenReturn(Boolean.TRUE);
        securityToken.getHeaders().put(TenantSecruityToken.AUTHORIZATION_HEADER, "TargetToken 12" + CONTROLLLER_ID);
        authenticationManager.doAuthenticate(securityToken);
        fail();
    }

    @Test
    @Description("Tests authentication successfull")
    public void testSuccessfullAuthentication() {
        final TenantSecruityToken securityToken = new TenantSecruityToken(TENANT, CONTROLLLER_ID, "12345");
        when(systemManagement.getConfigurationValue(
                eq(TenantConfigurationKey.AUTHENTICATION_MODE_TARGET_SECURITY_TOKEN_ENABLED), any()))
                        .thenReturn(Boolean.TRUE);
        securityToken.getHeaders().put(TenantSecruityToken.AUTHORIZATION_HEADER, "TargetToken " + CONTROLLLER_ID);
        final Authentication authentication = authenticationManager.doAuthenticate(securityToken);
        assertThat(authentication).isNotNull();
    }

    @Test
    @Description("Tests authentication message without principal")
    public void testAuthenticationMessageBadCredantialsWithoutPricipal() {
        final MessageProperties messageProperties = createMessageProperties(MessageType.AUTHENTIFICATION);

        final TenantSecruityToken securityToken = new TenantSecruityToken(TENANT, CONTROLLLER_ID, "12345");
        final Message message = amqpMessageHandlerService.getMessageConverter().toMessage(securityToken,
                messageProperties);

        // test
        final Message onMessage = amqpMessageHandlerService.onMessage(message, MessageType.AUTHENTIFICATION.name(),
                TENANT, "vHost");

        // verify
        final DownloadResponse downloadResponse = (DownloadResponse) messageConverter.fromMessage(onMessage);
        assertThat(downloadResponse).isNotNull();
        assertThat(downloadResponse.getResponseCode()).isEqualTo(HttpStatus.FORBIDDEN.value());
    }

    @Test
    @Description("Tests authentication message without wrong credential")
    public void testAuthenticationMessageBadCredantialsWithWrongCredential() {
        final MessageProperties messageProperties = createMessageProperties(MessageType.AUTHENTIFICATION);
        final TenantSecruityToken securityToken = new TenantSecruityToken(TENANT, CONTROLLLER_ID, "12345");
        when(systemManagement.getConfigurationValue(
                eq(TenantConfigurationKey.AUTHENTICATION_MODE_TARGET_SECURITY_TOKEN_ENABLED), any()))
                        .thenReturn(Boolean.TRUE);
        securityToken.getHeaders().put(TenantSecruityToken.AUTHORIZATION_HEADER, "TargetToken 12" + CONTROLLLER_ID);
        final Message message = amqpMessageHandlerService.getMessageConverter().toMessage(securityToken,
                messageProperties);

        // test
        final Message onMessage = amqpMessageHandlerService.onMessage(message, MessageType.AUTHENTIFICATION.name(),
                TENANT, "vHost");

        // verify
        final DownloadResponse downloadResponse = (DownloadResponse) messageConverter.fromMessage(onMessage);
        assertThat(downloadResponse).isNotNull();
        assertThat(downloadResponse.getResponseCode()).isEqualTo(HttpStatus.FORBIDDEN.value());
    }

    @Test
    @Description("Tests authentication message successfull")
    public void testSuccessfullMessageAuthentication() {
        final MessageProperties messageProperties = createMessageProperties(MessageType.AUTHENTIFICATION);
        final TenantSecruityToken securityToken = new TenantSecruityToken(TENANT, CONTROLLLER_ID, "12345");
        when(systemManagement.getConfigurationValue(
                eq(TenantConfigurationKey.AUTHENTICATION_MODE_TARGET_SECURITY_TOKEN_ENABLED), any()))
                        .thenReturn(Boolean.TRUE);
        securityToken.getHeaders().put(TenantSecruityToken.AUTHORIZATION_HEADER, "TargetToken " + CONTROLLLER_ID);
        final Message message = amqpMessageHandlerService.getMessageConverter().toMessage(securityToken,
                messageProperties);

        // test
        final Message onMessage = amqpMessageHandlerService.onMessage(message, MessageType.AUTHENTIFICATION.name(),
                TENANT, "vHost");

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
        messageProperties.setHeader(MessageHeaderKey.TYPE, type.name());
        messageProperties.setHeader(MessageHeaderKey.TENANT, TENANT);
        messageProperties.setContentType(MessageProperties.CONTENT_TYPE_JSON);
        messageProperties.setReplyTo(replyTo);
        return messageProperties;
    }

}
