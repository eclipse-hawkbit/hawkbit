/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ddi.rest.resource;

import static org.fest.assertions.api.Assertions.assertThat;
import static org.junit.Assert.assertTrue;
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

import org.apache.commons.lang3.RandomUtils;
import org.eclipse.hawkbit.repository.eventbus.event.DownloadProgressEvent;
import org.eclipse.hawkbit.repository.model.Action;
import org.eclipse.hawkbit.repository.model.Action.Status;
import org.eclipse.hawkbit.repository.model.Artifact;
import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.repository.model.LocalArtifact;
import org.eclipse.hawkbit.repository.model.Target;
import org.eclipse.hawkbit.repository.test.util.WithUser;
import org.eclipse.hawkbit.rest.AbstractRestIntegrationTestWithMongoDB;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import com.google.common.base.Charsets;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.google.common.net.HttpHeaders;

import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

/**
 * Test artifact downloads from the controller.
 */
@Features("Component Tests - Direct Device Integration API")
@Stories("Artifact Download Resource")
public class DdiArtifactDownloadTest extends AbstractRestIntegrationTestWithMongoDB {

    private static final int ARTIFACT_SIZE = 5 * 1024 * 1024;

    public DdiArtifactDownloadTest() {
        LOG = LoggerFactory.getLogger(DdiArtifactDownloadTest.class);
    }

    private volatile int downLoadProgress = 0;
    private volatile long shippedBytes = 0;
    private volatile long shippedBytesTotal = 0;

    private final SimpleDateFormat dateFormat = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz");

    @Before
    public void setup() {
        dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
    }

    @Autowired
    private EventBus eventBus;

