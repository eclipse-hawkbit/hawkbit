/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eclipse.hawkbit.dmf.amqp.api.AmqpSettings;
import org.eclipse.hawkbit.dmf.amqp.api.MessageHeaderKey;
import org.eclipse.hawkbit.dmf.json.model.DownloadResponse;
import org.eclipse.hawkbit.dmf.json.model.TenantSecurityToken;
import org.eclipse.hawkbit.dmf.json.model.TenantSecurityToken.FileResource;
import org.eclipse.hawkbit.repository.model.Artifact;
import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.repository.model.SoftwareModule;
import org.eclipse.hawkbit.repository.model.Target;
import org.eclipse.hawkbit.repository.model.TargetWithActionType;
import org.eclipse.hawkbit.tenancy.configuration.TenantConfigurationProperties.TenantConfigurationKey;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.amqp.UncategorizedAmqpException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.listener.exception.ListenerExecutionFailedException;
import org.springframework.context.annotation.Description;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.BadCredentialsException;

import com.google.common.base.Strings;

import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

/**
 * All tenant configuration properties concerning the authentication are set to
 * false!
 *
 */
@Features("Component Tests - Device Management Federation API")
@Stories("Amqp Authentication Message Handler")
public class AmqpAuthenticationMessageHandlerIntegrationTest extends AbstractAmqpIntegrationTest {

    protected static final String TENANT_EXIST = "DEFAULT";
    protected static final String TENANT_NOT_EXIST = "NOT_EXISTING_TENANT";

    protected static final String TARGET = "NewDmfTarget";

    @Test
    @Description("Login fails as the given tenant does not exists.")
    public void loginFailedBadCredentials() {
        try {
            final TenantSecurityToken securityToken = createTenantSecurityToken(TENANT_NOT_EXIST, TARGET,
                    FileResource.createFileResourceBySha1("12345"));

            createAndSendMessage(TENANT_NOT_EXIST, securityToken);

            fail("BadCredentialsException is thrown as no anonymous download is allowed");
        } catch (final BadCredentialsException e) {
        } finally {
            verifyDeadLetterZeroInteractions();
        }
    }

    @Test
    @Description("UncategorizedAmqpException is thrown as message sent to dmf client is null")
    public void messageIsNull() {
        try {
            getDmfClient().sendAndReceive(null);
            fail("UncategorizedAmqpException is thrown as message sent to dmf client is null");
        } catch (final UncategorizedAmqpException e) {
        } finally {
            verifyDeadLetterZeroInteractions();
        }
    }

    @Test
    @Description("UncategorizedAmqpException is thrown as message sent to dmf client is empty")
    public void messageIsEmpty() {
        try {
            getDmfClient().sendAndReceive(new Message(null, null));
            fail("UncategorizedAmqpException is thrown as message sent to dmf client is empty");
        } catch (final UncategorizedAmqpException e) {
        } finally {
            verifyDeadLetterZeroInteractions();
        }
    }

    @Test
    @Description("Tenant in the message is null. Test fails due to artifact is not found, anonymous download is allowed.")
    public void tenantInMessageIsNull() {
        allowAnonymousDownload();
        final TenantSecurityToken securityToken = createTenantSecurityToken(TENANT_EXIST, TARGET,
                FileResource.createFileResourceBySha1("12345"));

        final Message returnMessage = createAndSendMessage(null, securityToken);
        verifyResult(returnMessage, HttpStatus.NOT_FOUND,
                "Artifact for resource FileResource [sha1=12345, artifactId=null, filename=null]not found ");

        verifyDeadLetterZeroInteractions();
        denyAnonymousDownload();
    }

    @Test
    @Description("Security Token in the message is null.")
    public void securityTokenIsNull() {
        try {
            createAndSendMessage(TENANT_EXIST, null);
            fail("Should fail as tenant security token is null");
        } catch (final ListenerExecutionFailedException e) {
        } finally {
            verifyDeadLetterZeroInteractions();
        }
    }

