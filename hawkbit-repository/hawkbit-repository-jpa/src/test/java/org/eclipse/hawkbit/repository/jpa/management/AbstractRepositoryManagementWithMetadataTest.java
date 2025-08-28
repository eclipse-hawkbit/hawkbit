/**
 * Copyright (c) 2025 Contributors to the Eclipse Foundation
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.repository.jpa.management;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

import lombok.SneakyThrows;
import org.eclipse.hawkbit.repository.Identifiable;
import org.eclipse.hawkbit.repository.MetadataSupport;
import org.eclipse.hawkbit.repository.QuotaManagement;
import org.eclipse.hawkbit.repository.exception.AssignmentQuotaExceededException;
import org.eclipse.hawkbit.repository.model.BaseEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@SuppressWarnings("java:S119") // java:S119 - better self explainable
public abstract class AbstractRepositoryManagementWithMetadataTest<T extends BaseEntity, C, U extends Identifiable<Long>, MV, MVI extends MV>
        extends AbstractRepositoryManagementTest<T, C, U> {

    protected MetadataSupport<MV> metadataSupport; // repository management, just casted to MetadataSupport
    private Class<MVI> metadataValueImplType;
    private int maxMetaDataEntries;

    @SneakyThrows
    @SuppressWarnings("unchecked")
    @BeforeEach
    @Override
    void setup() {
        super.setup();

        synchronized (AbstractRepositoryManagementWithMetadataTest.class) {
            if (metadataSupport == null) {
                setMetadataSupport();
                final Type[] genericTypes = genericTypes(getClass(), AbstractRepositoryManagementWithMetadataTest.class);
                metadataValueImplType = (Class<MVI>) genericTypes[4];
                maxMetaDataEntries = (Integer)QuotaManagement.class.getMethod("getMaxMetaDataEntriesPer" + getEntityType().getSimpleName()).invoke(quotaManagement);
            }
        }
    }

    @SuppressWarnings("unchecked")
    protected void setMetadataSupport() {
        metadataSupport = (MetadataSupport<MV>) repositoryManagement;
    }

    @Test
    void createMetadata() {
        final String key = forType(String.class);
        final MV createValue = getCreateValue();
        // since based on counter, forType returns always different values (unless boolean), so this is different value
        final MV updateValue = getCreateValue();

        // create an entity
        final T instance = instance();
        // initial opt lock revision must be 1
        assertThat(instance.getOptLockRevision()).isEqualTo(1);

        // create an entity meta data entry
        waitNextMillis(); // wait that last modified at is different so the opt lock revision is increased
        metadataSupport.createMetadata(instance.getId(), key, createValue);

        final T createResult = repositoryManagement.get(instance.getId());
        assertThat(createResult.getOptLockRevision()).isEqualTo(2);
        assertThat(createResult.getLastModifiedAt()).isPositive();
        T reloaded = repositoryManagement.get(instance.getId());
        assertThat(reloaded.getOptLockRevision()).isEqualTo(2);
        assertThat(reloaded.getLastModifiedAt()).isPositive();

        // update the entity metadata
        waitNextMillis(); // wait that last modified at is different so the opt lock revision is increased
        metadataSupport.createMetadata(instance.getId(), key, updateValue);
        reloaded = repositoryManagement.get(instance.getId());
        assertThat(reloaded.getOptLockRevision()).isEqualTo(3);
        assertThat(reloaded.getLastModifiedAt()).isPositive();

        // verify updated metadata is with the updated value
        assertEquals(metadataSupport.getMetadata(instance.getId()).get(key), updateValue);
    }

    @Test
    void getMetadata() {
        final T instance = instance();
        for (int i = 0; i < maxMetaDataEntries; i++) {
            metadataSupport.createMetadata(instance.getId(), "key" + i, getCreateValue());
        }

        final T instance2 = instance();
        for (int i = 0; i < maxMetaDataEntries - 1; i++) {
            metadataSupport.createMetadata(instance2.getId(), "key" + i, getCreateValue());
        }

        assertThat(metadataSupport.getMetadata(instance.getId())).hasSize(maxMetaDataEntries);
        assertThat(metadataSupport.getMetadata(instance2.getId())).hasSize(maxMetaDataEntries - 1);
    }

    @Test
    void deleteMetadata() {
        final T instance = instance();
        for (int i = 0; i < maxMetaDataEntries; i++) {
            metadataSupport.createMetadata(instance.getId(), "key" + i, getCreateValue());
        }
        assertThat(metadataSupport.getMetadata(instance.getId())).hasSize(maxMetaDataEntries);

        for (int i = 0; i < 2; i++) {
            metadataSupport.deleteMetadata(instance.getId(), "key" + i);
        }
        assertThat(metadataSupport.getMetadata(instance.getId())).hasSize(maxMetaDataEntries - 2);
    }

    @Test
    void failIfMetadataQuotaExceeded() {
        // add metadata one by one
        final Long id1 = instance().getId();
        for (int i = 0; i < maxMetaDataEntries; ++i) {
            metadataSupport.createMetadata(id1, "k" + i, getCreateValue());
        }
        // verify quota exceeded exception
        final MV exceedValue = getCreateValue();
        assertThatExceptionOfType(AssignmentQuotaExceededException.class)
                .isThrownBy(() -> metadataSupport.createMetadata(id1, "k" + maxMetaDataEntries, exceedValue));

        // add multiple meta data entries at once
        final T instance2 = instance();
        final Map<String, MV> metaData2 = new HashMap<>();
        for (int i = 0; i < maxMetaDataEntries + 1; ++i) {
            metaData2.put("k" + i, getCreateValue());
        }
        // verify quota exceeded exception
        final Long id2 = instance2.getId();
        assertThatExceptionOfType(AssignmentQuotaExceededException.class).isThrownBy(() -> metadataSupport.createMetadata(id2, metaData2));

        // add some meta data entries
        final int firstHalf = Math.round((maxMetaDataEntries) / 2.f);
        final Long id3 = instance().getId();
        for (int i = 0; i < firstHalf; ++i) {
            metadataSupport.createMetadata(id3, "k" + i, getCreateValue());
        }
        // add too many data entries
        final int secondHalf = maxMetaDataEntries - firstHalf;
        final Map<String, MV> metaData3 = new HashMap<>();
        for (int i = 0; i < secondHalf + 1; ++i) {
            metaData3.put("kk" + i, getCreateValue());
        }
        // verify quota is exceeded
        assertThatExceptionOfType(AssignmentQuotaExceededException.class).isThrownBy(() -> metadataSupport.createMetadata(id3, metaData3));
    }

    private MVI getCreateValue() {
        return forType(metadataValueImplType);
    }
}