    @Test
    @Description("Tests non allowed requests on the artifact ressource, e.g. invalid URI, wrong if-match, wrong command.")
    public void invalidRequestsOnArtifactResource() throws Exception {
        // create target
        Target target = entityFactory.generateTarget("4712");
        target = targetManagement.createTarget(target);
        final List<Target> targets = new ArrayList<>();
        targets.add(target);

        // create ds
        final DistributionSet ds = testdataFactory.createDistributionSet("");
        deploymentManagement.assignDistributionSet(ds, targets);

        // create artifact
        final byte random[] = RandomUtils.nextBytes(5 * 1024);
        final LocalArtifact artifact = artifactManagement.createLocalArtifact(new ByteArrayInputStream(random),
                ds.findFirstModuleByType(osType).getId(), "file1", false);

        // no artifact available
        mvc.perform(get("/controller/v1/{targetid}/softwaremodules/{softwareModuleId}/artifacts/123455",
                target.getControllerId(), ds.findFirstModuleByType(osType).getId())).andExpect(status().isNotFound());
        mvc.perform(get("/controller/v1/{targetid}/softwaremodules/{softwareModuleId}/artifacts/123455.MD5SUM",
                target.getControllerId(), ds.findFirstModuleByType(osType).getId())).andExpect(status().isNotFound());

        // SM does not exist
        mvc.perform(get("/controller/v1/{targetid}/softwaremodules/1234567890/artifacts/{filename}",
                target.getControllerId(), artifact.getFilename())).andExpect(status().isNotFound());
        mvc.perform(get("/controller/v1/{targetid}/softwaremodules/1234567890/artifacts/{filename}.MD5SUM",
                target.getControllerId(), artifact.getFilename())).andExpect(status().isNotFound());

        // test now consistent data to test allowed methods
        mvc.perform(get("/{tenant}/controller/v1/{targetid}/softwaremodules/{smId}/artifacts/{filename}",
                tenantAware.getCurrentTenant(), target.getControllerId(), ds.findFirstModuleByType(osType).getId(),
                artifact.getFilename()).header(HttpHeaders.IF_MATCH, artifact.getSha1Hash()))
                .andExpect(status().isOk());
        mvc.perform(get("/{tenant}/controller/v1/{targetid}/softwaremodules/{smId}/artifacts/{filename}.MD5SUM",
                tenantAware.getCurrentTenant(), target.getControllerId(), ds.findFirstModuleByType(osType).getId(),
                artifact.getFilename())).andExpect(status().isOk());

        // test failed If-match
        mvc.perform(get("/{tenant}/controller/v1/{targetid}/softwaremodules/{smId}/artifacts/{filename}",
                tenantAware.getCurrentTenant(), target.getControllerId(), ds.findFirstModuleByType(osType).getId(),
                artifact.getFilename()).header("If-Match", "fsjkhgjfdhg")).andExpect(status().isPreconditionFailed());

        // test invalid range
        mvc.perform(get("/{tenant}/controller/v1/{targetid}/softwaremodules/{smId}/artifacts/{filename}",
                tenantAware.getCurrentTenant(), target.getControllerId(), ds.findFirstModuleByType(osType).getId(),
                artifact.getFilename()).header("Range", "bytes=1-10,hdsfjksdh"))
                .andExpect(header().string("Content-Range", "bytes */" + 5 * 1024))
                .andExpect(status().isRequestedRangeNotSatisfiable());

        mvc.perform(get("/{tenant}/controller/v1/{targetid}/softwaremodules/{smId}/artifacts/{filename}",
                tenantAware.getCurrentTenant(), target.getControllerId(), ds.findFirstModuleByType(osType).getId(),
                artifact.getFilename()).header("Range", "bytes=100-10"))
                .andExpect(header().string("Content-Range", "bytes */" + 5 * 1024))
                .andExpect(status().isRequestedRangeNotSatisfiable());

        // not allowed methods
        mvc.perform(put("/{tenant}/controller/v1/{targetid}/softwaremodules/{smId}/artifacts/{filename}",
                tenantAware.getCurrentTenant(), target.getControllerId(), ds.findFirstModuleByType(osType).getId(),
                artifact.getFilename())).andExpect(status().isMethodNotAllowed());

        mvc.perform(delete("/{tenant}/controller/v1/{targetid}/softwaremodules/{smId}/artifacts/{filename}",
                tenantAware.getCurrentTenant(), target.getControllerId(), ds.findFirstModuleByType(osType).getId(),
                artifact.getFilename())).andExpect(status().isMethodNotAllowed());

        mvc.perform(post("/{tenant}/controller/v1/{targetid}/softwaremodules/{smId}/artifacts/{filename}",
                tenantAware.getCurrentTenant(), target.getControllerId(), ds.findFirstModuleByType(osType).getId(),
                artifact.getFilename())).andExpect(status().isMethodNotAllowed());

        mvc.perform(put("/{tenant}/controller/v1/{targetid}/softwaremodules/{smId}/artifacts/{filename}.MD5SUM",
                tenantAware.getCurrentTenant(), target.getControllerId(), ds.findFirstModuleByType(osType).getId(),
                artifact.getFilename())).andExpect(status().isMethodNotAllowed());

        mvc.perform(delete("/{tenant}/controller/v1/{targetid}/softwaremodules/{smId}/artifacts/{filename}.MD5SUM",
                tenantAware.getCurrentTenant(), target.getControllerId(), ds.findFirstModuleByType(osType).getId(),
                artifact.getFilename())).andExpect(status().isMethodNotAllowed());

        mvc.perform(post("/{tenant}/controller/v1/{targetid}/softwaremodules/{smId}/artifacts/{filename}.MD5SUM",
                tenantAware.getCurrentTenant(), target.getControllerId(), ds.findFirstModuleByType(osType).getId(),
                artifact.getFilename())).andExpect(status().isMethodNotAllowed());
    }

