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

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.hawkbit.context.AccessContext.tenant;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.springframework.http.HttpHeaders.ACCEPT_RANGES;
import static org.springframework.http.HttpHeaders.CONTENT_DISPOSITION;
import static org.springframework.http.HttpHeaders.CONTENT_LENGTH;
import static org.springframework.http.HttpHeaders.CONTENT_RANGE;
import static org.springframework.http.HttpHeaders.CONTENT_TYPE;
import static org.springframework.http.HttpHeaders.ETAG;
import static org.springframework.http.HttpHeaders.IF_MATCH;
import static org.springframework.http.HttpHeaders.LAST_MODIFIED;
import static org.springframework.http.HttpHeaders.RANGE;
import static org.springframework.http.HttpStatus.PARTIAL_CONTENT;
import static org.springframework.http.HttpStatus.REQUESTED_RANGE_NOT_SATISFIABLE;
import static org.springframework.http.MediaType.APPLICATION_OCTET_STREAM;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import org.eclipse.hawkbit.auth.SpRole;
import org.eclipse.hawkbit.ddi.rest.resource.DdiArtifactDownloadTest.DownloadTestConfiguration;
import org.eclipse.hawkbit.repository.event.remote.DownloadProgressEvent;
import org.eclipse.hawkbit.repository.model.Artifact;
import org.eclipse.hawkbit.repository.model.ArtifactUpload;
import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.repository.model.DistributionSetAssignmentResult;
import org.eclipse.hawkbit.repository.model.Target;
import org.eclipse.hawkbit.repository.test.util.TestdataFactory;
import org.eclipse.hawkbit.repository.test.util.WithUser;
import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MvcResult;

/**
 * Test artifact downloads from the controller.
 * <p/>
 * Feature: Component Tests - Direct Device Integration API<br/>
 * Story: Artifact Download Resource
 */
@SpringBootTest(classes = { DownloadTestConfiguration.class })
@TestPropertySource(properties = "hawkbit.ddi.skip-artifact-access-check.tenants=" + DdiArtifactDownloadTest.SKIP_ARTIFACT_CHECK_TENANT)
class DdiArtifactDownloadTest extends AbstractDDiApiIntegrationTest {

    static final String SKIP_ARTIFACT_CHECK_TENANT = "SKIP_ARTIFACT_CHECK_TENANT";

    private static final String DOWNLOAD_FN = "/{tenant}/controller/v1/{controllerId}/softwaremodules/{smId}/artifacts/{filename}";

    private static int downloadProgress;
    private static long shippedBytes;

    private final SimpleDateFormat dateFormat = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz", Locale.ENGLISH);

    @BeforeEach
    void setup() {
        dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
    }

    /**
     * Tests not allowed requests on the artifact resource, e.g. invalid URI, wrong if-match, wrong command.
     */
    @Test
    void invalidRequestsOnArtifactResource() throws Exception {
        final TestData testData = createTargetAndDs(5 * 1024);
        final Target target = testData.target();
        final DistributionSet ds = testData.ds();
        final Artifact artifact = testData.artifact();

        assignDistributionSet(ds, target);

        // no artifact available
        final Long module = testData.moduleId();
        mvc.perform(get(DOWNLOAD_FN, tenant(), target.getControllerId(), module, "123455")).andExpect(status().isNotFound());
        mvc.perform(get(DOWNLOAD_FN + ".MD5SUM", tenant(), target.getControllerId(), module, "123455"))
                .andExpect(status().isNotFound());

        // SM does not exist
        mvc.perform(get(DOWNLOAD_FN, tenant(), target.getControllerId(), "1234567890", artifact.getFilename()))
                .andExpect(status().isNotFound());
        mvc.perform(get(DOWNLOAD_FN + ".MD5SUM", tenant(), target.getControllerId(), "1234567890", artifact.getFilename()))
                .andExpect(status().isNotFound());

        // test now consistent data to test allowed methods
        mvc.perform(get(DOWNLOAD_FN, tenant(), target.getControllerId(), module, artifact.getFilename())
                        .header(IF_MATCH, artifact.getSha1Hash()))
                .andExpect(status().isOk());
        mvc.perform(get(DOWNLOAD_FN + ".MD5SUM", tenant(), target.getControllerId(), module, artifact.getFilename()))
                .andExpect(status().isOk());

        // test failed If-match
        mvc.perform(get(DOWNLOAD_FN, tenant(), target.getControllerId(), module, artifact.getFilename())
                        .header(IF_MATCH, "fsjkhgjfdhg"))
                .andExpect(status().isPreconditionFailed());

        // test invalid range
        mvc.perform(get(DOWNLOAD_FN, tenant(), target.getControllerId(), module, artifact.getFilename())
                        .header(RANGE, "bytes=1-10,hdsfjksdh"))
                .andExpect(header().string(CONTENT_RANGE, "bytes */" + 5 * 1024))
                .andExpect(status().isRequestedRangeNotSatisfiable());

        mvc.perform(get(DOWNLOAD_FN, tenant(), target.getControllerId(), module, artifact.getFilename())
                        .header(RANGE, "bytes=100-10"))
                .andExpect(header().string(CONTENT_RANGE, "bytes */" + 5 * 1024))
                .andExpect(status().isRequestedRangeNotSatisfiable());

        // not allowed methods
        mvc.perform(put(DOWNLOAD_FN, tenant(), target.getControllerId(), module, artifact.getFilename()))
                .andExpect(status().isMethodNotAllowed());
        mvc.perform(delete(DOWNLOAD_FN, tenant(), target.getControllerId(), module, artifact.getFilename()))
                .andExpect(status().isMethodNotAllowed());
        mvc.perform(post(DOWNLOAD_FN, tenant(), target.getControllerId(), module, artifact.getFilename()))
                .andExpect(status().isMethodNotAllowed());
        mvc.perform(put(DOWNLOAD_FN + ".MD5SUM", tenant(), target.getControllerId(), module, artifact.getFilename()))
                .andExpect(status().isMethodNotAllowed());
        mvc.perform(delete(DOWNLOAD_FN + ".MD5SUM", tenant(), target.getControllerId(), module, artifact.getFilename()))
                .andExpect(status().isMethodNotAllowed());
        mvc.perform(post(DOWNLOAD_FN + ".MD5SUM", tenant(), target.getControllerId(), module, artifact.getFilename()))
                .andExpect(status().isMethodNotAllowed());
    }

