/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ddi.rest.resource;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.Matchers.contains;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.StringWriter;

import org.eclipse.hawkbit.repository.jpa.RepositoryApplicationConfiguration;
import org.eclipse.hawkbit.repository.model.Action;
import org.eclipse.hawkbit.repository.model.Artifact;
import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.repository.test.TestConfiguration;
import org.eclipse.hawkbit.rest.AbstractRestIntegrationTest;
import org.eclipse.hawkbit.rest.RestConfiguration;
import org.eclipse.hawkbit.rest.util.MockMvcResultPrinter;
import org.springframework.cloud.stream.test.binder.TestSupportBinderAutoConfiguration;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.dataformat.cbor.CBORFactory;
import com.fasterxml.jackson.dataformat.cbor.CBORGenerator;
import com.fasterxml.jackson.dataformat.cbor.CBORParser;

@ContextConfiguration(classes = { DdiApiConfiguration.class, RestConfiguration.class,
        RepositoryApplicationConfiguration.class, TestConfiguration.class, TestSupportBinderAutoConfiguration.class })
@TestPropertySource(locations = "classpath:/ddi-test.properties")
public abstract class AbstractDDiApiIntegrationTest extends AbstractRestIntegrationTest {

    protected static final String HTTP_LOCALHOST = "http://localhost:8080/";
    protected static final String CONTROLLER_BASE = "/{tenant}/controller/v1/{controllerId}";

    protected static final String SOFTWARE_MODULE_ARTIFACTS = CONTROLLER_BASE
            + "/softwaremodules/{softwareModuleId}/artifacts";
    protected static final String DEPLOYMENT_BASE = CONTROLLER_BASE + "/deploymentBase/{actionId}";
    protected static final String CANCEL_ACTION = CONTROLLER_BASE + "/cancelAction/{actionId}";
    protected static final String INSTALLED_BASE = CONTROLLER_BASE + "/installedBase/{actionId}";
    protected static final String DEPLOYMENT_FEEDBACK = DEPLOYMENT_BASE + "/feedback";
    protected static final String CANCEL_FEEDBACK = CANCEL_ACTION + "/feedback";

    protected static final int ARTIFACT_SIZE = 5 * 1024;

    /**
     * Convert JSON to a CBOR equivalent.
     * 
     * @param json
     *            JSON object to convert
     * @return Equivalent CBOR data
     * @throws IOException
     *             Invalid JSON input
     */
    protected static byte[] jsonToCbor(String json) throws IOException {
        JsonFactory jsonFactory = new JsonFactory();
        JsonParser jsonParser = jsonFactory.createParser(json);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        CBORFactory cborFactory = new CBORFactory();
        CBORGenerator cborGenerator = cborFactory.createGenerator(out);
        while (jsonParser.nextToken() != null) {
            cborGenerator.copyCurrentEvent(jsonParser);
        }
        cborGenerator.flush();
        return out.toByteArray();
    }

    /**
     * Convert CBOR to JSON equivalent.
     * 
     * @param input
     *            CBOR data to convert
     * @return Equivalent JSON string
     * @throws IOException
     *             Invalid CBOR input
     */
    protected static String cborToJson(byte[] input) throws IOException {
        CBORFactory cborFactory = new CBORFactory();
        CBORParser cborParser = cborFactory.createParser(input);
        JsonFactory jsonFactory = new JsonFactory();
        StringWriter stringWriter = new StringWriter();
        JsonGenerator jsonGenerator = jsonFactory.createGenerator(stringWriter);
        while (cborParser.nextToken() != null) {
            jsonGenerator.copyCurrentEvent(cborParser);
        }
        jsonGenerator.flush();
        return stringWriter.toString();
    }

    protected ResultActions postDeploymentFeedback(final String controllerId, final Long actionId, final String content,
            final ResultMatcher statusMatcher) throws Exception {
        return postDeploymentFeedback(MediaType.APPLICATION_JSON_UTF8, controllerId, actionId, content.getBytes(),
                statusMatcher);
    }

    protected ResultActions postDeploymentFeedback(final MediaType mediaType, final String controllerId,
            final Long actionId, final byte[] content, final ResultMatcher statusMatcher) throws Exception {
        return mvc
                .perform(post(DEPLOYMENT_FEEDBACK, tenantAware.getCurrentTenant(), controllerId, actionId)
                        .content(content).contentType(mediaType).accept(mediaType))
                .andDo(MockMvcResultPrinter.print()).andExpect(statusMatcher);
    }

