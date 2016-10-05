/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.api;

/**
 * Container for a generated Artifact URL.
 *
 */
public class ArtifactUrl {

    private final String protocol;
    private final String rel;
    private final String ref;

    /**
     * Constructor.
     * 
     * @param protocol
     *            string, e.g. ftp, http, https
     * @param rel
     *            hypermedia value
     * @param ref
     *            hypermedia value
     */
    public ArtifactUrl(final String protocol, final String rel, final String ref) {
        this.protocol = protocol;
        this.rel = rel;
        this.ref = ref;
    }

    /**
     * @return protocol name used in DMF API messages.
     */
    public String getProtocol() {
        return protocol;
    }

    /**
     * @return rel links value useful in hypermedia.
     */
    public String getRel() {
        return rel;
    }

    /**
     * @return generated artifact download URL
     */
    public String getRef() {
        return ref;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((protocol == null) ? 0 : protocol.hashCode());
        result = prime * result + ((ref == null) ? 0 : ref.hashCode());
        result = prime * result + ((rel == null) ? 0 : rel.hashCode());
        return result;
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final ArtifactUrl other = (ArtifactUrl) obj;
        if (protocol == null) {
            if (other.protocol != null) {
                return false;
            }
        } else if (!protocol.equals(other.protocol)) {
            return false;
        }
        if (ref == null) {
            if (other.ref != null) {
                return false;
            }
        } else if (!ref.equals(other.ref)) {
            return false;
        }
        if (rel == null) {
            if (other.rel != null) {
                return false;
            }
        } else if (!rel.equals(other.rel)) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "ArtifactUrl [protocol=" + protocol + ", rel=" + rel + ", ref=" + ref + "]";
    }

}
