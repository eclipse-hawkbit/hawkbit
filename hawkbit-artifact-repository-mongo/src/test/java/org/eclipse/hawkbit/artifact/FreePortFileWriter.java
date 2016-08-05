/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.artifact;

import java.io.File;
import java.net.InetSocketAddress;
import java.net.ServerSocket;

/**
 *
 * Look for a free port.
 */
public class FreePortFileWriter {

    private final String filePortPath;
    private final int from;
    private final int to;

    /**
     * @param from
     *            port range from (start point)
     * @param to
     *            port range to (end point)
     */
    public FreePortFileWriter(final int from, final int to, final String filePortPath) {
        this.from = from;
        this.to = to;
        this.filePortPath = filePortPath;
    }

    public int getPort() {
        return findFree();
    }

    protected int findFree() {
        for (int i = from; i <= to; i++) {
            if (isFree(i)) {
                return i;
            }
        }
        throw new RuntimeException("No free port in range " + from + ":" + to);
    }

    boolean isFree(final int port) {
        try {
            final File portFile = new File(filePortPath + File.separator + port + ".port");
            portFile.getParentFile().mkdirs();
            if (portFile.exists()) {
                return false;
            }
            boolean isFree = false;
            final ServerSocket sock = new ServerSocket();
            sock.setReuseAddress(true);
            sock.bind(new InetSocketAddress(port));
            if (portFile.createNewFile()) {
                portFile.deleteOnExit();
                isFree = true;
            }
            sock.close();
            return isFree;
            // We rely on an exception thrown to determine availability or
            // not availability and don't want to log the exception.
        } catch (@SuppressWarnings({ "squid:S2221", "squid:S1166" }) final Exception e) {
            return false;
        }
    }

}