    protected ResultActions postCancelFeedback(final String controllerId, final Long actionId, final String content,
            final ResultMatcher statusMatcher) throws Exception {
        return postCancelFeedback(MediaType.APPLICATION_JSON_UTF8, controllerId, actionId, content.getBytes(),
                statusMatcher);
    }

    protected ResultActions postCancelFeedback(final MediaType mediaType, final String controllerId,
            final Long actionId, final byte[] content, final ResultMatcher statusMatcher) throws Exception {
        return mvc
                .perform(post(CANCEL_FEEDBACK, tenantAware.getCurrentTenant(), controllerId, actionId).content(content)
                        .contentType(mediaType).accept(mediaType))
                .andDo(MockMvcResultPrinter.print()).andExpect(statusMatcher);
    }

    protected ResultActions performGet(final String url, final MediaType mediaType, final ResultMatcher statusMatcher,
            final String... values) throws Exception {
        return mvc.perform(MockMvcRequestBuilders.get(url, values).accept(mediaType))
                .andDo(MockMvcResultPrinter.print()).andExpect(statusMatcher)
                .andExpect(content().contentTypeCompatibleWith(mediaType));
    }

    protected ResultActions getAndVerifyDeploymentBasePayload(final String controllerId, final MediaType mediaType,
            final DistributionSet ds, final Artifact artifact, final Artifact artifactSignature, final Long actionId,
            final Long osModuleId, final String downloadType, final String updateType) throws Exception {
        final ResultActions resultActions = performGet(DEPLOYMENT_BASE, mediaType, status().isOk(),
                tenantAware.getCurrentTenant(), controllerId, actionId.toString());
        return verifyBasePayload(resultActions, controllerId, ds, artifact, artifactSignature, actionId, osModuleId,
                downloadType, updateType);
    }

    protected ResultActions getAndVerifyDeploymentBasePayload(final String controllerId, final MediaType mediaType,
            final DistributionSet ds, final Artifact artifact, final Artifact artifactSignature, final Long actionId,
            final Long osModuleId, final Action.ActionType actionType) throws Exception {
        return getAndVerifyDeploymentBasePayload(controllerId, mediaType, ds, artifact, artifactSignature, actionId,
                osModuleId, getDownloadAndUploadType(actionType), getDownloadAndUploadType(actionType));
    }

    protected ResultActions getAndVerifyInstalledBasePayload(final String controllerId, final MediaType mediaType,
            final DistributionSet ds, final Artifact artifact, final Artifact artifactSignature, final Long actionId,
            final Long osModuleId, final Action.ActionType actionType) throws Exception {
        final ResultActions resultActions = performGet(INSTALLED_BASE, mediaType, status().isOk(),
                tenantAware.getCurrentTenant(), controllerId, actionId.toString());
        return verifyBasePayload(resultActions, controllerId, ds, artifact, artifactSignature, actionId, osModuleId,
                getDownloadAndUploadType(actionType), getDownloadAndUploadType(actionType));
    }

