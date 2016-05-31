/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.jpa;

import static org.junit.Assert.fail;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import org.apache.commons.lang3.RandomStringUtils;
import org.eclipse.hawkbit.repository.exception.ArtifactUploadFailedException;
import org.eclipse.hawkbit.repository.jpa.model.JpaSoftwareModule;
import org.junit.BeforeClass;
import org.junit.Test;

import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

/**
 * Addition tests next to {@link ArtifactManagementTest} with no running MongoDB
 *
 */
@Features("Component Tests - Repository")
@Stories("Artifact Management")
public class ArtifactManagementNoMongoDbTest extends AbstractJpaIntegrationTest {

    @BeforeClass
    public static void initialize() {
        // set property to mongoPort which does not start any mongoDB of
        // parallel test execution
        System.setProperty("spring.data.mongodb.port", "1020");
    }

    @Test
    @Description("Checks if the expected ArtifactUploadFailedException is thrown in case of MongoDB down")
    public void createLocalArtifactWithMongoDbDown() throws IOException {
        JpaSoftwareModule sm = new JpaSoftwareModule(softwareManagement.findSoftwareModuleTypeByKey("os"), "name 1",
                "version 1", null, null);
        sm = softwareModuleRepository.save(sm);

        final byte random[] = RandomStringUtils.random(5 * 1024).getBytes();

        try {
            artifactManagement.createLocalArtifact(new ByteArrayInputStream(random), sm.getId(), "file1", false);
            fail("Should not have worked with MongoDb down.");
        } catch (final ArtifactUploadFailedException e) {

        }
    }

}
