/**
 * Copyright (c) 2024 Contributors to the Eclipse Foundation
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.sdk.device;

import java.nio.file.Path;
import java.util.Optional;

/**
 * Artifact handler provide plug-in endpoint allowing for customization of the artifact processing.
 * For instance, it could save downloaded files in some location and on successful finish could
 * apply them.
 */
public interface ArtifactHandler {

    ArtifactHandler SKIP = url -> DownloadHandler.SKIP;

    DownloadHandler getDownloadHandler(final String url);

    interface DownloadHandler {

        enum Status {
            SUCCESS, ERROR
        }

        DownloadHandler SKIP = new DownloadHandler() {
            @Override
            public void read(byte[] buff, int off, int len) {
                // skip
            }

            @Override
            public void finished(Status status) {
                // skip
            }

            @Override
            public Optional<Path> download() {
                return Optional.empty();
            }
        };

        /**
         * Called on every read chunk of data
         *
         * @param buff buffer
         * @param off offset
         * @param len read bytes
         */
        void read(byte[] buff, int off, int len);

        /**
         * Called when the download has finished. It could have finished with error in case of network problems,
         * invalid hashes or size. In case of success the hashes and size are already checked and valid.
         *
         * @param status finish status. On error the download shall be discarded and related resources shall be released
         */
        void finished(Status status);

        /**
         * Return download path if existing
         *
         * @return the path to the download
         */
        Optional<Path> download();
    }
}