    private ResultActions verifyBasePayload(ResultActions resultActions, final String controllerId,
            final DistributionSet ds, final Artifact artifact, final Artifact artifactSignature, final Long actionId,
            final Long osModuleId, final String downloadType, final String updateType) throws Exception {
        return resultActions.andExpect(jsonPath("$.id", equalTo(String.valueOf(actionId))))
                .andExpect(jsonPath("$.deployment.download", equalTo(downloadType)))
                .andExpect(jsonPath("$.deployment.update", equalTo(updateType)))
                .andExpect(jsonPath("$.deployment.chunks[?(@.part=='jvm')].name",
                        contains(ds.findFirstModuleByType(runtimeType).get().getName())))
                .andExpect(jsonPath("$.deployment.chunks[?(@.part=='jvm')].version",
                        contains(ds.findFirstModuleByType(runtimeType).get().getVersion())))
                .andExpect(jsonPath("$.deployment.chunks[?(@.part=='os')].name",
                        contains(ds.findFirstModuleByType(osType).get().getName())))
                .andExpect(jsonPath("$.deployment.chunks[?(@.part=='os')].version",
                        contains(ds.findFirstModuleByType(osType).get().getVersion())))
                .andExpect(jsonPath("$.deployment.chunks[?(@.part=='os')].artifacts[0].size", contains(ARTIFACT_SIZE)))
                .andExpect(jsonPath("$.deployment.chunks[?(@.part=='os')].artifacts[0].filename",
                        contains(artifact.getFilename())))
                .andExpect(jsonPath("$.deployment.chunks[?(@.part=='os')].artifacts[0].hashes.md5",
                        contains(artifact.getMd5Hash())))
                .andExpect(jsonPath("$.deployment.chunks[?(@.part=='os')].artifacts[0].hashes.sha1",
                        contains(artifact.getSha1Hash())))
                .andExpect(jsonPath("$.deployment.chunks[?(@.part=='os')].artifacts[0].hashes.sha256",
                        contains(artifact.getSha256Hash())))
                .andExpect(jsonPath("$.deployment.chunks[?(@.part=='os')].artifacts[0]._links.download-http.href",
                        contains(HTTP_LOCALHOST + tenantAware.getCurrentTenant() + "/controller/v1/" + controllerId
                                + "/softwaremodules/" + osModuleId + "/artifacts/" + artifact.getFilename())))
                .andExpect(jsonPath("$.deployment.chunks[?(@.part=='os')].artifacts[0]._links.md5sum-http.href",
                        contains(HTTP_LOCALHOST + tenantAware.getCurrentTenant() + "/controller/v1/" + controllerId
                                + "/softwaremodules/" + osModuleId + "/artifacts/" + artifact.getFilename()
                                + ".MD5SUM")))
                .andExpect(jsonPath("$.deployment.chunks[?(@.part=='os')].artifacts[1].size", contains(ARTIFACT_SIZE)))
                .andExpect(jsonPath("$.deployment.chunks[?(@.part=='os')].artifacts[1].filename",
                        contains(artifactSignature.getFilename())))
                .andExpect(jsonPath("$.deployment.chunks[?(@.part=='os')].artifacts[1].hashes.md5",
                        contains(artifactSignature.getMd5Hash())))
                .andExpect(jsonPath("$.deployment.chunks[?(@.part=='os')].artifacts[1].hashes.sha1",
                        contains(artifactSignature.getSha1Hash())))
                .andExpect(jsonPath("$.deployment.chunks[?(@.part=='os')].artifacts[1].hashes.sha256",
                        contains(artifactSignature.getSha256Hash())))
                .andExpect(jsonPath("$.deployment.chunks[?(@.part=='os')].artifacts[1]._links.download-http.href",
                        contains(HTTP_LOCALHOST + tenantAware.getCurrentTenant() + "/controller/v1/" + controllerId
                                + "/softwaremodules/" + osModuleId + "/artifacts/" + artifactSignature.getFilename())))
                .andExpect(jsonPath("$.deployment.chunks[?(@.part=='os')].artifacts[1]._links.md5sum-http.href",
                        contains(HTTP_LOCALHOST + tenantAware.getCurrentTenant() + "/controller/v1/" + controllerId
                                + "/softwaremodules/" + osModuleId + "/artifacts/" + artifactSignature.getFilename()
                                + ".MD5SUM")))
                .andExpect(jsonPath("$.deployment.chunks[?(@.part=='bApp')].version",
                        contains(ds.findFirstModuleByType(appType).get().getVersion())))
                .andExpect(jsonPath("$.deployment.chunks[?(@.part=='bApp')].metadata").doesNotExist())
                .andExpect(jsonPath("$.deployment.chunks[?(@.part=='bApp')].name")
                        .value(ds.findFirstModuleByType(appType).get().getName()));
    }

    protected String installedBaseLink(final String controllerId, final String actionId) {
        return "http://localhost/" + tenantAware.getCurrentTenant() + "/controller/v1/" + controllerId
                + "/installedBase/" + actionId;
    }

    protected String deploymentBaseLink(final String controllerId, final String actionId) {
        return "http://localhost/" + tenantAware.getCurrentTenant() + "/controller/v1/" + controllerId
                + "/deploymentBase/" + actionId;
    }

    private static String getDownloadAndUploadType(final Action.ActionType actionType) {
        if (Action.ActionType.FORCED.equals(actionType)) {
            return "forced";
        }
        return "attempt";
    }

}
