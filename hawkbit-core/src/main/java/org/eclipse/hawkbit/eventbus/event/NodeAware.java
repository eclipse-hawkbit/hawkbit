/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.eventbus.event;

/**
 * Interface to be implemented by events which are distributed to other nodes
 * where it's necessary to know which node sent an event and which nodes
 * retrieved the event from which node. Using the EventDistributor the
 * implementation only needs to contain the necessary node IDs, setting and
 * retrieving the node IDs is transparent by the EventDistributor so the event
 * distributor does not hang in an endless loop of distributing the events which
 * self posted.
 *
 *
 *
 *
 */
public interface NodeAware {

    /**
     * @return the origin node ID in case the event has been forwarded to other
     *         nodes or {@code null} if the event has not been forwarded to
     *         other nodes
     */
    String getOriginNodeId();

    /**
     * @param originNodeId
     *            the origin node ID where this event has been sent originally
     */
    void setOriginNodeId(String originNodeId);

    /**
     * @return the node ID which is processing this event locally, set by the
     *         EventDistributor so he can determine if this event has been
     *         received by another node and is processing on the current node.
     */
    String getNodeId();

    /**
     * @param nodeId
     *            the ID of the node this event is processing.
     */
    void setNodeId(String nodeId);

}
