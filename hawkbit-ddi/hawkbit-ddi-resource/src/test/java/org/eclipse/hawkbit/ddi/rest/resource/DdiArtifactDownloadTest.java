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
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import org.eclipse.hawkbit.ddi.rest.resource.DdiArtifactDownloadTest.DownloadTestConfiguration;
import org.eclipse.hawkbit.repository.event.remote.DownloadProgressEvent;
import org.eclipse.hawkbit.repository.model.Artifact;
import org.eclipse.hawkbit.repository.model.ArtifactUpload;
import org.eclipse.hawkbit.repository.model.DistributionSet;
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
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.MvcResult;

/**
 * Test artifact downloads from the controller.
 * <p/>
 * Feature: Component Tests - Direct Device Integration API<br/>
 * Story: Artifact Download Resource
 */
@SpringBootTest(classes = { DownloadTestConfiguration.class })
class DdiArtifactDownloadTest extends AbstractDDiApiIntegrationTest {

    private static final String DOWNLOAD_FN = "/{tenant}/controller/v1/{controllerId}/softwaremodules/{smId}/artifacts/{filename}";

    private static int downloadProgress = 0;
    private static long shippedBytes = 0;

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
        // create target
        final Target target = testdataFactory.createTarget();
        final List<Target> targets = Collections.singletonList(target);
        // create ds
        final DistributionSet ds = testdataFactory.createDistributionSet("");
        // create artifact
        final int artifactSize = 5 * 1024;
        final byte[] random = nextBytes(artifactSize);
        final Artifact artifact = artifactManagement.create(new ArtifactUpload(
                new ByteArrayInputStream(random),
                null, artifactSize, null, findFirstModuleByType(ds, osType).orElseThrow().getId(), "file1", false));

        assignDistributionSet(ds, targets);

        // no artifact available
        mvc.perform(get(DOWNLOAD_FN, tenant(), target.getControllerId(), getOsModule(ds), "123455"))
                .andExpect(status().isNotFound());
        mvc.perform(get(DOWNLOAD_FN + ".MD5SUM", tenant(), target.getControllerId(), getOsModule(ds), "123455"))
                .andExpect(status().isNotFound());

        // SM does not exist
        mvc.perform(get(DOWNLOAD_FN, tenant(), target.getControllerId(), "1234567890", artifact.getFilename()))
                .andExpect(status().isNotFound());
        mvc.perform(get(DOWNLOAD_FN + ".MD5SUM", tenant(), target.getControllerId(), "1234567890", artifact.getFilename()))
                .andExpect(status().isNotFound());

        // test now consistent data to test allowed methods
        mvc.perform(get(DOWNLOAD_FN, tenant(), target.getControllerId(), getOsModule(ds), artifact.getFilename())
                        .header(HttpHeaders.IF_MATCH, artifact.getSha1Hash()))
                .andExpect(status().isOk());
        mvc.perform(get(DOWNLOAD_FN + ".MD5SUM",
                        tenant(), target.getControllerId(), getOsModule(ds), artifact.getFilename()))
                .andExpect(status().isOk());

        // test failed If-match
        mvc.perform(get(DOWNLOAD_FN, tenant(), target.getControllerId(), getOsModule(ds), artifact.getFilename())
                        .header(HttpHeaders.IF_MATCH, "fsjkhgjfdhg"))
                .andExpect(status().isPreconditionFailed());

        // test invalid range
        mvc.perform(get(DOWNLOAD_FN, tenant(), target.getControllerId(), getOsModule(ds), artifact.getFilename())
                        .header(RANGE, "bytes=1-10,hdsfjksdh"))
                .andExpect(header().string(CONTENT_RANGE, "bytes */" + 5 * 1024))
                .andExpect(status().isRequestedRangeNotSatisfiable());

        mvc.perform(get(DOWNLOAD_FN, tenant(), target.getControllerId(), getOsModule(ds), artifact.getFilename())
                        .header(RANGE, "bytes=100-10"))
                .andExpect(header().string(CONTENT_RANGE, "bytes */" + 5 * 1024))
                .andExpect(status().isRequestedRangeNotSatisfiable());