    @Test
    @WithUser(principal = "4712", authorities = "ROLE_CONTROLLER", allSpPermissions = true)
    @Description("Tests non allowed requests on the artifact ressource, e.g. invalid URI, wrong if-match, wrong command.")
    public void invalidRequestsOnArtifactResourceByName() throws Exception {
        // create target
        Target target = entityFactory.generateTarget("4712");
        target = targetManagement.createTarget(target);
        final List<Target> targets = new ArrayList<>();
        targets.add(target);

        // create ds
        final DistributionSet ds = testdataFactory.createDistributionSet("");
        deploymentManagement.assignDistributionSet(ds, targets);

        // create artifact
        final byte random[] = RandomUtils.nextBytes(5 * 1024);

        // Binary
        // no artifact available
        mvc.perform(get("/controller/artifacts/v1/filename/{filename}", "file1")).andExpect(status().isNotFound());

        // no artifact available
        mvc.perform(get("/controller/artifacts/v1/filename/{filename}.MD5SUM", "file1"))
                .andExpect(status().isNotFound());

        // test now consistent data to test allowed methods
        final Artifact artifact = artifactManagement.createLocalArtifact(new ByteArrayInputStream(random),
                ds.findFirstModuleByType(osType).getId(), "file1", false);

        mvc.perform(
                get("/{tenant}/controller/artifacts/v1/filename/{filename}", tenantAware.getCurrentTenant(), "file1")
                        .header(HttpHeaders.IF_MATCH, artifact.getSha1Hash()))
                .andExpect(status().isOk());

        mvc.perform(get("/{tenant}/controller/artifacts/v1/filename/{filename}.MD5SUM", tenantAware.getCurrentTenant(),
                "file1")).andExpect(status().isOk());

        // test failed If-match
        mvc.perform(
                get("/{tenant}/controller/artifacts/v1/filename/{filename}", tenantAware.getCurrentTenant(), "file1")
                        .header("If-Match", "fsjkhgjfdhg"))
                .andExpect(status().isPreconditionFailed());

        // test invalid range
        mvc.perform(
                get("/{tenant}/controller/artifacts/v1/filename/{filename}", tenantAware.getCurrentTenant(), "file1")
                        .header("Range", "bytes=1-10,hdsfjksdh"))
                .andExpect(header().string("Content-Range", "bytes */" + 5 * 1024))
                .andExpect(status().isRequestedRangeNotSatisfiable());

        mvc.perform(
                get("/{tenant}/controller/artifacts/v1/filename/{filename}", tenantAware.getCurrentTenant(), "file1")
                        .header("Range", "bytes=100-10"))
                .andExpect(header().string("Content-Range", "bytes */" + 5 * 1024))
                .andExpect(status().isRequestedRangeNotSatisfiable());

        // not allowed methods
        mvc.perform(
                put("/{tenant}/controller/artifacts/v1/filename/{filename}", tenantAware.getCurrentTenant(), "file1"))
                .andExpect(status().isMethodNotAllowed());

        mvc.perform(delete("/{tenant}/controller/artifacts/v1/filename/{filename}", tenantAware.getCurrentTenant(),
                "file1")).andExpect(status().isMethodNotAllowed());

        mvc.perform(
                post("/{tenant}/controller/artifacts/v1/filename/{filename}", tenantAware.getCurrentTenant(), "file1"))
                .andExpect(status().isMethodNotAllowed());

        // not allowed methods
        mvc.perform(put("/{tenant}/controller/artifacts/v1/filename/{filename}.MD5SUM", tenantAware.getCurrentTenant(),
                "file1")).andExpect(status().isMethodNotAllowed());

        mvc.perform(delete("/{tenant}/controller/artifacts/v1/filename/{filename}.MD5SUM",
                tenantAware.getCurrentTenant(), "file1")).andExpect(status().isMethodNotAllowed());

        mvc.perform(post("/{tenant}/controller/artifacts/v1/filename/{filename}.MD5SUM", tenantAware.getCurrentTenant(),
                "file1")).andExpect(status().isMethodNotAllowed());

    }

