/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.model;

import java.net.URL;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.ConstraintMode;
import javax.persistence.Entity;
import javax.persistence.ForeignKey;
import javax.persistence.Index;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;

/**
 * External artifact representation with all the necessray informattion to
 * generate an artifact {@link URL} at runtime.
 *
 *
 *
 *
 */
@Table(name = "sp_external_artifact", indexes = {
        @Index(name = "sp_idx_external_artifact_prim", columnList = "id,tenant") })
@Entity
public class ExternalArtifact extends Artifact {
    private static final long serialVersionUID = 1L;

    @ManyToOne
    @JoinColumn(name = "provider", nullable = false, updatable = false, foreignKey = @ForeignKey(value = ConstraintMode.CONSTRAINT, name = "fk_art_to_ext_provider") )
    private ExternalArtifactProvider externalArtifactProvider;

    @Column(name = "url_suffix", length = 512)
    private String urlSuffix;

    // CascadeType.PERSIST as we register ourself at the BSM
    @ManyToOne(optional = false, cascade = { CascadeType.PERSIST })
    @JoinColumn(name = "software_module", nullable = false, updatable = false, foreignKey = @ForeignKey(value = ConstraintMode.CONSTRAINT, name = "fk_external_assigned_sm") )
    private SoftwareModule softwareModule;

    /**
     * Default constructor.
     */
    public ExternalArtifact() {
        super();
    }

    /**
     * Constructs {@link ExternalArtifact}.
     *
     * @param externalArtifactProvider
     *            of the artifact
     * @param urlSuffix
     *            of the artifact
     * @param softwareModule
     *            of the artifact
     */
    public ExternalArtifact(@NotNull final ExternalArtifactProvider externalArtifactProvider, final String urlSuffix,
            final SoftwareModule softwareModule) {
        setSoftwareModule(softwareModule);
        this.externalArtifactProvider = externalArtifactProvider;

        if (urlSuffix != null) {
            this.urlSuffix = urlSuffix;
        } else {
            this.urlSuffix = externalArtifactProvider.getDefaultSuffix();
        }
    }

    /**
     * @return the softwareModule
     */
    @Override
    public SoftwareModule getSoftwareModule() {
        return softwareModule;
    }

    /**
     * @param softwareModule
     *            the softwareModule to set
     */
    public final void setSoftwareModule(final SoftwareModule softwareModule) {
        this.softwareModule = softwareModule;
        this.softwareModule.addArtifact(this);
    }

    /**
     * @return the externalArtifactProvider
     */
    public ExternalArtifactProvider getExternalArtifactProvider() {
        return externalArtifactProvider;
    }

    public String getUrl() {
        return new StringBuilder().append(externalArtifactProvider.getBasePath()).append(urlSuffix).toString();
    }

    /**
     * @return the urlSuffix
     */
    public String getUrlSuffix() {
        return urlSuffix;
    }

    /**
     * @param externalArtifactProvider
     *            the externalArtifactProvider to set
     */
    public void setExternalArtifactProvider(final ExternalArtifactProvider externalArtifactProvider) {
        this.externalArtifactProvider = externalArtifactProvider;
    }

    /**
     * @param urlSuffix
     *            the urlSuffix to set
     */
    public void setUrlSuffix(final String urlSuffix) {
        this.urlSuffix = urlSuffix;
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + this.getClass().getName().hashCode();
        return result;
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (!super.equals(obj)) {
            return false;
        }
        if (!(obj instanceof ExternalArtifact)) {
            return false;
        }

        return true;
    }
}