    @Test
    @Description("Security Token in the message is empty String. JsonMappingException is thrown as the TenantSecurityToken cannot be constructed.")
    public void securityTokenIsEmptyString() {
        try {
            final Message message = createAuthenticationMessage(TENANT_EXIST, "");
            getDmfClient().sendAndReceive(message);
            fail("Should fail as tenant security token is an empty String");
        } catch (final ListenerExecutionFailedException e) {
        } finally {
            verifyDeadLetterZeroInteractions();
        }
    }

    @Test
    @Description("Tenant in the securityToken is null.")
    public void tenantInSecurityTokenIsNull() {
        denyAnonymousDownload();
        try {
            final TenantSecurityToken securityToken = createTenantSecurityToken(null, TARGET,
                    FileResource.createFileResourceBySha1("12345"));

            createAndSendMessage(TENANT_EXIST, securityToken);
            fail("Should fail as tenant in tenant security token is null");
        } catch (final Exception e) {
        } finally {
            verifyDeadLetterZeroInteractions();
            allowAnonymousDownload();
        }
    }

    @Test
    @Description("NullPointerException is thrown as the given fileResource is null")
    public void fileResourceInSecurityTokenIsNull() {
        try {
            final TenantSecurityToken securityToken = createTenantSecurityToken(TENANT_EXIST, TARGET, null);

            createAndSendMessage(TENANT_EXIST, securityToken);
            fail("Should fail due to a NullPointerException as the fileResource is null");
        } catch (final NullPointerException e) {
        } finally {
            verifyDeadLetterZeroInteractions();
        }
    }

    @Test
    @Description("EntityNotFoundException is thrown as the given sha1 key of the fileResource is null")
    public void fileResourceGetSha1InSecurityTokenIsNull() {

        allowAnonymousDownload();
        final TenantSecurityToken securityToken = createTenantSecurityToken(TENANT_EXIST, TARGET,
                FileResource.createFileResourceBySha1(null));

        final Message returnMessage = createAndSendMessage(TENANT_EXIST, securityToken);
        verifyResult(returnMessage, HttpStatus.NOT_FOUND,
                "Artifact for resource FileResource [sha1=null, artifactId=null, filename=null]not found ");

        verifyDeadLetterZeroInteractions();
        denyAnonymousDownload();
    }

    @Test
    @Description("The controllerId of the securityToken is null which results in an EntityNotFoundException as the belonging artifact is not found")
    public void controllerIdInSecurityTokenIsNull() {
        allowAnonymousDownload();
        createTarget(TARGET);

        final TenantSecurityToken securityToken = createTenantSecurityToken(TENANT_EXIST, null,
                FileResource.createFileResourceBySha1("12345"));

        final Message returnMessage = createAndSendMessage(TENANT_EXIST, securityToken);
        verifyResult(returnMessage, HttpStatus.NOT_FOUND,
                "Artifact for resource FileResource [sha1=12345, artifactId=null, filename=null]not found ");

        verifyDeadLetterZeroInteractions();
        denyAnonymousDownload();
    }

    @Test
    @Description("EntityNotFoundException is thrown as there is no artifact to the given SHA1 key of the fileResource")
    public void artifactForFileResourceSHA1NotFound() {
        allowAnonymousDownload();
        final TenantSecurityToken securityToken = createTenantSecurityToken(TENANT_EXIST, TARGET,
                FileResource.createFileResourceBySha1("12345"));

        final Message returnMessage = createAndSendMessage(TENANT_EXIST, securityToken);
        verifyResult(returnMessage, HttpStatus.NOT_FOUND,
                "Artifact for resource FileResource [sha1=12345, artifactId=null, filename=null]not found ");

        verifyDeadLetterZeroInteractions();
        denyAnonymousDownload();
    }

    @Test
    @Description("EntityNotFoundException is thrown as the target with the given controller id does not exists")
    public void artifactForFileResourceSHA1FoundTargetNotExists() {
        allowAnonymousDownload();
        final DistributionSet distributionSet = testdataFactory.createDistributionSet("one");
        final List<Artifact> artifacts = createArtifacts(distributionSet);
        final TenantSecurityToken securityToken = createTenantSecurityToken(TENANT_EXIST, TARGET,
                FileResource.createFileResourceBySha1(artifacts.get(0).getSha1Hash()));

        final Message returnMessage = createAndSendMessage(TENANT_EXIST, securityToken);
        verifyResult(returnMessage, HttpStatus.NOT_FOUND,
                "Artifact for resource FileResource [sha1=2f532b93ed23b4341a81dc9b1ee8a1c44b5526ab, artifactId=null, filename=null]not found ");

        verifyDeadLetterZeroInteractions();
        denyAnonymousDownload();
    }

