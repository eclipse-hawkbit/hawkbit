/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.ddi.rest.resource;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.Matchers.contains;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.cbor.CBORFactory;
import com.fasterxml.jackson.dataformat.cbor.CBORGenerator;
import com.fasterxml.jackson.dataformat.cbor.CBORParser;
import org.eclipse.hawkbit.ddi.json.model.DdiActionFeedback;
import org.eclipse.hawkbit.ddi.json.model.DdiAssignedVersion;
import org.eclipse.hawkbit.ddi.json.model.DdiConfirmationFeedback;
import org.eclipse.hawkbit.ddi.json.model.DdiProgress;
import org.eclipse.hawkbit.ddi.json.model.DdiResult;
import org.eclipse.hawkbit.ddi.json.model.DdiStatus;
import org.eclipse.hawkbit.repository.jpa.JpaRepositoryConfiguration;
import org.eclipse.hawkbit.repository.jpa.model.JpaDistributionSet;
import org.eclipse.hawkbit.repository.model.Action;
import org.eclipse.hawkbit.repository.model.Artifact;
import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.repository.test.TestConfiguration;
import org.eclipse.hawkbit.rest.AbstractRestIntegrationTest;
import org.eclipse.hawkbit.rest.RestConfiguration;
import org.eclipse.hawkbit.rest.util.MockMvcResultPrinter;
import org.eclipse.hawkbit.tenancy.TenantAware;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

@ContextConfiguration(
        classes = { DdiApiConfiguration.class, RestConfiguration.class, JpaRepositoryConfiguration.class, TestConfiguration.class })
@TestPropertySource(locations = "classpath:/ddi-test.properties")
public abstract class AbstractDDiApiIntegrationTest extends AbstractRestIntegrationTest {

    public static final int HTTP_PORT = 8080;
    protected static final String HTTP_LOCALHOST = String.format("http://localhost:%s/", HTTP_PORT);
    protected static final String CONTROLLER_BASE = "/{tenant}/controller/v1/{controllerId}";

    protected static final String SOFTWARE_MODULE_ARTIFACTS = CONTROLLER_BASE + "/softwaremodules/{softwareModuleId}/artifacts";
    protected static final String DEPLOYMENT_BASE = CONTROLLER_BASE + "/deploymentBase/{actionId}";
    protected static final String DEPLOYMENT_FEEDBACK = DEPLOYMENT_BASE + "/feedback";
    protected static final String CANCEL_ACTION = CONTROLLER_BASE + "/cancelAction/{actionId}";
    protected static final String CANCEL_FEEDBACK = CANCEL_ACTION + "/feedback";
    protected static final String INSTALLED_BASE = CONTROLLER_BASE + "/installedBase/{actionId}";
    protected static final String INSTALLED_BASE_ROOT = CONTROLLER_BASE + "/installedBase";
    protected static final String CONFIRMATION_BASE = CONTROLLER_BASE + "/confirmationBase";
    protected static final String ACTIVATE_AUTO_CONFIRM = CONFIRMATION_BASE + "/activateAutoConfirm";
    protected static final String DEACTIVATE_AUTO_CONFIRM = CONFIRMATION_BASE + "/deactivateAutoConfirm";
    protected static final String CONFIRMATION_BASE_ACTION = CONTROLLER_BASE + "/confirmationBase/{actionId}";

    protected static final String CONFIRMATION_FEEDBACK = CONFIRMATION_BASE_ACTION + "/feedback";

    protected static final int ARTIFACT_SIZE = 5 * 1024;
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    /**
     * Convert JSON to a CBOR equivalent.
     *
     * @param json JSON object to convert
     * @return Equivalent CBOR data
     * @throws IOException Invalid JSON input
     */
    protected static byte[] jsonToCbor(final String json) throws IOException {
        final JsonFactory jsonFactory = new JsonFactory();
        final JsonParser jsonParser = jsonFactory.createParser(json);
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        final CBORFactory cborFactory = new CBORFactory();
        final CBORGenerator cborGenerator = cborFactory.createGenerator(out);
        while (jsonParser.nextToken() != null) {
            cborGenerator.copyCurrentEvent(jsonParser);
        }
        cborGenerator.flush();
        return out.toByteArray();
    }

