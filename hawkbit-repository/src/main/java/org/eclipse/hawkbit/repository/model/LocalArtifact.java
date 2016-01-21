/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.model;

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

import com.mongodb.gridfs.GridFS;
import com.mongodb.gridfs.GridFSFile;

/**
 * Tenant specific locally stored artifact representation that is used by
 * {@link SoftwareModule} . It contains all information that is provided by the
 * user while all SP server generated information related to the artifact (hash,
 * length) is stored directly with the binary itself.
 *
 *
 *
 */
@Table(name = "sp_artifact", indexes = { @Index(name = "sp_idx_artifact_01", columnList = "tenant,software_module"),
        @Index(name = "sp_idx_artifact_prim", columnList = "tenant,id") })
@Entity
public class LocalArtifact extends Artifact {
    private static final long serialVersionUID = 1L;

    @NotNull
    @Column(name = "gridfs_file_name", length = 40)
    private String gridFsFileName;

    @NotNull
    @Column(name = "provided_file_name", length = 256)
    private String filename;

    @ManyToOne(optional = false, cascade = { CascadeType.PERSIST })
    @JoinColumn(name = "software_module", nullable = false, updatable = false, foreignKey = @ForeignKey(value = ConstraintMode.CONSTRAINT, name = "fk_assigned_sm") )
    private SoftwareModule softwareModule;

    /**
     * Default constructor.
     */
    public LocalArtifact() {
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
    public LocalArtifact(@NotNull final String gridFsFileName, @NotNull final String filename,
            final SoftwareModule softwareModule) {
        setSoftwareModule(softwareModule);
        this.gridFsFileName = gridFsFileName;
        this.filename = filename;
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
        if (!(obj instanceof LocalArtifact)) {
            return false;
        }

        return true;
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
     * @return the gridFsFileName
     */
    public String getGridFsFileName() {
        return gridFsFileName;
    }

    /**
     * @return the filename
     */
    public String getFilename() {
        return filename;
    }
}