    /**
     * Tests valid downloads through the artifact resource by identifying the artifact not by ID but file name.
     */
    @Test
    @WithUser(principal = "4712", authorities = { SpRole.CONTROLLER_ROLE, SpRole.TENANT_ADMIN })
    void downloadArtifactThroughFileName() throws Exception {
        synchronized (DdiArtifactDownloadTest.class) {
            downloadProgress = 1;
            shippedBytes = 0;
        }
        assertThat(softwareModuleManagement.findAll(PAGE)).isEmpty();

        final int artifactSize = (int) quotaManagement.getMaxArtifactSize();
        final TestData testData = createTargetAndDs(artifactSize);
        final Target target = testData.target();
        final DistributionSet ds = testData.ds();
        final Artifact artifact = testData.artifact();
        final byte[] random = testData.randomBytes();
        // download fails as artifact is not yet assigned
        final Long module = testData.moduleId();
        mvc.perform(get(DOWNLOAD_FN, tenant(), target.getControllerId(), module, artifact.getFilename()))
                .andExpect(status().isNotFound());

        // now assign and download successful
        assignDistributionSet(ds, target);
        final MvcResult result = mvc.perform(get(DOWNLOAD_FN, tenant(), target.getControllerId(), module, artifact.getFilename()))
                .andExpect(status().isOk())
                .andExpect(content().contentType(APPLICATION_OCTET_STREAM))
                .andExpect(header().string(ACCEPT_RANGES, "bytes"))
                .andExpect(header().string(LAST_MODIFIED, dateFormat.format(new Date(artifact.getCreatedAt()))))
                .andExpect(header().string(CONTENT_DISPOSITION, "attachment;filename=" + artifact.getFilename()))
                .andReturn();

        assertArrayEquals(result.getResponse().getContentAsByteArray(), random, "The same file that was uploaded is expected when downloaded");

        // download complete
        synchronized (DdiArtifactDownloadTest.class) {
            assertThat(downloadProgress).isEqualTo(10);
            assertThat(shippedBytes).isEqualTo(artifactSize);
        }
    }

    @Test
    void listArtifactsAccessCheck() throws Exception {
        listArtifactsAccessCheck0(false);
    }

    @Test
    @WithUser(tenant = SKIP_ARTIFACT_CHECK_TENANT, principal = "controller1", authorities = { SpRole.CONTROLLER_ROLE, SpRole.TENANT_ADMIN })
    void listArtifactsAccessCheckSkip() throws Exception {
        listArtifactsAccessCheck0(true);
    }