    @Test
    @Description("EntityNotFoundException is thrown as there are no artifacts assigned to the given target")
    public void artifactForFileResourceSHA1FoundTargetExistsButNotAssigned() {
        allowAnonymousDownload();
        createTarget(TARGET);

        final DistributionSet distributionSet = testdataFactory.createDistributionSet("one");
        final List<Artifact> artifacts = createArtifacts(distributionSet);
        final TenantSecurityToken securityToken = createTenantSecurityToken(TENANT_EXIST, TARGET,
                FileResource.createFileResourceBySha1(artifacts.get(0).getSha1Hash()));

        final Message returnMessage = createAndSendMessage(TENANT_EXIST, securityToken);
        verifyResult(returnMessage, HttpStatus.NOT_FOUND,
                "Artifact for resource FileResource [sha1=2f532b93ed23b4341a81dc9b1ee8a1c44b5526ab, artifactId=null, filename=null]not found ");

        verifyDeadLetterZeroInteractions();
        denyAnonymousDownload();
    }

    @Test
    @Description("Artifact to the given SHA1 key of the fileResource is found, distributionSet is assigned to the target. Successful.")
    public void artifactForFileResourceSHA1FoundTargetExistsIsAssigned() {
        allowAnonymousDownload();
        createTarget(TARGET);

        final DistributionSet distributionSet = testdataFactory.createDistributionSet("one");
        final List<Artifact> artifacts = createArtifacts(distributionSet);
        final TenantSecurityToken securityToken = createTenantSecurityToken(TENANT_EXIST, TARGET,
                FileResource.createFileResourceBySha1(artifacts.get(0).getSha1Hash()));

        deploymentManagement.assignDistributionSet(distributionSet.getId(),
                Arrays.asList(new TargetWithActionType(TARGET)));

        final Message returnMessage = createAndSendMessage(TENANT_EXIST, securityToken);
        verifyResult(returnMessage, HttpStatus.OK, null);

        verifyDeadLetterZeroInteractions();
        denyAnonymousDownload();
    }

    @Test
    @Description("Artifact to the given SHA1 key of the fileResource is found, distributionSet is assigned to the target. TargetId is used for TenantSecurityToken. Successful.")
    public void artifactForFileResourceSHA1FoundByTargetIdTargetExistsIsAssigned() {
        allowAnonymousDownload();
        final Target target = createTarget(TARGET);

        final DistributionSet distributionSet = testdataFactory.createDistributionSet("one");
        final List<Artifact> artifacts = createArtifacts(distributionSet);
        final TenantSecurityToken securityToken = createTenantSecurityToken(TENANT_EXIST, target.getId(), null,
                FileResource.createFileResourceBySha1(artifacts.get(0).getSha1Hash()));

        deploymentManagement.assignDistributionSet(distributionSet.getId(),
                Arrays.asList(new TargetWithActionType(TARGET)));

        final Message returnMessage = createAndSendMessage(TENANT_EXIST, securityToken);
        verifyResult(returnMessage, HttpStatus.OK, null);

        verifyDeadLetterZeroInteractions();
        denyAnonymousDownload();
    }

    @Test
    @Description("Artifact to the given SHA1 key of the fileResource is found, distributionSet is NOT assigned to the target. TargetId is used for TenantSecurityToken.")
    public void artifactForFileResourceSHA1FoundByTargetIdTargetExistsButIsNotAssigned() {
        allowAnonymousDownload();
        final Target target = createTarget(TARGET);

        final DistributionSet distributionSet = testdataFactory.createDistributionSet("one");
        final List<Artifact> artifacts = createArtifacts(distributionSet);
        final TenantSecurityToken securityToken = createTenantSecurityToken(target.getTenant(), target.getId(), null,
                FileResource.createFileResourceBySha1(artifacts.get(0).getSha1Hash()));

        final Message returnMessage = createAndSendMessage(TENANT_EXIST, securityToken);
        verifyResult(returnMessage, HttpStatus.NOT_FOUND,
                "Artifact for resource FileResource [sha1=2f532b93ed23b4341a81dc9b1ee8a1c44b5526ab, artifactId=null, filename=null]not found ");

        verifyDeadLetterZeroInteractions();
        denyAnonymousDownload();
    }

