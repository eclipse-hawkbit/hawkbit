/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.repository.jpa.model;

import java.io.Serial;

import jakarta.persistence.Access;
import jakarta.persistence.AccessType;
import jakarta.persistence.Column;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.Version;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;

/**
 * Base hawkBit entity class containing the common attributes for Hibernate.
 */
@NoArgsConstructor(access = AccessLevel.PROTECTED) // Default constructor needed for JPA entities.
@MappedSuperclass
// exception squid:S2160 - BaseEntity equals/hashcode is handling correctly for sub entities
@SuppressWarnings("squid:S2160")
public abstract class AbstractJpaBaseEntity extends AbstractBaseEntity {

    protected static final int USERNAME_FIELD_LENGTH = 64;

    @Serial
    private static final long serialVersionUID = 1L;

    @Setter // should be used just for test purposes
    @Getter
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Setter // should be used just for test purposes
    @Getter
    @Version
    @Column(name = "optlock_revision")
    private int optLockRevision;

    // Audit fields. use property access to ensure that setters will be called and checked for modification
    // (touch implementation depends on setLastModifiedAt(1).
    private String createdBy;
    private long createdAt;
    private String lastModifiedBy;
    private long lastModifiedAt;

    @CreatedBy
    public void setCreatedBy(final String createdBy) {
        this.createdBy = createdBy;
    }

    @Column(name = "created_by", updatable = false, nullable = false, length = USERNAME_FIELD_LENGTH)
    @Access(AccessType.PROPERTY)
    public String getCreatedBy() {
        return createdBy;
    }

    @CreatedDate
    public void setCreatedAt(final long createdAt) {
        this.createdAt = createdAt;
    }

    @Column(name = "created_at", updatable = false, nullable = false)
    @Access(AccessType.PROPERTY)
    public long getCreatedAt() {
        return createdAt;
    }

    @LastModifiedBy
    public void setLastModifiedBy(final String lastModifiedBy) {
        if (this.lastModifiedBy != null && isController()) {
            // initialized and controller = doesn't update
            return;
        }

        this.lastModifiedBy = lastModifiedBy;
    }

    @Column(name = "last_modified_by", nullable = false, length = USERNAME_FIELD_LENGTH)
    @Access(AccessType.PROPERTY)
    public String getLastModifiedBy() {
        return lastModifiedBy == null ? createdBy : lastModifiedBy;
    }

    @LastModifiedDate
    public void setLastModifiedAt(final long lastModifiedAt) {
        if (this.lastModifiedAt != 0 && isController()) {
            // initialized and controller = doesn't update
            return;
        }

        this.lastModifiedAt = lastModifiedAt;
    }

    @Column(name = "last_modified_at", nullable = false)
    @Access(AccessType.PROPERTY)
    public long getLastModifiedAt() {
        return lastModifiedAt == 0 ? createdAt : lastModifiedAt;
    }
}