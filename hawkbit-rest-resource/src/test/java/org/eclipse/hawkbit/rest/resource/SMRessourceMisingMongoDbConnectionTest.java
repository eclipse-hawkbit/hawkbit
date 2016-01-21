/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.rest.resource;

import static org.fest.assertions.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.fileUpload;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.apache.commons.lang3.RandomStringUtils;
import org.eclipse.hawkbit.AbstractIntegrationTest;
import org.eclipse.hawkbit.MockMvcResultPrinter;
import org.eclipse.hawkbit.exception.SpServerError;
import org.eclipse.hawkbit.repository.model.SoftwareModule;
import org.eclipse.hawkbit.rest.resource.model.ExceptionInfo;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MvcResult;

/**
 * Tests {@link SoftwareModuleResource} in case of missing MongoDB connection.
 *
 *
 *
 *
 */
public class SMRessourceMisingMongoDbConnectionTest extends AbstractIntegrationTest {

    @BeforeClass
    public static void initialize() {
        // set property to mongoPort which does not start any mongoDB of
        // parallel test execution
        System.setProperty("spring.data.mongodb.port", "1020");
    }

    @Test
    public void testMissingMongoDbConnection() throws Exception {

        assertThat(softwareManagement.findSoftwareModulesAll(pageReq)).hasSize(0);
        assertThat(artifactRepository.findAll()).hasSize(0);
        SoftwareModule sm = new SoftwareModule(softwareManagement.findSoftwareModuleTypeByKey("os"), "name 1",
                "version 1", null, null);
        sm = softwareManagement.createSoftwareModule(sm);
        assertThat(artifactRepository.findAll()).hasSize(0);

        // create test file
        final byte random[] = RandomStringUtils.random(5 * 1024).getBytes();
        final MockMultipartFile file = new MockMultipartFile("file", "origFilename", null, random);

        // upload
        final MvcResult mvcResult = mvc
                .perform(fileUpload("/rest/v1/softwaremodules/{smId}/artifacts", sm.getId()).file(file))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isInternalServerError()).andReturn();

        // check error result
        final ExceptionInfo exceptionInfo = ResourceUtility
                .convertException(mvcResult.getResponse().getContentAsString());
        assertThat(exceptionInfo.getErrorCode()).isEqualTo(SpServerError.SP_ARTIFACT_UPLOAD_FAILED.getKey());
        assertThat(exceptionInfo.getMessage()).isEqualTo(SpServerError.SP_ARTIFACT_UPLOAD_FAILED.getMessage());

        // ensure that the JPA transaction was rolled back
        assertThat(artifactRepository.findAll()).hasSize(0);

    }

}