    void listArtifactsAccessCheck0(final boolean skip) throws Exception {
        final TestData testData = createTargetAndDs(5 * 1024);
        // no assigment check
        if (skip) {
            // doesn't fail if skip
            mvc.perform(get(SOFTWARE_MODULE_ARTIFACTS, SKIP_ARTIFACT_CHECK_TENANT, testData.target().getControllerId(), testData.moduleId()))
                    .andExpect(status().isOk());
        } else {
            // fail with no assigned or installed
            mvc.perform(get(SOFTWARE_MODULE_ARTIFACTS, tenant(), testData.target().getControllerId(), testData.moduleId()))
                    .andExpect(status().isNotFound());
        }

        // assign
        final DistributionSetAssignmentResult firstAssignment = assignDistributionSet(testData.ds(), testData.target());

        // succeeds with assigned action
        mvc.perform(get(SOFTWARE_MODULE_ARTIFACTS, tenant(), testData.target().getControllerId(), testData.moduleId()))
                .andExpect(status().isOk());

        // succeeds with installed action
        final Long firstActionId = getFirstAssignedActionId(firstAssignment);
        postDeploymentFeedback(testData.target().getControllerId(), firstActionId, getJsonClosedDeploymentActionFeedback(), status().isOk());
        final DistributionSetAssignmentResult secondAssignment = assignDistributionSet(testdataFactory.createDistributionSet("2nd_"),
                testData.target()); // assign new ds
        mvc.perform(get(SOFTWARE_MODULE_ARTIFACTS, tenant(), testData.target().getControllerId(), testData.moduleId()))
                .andExpect(status().isOk());

        // again not assigned or installed
        final Long secondActionId = getFirstAssignedActionId(secondAssignment);
        postDeploymentFeedback(testData.target().getControllerId(), secondActionId, getJsonClosedDeploymentActionFeedback(), status().isOk());
        if (skip) {
            // doesn't fail if skip
            mvc.perform(get(SOFTWARE_MODULE_ARTIFACTS, SKIP_ARTIFACT_CHECK_TENANT, testData.target().getControllerId(), testData.moduleId()))
                    .andExpect(status().isOk());
        } else {
            // fail with no assigned or installed
            mvc.perform(get(SOFTWARE_MODULE_ARTIFACTS, tenant(), testData.target().getControllerId(), testData.moduleId()))
                    .andExpect(status().isNotFound());
        }
    }

    @Test
    void downloadAccessCheck() throws Exception {
        downloadAccessCheck0(false);
    }

    @Test
    @WithUser(tenant = SKIP_ARTIFACT_CHECK_TENANT, principal = "controller1", authorities = { SpRole.CONTROLLER_ROLE, SpRole.TENANT_ADMIN })
    void downloadAccessCheckSkip() throws Exception {
        downloadAccessCheck0(true);
    }

    void downloadAccessCheck0(final boolean skip) throws Exception {
        final int artifactSize = 5 * 1024;
        final TestData testData = createTargetAndDs(artifactSize);
        final Target target = testData.target();
        final DistributionSet ds = testData.ds();
        final Artifact artifact = testData.artifact();
        if (skip) {
            // doesn't fail if skip
            downloadSucceeds(testData, target, artifact, artifactSize);
        } else {
            // fail with no assigned or installed
            // without range
            mvc.perform(get(DOWNLOAD_FN, tenant(), testData.target().getControllerId(), testData.moduleId(), "file1"))
                    .andExpect(status().isNotFound());
            // with range
            mvc.perform(get(DOWNLOAD_FN, tenant(), testData.target().getControllerId(), testData.moduleId(), "file1")
                            .header(RANGE, "bytes=0-99"))
                    .andExpect(status().isNotFound());
        }
        // succeeds with assigned action
        assignDistributionSet(ds, target);

        downloadSucceeds(testData, target, artifact, artifactSize);
    }

    // asserts that download succeeds with or without range
    private void downloadSucceeds(
            final TestData testData, final Target target, final Artifact artifact, final int artifactSize) throws Exception {
        // without range
        assertArrayEquals(mvc.perform(get(DOWNLOAD_FN, tenant(), target.getControllerId(), testData.moduleId(), artifact.getFilename()))
                .andExpect(status().isOk())
                .andExpect(content().contentType(APPLICATION_OCTET_STREAM))
                .andReturn().getResponse().getContentAsByteArray(), testData.randomBytes());

        // with range
        assertArrayEquals(mvc.perform(get(DOWNLOAD_FN, tenant(), target.getControllerId(), testData.moduleId(), artifact.getFilename())
                        .header(RANGE, "bytes=0-99"))
                .andExpect(status().is(PARTIAL_CONTENT.value()))
                .andExpect(content().contentType(APPLICATION_OCTET_STREAM))
                .andExpect(header().string(ACCEPT_RANGES, "bytes"))
                .andExpect(header().string(CONTENT_RANGE, "bytes 0-99/" + artifactSize))
                .andExpect(header().string(CONTENT_DISPOSITION, "attachment;filename=" + artifact.getFilename()))
                .andReturn().getResponse().getContentAsByteArray(), Arrays.copyOfRange(testData.randomBytes(), 0, 100));
    }