    @Test
    @WithUser(principal = "4712", authorities = "ROLE_CONTROLLER", allSpPermissions = true)
    @Description("Tests valid downloads through the artifact resource by identifying the artifact not by ID but file name.")
    public void downloadArtifactThroughFileName() throws Exception {
        downLoadProgress = 1;
        shippedBytes = 0;
        shippedBytesTotal = 0;
        eventBus.register(this);
        assertThat(softwareManagement.findSoftwareModulesAll(pageReq)).hasSize(0);

        // create target
        Target target = entityFactory.generateTarget("4712");
        target = targetManagement.createTarget(target);
        final List<Target> targets = new ArrayList<Target>();
        targets.add(target);

        // create ds
        final DistributionSet ds = testdataFactory.createDistributionSet("");

        // create artifact
        final byte random[] = RandomUtils.nextBytes(ARTIFACT_SIZE);
        final LocalArtifact artifact = artifactManagement.createLocalArtifact(new ByteArrayInputStream(random),
                ds.findFirstModuleByType(osType).getId(), "file1", false);

        // download fails as artifact is not yet assigned
        mvc.perform(get("/controller/v1/{targetid}/softwaremodules/{softwareModuleId}/artifacts/{filename}",
                target.getControllerId(), ds.findFirstModuleByType(osType).getId(), artifact.getFilename()))
                .andExpect(status().isNotFound());

        // now assign and download successful
        deploymentManagement.assignDistributionSet(ds, targets);
        final MvcResult result = mvc
                .perform(
                        get("/{tenant}/controller/v1/{targetid}/softwaremodules/{softwareModuleId}/artifacts/{filename}",
                                tenantAware.getCurrentTenant(), target.getControllerId(),
                                ds.findFirstModuleByType(osType).getId(), artifact.getFilename()))
                .andExpect(status().isOk()).andExpect(content().contentType(MediaType.APPLICATION_OCTET_STREAM))
                .andExpect(header().string("Accept-Ranges", "bytes"))
                .andExpect(header().string("Last-Modified", dateFormat.format(new Date(artifact.getCreatedAt()))))
                .andExpect(header().string("Content-Disposition", "attachment;filename=" + artifact.getFilename()))
                .andReturn();

        assertTrue("The same file that was uploaded is expected when downloaded",
                Arrays.equals(result.getResponse().getContentAsByteArray(), random));

        // download complete
        assertThat(downLoadProgress).isEqualTo(10);
        assertThat(shippedBytes).isEqualTo(shippedBytesTotal).isEqualTo(ARTIFACT_SIZE);
    }

    @Test
    @Description("Tests valid MD5SUm file downloads through the artifact resource by identifying the artifact by ID.")
    public void downloadMd5sumThroughControllerApi() throws Exception {
        // create target
        Target target = entityFactory.generateTarget("4712");
        target = targetManagement.createTarget(target);

        // create ds
        final DistributionSet ds = testdataFactory.createDistributionSet("");

        // create artifact
        final byte random[] = RandomUtils.nextBytes(5 * 1024);
        final LocalArtifact artifact = artifactManagement.createLocalArtifact(new ByteArrayInputStream(random),
                ds.findFirstModuleByType(osType).getId(), "file1", false);

        // download
        final MvcResult result = mvc
                .perform(
                        get("/{tenant}/controller/v1/{targetid}/softwaremodules/{softwareModuleId}/artifacts/{filename}.MD5SUM",
                                tenantAware.getCurrentTenant(), target.getControllerId(),
                                ds.findFirstModuleByType(osType).getId(), artifact.getFilename()))
                .andExpect(status().isOk()).andExpect(header().string("Content-Disposition",
                        "attachment;filename=" + artifact.getFilename() + ".MD5SUM"))
                .andReturn();

        assertThat(result.getResponse().getContentAsByteArray()).isEqualTo(
                new String(artifact.getMd5Hash() + "  " + artifact.getFilename()).getBytes(Charsets.US_ASCII));
    }

