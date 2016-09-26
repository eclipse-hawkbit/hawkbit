/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.jpa.model;

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
import javax.validation.constraints.Size;

import org.eclipse.hawkbit.repository.model.LocalArtifact;
import org.eclipse.hawkbit.repository.model.SoftwareModule;

import com.mongodb.gridfs.GridFS;
import com.mongodb.gridfs.GridFSFile;

/**
 * JPA implementation of {@link LocalArtifact}.
 *
 */
@Table(name = "sp_artifact", indexes = { @Index(name = "sp_idx_artifact_01", columnList = "tenant,software_module"),
        @Index(name = "sp_idx_artifact_prim", columnList = "tenant,id") })
@Entity
// exception squid:S2160 - BaseEntity equals/hashcode is handling correctly for
// sub entities
@SuppressWarnings("squid:S2160")
public class JpaLocalArtifact extends AbstractJpaArtifact implements LocalArtifact {
    private static final long serialVersionUID = 1L;

    @NotNull
    @Column(name = "gridfs_file_name", length = 40)
    @Size(max = 40)
    private String gridFsFileName;

    @NotNull
    @Column(name = "provided_file_name", length = 256)
    @Size(max = 256)
    private String filename;

    @ManyToOne(optional = false, cascade = { CascadeType.PERSIST })
    @JoinColumn(name = "software_module", nullable = false, updatable = false, foreignKey = @ForeignKey(value = ConstraintMode.CONSTRAINT, name = "fk_assigned_sm"))
    private JpaSoftwareModule softwareModule;

    /**
     * Default constructor.
     */
    public JpaLocalArtifact() {
        super();
    }

    /**
     * Constructs artifact.
     *
     * @param gridFsFileName
     *            that is the link to the {@link GridFS} entity.
     * @param filename
     *            that is used by {@link GridFSFile} store.
     * @param softwareModule
     *            of this artifact
     */
    public JpaLocalArtifact(@NotNull final String gridFsFileName, @NotNull final String filename,
            final SoftwareModule softwareModule) {
        setSoftwareModule(softwareModule);
        this.gridFsFileName = gridFsFileName;
        this.filename = filename;
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
        if (!(obj instanceof LocalArtifact)) {
            return false;
        }

        return true;
    }

    @Override
    public SoftwareModule getSoftwareModule() {
        return softwareModule;
    }

    public final void setSoftwareModule(final SoftwareModule softwareModule) {
        this.softwareModule = (JpaSoftwareModule) softwareModule;
        this.softwareModule.addArtifact(this);
    }

    public String getGridFsFileName() {
        return gridFsFileName;
    }

    @Override
    public String getFilename() {
        return filename;
    }
}