    @Test
    void downloadMd5AccessCheck() throws Exception {
        downloadMd5AccessCheck0(false);
    }

    @Test
    @WithUser(tenant = SKIP_ARTIFACT_CHECK_TENANT, principal = "controller1", authorities = { SpRole.CONTROLLER_ROLE, SpRole.TENANT_ADMIN })
    void downloadMd5AccessCheckSkip() throws Exception {
        downloadMd5AccessCheck0(true);
    }

    void downloadMd5AccessCheck0(final boolean skip) throws Exception {
        final TestData testData = createTargetAndDs(5 * 1024);
        final Target target = testData.target();
        final DistributionSet ds = testData.ds();
        final Artifact artifact = testData.artifact();
        if (skip) {
            // doesn't fail if skip
            downloadMd5Succeeds(SKIP_ARTIFACT_CHECK_TENANT, testData.target(), testData.moduleId(), artifact);
        } else {
            // fail with no assigned or installed
            mvc.perform(get(DOWNLOAD_FN + ".MD5SUM", tenant(), testData.target().getControllerId(), testData.moduleId(), "file1"))
                    .andExpect(status().isNotFound());
        }
        // succeeds with assigned action
        assignDistributionSet(ds, target);

        downloadMd5Succeeds(tenant(), target, testData.moduleId(), artifact);
    }

    private void downloadMd5Succeeds(final String tenant, final Target target, final Long moduleId, final Artifact artifact) throws Exception {
        assertThat(mvc.perform(get(DOWNLOAD_FN + ".MD5SUM", tenant, target.getControllerId(), moduleId, artifact.getFilename()))
                .andExpect(status().isOk())
                .andExpect(header().string(CONTENT_DISPOSITION, "attachment;filename=" + artifact.getFilename() + ".MD5SUM"))
                .andReturn().getResponse().getContentAsByteArray())
                .isEqualTo((artifact.getMd5Hash() + "  " + artifact.getFilename()).getBytes(StandardCharsets.US_ASCII));
    }

    /**
     * Test various HTTP range requests for artifact download, e.g. chunk download or download resume.
     */
    @Test
    @WithUser(principal = TestdataFactory.DEFAULT_CONTROLLER_ID, authorities = { SpRole.CONTROLLER_ROLE, SpRole.TENANT_ADMIN })
    void rangeDownloadArtifact() throws Exception {
        final int resultLength = (int) quotaManagement.getMaxArtifactSize();
        final TestData testData = createTargetAndDs(resultLength);
        final Target target = testData.target();
        final DistributionSet ds = testData.ds();
        final Long moduleId = testData.moduleId();
        final Artifact artifact = testData.artifact();
        final byte[] random = testData.randomBytes();

        // now assign and download successful
        assignDistributionSet(ds, target);
        final int range = resultLength / 50;
        // full file download with standard range request
        final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        for (int i = 0; i < resultLength / range; i++) {
            final String rangeString = (i * range) + "-" + ((i + 1) * range - 1);
            final MvcResult result = getRange(target, moduleId, artifact, rangeString, PARTIAL_CONTENT, range, rangeString + "/" + resultLength);
            outputStream.write(result.getResponse().getContentAsByteArray());
        }

        assertThat(outputStream.toByteArray()).isEqualTo(random);

        // return last 1000 Bytes
        MvcResult result = getRange(
                target, moduleId, artifact, "-1000",
                PARTIAL_CONTENT, 1000, (resultLength - 1000) + "-" + (resultLength - 1) + "/" + resultLength);
        assertThat(result.getResponse().getContentAsByteArray()).isEqualTo(Arrays.copyOfRange(random, resultLength - 1000, resultLength));

        // skip first 1000 Bytes and return the rest
        result = getRange(
                target, moduleId, artifact, "1000-",
                PARTIAL_CONTENT, resultLength - 1000, 1000 + "-" + (resultLength - 1) + "/" + resultLength);
        assertThat(result.getResponse().getContentAsByteArray()).isEqualTo(Arrays.copyOfRange(random, 1000, resultLength));

        // Start download from file end fails
        getRange(target, moduleId, artifact, random.length + "-", REQUESTED_RANGE_NOT_SATISFIABLE, null, "*/" + resultLength);

        // multipart download - first 20 bytes in 2 parts
        result = getRange(target, moduleId, artifact, "0-9,10-19", PARTIAL_CONTENT, null, null);

        outputStream.reset();
        outputStream.write("--THIS_STRING_SEPARATES_MULTIPART\r\n".getBytes(StandardCharsets.ISO_8859_1));
        outputStream.write(("Content-Type: application/octet-stream\r\n").getBytes(StandardCharsets.ISO_8859_1));
        outputStream.write(("Content-Range: bytes 0-9/" + resultLength + "\r\n\r\n").getBytes(StandardCharsets.ISO_8859_1));
        outputStream.write(Arrays.copyOfRange(random, 0, 10));
        outputStream.write("\r\n--THIS_STRING_SEPARATES_MULTIPART\r\n".getBytes(StandardCharsets.ISO_8859_1));
        outputStream.write(("Content-Type: application/octet-stream\r\n").getBytes(StandardCharsets.ISO_8859_1));
        outputStream.write(("Content-Range: bytes 10-19/" + resultLength + "\r\n\r\n").getBytes(StandardCharsets.ISO_8859_1));
        outputStream.write(Arrays.copyOfRange(random, 10, 20));
        outputStream.write("\r\n--THIS_STRING_SEPARATES_MULTIPART--".getBytes(StandardCharsets.ISO_8859_1));

        assertThat(result.getResponse().getContentAsByteArray()).isEqualTo(outputStream.toByteArray());
    }