    @Test
    @WithUser(authorities = "ROLE_CONTROLLER_ANONYMOUS", allSpPermissions = true)
    @Description("Ensures that even an authenticated controller is not permitted to download if "
            + "anonymous as authorization is notpossible, e.g. chekc if the controller has the artifact assigned.")
    public void downloadArtifactByNameFailsIfNotAuthenticated() throws Exception {
        downLoadProgress = 1;
        shippedBytes = 0;
        shippedBytesTotal = 0;
        eventBus.register(this);

        assertThat(softwareManagement.findSoftwareModulesAll(pageReq)).hasSize(0);

        // create target
        Target target = entityFactory.generateTarget("4712");
        target = targetManagement.createTarget(target);
        final List<Target> targets = new ArrayList<>();
        targets.add(target);

        // create ds
        final DistributionSet ds = testdataFactory.createDistributionSet("");

        // create artifact
        final byte random[] = RandomUtils.nextBytes(ARTIFACT_SIZE);
        artifactManagement.createLocalArtifact(new ByteArrayInputStream(random),
                ds.findFirstModuleByType(osType).getId(), "file1.tar.bz2", false);

        // download fails as artifact is not yet assigned to target
        deploymentManagement.assignDistributionSet(ds, targets);
        mvc.perform(get("/controller/artifacts/v1/filename/{filename}", "file1.tar.bz2"))
                .andExpect(status().isNotFound());

        assertThat(downLoadProgress).isEqualTo(1);
        assertThat(shippedBytes).isEqualTo(shippedBytesTotal).isEqualTo(0L);
    }

    @Test
    @WithUser(principal = "4712", authorities = "ROLE_CONTROLLER", allSpPermissions = true)
    @Description("Ensures that an authenticated and named controller is permitted to download.")
    public void downloadArtifactByNameByNamedController() throws Exception {
        downLoadProgress = 1;
        shippedBytes = 0;
        shippedBytesTotal = 0;
        eventBus.register(this);

        assertThat(softwareManagement.findSoftwareModulesAll(pageReq)).hasSize(0);

        // create target
        Target target = entityFactory.generateTarget("4712");
        target = targetManagement.createTarget(target);
        final List<Target> targets = new ArrayList<>();
        targets.add(target);

        // create ds
        final DistributionSet ds = testdataFactory.createDistributionSet("");

        // create artifact
        final byte random[] = RandomUtils.nextBytes(ARTIFACT_SIZE);
        final Artifact artifact = artifactManagement.createLocalArtifact(new ByteArrayInputStream(random),
                ds.findFirstModuleByType(osType).getId(), "file1", false);

        // download fails as artifact is not yet assigned to target
        mvc.perform(get("/{tenant}/controller/artifacts/v1/filename/{filename}", tenantAware.getCurrentTenant(),
                "file1.tar.bz2")).andExpect(status().isNotFound());

        // now assign and download successful
        deploymentManagement.assignDistributionSet(ds, targets);
        final MvcResult result = mvc
                .perform(get("/{tenant}/controller/artifacts/v1/filename/{filename}", tenantAware.getCurrentTenant(),
                        "file1"))
                .andExpect(status().isOk()).andExpect(header().string("ETag", artifact.getSha1Hash()))
                .andExpect(content().contentType(MediaType.APPLICATION_OCTET_STREAM))
                .andExpect(header().string("Accept-Ranges", "bytes"))
                .andExpect(header().string("Last-Modified", dateFormat.format(new Date(artifact.getCreatedAt()))))
                .andExpect(header().string("Content-Disposition", "attachment;filename=file1")).andReturn();

        assertTrue("The same file that was uploaded is expected when downloaded",
                Arrays.equals(result.getResponse().getContentAsByteArray(), random));

        // one (update) action
        assertThat(deploymentManagement.findActionsByTarget(target)).hasSize(1);
        final Action action = deploymentManagement.findActionsByTarget(target).get(0);

        // one status - download
        assertThat(action.getActionStatus()).hasSize(2);
        assertThat(deploymentManagement.findActionStatusByAction(new PageRequest(0, 400, Direction.DESC, "id"), action)
                .getContent().get(0).getStatus()).isEqualTo(Status.DOWNLOAD);

        // download complete
        assertThat(downLoadProgress).isEqualTo(10);
        assertThat(shippedBytes).isEqualTo(shippedBytesTotal).isEqualTo(ARTIFACT_SIZE);
    }