    @Test
    @Description("Artifact is downloaded anonymous as there is not target id or controller id assigned to the target.")
    public void anonymousDownloadSuccessful() {
        allowAnonymousDownload();
        final DistributionSet distributionSet = testdataFactory.createDistributionSet("one");
        final List<Artifact> artifacts = createArtifacts(distributionSet);
        final TenantSecurityToken securityToken = createTenantSecurityToken(TENANT_EXIST, null, null,
                FileResource.createFileResourceBySha1(artifacts.get(0).getSha1Hash()));

        final Message returnMessage = createAndSendMessage(TENANT_EXIST, securityToken);
        verifyResult(returnMessage, HttpStatus.OK, null);

        verifyDeadLetterZeroInteractions();
        denyAnonymousDownload();
    }

    @Test
    @Description("EntityNotFoundException is thrown as there is no artifact to the given filename of the fileResource")
    public void artifactForFileResourceFileNameNotFound() {
        allowAnonymousDownload();
        final TenantSecurityToken securityToken = createTenantSecurityToken(TENANT_EXIST, TARGET,
                FileResource.createFileResourceByFilename("Test.txt"));

        final Message returnMessage = createAndSendMessage(TENANT_EXIST, securityToken);
        verifyResult(returnMessage, HttpStatus.NOT_FOUND,
                "Artifact for resource FileResource [sha1=null, artifactId=null, filename=Test.txt]not found ");

        verifyDeadLetterZeroInteractions();
        denyAnonymousDownload();
    }

    @Test
    @Description("Artifact to the given filename of the fileResource is found, but target does not exists")
    public void artifactForFileResourceFileNameFoundTargetNotExists() {
        allowAnonymousDownload();
        final DistributionSet distributionSet = testdataFactory.createDistributionSet("one");
        final List<Artifact> artifacts = createArtifacts(distributionSet);
        final TenantSecurityToken securityToken = createTenantSecurityToken(TENANT_EXIST, TARGET,
                FileResource.createFileResourceByFilename(artifacts.get(0).getFilename()));

        final Message returnMessage = createAndSendMessage(TENANT_EXIST, securityToken);
        verifyResult(returnMessage, HttpStatus.NOT_FOUND,
                "Artifact for resource FileResource [sha1=null, artifactId=null, filename=filename0]not found ");

        verifyDeadLetterZeroInteractions();
        denyAnonymousDownload();
        ;
    }

    @Test
    @Description("Artifact to the given filename of the fileResource is found, but there is no distributionSet assigned to the target")
    public void artifactForFileResourceFileNameFoundTargetExistsButNotAssigned() {
        allowAnonymousDownload();
        createTarget(TARGET);

        final DistributionSet distributionSet = testdataFactory.createDistributionSet("one");
        final List<Artifact> artifacts = createArtifacts(distributionSet);
        final TenantSecurityToken securityToken = createTenantSecurityToken(TENANT_EXIST, TARGET,
                FileResource.createFileResourceByFilename(artifacts.get(0).getFilename()));

        final Message returnMessage = createAndSendMessage(TENANT_EXIST, securityToken);
        verifyResult(returnMessage, HttpStatus.NOT_FOUND,
                "Artifact for resource FileResource [sha1=null, artifactId=null, filename=filename0]not found ");

        verifyDeadLetterZeroInteractions();
        denyAnonymousDownload();
    }

