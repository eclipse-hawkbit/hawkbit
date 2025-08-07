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

import static org.assertj.core.api.Assertions.assertThat;

import org.eclipse.hawkbit.repository.SoftwareModuleTypeManagement;
import org.eclipse.hawkbit.repository.TargetManagement.Update;
import org.eclipse.hawkbit.repository.jpa.AbstractJpaIntegrationTest;
import org.eclipse.hawkbit.repository.jpa.EntityInterceptor;
import org.eclipse.hawkbit.repository.jpa.model.helper.EntityInterceptorHolder;
import org.eclipse.hawkbit.repository.model.SoftwareModuleType;
import org.eclipse.hawkbit.repository.model.Target;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/**
 * Test the entity listener interceptor.
 * <p/>
 * Feature: Component Tests - Repository<br/>
 * Story: Entity Listener Interceptor
 */
class EntityInterceptorListenerTest extends AbstractJpaIntegrationTest {

    @AfterEach
    void tearDown() {
        EntityInterceptorHolder.getInstance().getEntityInterceptors().clear();
    }

    /**
     * Verifies that the pre persist is called after a entity creation.
     */
    @Test
    void prePersistIsCalledWhenPersistingATarget() {
        executePersistAndAssertCallbackResult(new PrePersistEntityListener());
    }

    /**
     * Verifies that the post persist is called after a entity creation.
     */
    @Test
    void postPersistIsCalledWhenPersistingATarget() {
        executePersistAndAssertCallbackResult(new PostPersistEntityListener());
    }

    /**
     * Verifies that the post load is called after a entity is loaded.
     */
    @Test
    void postLoadIsCalledWhenLoadATarget() {
        final PostLoadEntityListener postLoadEntityListener = new PostLoadEntityListener();
        EntityInterceptorHolder.getInstance().getEntityInterceptors().add(postLoadEntityListener);

        final Target targetToBeCreated = testdataFactory.createTarget("targetToBeCreated");

        final Target loadedTarget = targetManagement.getByControllerId(targetToBeCreated.getControllerId()).get();
        assertThat(postLoadEntityListener.getEntity()).isNotNull();
        assertThat(postLoadEntityListener.getEntity()).isEqualTo(loadedTarget);
    }

    /**
     * Verifies that the pre update is called after a entity update.
     */
    @Test
    void preUpdateIsCalledWhenUpdateATarget() {
        executeUpdateAndAssertCallbackResult(new PreUpdateEntityListener());
    }

    /**
     * Verifies that the post update is called after a entity update.
     */
    @Test
    void postUpdateIsCalledWhenUpdateATarget() {
        executeUpdateAndAssertCallbackResult(new PostUpdateEntityListener());
    }

    /**
     * Verifies that the pre remove is called after a entity deletion.
     */
    @Test
    void preRemoveIsCalledWhenDeletingATarget() {
        executeDeleteAndAssertCallbackResult(new PreRemoveEntityListener());
    }

    /**
     * Verifies that the post remove is called after a entity deletion.
     */
    @Test
    void postRemoveIsCalledWhenDeletingATarget() {
        executeDeleteAndAssertCallbackResult(new PostRemoveEntityListener());
    }

    private void executePersistAndAssertCallbackResult(final AbstractEntityListener entityInterceptor) {
        final Target targetToBeCreated = addListenerAndCreateTarget(entityInterceptor, "targetToBeCreated");

        assertThat(entityInterceptor.getEntity()).isNotNull();
        assertThat(entityInterceptor.getEntity()).isEqualTo(targetToBeCreated);
    }

    private void executeUpdateAndAssertCallbackResult(final AbstractEntityListener entityInterceptor) {
        final Target updateTarget = targetManagement.update(
                Update.builder().id(addListenerAndCreateTarget(entityInterceptor, "targetToBeCreated").getId())
                        .name("New").build());

        assertThat(entityInterceptor.getEntity()).isNotNull();
        assertThat(entityInterceptor.getEntity()).isEqualTo(updateTarget);
    }

    private void executeDeleteAndAssertCallbackResult(final AbstractEntityListener entityInterceptor) {
        EntityInterceptorHolder.getInstance().getEntityInterceptors().add(entityInterceptor);
        final SoftwareModuleType type = softwareModuleTypeManagement
                .create(SoftwareModuleTypeManagement.Create.builder().name("test").key("test").build());

        softwareModuleTypeManagement.delete(type.getId());
        assertThat(entityInterceptor.getEntity()).isNotNull();
        assertThat(entityInterceptor.getEntity()).isEqualTo(type);
    }

    private Target addListenerAndCreateTarget(final AbstractEntityListener entityInterceptor,
            final String targetToBeCreated) {
        EntityInterceptorHolder.getInstance().getEntityInterceptors().add(entityInterceptor);
        return testdataFactory.createTarget(targetToBeCreated);
    }

    private abstract static class AbstractEntityListener implements EntityInterceptor {

        private Object entity;

        public Object getEntity() {
            return entity;
        }

        public void setEntity(final Object entity) {
            this.entity = entity;
        }
    }

    private static class PrePersistEntityListener extends AbstractEntityListener {

        @Override
        public void prePersist(final Object entity) {
            setEntity(entity);
        }
    }

    private static class PostPersistEntityListener extends AbstractEntityListener {

        @Override
        public void postPersist(final Object entity) {
            setEntity(entity);
        }

    }

    private static class PostLoadEntityListener extends AbstractEntityListener {

        @Override
        public void postLoad(final Object entity) {
            setEntity(entity);
        }

    }

    private static class PreUpdateEntityListener extends AbstractEntityListener {

        @Override
        public void preUpdate(final Object entity) {
            setEntity(entity);
        }
    }

    private static class PostUpdateEntityListener extends AbstractEntityListener {

        @Override
        public void postUpdate(final Object entity) {
            setEntity(entity);
        }
    }

    private static class PreRemoveEntityListener extends AbstractEntityListener {

        @Override
        public void preRemove(final Object entity) {
            setEntity(entity);
        }
    }

    private static class PostRemoveEntityListener extends AbstractEntityListener {

        @Override
        public void postRemove(final Object entity) {
            setEntity(entity);
        }
    }
}