    @Test
    @WithUser(principal = "4712", authorities = "ROLE_CONTROLLER", allSpPermissions = true)
    @Description("Test various HTTP range requests for artifact download, e.g. chunk download or download resume.")
    public void rangeDownloadArtifactByName() throws Exception {
        // create target
        Target target = entityFactory.generateTarget("4712");
        target = targetManagement.createTarget(target);
        final List<Target> targets = new ArrayList<>();
        targets.add(target);

        // create ds
        final DistributionSet ds = testdataFactory.createDistributionSet("");

        final int resultLength = 5 * 1000 * 1024;

        // create artifact
        final byte random[] = RandomUtils.nextBytes(resultLength);
        final Artifact artifact = artifactManagement.createLocalArtifact(new ByteArrayInputStream(random),
                ds.findFirstModuleByType(osType).getId(), "file1", false);

        assertThat(random.length).isEqualTo(resultLength);

        // now assign and download successful
        deploymentManagement.assignDistributionSet(ds, targets);

        final int range = 100 * 1024;

        // full file download with standard range request
        final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        for (int i = 0; i < resultLength / range; i++) {
            final String rangeString = "" + i * range + "-" + ((i + 1) * range - 1);

            final MvcResult result = mvc
                    .perform(get("/{tenant}/controller/artifacts/v1/filename/{filename}",
                            tenantAware.getCurrentTenant(), "file1").header("Range", "bytes=" + rangeString))
                    .andExpect(status().isPartialContent()).andExpect(header().string("ETag", artifact.getSha1Hash()))
                    .andExpect(content().contentType(MediaType.APPLICATION_OCTET_STREAM))
                    .andExpect(header().string("Accept-Ranges", "bytes"))
                    .andExpect(header().string("Last-Modified", dateFormat.format(new Date(artifact.getCreatedAt()))))
                    .andExpect(header().longValue("Content-Length", range))
                    .andExpect(header().string("Content-Range", "bytes " + rangeString + "/" + resultLength))
                    .andExpect(header().string("Content-Disposition", "attachment;filename=file1")).andReturn();

            outputStream.write(result.getResponse().getContentAsByteArray());
        }

        assertThat(outputStream.toByteArray()).isEqualTo(random);

        // return last 1000 Bytes
        MvcResult result = mvc
                .perform(get("/${tenant}/controller/artifacts/v1/filename/{filename}", tenantAware.getCurrentTenant(),
                        "file1").header("Range", "bytes=-1000"))
                .andExpect(status().isPartialContent()).andExpect(header().string("ETag", artifact.getSha1Hash()))
                .andExpect(content().contentType(MediaType.APPLICATION_OCTET_STREAM))
                .andExpect(header().string("Accept-Ranges", "bytes"))
                .andExpect(header().string("Last-Modified", dateFormat.format(new Date(artifact.getCreatedAt()))))
                .andExpect(header().longValue("Content-Length", 1000))
                .andExpect(header().string("Content-Range",
                        "bytes " + (resultLength - 1000) + "-" + (resultLength - 1) + "/" + resultLength))
                .andExpect(header().string("Content-Disposition", "attachment;filename=file1")).andReturn();

        assertThat(result.getResponse().getContentAsByteArray())
                .isEqualTo(Arrays.copyOfRange(random, resultLength - 1000, resultLength));

        // skip first 1000 Bytes and return the rest
        result = mvc
                .perform(get("/{tenant}/controller/artifacts/v1/filename/{filename}", tenantAware.getCurrentTenant(),
                        "file1").header("Range", "bytes=1000-"))
                .andExpect(status().isPartialContent()).andExpect(header().string("ETag", artifact.getSha1Hash()))
                .andExpect(content().contentType(MediaType.APPLICATION_OCTET_STREAM))
                .andExpect(header().string("Accept-Ranges", "bytes"))
                .andExpect(header().string("Last-Modified", dateFormat.format(new Date(artifact.getCreatedAt()))))
                .andExpect(header().longValue("Content-Length", resultLength - 1000))
                .andExpect(header().string("Content-Range",
                        "bytes " + 1000 + "-" + (resultLength - 1) + "/" + resultLength))
                .andExpect(header().string("Content-Disposition", "attachment;filename=file1")).andReturn();

        assertThat(result.getResponse().getContentAsByteArray())
                .isEqualTo(Arrays.copyOfRange(random, 1000, resultLength));

        // multipart download - first 20 bytes in 2 parts
        result = mvc
                .perform(get("/{tenant}/controller/artifacts/v1/filename/{filename}", tenantAware.getCurrentTenant(),
                        "file1").header("Range", "bytes=0-9,10-19"))
                .andExpect(status().isPartialContent()).andExpect(header().string("ETag", artifact.getSha1Hash()))
                .andExpect(content().contentType("multipart/byteranges; boundary=THIS_STRING_SEPARATES_MULTIPART"))
                .andExpect(header().string("Accept-Ranges", "bytes"))
                .andExpect(header().string("Last-Modified", dateFormat.format(new Date(artifact.getCreatedAt()))))
                .andExpect(header().string("Content-Disposition", "attachment;filename=file1")).andReturn();

        outputStream.reset();

        outputStream.write("\r\n--THIS_STRING_SEPARATES_MULTIPART\r\n".getBytes(StandardCharsets.ISO_8859_1));
        outputStream.write(("Content-Range: bytes 0-9/" + resultLength + "\r\n").getBytes(StandardCharsets.ISO_8859_1));
        outputStream.write(Arrays.copyOfRange(random, 0, 10));
        outputStream.write("\r\n--THIS_STRING_SEPARATES_MULTIPART\r\n".getBytes(StandardCharsets.ISO_8859_1));
        outputStream
                .write(("Content-Range: bytes 10-19/" + resultLength + "\r\n").getBytes(StandardCharsets.ISO_8859_1));
        outputStream.write(Arrays.copyOfRange(random, 10, 20));
        outputStream.write("\r\n--THIS_STRING_SEPARATES_MULTIPART--".getBytes(StandardCharsets.ISO_8859_1));

        assertThat(result.getResponse().getContentAsByteArray()).isEqualTo(outputStream.toByteArray());

    }

