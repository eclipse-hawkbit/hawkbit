/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ddi.client.strategy;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import com.google.common.io.ByteStreams;
import com.google.common.io.Files;

/**
 * Implementation of {@link PersistenceStrategy} for downloading artifacts to
 * the temp directory.
 */
public class SaveArtifactsStrategy implements PersistenceStrategy {

    @Override
    public void handleInputStream(final InputStream in, final String artifactName) throws IOException {
        final File tempDir = Files.createTempDir();
        final File file = new File(tempDir + "\\" + artifactName);
        final OutputStream out = new FileOutputStream(file);
        ByteStreams.copy(in, out);
    }

}
