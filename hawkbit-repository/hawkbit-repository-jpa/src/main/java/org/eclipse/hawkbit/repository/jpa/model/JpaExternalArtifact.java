/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.jpa.model;

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

import org.eclipse.hawkbit.repository.model.ExternalArtifact;
import org.eclipse.hawkbit.repository.model.ExternalArtifactProvider;
import org.eclipse.hawkbit.repository.model.SoftwareModule;

/**
 * External artifact representation with all the necessary information to
 * generate an artifact {@link URL} at runtime.
 *
 */
@Table(name = "sp_external_artifact", indexes = {
        @Index(name = "sp_idx_external_artifact_prim", columnList = "id,tenant") })
@Entity
// exception squid:S2160 - BaseEntity equals/hashcode is handling correctly for
// sub entities
@SuppressWarnings("squid:S2160")
public class JpaExternalArtifact extends AbstractJpaArtifact implements ExternalArtifact {
    private static final long serialVersionUID = 1L;

    @ManyToOne
    @JoinColumn(name = "provider", nullable = false, updatable = false, foreignKey = @ForeignKey(value = ConstraintMode.CONSTRAINT, name = "fk_art_to_ext_provider"))
    private JpaExternalArtifactProvider externalArtifactProvider;

    @Column(name = "url_suffix", length = 512)
    private String urlSuffix;

    // CascadeType.PERSIST as we register ourself at the BSM
    @ManyToOne(optional = false, cascade = { CascadeType.PERSIST })
    @JoinColumn(name = "software_module", nullable = false, updatable = false, foreignKey = @ForeignKey(value = ConstraintMode.CONSTRAINT, name = "fk_external_assigned_sm"))
    private JpaSoftwareModule softwareModule;

    /**
     * Default constructor.
     */
    public JpaExternalArtifact() {
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
    public JpaExternalArtifact(@NotNull final ExternalArtifactProvider externalArtifactProvider, final String urlSuffix,
            final SoftwareModule softwareModule) {
        setSoftwareModule(softwareModule);
        this.externalArtifactProvider = (JpaExternalArtifactProvider) externalArtifactProvider;

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

    public final void setSoftwareModule(final SoftwareModule softwareModule) {
        this.softwareModule = (JpaSoftwareModule) softwareModule;
        this.softwareModule.addArtifact(this);
    }

    @Override
    public ExternalArtifactProvider getExternalArtifactProvider() {
        return externalArtifactProvider;
    }

    @Override
    public String getUrl() {
        return new StringBuilder().append(externalArtifactProvider.getBasePath()).append(urlSuffix).toString();
    }

    @Override
    public String getUrlSuffix() {
        return urlSuffix;
    }

    public void setExternalArtifactProvider(final JpaExternalArtifactProvider externalArtifactProvider) {
        this.externalArtifactProvider = externalArtifactProvider;
    }

    /**
     * @param urlSuffix
     *            the urlSuffix to set
     */
    @Override
    public void setUrlSuffix(final String urlSuffix) {
        this.urlSuffix = urlSuffix;
    }

    @Override
    public int hashCode() { // NOSONAR - as this is generated
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + this.getClass().getName().hashCode();
        return result;
    }

    @Override
    public boolean equals(final Object obj) { // NOSONAR - as this is generated
        if (!super.equals(obj)) {
            return false;
        }
        if (!(obj instanceof JpaExternalArtifact)) {
            return false;
        }

        return true;
    }
}
