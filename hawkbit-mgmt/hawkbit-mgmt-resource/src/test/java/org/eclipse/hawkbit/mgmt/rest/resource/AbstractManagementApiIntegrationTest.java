/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.mgmt.rest.resource;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

import org.eclipse.hawkbit.mgmt.json.model.distributionset.MgmtActionType;
import org.eclipse.hawkbit.repository.jpa.RepositoryApplicationConfiguration;
import org.eclipse.hawkbit.repository.jpa.model.JpaDistributionSet;
import org.eclipse.hawkbit.repository.model.BaseEntity;
import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.repository.model.NamedEntity;
import org.eclipse.hawkbit.repository.model.NamedVersionedEntity;
import org.eclipse.hawkbit.repository.model.Tag;
import org.eclipse.hawkbit.repository.model.Target;
import org.eclipse.hawkbit.repository.test.TestConfiguration;
import org.eclipse.hawkbit.rest.AbstractRestIntegrationTest;
import org.eclipse.hawkbit.rest.RestConfiguration;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.cloud.stream.binder.test.TestChannelBinderConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.ResultMatcher;

@ContextConfiguration(classes = { MgmtApiConfiguration.class, RestConfiguration.class,
        RepositoryApplicationConfiguration.class, TestConfiguration.class })
@Import(TestChannelBinderConfiguration.class)
@TestPropertySource(locations = "classpath:/mgmt-test.properties")
public abstract class AbstractManagementApiIntegrationTest extends AbstractRestIntegrationTest {

    protected static ResultMatcher applyBaseEntityMatcherOnArrayResult(final BaseEntity entity,
            final String arrayElement) throws Exception {
        return mvcResult -> {
            jsonPath("$." + arrayElement + ".[?(@.id==" + entity.getId() + ")].createdBy",
                    contains(entity.getCreatedBy())).match(mvcResult);
            jsonPath("$." + arrayElement + ".[?(@.id==" + entity.getId() + ")].createdAt",
                    contains(entity.getCreatedAt())).match(mvcResult);
            jsonPath("$." + arrayElement + ".[?(@.id==" + entity.getId() + ")].lastModifiedBy",
                    contains(entity.getLastModifiedBy())).match(mvcResult);
            jsonPath("$." + arrayElement + ".[?(@.id==" + entity.getId() + ")].lastModifiedAt",
                    contains(entity.getLastModifiedAt())).match(mvcResult);
        };
    }

    protected static ResultMatcher applyTargetEntityMatcherOnArrayResult(final Target entity, final String arrayElement)
            throws Exception {
        return mvcResult -> {
            jsonPath("$." + arrayElement + ".[?(@.controllerId=='" + entity.getControllerId() + "')].createdBy",
                    contains(entity.getCreatedBy())).match(mvcResult);
            jsonPath("$." + arrayElement + ".[?(@.controllerId=='" + entity.getControllerId() + "')].createdAt",
                    contains(entity.getCreatedAt())).match(mvcResult);
            jsonPath("$." + arrayElement + ".[?(@.controllerId=='" + entity.getControllerId() + "')].lastModifiedBy",
                    contains(entity.getLastModifiedBy())).match(mvcResult);
            jsonPath("$." + arrayElement + ".[?(@.controllerId=='" + entity.getControllerId() + "')].lastModifiedAt",
                    contains(entity.getLastModifiedAt())).match(mvcResult);
        };
    }

    protected static ResultMatcher applyBaseEntityMatcherOnPagedResult(final BaseEntity entity) throws Exception {
        return applyBaseEntityMatcherOnArrayResult(entity, "content");
    }

    protected static ResultMatcher applyNamedEntityMatcherOnPagedResult(final NamedEntity entity) throws Exception {
        return mvcResult -> {
            applyBaseEntityMatcherOnPagedResult(entity).match(mvcResult);
            jsonPath("$.content.[?(@.id==" + entity.getId() + ")].name", contains(entity.getName())).match(mvcResult);
            jsonPath("$.content.[?(@.id==" + entity.getId() + ")].description", contains(entity.getDescription()))
                    .match(mvcResult);
        };

    }

    protected static ResultMatcher applyNamedVersionedEntityMatcherOnPagedResult(final NamedVersionedEntity entity)
            throws Exception {
        return mvcResult -> {
            applyNamedEntityMatcherOnPagedResult(entity).match(mvcResult);
            jsonPath("$.content.[?(@.id==" + entity.getId() + ")].version", contains(entity.getVersion()))
                    .match(mvcResult);
        };
    }

    protected static ResultMatcher applyTagMatcherOnPagedResult(final Tag entity) throws Exception {
        return mvcResult -> {
            applyNamedEntityMatcherOnPagedResult(entity).match(mvcResult);
            jsonPath("$.content.[?(@.id==" + entity.getId() + ")].colour", contains(entity.getColour()))
                    .match(mvcResult);
        };
    }

    protected static ResultMatcher applySelfLinkMatcherOnPagedResult(final BaseEntity entity, final String link)
            throws Exception {

        return mvcResult -> {
            jsonPath("$.content.[?(@.id==" + entity.getId() + ")]._links.self.href", contains(link)).match(mvcResult);
        };
    }