    @Test
    @Description("Artifact to the given filename of the fileResource is found, distributionSet is assigned to the target. Successful.")
    public void artifactForFileResourceFileNameFoundTargetExistsIsAssigned() {
        allowAnonymousDownload();
        createTarget(TARGET);

        final DistributionSet distributionSet = testdataFactory.createDistributionSet("one");
        final List<Artifact> artifacts = createArtifacts(distributionSet);
        final TenantSecurityToken securityToken = createTenantSecurityToken(TENANT_EXIST, TARGET,
                FileResource.createFileResourceByFilename(artifacts.get(0).getFilename()));

        deploymentManagement.assignDistributionSet(distributionSet.getId(),
                Arrays.asList(new TargetWithActionType(TARGET)));

        final Message returnMessage = createAndSendMessage(TENANT_EXIST, securityToken);
        verifyResult(returnMessage, HttpStatus.OK, null);

        verifyDeadLetterZeroInteractions();
        denyAnonymousDownload();
    }

    @Test
    @Description("EntityNotFoundException is thrown as there is no artifact to the given artifactId of the fileResource.")
    public void artifactForFileResourceArtifactIdNotFound() {
        allowAnonymousDownload();
        final TenantSecurityToken securityToken = createTenantSecurityToken(TENANT_EXIST, TARGET,
                FileResource.createFileResourceByArtifactId(1L));

        final Message returnMessage = createAndSendMessage(TENANT_EXIST, securityToken);
        verifyResult(returnMessage, HttpStatus.NOT_FOUND,
                "Artifact for resource FileResource [sha1=null, artifactId=1, filename=null]not found ");

        verifyDeadLetterZeroInteractions();
        denyAnonymousDownload();
    }

    @Test
    @Description("Artifact to the given artifactId of the fileResource is found but target is not assigned to distributionSet.")
    public void artifactForFileResourceArtifactIdFoundTargetExistsButNotAssigned() {
        allowAnonymousDownload();
        createTarget(TARGET);

        final DistributionSet distributionSet = createDistributionSet();
        final List<Artifact> artifacts = createArtifacts(distributionSet);
        final FileResource fileResource = FileResource.createFileResourceByArtifactId(artifacts.get(0).getId());
        final TenantSecurityToken securityToken = createTenantSecurityToken(TENANT_EXIST, TARGET, fileResource);

        final Message returnMessage = createAndSendMessage(TENANT_EXIST, securityToken);
        verifyResult(returnMessage, HttpStatus.NOT_FOUND, "Artifact for resource FileResource [sha1=null, artifactId="
                + artifacts.get(0).getId() + ", filename=null]not found ");

        verifyDeadLetterZeroInteractions();
        denyAnonymousDownload();
    }

    @Test
    @Description("Artifact to the given artifactId of the fileResource is found but target does not exists.")
    public void artifactForFileResourceArtifactIdFoundTargetNotExists() {
        allowAnonymousDownload();
        final DistributionSet distributionSet = createDistributionSet();
        final List<Artifact> artifacts = createArtifacts(distributionSet);
        final FileResource fileResource = FileResource.createFileResourceByArtifactId(artifacts.get(0).getId());
        final TenantSecurityToken securityToken = createTenantSecurityToken(TENANT_EXIST, TARGET, fileResource);

        final Message returnMessage = createAndSendMessage(TENANT_EXIST, securityToken);
        verifyResult(returnMessage, HttpStatus.NOT_FOUND, "Artifact for resource FileResource [sha1=null, artifactId="
                + artifacts.get(0).getId() + ", filename=null]not found ");

        verifyDeadLetterZeroInteractions();
        denyAnonymousDownload();
    }

    @Test
    @Description("Artifact to the given artifactId of the fileResource is found and target exists and is assigned to the distributionSet. Successful.")
    public void artifactForFileResourceArtifactIdFoundTargetExistsIsAssigned() {
        allowAnonymousDownload();
        createTarget(TARGET);

        final DistributionSet distributionSet = createDistributionSet();
        final List<Artifact> artifacts = createArtifacts(distributionSet);
        final FileResource fileResource = FileResource.createFileResourceByArtifactId(artifacts.get(0).getId());
        final TenantSecurityToken securityToken = createTenantSecurityToken(TENANT_EXIST, TARGET, fileResource);

        deploymentManagement.assignDistributionSet(distributionSet.getId(),
                Arrays.asList(new TargetWithActionType(TARGET)));

        final Message returnMessage = createAndSendMessage(TENANT_EXIST, securityToken);
        verifyResult(returnMessage, HttpStatus.OK, null);

        verifyDeadLetterZeroInteractions();
        denyAnonymousDownload();
    }

