/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.jpa.model;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.ConstraintMode;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.ForeignKey;
import javax.persistence.Index;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.NamedAttributeNode;
import javax.persistence.NamedEntityGraph;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import org.eclipse.hawkbit.repository.model.Artifact;
import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.repository.model.ExternalArtifact;
import org.eclipse.hawkbit.repository.model.LocalArtifact;
import org.eclipse.hawkbit.repository.model.SoftwareModule;
import org.eclipse.hawkbit.repository.model.SoftwareModuleMetadata;
import org.eclipse.hawkbit.repository.model.SoftwareModuleType;
import org.eclipse.persistence.annotations.CascadeOnDelete;

import com.google.common.collect.Lists;

/**
 * Base Software Module that is supported by OS level provisioning mechanism on
 * the edge controller, e.g. OS, JVM, AgentHub.
 *
 */
@Entity
@Table(name = "sp_base_software_module", uniqueConstraints = @UniqueConstraint(columnNames = { "module_type", "name",
        "version", "tenant" }, name = "uk_base_sw_mod"), indexes = {
                @Index(name = "sp_idx_base_sw_module_01", columnList = "tenant,deleted,name,version"),
                @Index(name = "sp_idx_base_sw_module_02", columnList = "tenant,deleted,module_type"),
                @Index(name = "sp_idx_base_sw_module_prim", columnList = "tenant,id") })
@NamedEntityGraph(name = "SoftwareModule.artifacts", attributeNodes = { @NamedAttributeNode("artifacts") })
// exception squid:S2160 - BaseEntity equals/hashcode is handling correctly for
// sub entities
@SuppressWarnings("squid:S2160")
public class JpaSoftwareModule extends AbstractJpaNamedVersionedEntity implements SoftwareModule {
    private static final long serialVersionUID = 1L;

    @ManyToOne
    @JoinColumn(name = "module_type", nullable = false, foreignKey = @ForeignKey(value = ConstraintMode.CONSTRAINT, name = "fk_module_type"))
    @NotNull
    private JpaSoftwareModuleType type;

    @ManyToMany(mappedBy = "modules", targetEntity = JpaDistributionSet.class, fetch = FetchType.LAZY)
    private List<DistributionSet> assignedTo;

    @Column(name = "deleted")
    private boolean deleted;

    @Column(name = "vendor", nullable = true, length = 256)
    @Size(max = 256)
    private String vendor;

    @OneToMany(mappedBy = "softwareModule", cascade = { CascadeType.ALL }, targetEntity = JpaLocalArtifact.class)
    private List<LocalArtifact> artifacts;

    @OneToMany(mappedBy = "softwareModule", cascade = { CascadeType.ALL }, targetEntity = JpaExternalArtifact.class)
    private List<ExternalArtifact> externalArtifacts;

    @CascadeOnDelete
    @OneToMany(fetch = FetchType.LAZY, cascade = { CascadeType.REMOVE }, targetEntity = JpaSoftwareModuleMetadata.class)
    @JoinColumn(name = "sw_id", insertable = false, updatable = false)
    private List<SoftwareModuleMetadata> metadata;

    /**
     * Default constructor.
     */
    public JpaSoftwareModule() {

    }

    /**
     * parameterized constructor.
     *
     * @param type
     *            of the {@link SoftwareModule}
     * @param name
     *            abstract name of the {@link SoftwareModule}
     * @param version
     *            of the {@link SoftwareModule}
     * @param description
     *            of the {@link SoftwareModule}
     * @param vendor
     *            of the {@link SoftwareModule}
     */
    public JpaSoftwareModule(final SoftwareModuleType type, final String name, final String version,
            final String description, final String vendor) {
        super(name, version, description);
        this.vendor = vendor;
        this.type = (JpaSoftwareModuleType) type;
    }

    /**
     * @param artifact
     *            is added to the assigned {@link Artifact}s.
     */
    @Override
    public void addArtifact(final LocalArtifact artifact) {
        if (null == artifacts) {
            artifacts = new LinkedList<>();
            artifacts.add(artifact);
            return;
        }

        if (!artifacts.contains(artifact)) {
            artifacts.add(artifact);
        }
    }

    /**
     * @param artifact
     *            is added to the assigned {@link Artifact}s.
     */
    @Override
    public void addArtifact(final ExternalArtifact artifact) {
        if (null == externalArtifacts) {
            externalArtifacts = new LinkedList<>();
            externalArtifacts.add(artifact);
            return;
        }

        if (!externalArtifacts.contains(artifact)) {
            externalArtifacts.add(artifact);
        }

    }

    @Override
    public Optional<LocalArtifact> getLocalArtifact(final Long artifactId) {
        if (artifacts == null) {
            return Optional.empty();
        }

        return artifacts.stream().filter(artifact -> artifact.getId().equals(artifactId)).findFirst();
    }

    @Override
    public Optional<LocalArtifact> getLocalArtifactByFilename(final String fileName) {
        if (artifacts == null) {
            return Optional.empty();
        }

        return artifacts.stream().filter(artifact -> artifact.getFilename().equalsIgnoreCase(fileName.trim()))
                .findFirst();
    }

    /**
     * @return the artifacts
     */
    @Override
    public List<Artifact> getArtifacts() {
        if (artifacts == null && externalArtifacts == null) {
            return Collections.emptyList();
        } else if (artifacts == null) {
            return Collections.unmodifiableList(externalArtifacts);
        } else if (externalArtifacts == null) {
            return Collections.unmodifiableList(artifacts);
        }

        final List<Artifact> result = Lists.newLinkedList(artifacts);
        result.addAll(externalArtifacts);

        return Collections.unmodifiableList(result);
    }

    /**
     * @return local artifacts only
     */
    @Override
    public List<LocalArtifact> getLocalArtifacts() {
        if (artifacts == null) {
            return Collections.emptyList();
        }

        return artifacts;
    }

    @Override
    public String getVendor() {
        return vendor;
    }

    /**
     * @param artifact
     *            is removed from the assigned {@link LocalArtifact}s.
     */
    @Override
    public void removeArtifact(final LocalArtifact artifact) {
        if (artifacts != null) {
            artifacts.remove(artifact);
        }
    }

    /**
     * @param artifact
     *            is removed from the assigned {@link ExternalArtifact}s.
     */
    @Override
    public void removeArtifact(final ExternalArtifact artifact) {
        if (externalArtifacts != null) {
            externalArtifacts.remove(artifact);
        }
    }

    @Override
    public void setVendor(final String vendor) {
        this.vendor = vendor;
    }

    @Override
    public SoftwareModuleType getType() {
        return type;
    }

    @Override
    public boolean isDeleted() {
        return deleted;
    }

    @Override
    public void setDeleted(final boolean deleted) {
        this.deleted = deleted;
    }

    @Override
    public void setType(final SoftwareModuleType type) {
        this.type = (JpaSoftwareModuleType) type;
    }

    @Override
    public List<SoftwareModuleMetadata> getMetadata() {
        if (metadata == null) {
            return Collections.emptyList();
        }

        return Collections.unmodifiableList(metadata);
    }

    @Override
    public String toString() {
        return "SoftwareModule [deleted=" + deleted + ", name=" + getName() + ", version=" + getVersion()
                + ", revision=" + getOptLockRevision() + ", Id=" + getId() + ", type=" + getType().getName() + "]";
    }

    @Override
    public List<DistributionSet> getAssignedTo() {
        if (assignedTo == null) {
            return Collections.emptyList();
        }

        return Collections.unmodifiableList(assignedTo);
    }

}
