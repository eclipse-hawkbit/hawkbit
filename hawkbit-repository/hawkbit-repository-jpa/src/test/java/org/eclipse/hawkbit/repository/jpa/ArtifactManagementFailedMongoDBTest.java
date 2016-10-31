/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.jpa;

import static org.fest.assertions.api.Assertions.assertThat;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.net.UnknownHostException;

import org.eclipse.hawkbit.repository.exception.ArtifactDeleteFailedException;
import org.eclipse.hawkbit.repository.exception.ArtifactUploadFailedException;
import org.eclipse.hawkbit.repository.jpa.model.JpaSoftwareModule;
import org.eclipse.hawkbit.repository.model.Artifact;
import org.junit.After;
import org.junit.Test;

import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

@Features("Component Tests - Repository")
@Stories("Artifact Management")
public class ArtifactManagementFailedMongoDBTest extends AbstractJpaIntegrationTestWithMongoDB {

    @Test
    @Description("Trys and fails to delete or create local artifact with a down mongodb and checks if expected ArtifactDeleteFailedException is thrown.")
    public void deleteArtifactsWithNoMongoDb() throws UnknownHostException, IOException {
        // ensure baseline
        assertThat(artifactRepository.findAll()).isEmpty();

        // prepare test
        JpaSoftwareModule sm = new JpaSoftwareModule(softwareManagement.findSoftwareModuleTypeByKey("os"), "name 1",
                "version 1", null, null);
        sm = softwareModuleRepository.save(sm);

        final Artifact result = artifactManagement.createArtifact(new RandomGeneratedInputStream(5 * 1024), sm.getId(),
                "file1", false);

        assertThat(artifactRepository.findAll()).hasSize(1);

        mongodExecutable.stop();
        try {
            artifactManagement.deleteArtifact(result.getId());
            fail("deletion should have failed");
        } catch (final ArtifactDeleteFailedException e) {

        }

        try {
            artifactManagement.createArtifact(new RandomGeneratedInputStream(5 * 1024), sm.getId(), "file2", false);
            fail("Should not have worked with MongoDb down.");
        } catch (final ArtifactUploadFailedException e) {

        }

        assertThat(artifactRepository.findAll()).hasSize(1);

    }

    @Override
    @After
    public void cleanCurrentCollection() {
        // no need to clean, is stopped already
    }

}