    @Test
    @Description("EntityNotFoundException is thrown as there is no artifact to the given softwareModuleFilename of the fileResource.")
    public void artifactForFileResourceSoftwareModuleFilenameNotFound() {
        allowAnonymousDownload();
        final TenantSecurityToken securityToken = createTenantSecurityToken(TENANT_EXIST, TARGET,
                FileResource.softwareModuleFilename(1L, "Test.txt"));

        final Message returnMessage = createAndSendMessage(TENANT_EXIST, securityToken);
        verifyResult(returnMessage, HttpStatus.NOT_FOUND,
                "Artifact for resource FileResource [sha1=null, artifactId=null, filename=null]not found ");

        verifyDeadLetterZeroInteractions();
        denyAnonymousDownload();
    }

    @Test
    @Description("Artifact to the given softwareModuleFilename of the fileResource is found but target is not assigned to distributionSet.")
    public void artifactForFileResourceSoftwareModuleFilenameFoundTargetExistsButNotAssigned() {
        allowAnonymousDownload();
        createTarget(TARGET);

        final DistributionSet distributionSet = createDistributionSet();
        final List<Artifact> artifacts = createArtifacts(distributionSet);

        final SoftwareModule softwareModule = distributionSet.getModules().stream().findFirst().get();
        final Artifact artifact = findArtifactOfSoftwareModule(artifacts, softwareModule);

        final FileResource fileResource = FileResource.softwareModuleFilename(softwareModule.getId(),
                softwareModule.getArtifact(artifact.getId()).get().getFilename());
        final TenantSecurityToken securityToken = createTenantSecurityToken(TENANT_EXIST, TARGET, fileResource);

        final Message returnMessage = createAndSendMessage(TENANT_EXIST, securityToken);
        verifyResult(returnMessage, HttpStatus.NOT_FOUND,
                "Artifact for resource FileResource [sha1=null, artifactId=null, filename=null]not found ");

        verifyDeadLetterZeroInteractions();
        denyAnonymousDownload();
    }

    @Test
    @Description("Artifact to the given softwareModuleFilename of the fileResource is found but target does not exists.")
    public void artifactForFileResourceSoftwareModuleFilenameFoundTargetNotExists() {
        allowAnonymousDownload();
        final DistributionSet distributionSet = createDistributionSet();
        final List<Artifact> artifacts = createArtifacts(distributionSet);

        final SoftwareModule softwareModule = distributionSet.getModules().stream().findFirst().get();
        final Artifact artifact = findArtifactOfSoftwareModule(artifacts, softwareModule);

        final FileResource fileResource = FileResource.softwareModuleFilename(softwareModule.getId(),
                softwareModule.getArtifact(artifact.getId()).get().getFilename());
        final TenantSecurityToken securityToken = createTenantSecurityToken(TENANT_EXIST, TARGET, fileResource);

        final Message returnMessage = createAndSendMessage(TENANT_EXIST, securityToken);
        verifyResult(returnMessage, HttpStatus.NOT_FOUND,
                "Artifact for resource FileResource [sha1=null, artifactId=null, filename=null]not found ");

        verifyDeadLetterZeroInteractions();
        denyAnonymousDownload();
    }