    private MvcResult getRange(
            final Target target, final Long moduleId, final Artifact artifact, final String range,
            final HttpStatus expectStatus, final Integer expectedContentLength, final String expectContentRange) throws Exception {
        return mvc.perform(get(DOWNLOAD_FN, tenant(), target.getControllerId(), moduleId, "file1").header(RANGE, "bytes=" + range))
                .andExpect(status().is(expectStatus.value()))
                .andExpect(header().string(
                        CONTENT_TYPE,
                        range.contains(",")
                                ? "multipart/byteranges; boundary=THIS_STRING_SEPARATES_MULTIPART"
                                : APPLICATION_OCTET_STREAM.toString()))
                .andExpect(header().string(
                        ETAG, new HeaderMatcher(expectStatus.value() == 419 ? null : ('"' + artifact.getSha1Hash() + '"'))))
                .andExpect(header().string(ACCEPT_RANGES, "bytes"))
                .andExpect(header().string(LAST_MODIFIED, dateFormat.format(new Date(artifact.getCreatedAt()))))
                .andExpect(header().string(
                        CONTENT_LENGTH, new HeaderMatcher(expectedContentLength == null ? null : String.valueOf(expectedContentLength))))
                .andExpect(header().string(
                        CONTENT_RANGE, new HeaderMatcher(expectContentRange == null ? null : ("bytes " + expectContentRange))))
                .andExpect(header().string(CONTENT_DISPOSITION, "attachment;filename=file1"))
                .andReturn();
    }

    private TestData createTargetAndDs(final int artifactSize) {
        final Target target = testdataFactory.createTarget();
        final DistributionSet ds = testdataFactory.createDistributionSet("");
        final Long moduleId = ds.getModules().iterator().next().getId();
        final byte[] randomBytes = nextBytes(artifactSize);
        final Artifact artifact = artifactManagement.create(
                new ArtifactUpload(new ByteArrayInputStream(randomBytes), null, artifactSize, null, moduleId, "file1", false));
        return new TestData(target, ds, moduleId, artifact, randomBytes);
    }

    private record TestData(Target target, DistributionSet ds, Long moduleId, Artifact artifact, byte[] randomBytes) {}

    @Configuration
    static class DownloadTestConfiguration {

        @Bean
        Listener cancelEventHandlerStubBean() {
            return new Listener();
        }
    }

    private static class HeaderMatcher extends BaseMatcher<String> {

        private final String expectedValue;

        private HeaderMatcher(final String expectedValue) {
            this.expectedValue = expectedValue;
        }

        @Override
        public boolean matches(final Object actual) {
            return expectedValue == null ? actual == null : expectedValue.equals(String.valueOf(actual));
        }

        @Override
        public void describeTo(final Description description) {
            description.appendValue(expectedValue);
        }
    }

    private static class Listener {

        @EventListener(classes = DownloadProgressEvent.class)
        void listen(final DownloadProgressEvent event) {
            synchronized (DdiArtifactDownloadTest.class) {
                downloadProgress++;
                shippedBytes += event.getShippedBytesSinceLast();
            }
        }
    }
}