        // not allowed methods
        mvc.perform(put(DOWNLOAD_FN, tenant(), target.getControllerId(), getOsModule(ds), artifact.getFilename()))
                .andExpect(status().isMethodNotAllowed());
        mvc.perform(delete(DOWNLOAD_FN, tenant(), target.getControllerId(), getOsModule(ds), artifact.getFilename()))
                .andExpect(status().isMethodNotAllowed());
        mvc.perform(post(DOWNLOAD_FN, tenant(), target.getControllerId(), getOsModule(ds), artifact.getFilename()))
                .andExpect(status().isMethodNotAllowed());
        mvc.perform(put(DOWNLOAD_FN + ".MD5SUM", tenant(), target.getControllerId(), getOsModule(ds), artifact.getFilename()))
                .andExpect(status().isMethodNotAllowed());
        mvc.perform(delete(DOWNLOAD_FN + ".MD5SUM", tenant(), target.getControllerId(), getOsModule(ds), artifact.getFilename()))
                .andExpect(status().isMethodNotAllowed());
        mvc.perform(post(DOWNLOAD_FN + ".MD5SUM", tenant(), target.getControllerId(), getOsModule(ds), artifact.getFilename()))
                .andExpect(status().isMethodNotAllowed());
    }

    /**
     * Tests valid downloads through the artifact resource by identifying the artifact not by ID but file name.
     */
    @Test
    @WithUser(principal = "4712", authorities = "ROLE_CONTROLLER", allSpPermissions = true)
    void downloadArtifactThroughFileName() throws Exception {
        synchronized (DdiArtifactDownloadTest.class) {
            downloadProgress = 1;
            shippedBytes = 0;
        }
        assertThat(softwareModuleManagement.findAll(PAGE)).isEmpty();

        // create target
        final Target target = testdataFactory.createTarget();
        final List<Target> targets = Collections.singletonList(target);
        // create ds
        final DistributionSet ds = testdataFactory.createDistributionSet("");
        // create artifact
        final int artifactSize = (int) quotaManagement.getMaxArtifactSize();
        final byte[] random = nextBytes(artifactSize);
        final Artifact artifact = artifactManagement.create(new ArtifactUpload(
                new ByteArrayInputStream(random),
                null, artifactSize, null, findFirstModuleByType(ds, osType).orElseThrow().getId(), "file1", false));
        // download fails as artifact is not yet assigned
        mvc.perform(get(DOWNLOAD_FN, tenant(), target.getControllerId(), getOsModule(ds), artifact.getFilename()))
                .andExpect(status().isNotFound());

        // now assign and download successful
        assignDistributionSet(ds, targets);
        final MvcResult result = mvc.perform(get(DOWNLOAD_FN, tenant(), target.getControllerId(), getOsModule(ds), artifact.getFilename()))
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

    /**
     * Tests valid MD5SUm file downloads through the artifact resource by identifying the artifact by ID.
     */
    @Test
    void downloadMd5SumThroughControllerApi() throws Exception {
        // create target
        final Target target = testdataFactory.createTarget();
        // create ds
        final DistributionSet ds = testdataFactory.createDistributionSet("");
        // create artifact
        final int artifactSize = 5 * 1024;
        final byte[] random = nextBytes(artifactSize);
        final Artifact artifact = artifactManagement.create(new ArtifactUpload(
                new ByteArrayInputStream(random), null, artifactSize, null, getOsModule(ds), "file1", false));

        assignDistributionSet(ds, target);

        // download
        final MvcResult result = mvc.perform(
                        get(DOWNLOAD_FN + ".MD5SUM", tenant(), target.getControllerId(), getOsModule(ds), artifact.getFilename()))
                .andExpect(status().isOk())
                .andExpect(header().string(CONTENT_DISPOSITION, "attachment;filename=" + artifact.getFilename() + ".MD5SUM"))
                .andReturn();

        assertThat(result.getResponse().getContentAsByteArray())
                .isEqualTo((artifact.getMd5Hash() + "  " + artifact.getFilename()).getBytes(StandardCharsets.US_ASCII));
    }

    /**
     * Test various HTTP range requests for artifact download, e.g. chunk download or download resume.
     */
    @Test
    @WithUser(principal = TestdataFactory.DEFAULT_CONTROLLER_ID, authorities = "ROLE_CONTROLLER", allSpPermissions = true)
    void rangeDownloadArtifact() throws Exception {
        // create target
        final Target target = testdataFactory.createTarget();
        final List<Target> targets = Collections.singletonList(target);
        // create ds
        final DistributionSet ds = testdataFactory.createDistributionSet("");
        // create artifact
        final int resultLength = (int) quotaManagement.getMaxArtifactSize();
        final byte[] random = nextBytes(resultLength);
        final Artifact artifact = artifactManagement.create(new ArtifactUpload(
                new ByteArrayInputStream(random), null, resultLength, null, getOsModule(ds), "file1", false));
        assertThat(random).hasSize(resultLength);

        // now assign and download successful
        assignDistributionSet(ds, targets);
        final int range = resultLength / 50;
        // full file download with standard range request
        final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        for (int i = 0; i < resultLength / range; i++) {
            final String rangeString = (i * range) + "-" + ((i + 1) * range - 1);
            final MvcResult result = getRange(target, ds, artifact, rangeString, PARTIAL_CONTENT, range, rangeString + "/" + resultLength);
            outputStream.write(result.getResponse().getContentAsByteArray());
        }

        assertThat(outputStream.toByteArray()).isEqualTo(random);

        // return last 1000 Bytes
        MvcResult result = getRange(
                target, ds, artifact, "-1000",
                PARTIAL_CONTENT, 1000, (resultLength - 1000) + "-" + (resultLength - 1) + "/" + resultLength);
        assertThat(result.getResponse().getContentAsByteArray()).isEqualTo(Arrays.copyOfRange(random, resultLength - 1000, resultLength));

        // skip first 1000 Bytes and return the rest
        result = getRange(
                target, ds, artifact, "1000-",
                PARTIAL_CONTENT, resultLength - 1000, 1000 + "-" + (resultLength - 1) + "/" + resultLength);
        assertThat(result.getResponse().getContentAsByteArray()).isEqualTo(Arrays.copyOfRange(random, 1000, resultLength));

        // Start download from file end fails
        getRange(target, ds, artifact, random.length + "-", REQUESTED_RANGE_NOT_SATISFIABLE, null, "*/" + resultLength);

        // multipart download - first 20 bytes in 2 parts
        result = getRange(target, ds, artifact, "0-9,10-19", PARTIAL_CONTENT, null, null);

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
            final Target target, final DistributionSet ds, final Artifact artifact, final String range,
            final HttpStatus expectStatus, final Integer expectedContentLength, final String expectContentRange) throws Exception {
        return mvc.perform(get(DOWNLOAD_FN, tenant(), target.getControllerId(), getOsModule(ds), "file1").header(RANGE, "bytes=" + range))
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