    /**
     * Convert CBOR to JSON equivalent.
     *
     * @param input CBOR data to convert
     * @return Equivalent JSON string
     * @throws IOException Invalid CBOR input
     */
    protected static String cborToJson(final byte[] input) throws IOException {
        final CBORFactory cborFactory = new CBORFactory();
        final CBORParser cborParser = cborFactory.createParser(input);
        final JsonFactory jsonFactory = new JsonFactory();
        final StringWriter stringWriter = new StringWriter();
        final JsonGenerator jsonGenerator = jsonFactory.createGenerator(stringWriter);
        while (cborParser.nextToken() != null) {
            jsonGenerator.copyCurrentEvent(cborParser);
        }
        jsonGenerator.flush();
        return stringWriter.toString();
    }

    protected static ObjectMapper getMapper() {
        return OBJECT_MAPPER;
    }

    protected ResultActions postDeploymentFeedback(final String controllerId, final Long actionId, final String content,
            final ResultMatcher statusMatcher) throws Exception {
        return postDeploymentFeedback(MediaType.APPLICATION_JSON, controllerId, actionId, content.getBytes(), statusMatcher);
    }

    protected ResultActions putInstalledBase(final String controllerId, final String content, final ResultMatcher statusMatcher)
            throws Exception {
        return mvc.perform(put(INSTALLED_BASE_ROOT, TenantAware.getCurrentTenant(), controllerId)
                        .content(content.getBytes()).contentType(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(statusMatcher);
    }

    protected ResultActions postDeploymentFeedback(
            final MediaType mediaType, final String controllerId,
            final Long actionId, final byte[] content, final ResultMatcher statusMatcher) throws Exception {
        return mvc
                .perform(post(DEPLOYMENT_FEEDBACK, TenantAware.getCurrentTenant(), controllerId, actionId)
                        .content(content).contentType(mediaType).accept(mediaType))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(statusMatcher);
    }

    protected ResultActions postCancelFeedback(
            final String controllerId, final Long actionId, final String content,
            final ResultMatcher statusMatcher) throws Exception {
        return postCancelFeedback(MediaType.APPLICATION_JSON, controllerId, actionId, content.getBytes(), statusMatcher);
    }

    protected ResultActions postCancelFeedback(
            final MediaType mediaType, final String controllerId,
            final Long actionId, final byte[] content, final ResultMatcher statusMatcher) throws Exception {
        return mvc
                .perform(post(CANCEL_FEEDBACK, TenantAware.getCurrentTenant(), controllerId, actionId).content(content)
                        .contentType(mediaType).accept(mediaType))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(statusMatcher);
    }

    protected ResultActions performGet(final String url, final MediaType mediaType, final ResultMatcher statusMatcher, final String... values)
            throws Exception {
        return mvc.perform(MockMvcRequestBuilders.get(url, (Object[]) values).accept(mediaType)
                        .with(new RequestOnHawkbitDefaultPortPostProcessor()))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(statusMatcher)
                .andExpect(content().contentTypeCompatibleWith(mediaType));
    }

    protected ResultActions getAndVerifyDeploymentBasePayload(
            final String controllerId, final MediaType mediaType,
            final DistributionSet ds, final Artifact artifact, final Artifact artifactSignature, final Long actionId,
            final Long osModuleId, final String downloadType, final String updateType) throws Exception {
        final ResultActions resultActions = performGet(DEPLOYMENT_BASE, mediaType, status().isOk(),
                TenantAware.getCurrentTenant(), controllerId, actionId.toString());
        return verifyBasePayload(
                "$.deployment", resultActions, controllerId, ds, artifact, artifactSignature, actionId, osModuleId, downloadType, updateType);
    }

    protected ResultActions getAndVerifyDeploymentBasePayload(
            final String controllerId, final MediaType mediaType,
            final DistributionSet ds, final Artifact artifact, final Artifact artifactSignature, final Long actionId,
            final Long osModuleId, final Action.ActionType actionType) throws Exception {
        return getAndVerifyDeploymentBasePayload(controllerId, mediaType, ds, artifact, artifactSignature, actionId,
                osModuleId, getDownloadAndUploadType(actionType), getDownloadAndUploadType(actionType));
    }

    protected ResultActions getAndVerifyInstalledBasePayload(final String controllerId, final MediaType mediaType,
            final DistributionSet ds, final Artifact artifact, final Artifact artifactSignature, final Long actionId,
            final Long osModuleId, final Action.ActionType actionType) throws Exception {
        final ResultActions resultActions = performGet(INSTALLED_BASE, mediaType, status().isOk(),
                TenantAware.getCurrentTenant(), controllerId, actionId.toString());
        return verifyBasePayload("$.deployment", resultActions, controllerId, ds, artifact, artifactSignature, actionId, osModuleId,
                getDownloadAndUploadType(actionType), getDownloadAndUploadType(actionType));
    }

    protected String installedBaseLink(final String controllerId, final String actionId) {
        return HTTP_LOCALHOST + TenantAware.getCurrentTenant() + "/controller/v1/" +
                controllerId + "/installedBase/" + actionId;
    }

    protected String deploymentBaseLink(final String controllerId, final String actionId) {
        return HTTP_LOCALHOST + TenantAware.getCurrentTenant() + "/controller/v1/" +
                controllerId + "/deploymentBase/" + actionId;
    }

    protected String getJsonRejectedCancelActionFeedback() throws JsonProcessingException {
        return getJsonActionFeedback(
                DdiStatus.ExecutionStatus.REJECTED, DdiResult.FinalResult.SUCCESS, Collections.singletonList("rejected"));
    }

    protected String getJsonRejectedDeploymentActionFeedback() throws JsonProcessingException {
        return getJsonActionFeedback(
                DdiStatus.ExecutionStatus.REJECTED, DdiResult.FinalResult.NONE, Collections.singletonList("rejected"));
    }

    protected String getJsonDownloadDeploymentActionFeedback() throws JsonProcessingException {
        return getJsonActionFeedback(
                DdiStatus.ExecutionStatus.DOWNLOAD, DdiResult.FinalResult.NONE, Collections.singletonList("download"));
    }

    protected String getJsonDownloadedDeploymentActionFeedback() throws JsonProcessingException {
        return getJsonActionFeedback(
                DdiStatus.ExecutionStatus.DOWNLOADED, DdiResult.FinalResult.NONE, Collections.singletonList("download"));
    }

    protected String getJsonCanceledCancelActionFeedback() throws JsonProcessingException {
        return getJsonActionFeedback(
                DdiStatus.ExecutionStatus.CANCELED, DdiResult.FinalResult.SUCCESS, Collections.singletonList("canceled"));
    }

    protected String getJsonCanceledDeploymentActionFeedback() throws JsonProcessingException {
        return getJsonActionFeedback(
                DdiStatus.ExecutionStatus.CANCELED, DdiResult.FinalResult.NONE, Collections.singletonList("canceled"));
    }

    protected String getJsonScheduledCancelActionFeedback() throws JsonProcessingException {
        return getJsonActionFeedback(
                DdiStatus.ExecutionStatus.SCHEDULED, DdiResult.FinalResult.SUCCESS, Collections.singletonList("scheduled"));
    }

    protected String getJsonScheduledDeploymentActionFeedback() throws JsonProcessingException {
        return getJsonActionFeedback(
                DdiStatus.ExecutionStatus.SCHEDULED, DdiResult.FinalResult.NONE, Collections.singletonList("scheduled"));
    }

    protected String getJsonResumedCancelActionFeedback() throws JsonProcessingException {
        return getJsonActionFeedback(
                DdiStatus.ExecutionStatus.RESUMED, DdiResult.FinalResult.SUCCESS, Collections.singletonList("resumed"));
    }

    protected String getJsonResumedDeploymentActionFeedback() throws JsonProcessingException {
        return getJsonActionFeedback(
                DdiStatus.ExecutionStatus.RESUMED, DdiResult.FinalResult.NONE, Collections.singletonList("resumed"));
    }

    protected String getJsonProceedingCancelActionFeedback() throws JsonProcessingException {
        return getJsonActionFeedback(
                DdiStatus.ExecutionStatus.PROCEEDING, DdiResult.FinalResult.SUCCESS, Collections.singletonList("proceeding"));
    }

    protected String getJsonProceedingDeploymentActionFeedback() throws JsonProcessingException {
        return getJsonActionFeedback(
                DdiStatus.ExecutionStatus.PROCEEDING, DdiResult.FinalResult.NONE, Collections.singletonList("proceeding"));
    }

    protected String getJsonClosedCancelActionFeedback() throws JsonProcessingException {
        return getJsonActionFeedback(
                DdiStatus.ExecutionStatus.CLOSED, DdiResult.FinalResult.SUCCESS, Collections.singletonList("closed"));
    }

    protected String getJsonClosedDeploymentActionFeedback() throws JsonProcessingException {
        return getJsonActionFeedback(DdiStatus.ExecutionStatus.CLOSED, DdiResult.FinalResult.NONE, Collections.singletonList("closed"));
    }

    protected String getJsonActionFeedback(
            final DdiStatus.ExecutionStatus executionStatus, final DdiResult.FinalResult finalResult) throws JsonProcessingException {
        return getJsonActionFeedback(executionStatus, finalResult, Collections.singletonList(randomString(1000)));
    }

    protected String getJsonActionFeedback(
            final DdiStatus.ExecutionStatus executionStatus, final DdiResult ddiResult,
            final List<String> messages) throws JsonProcessingException {
        final DdiStatus ddiStatus = new DdiStatus(executionStatus, ddiResult, null, messages);
        return OBJECT_MAPPER.writeValueAsString(new DdiActionFeedback(ddiStatus));
    }

    protected String getJsonActionFeedback(
            final DdiStatus.ExecutionStatus executionStatus,
            final DdiResult.FinalResult finalResult, final List<String> messages) throws JsonProcessingException {
        return getJsonActionFeedback(executionStatus, finalResult, null, messages);
    }

    protected String getJsonActionFeedback(
            final DdiStatus.ExecutionStatus executionStatus,
            final DdiResult.FinalResult finalResult, final Integer code, final List<String> messages) throws JsonProcessingException {
        final DdiStatus ddiStatus = new DdiStatus(executionStatus, new DdiResult(finalResult, new DdiProgress(2, 5)), code, messages);
        return OBJECT_MAPPER.writeValueAsString(new DdiActionFeedback(ddiStatus));
    }

    protected String getJsonConfirmationFeedback(
            final DdiConfirmationFeedback.Confirmation confirmation,
            final Integer code, final List<String> messages) throws JsonProcessingException {
        return OBJECT_MAPPER.writeValueAsString(new DdiConfirmationFeedback(confirmation, code, messages));
    }

    protected String getJsonInstalledBase(String name, String version) throws JsonProcessingException {
        return OBJECT_MAPPER.writeValueAsString(new DdiAssignedVersion(name, version));
    }

    protected ResultActions getAndVerifyConfirmationBasePayload(
            final String controllerId, final MediaType mediaType,
            final DistributionSet ds, final Artifact artifact, final Artifact artifactSignature, final Long actionId,
            final Long osModuleId, final String downloadType, final String updateType) throws Exception {
        final ResultActions resultActions = performGet(
                CONFIRMATION_BASE_ACTION, mediaType, status().isOk(),
                TenantAware.getCurrentTenant(), controllerId, actionId.toString());
        return verifyBasePayload(
                "$.confirmation", resultActions, controllerId, ds, artifact, artifactSignature, actionId, osModuleId,
                downloadType, updateType);
    }

    static byte[] nextBytes(final int size) {
        final byte[] bytes = new byte[size];
        RND.nextBytes(bytes);
        return bytes;
    }

    static void implicitLock(final DistributionSet set) {
        ((JpaDistributionSet) set).setOptLockRevision(set.getOptLockRevision() + 1);
    }

    private static String getDownloadAndUploadType(final Action.ActionType actionType) {
        if (Action.ActionType.FORCED.equals(actionType)) {
            return "forced";
        }
        return "attempt";
    }

    private ResultActions verifyBasePayload(
            final String prefix, final ResultActions resultActions, final String controllerId,
            final DistributionSet ds, final Artifact artifact, final Artifact artifactSignature, final Long actionId,
            final Long osModuleId, final String downloadType, final String updateType) throws Exception {
        return resultActions.andExpect(jsonPath("$.id", equalTo(String.valueOf(actionId))))
                .andExpect(jsonPath(prefix + ".download", equalTo(downloadType)))
                .andExpect(jsonPath(prefix + ".update", equalTo(updateType)))
                .andExpect(jsonPath(prefix + ".chunks[?(@.part=='jvm')].name",
                        contains(findFirstModuleByType(ds, runtimeType).orElseThrow().getName())))
                .andExpect(jsonPath(prefix + ".chunks[?(@.part=='jvm')].version",
                        contains(findFirstModuleByType(ds, runtimeType).orElseThrow().getVersion())))
                .andExpect(jsonPath(prefix + ".chunks[?(@.part=='os')].name",
                        contains(findFirstModuleByType(ds, osType).orElseThrow().getName())))
                .andExpect(jsonPath(prefix + ".chunks[?(@.part=='os')].version",
                        contains(findFirstModuleByType(ds, osType).orElseThrow().getVersion())))
                .andExpect(jsonPath(prefix + ".chunks[?(@.part=='os')].artifacts[0].size", contains(ARTIFACT_SIZE)))
                .andExpect(jsonPath(prefix + ".chunks[?(@.part=='os')].artifacts[0].filename",
                        contains(artifact.getFilename())))
                .andExpect(jsonPath(prefix + ".chunks[?(@.part=='os')].artifacts[0].hashes.md5",
                        contains(artifact.getMd5Hash())))
                .andExpect(jsonPath(prefix + ".chunks[?(@.part=='os')].artifacts[0].hashes.sha1",
                        contains(artifact.getSha1Hash())))
                .andExpect(jsonPath(prefix + ".chunks[?(@.part=='os')].artifacts[0].hashes.sha256",
                        contains(artifact.getSha256Hash())))
                .andExpect(jsonPath(prefix + ".chunks[?(@.part=='os')].artifacts[0]._links.download-http.href",
                        contains(HTTP_LOCALHOST + TenantAware.getCurrentTenant() + "/controller/v1/" + controllerId +
                                "/softwaremodules/" + osModuleId + "/artifacts/" + artifact.getFilename())))
                .andExpect(jsonPath(prefix + ".chunks[?(@.part=='os')].artifacts[0]._links.md5sum-http.href",
                        contains(HTTP_LOCALHOST + TenantAware.getCurrentTenant() + "/controller/v1/" + controllerId +
                                "/softwaremodules/" + osModuleId + "/artifacts/" + artifact.getFilename() + ".MD5SUM")))
                .andExpect(jsonPath(prefix + ".chunks[?(@.part=='os')].artifacts[1].size", contains(ARTIFACT_SIZE)))
                .andExpect(jsonPath(prefix + ".chunks[?(@.part=='os')].artifacts[1].filename",
                        contains(artifactSignature.getFilename())))
                .andExpect(jsonPath(prefix + ".chunks[?(@.part=='os')].artifacts[1].hashes.md5",
                        contains(artifactSignature.getMd5Hash())))
                .andExpect(jsonPath(prefix + ".chunks[?(@.part=='os')].artifacts[1].hashes.sha1",
                        contains(artifactSignature.getSha1Hash())))
                .andExpect(jsonPath(prefix + ".chunks[?(@.part=='os')].artifacts[1].hashes.sha256",
                        contains(artifactSignature.getSha256Hash())))
                .andExpect(jsonPath(prefix + ".chunks[?(@.part=='os')].artifacts[1]._links.download-http.href",
                        contains(HTTP_LOCALHOST + TenantAware.getCurrentTenant() + "/controller/v1/" + controllerId +
                                "/softwaremodules/" + osModuleId + "/artifacts/" + artifactSignature.getFilename())))
                .andExpect(jsonPath(prefix + ".chunks[?(@.part=='os')].artifacts[1]._links.md5sum-http.href",
                        contains(HTTP_LOCALHOST + TenantAware.getCurrentTenant() + "/controller/v1/" + controllerId +
                                "/softwaremodules/" + osModuleId + "/artifacts/" + artifactSignature.getFilename() + ".MD5SUM")))
                .andExpect(jsonPath(prefix + ".chunks[?(@.part=='bApp')].version",
                        contains(findFirstModuleByType(ds, appType).orElseThrow().getVersion())))
                .andExpect(jsonPath(prefix + ".chunks[?(@.part=='bApp')].metadata").doesNotExist())
                .andExpect(jsonPath(prefix + ".chunks[?(@.part=='bApp')].name")
                        .value(findFirstModuleByType(ds, appType).orElseThrow().getName()));
    }

    protected Optional<DistributionSet> findDsByAction(final long actionId) {
        return deploymentManagement.findAction(actionId).map(Action::getDistributionSet);
    }
}