    @Test
    @Description("Artifact to the given softwareModuleFilename of the fileResource is found and target exists and is assigned to the distributionSet. Successful.")
    public void artifactForFileResourceSoftwareModuleFilenameFoundTargetExistsIsAssigned() {
        allowAnonymousDownload();
        createTarget(TARGET);

        final DistributionSet distributionSet = createDistributionSet();
        final List<Artifact> artifacts = createArtifacts(distributionSet);

        final SoftwareModule softwareModule = distributionSet.getModules().stream().findFirst().get();
        final Artifact artifact = findArtifactOfSoftwareModule(artifacts, softwareModule);

        final FileResource fileResource = FileResource.softwareModuleFilename(softwareModule.getId(),
                softwareModule.getArtifact(artifact.getId()).get().getFilename());
        final TenantSecurityToken securityToken = createTenantSecurityToken(TENANT_EXIST, TARGET, fileResource);

        deploymentManagement.assignDistributionSet(distributionSet.getId(),
                Arrays.asList(new TargetWithActionType(TARGET)));

        final Message returnMessage = createAndSendMessage(TENANT_EXIST, securityToken);
        verifyResult(returnMessage, HttpStatus.OK, null);

        verifyDeadLetterZeroInteractions();
        denyAnonymousDownload();
    }

    private Target createTarget(final String controllerId) {
        return targetManagement.createTarget(entityFactory.target().create().controllerId(controllerId));
    }

    private TenantSecurityToken createTenantSecurityToken(final String tenant, final String controllerId,
            final FileResource fileResource) {
        return new TenantSecurityToken(tenant, controllerId, fileResource);
    }

    private TenantSecurityToken createTenantSecurityToken(final String tenant, final Long targetId,
            final String controllerId, final FileResource fileResource) {
        return new TenantSecurityToken(tenant, null, null, targetId, fileResource);
    }

    private void denyAnonymousDownload() {
        setAnonymousDownloadFlag(false);
    }

    private void allowAnonymousDownload() {
        setAnonymousDownloadFlag(true);
    }

    private void setAnonymousDownloadFlag(final boolean flag) {
        tenantConfigurationManagement.addOrUpdateConfiguration(TenantConfigurationKey.ANONYMOUS_DOWNLOAD_MODE_ENABLED,
                flag);
    }

    private Message createAuthenticationMessage(final String tenant, final Object payload) {
        final MessageProperties messageProperties = createMessagePropertiesWithTenant(tenant);

        return createMessage(payload, messageProperties);
    }

    protected MessageProperties createMessagePropertiesWithTenant(final String tenant) {
        final MessageProperties messageProperties = new MessageProperties();
        messageProperties.getHeaders().put(MessageHeaderKey.TENANT, tenant);
        return messageProperties;
    }

    protected Message createMessage(final Object payload, final MessageProperties messageProperties) {
        if (payload == null) {
            messageProperties.setContentType("json");
            return new Message(null, messageProperties);
        }
        return getDmfClient().getMessageConverter().toMessage(payload, messageProperties);
    }

    private DistributionSet createDistributionSet() {
        return testdataFactory.createDistributionSet("one");
    }

    private List<Artifact> createArtifacts(final DistributionSet distributionSet) {
        final List<Artifact> artifacts = new ArrayList<>();
        for (final org.eclipse.hawkbit.repository.model.SoftwareModule module : distributionSet.getModules()) {
            artifacts.addAll(testdataFactory.createArtifacts(module.getId()));
        }
        return artifacts;
    }

    private void verifyDeadLetterZeroInteractions() {
        Mockito.verifyZeroInteractions(getDeadletterListener());
    }

    private void verifyResult(final Message returnMessage, final HttpStatus expectedStatus,
            final String expectedMessage) {
        final DownloadResponse convertedMessage = (DownloadResponse) getDmfClient().getMessageConverter()
                .fromMessage(returnMessage);
        assertThat(convertedMessage.getResponseCode()).isEqualTo(expectedStatus.value());
        if (!Strings.isNullOrEmpty(expectedMessage)) {
            assertThat(convertedMessage.getMessage()).isEqualTo(expectedMessage);
        }
    }

    private Message createAndSendMessage(final String tenant, final TenantSecurityToken securityToken) {
        final Message message = createAuthenticationMessage(null, securityToken);
        return getDmfClient().sendAndReceive(message);
    }

    private Artifact findArtifactOfSoftwareModule(final List<Artifact> artifacts, final SoftwareModule softwareModule) {
        return artifacts.stream().filter(space -> space.getSoftwareModule().getId().equals(softwareModule.getId()))
                .findFirst().get();
    }

    @Override
    protected String getExchange() {
        return AmqpSettings.AUTHENTICATION_EXCHANGE;
    }

}
