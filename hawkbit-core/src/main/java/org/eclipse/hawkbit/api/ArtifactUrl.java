package org.eclipse.hawkbit.api;

public class ArtifactUrl {

    private final String protocol;
    private final String rel;
    private final String ref;

    public ArtifactUrl(final String protocol, final String rel, final String ref) {
        this.protocol = protocol;
        this.rel = rel;
        this.ref = ref;
    }

    public String getProtocol() {
        return protocol;
    }

    public String getRel() {
        return rel;
    }

    public String getRef() {
        return ref;
    }

}