    @Test
    @Description("Ensures that the download fails if te controller is not authenticated.")
    public void faildDownloadArtifactByNameIfAuthenticationMissing() throws Exception {
        assertThat(softwareManagement.findSoftwareModulesAll(pageReq)).hasSize(0);

        // create target
        Target target = entityFactory.generateTarget("4712");
        target = targetManagement.createTarget(target);
        final List<Target> targets = new ArrayList<>();
        targets.add(target);

        // create ds
        final DistributionSet ds = testdataFactory.createDistributionSet("");

        // create artifact
        final byte random[] = RandomUtils.nextBytes(5 * 1024);
        final Artifact artifact = artifactManagement.createLocalArtifact(new ByteArrayInputStream(random),
                ds.findFirstModuleByType(osType).getId(), "file1.tar.bz2", false);

        // download fails as artifact is not yet assigned to target
        mvc.perform(get("/controller/artifacts/v1/filename/{filename}", "file1.tar.bz2"))
                .andExpect(status().isNotFound());
    }

    @Test
    @Description("Downloads an MD5SUM file by the related artifacts filename.")
    public void downloadMd5sumFileByName() throws Exception {
        // create target
        Target target = entityFactory.generateTarget("4712");
        target = targetManagement.createTarget(target);

        // create ds
        final DistributionSet ds = testdataFactory.createDistributionSet("");

        // create artifact
        final byte random[] = RandomUtils.nextBytes(5 * 1024);
        final Artifact artifact = artifactManagement.createLocalArtifact(new ByteArrayInputStream(random),
                ds.findFirstModuleByType(osType).getId(), "file1.tar.bz2", false);

        // download
        final MvcResult result = mvc
                .perform(get("/{tenant}/controller/artifacts/v1/filename/file1.tar.bz2.MD5SUM",
                        tenantAware.getCurrentTenant()))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", "attachment;filename=file1.tar.bz2.MD5SUM"))
                .andReturn();

        assertThat(result.getResponse().getContentAsByteArray())
                .isEqualTo(new String(artifact.getMd5Hash() + "  file1.tar.bz2").getBytes(Charsets.US_ASCII));
    }

    @Subscribe
    public void listen(final DownloadProgressEvent event) {
        downLoadProgress++;
        shippedBytes += event.getShippedBytesSinceLast();
        shippedBytesTotal = event.getShippedBytesOverall();

    }
}