    protected static ResultMatcher applyBaseEntityMatcherOnArrayResult(final BaseEntity entity) throws Exception {
        return mvcResult -> {
            jsonPath("$.[?(@.id==" + entity.getId() + ")].createdBy", contains(entity.getCreatedBy())).match(mvcResult);
            jsonPath("$.[?(@.id==" + entity.getId() + ")].createdAt", contains(entity.getCreatedAt())).match(mvcResult);
            jsonPath("$.[?(@.id==" + entity.getId() + ")].lastModifiedBy", contains(entity.getLastModifiedBy()))
                    .match(mvcResult);
            jsonPath("$.[?(@.id==" + entity.getId() + ")].lastModifiedAt", contains(entity.getLastModifiedAt()))
                    .match(mvcResult);
        };
    }

    protected static ResultMatcher applyTargetEntityMatcherOnArrayResult(final Target entity) throws Exception {
        return mvcResult -> {
            jsonPath("$.[?(@.controllerId=='" + entity.getControllerId() + "')].createdBy",
                    contains(entity.getCreatedBy())).match(mvcResult);
            jsonPath("$.[?(@.controllerId=='" + entity.getControllerId() + "')].createdAt",
                    contains(entity.getCreatedAt())).match(mvcResult);
            jsonPath("$.[?(@.controllerId=='" + entity.getControllerId() + "')].lastModifiedBy",
                    contains(entity.getLastModifiedBy())).match(mvcResult);
            jsonPath("$.[?(@.controllerId=='" + entity.getControllerId() + "')].lastModifiedAt",
                    contains(entity.getLastModifiedAt())).match(mvcResult);
        };
    }

    protected static ResultMatcher applyNamedEntityMatcherOnArrayResult(final NamedEntity entity) throws Exception {
        return mvcResult -> {
            applyBaseEntityMatcherOnPagedResult(entity);

            jsonPath("$.[?(@.id=='" + entity.getId() + "')].name", contains(entity.getName())).match(mvcResult);
            jsonPath("$.[?(@.id=='" + entity.getId() + "')].description", contains(entity.getDescription()))
                    .match(mvcResult);
        };
    }

    protected static ResultMatcher applyNamedVersionedEntityMatcherOnArrayResult(final NamedVersionedEntity entity)
            throws Exception {
        return mvcResult -> {
            applyNamedEntityMatcherOnPagedResult(entity);

            jsonPath("$.[?(@.id=='" + entity.getId() + "')].version", contains(entity.getVersion())).match(mvcResult);
        };
    }

    protected static ResultMatcher applyTagMatcherOnArrayResult(final Tag entity) throws Exception {
        return mvcResult -> {
            applyNamedEntityMatcherOnPagedResult(entity);

            jsonPath("$.[?(@.id=='" + entity.getId() + "')].colour", contains(entity.getColour())).match(mvcResult);
        };
    }

    protected static ResultMatcher applySelfLinkMatcherOnArrayResult(final BaseEntity entity, final String link)
            throws Exception {
        return mvcResult -> {
            jsonPath("$.[?(@.id=='" + entity.getId() + "')]._links.self.href", contains(link)).match(mvcResult);
        };
    }

    protected static ResultMatcher applyBaseEntityMatcherOnSingleResult(final BaseEntity entity) throws Exception {
        return mvcResult -> {
            jsonPath("createdBy", equalTo(entity.getCreatedBy())).match(mvcResult);
            jsonPath("createdAt", equalTo(entity.getCreatedAt())).match(mvcResult);
            jsonPath("lastModifiedBy", equalTo(entity.getLastModifiedBy())).match(mvcResult);
            jsonPath("lastModifiedAt", equalTo(entity.getLastModifiedAt())).match(mvcResult);
        };
    }

    protected static ResultMatcher applyNamedEntityMatcherOnSingleResult(final NamedEntity entity) throws Exception {
        return mvcResult -> {
            applyBaseEntityMatcherOnSingleResult(entity);

            jsonPath("name", equalTo(entity.getName())).match(mvcResult);
            jsonPath("description", equalTo(entity.getDescription())).match(mvcResult);
        };
    }

    protected static ResultMatcher applyNamedVersionedEntityMatcherOnSingleResult(final NamedVersionedEntity entity)
            throws Exception {
        return mvcResult -> {
            applyNamedEntityMatcherOnSingleResult(entity);

            jsonPath("version", equalTo(entity.getVersion())).match(mvcResult);
        };
    }

    protected static ResultMatcher applyTagMatcherOnSingleResult(final Tag entity) throws Exception {
        return mvcResult -> {
            applyNamedEntityMatcherOnSingleResult(entity);

            jsonPath("colour", equalTo(entity.getColour())).match(mvcResult);
        };
    }

    protected static ResultMatcher applySelfLinkMatcherOnSingleResult(final String link) throws Exception {
        return mvcResult -> {
            jsonPath("_links.self.href", equalTo(link)).match(mvcResult);
        };
    }

    protected static JSONObject getAssignmentObject(final Object id, final MgmtActionType type) {
        final JSONObject obj = new JSONObject();
        try {
            obj.put("id", id);
            obj.put("type", type.getName());
        } catch (final JSONException e) {
            e.printStackTrace();
        }
        return obj;
    }

    protected static JSONObject getAssignmentObject(final Object id, final MgmtActionType type, final int weight) {
        try {
            return getAssignmentObject(id, type).put("weight", weight);
        } catch (final JSONException e) {
            e.printStackTrace();
        }
        return new JSONObject();
    }

    // the set is a candidate for implicitly locking. So, lock it if not already locked
    // set lock flag to true to avoid re-locking
    static void implicitLock(final DistributionSet set) {
        if (!set.isLocked()) {
            ((JpaDistributionSet) set).setOptLockRevision(set.getOptLockRevision() + 1);
            ((JpaDistributionSet) set).lock();
        }
    }
}