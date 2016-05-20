/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.eventbus;

import java.util.Collection;

import javax.persistence.EntityManager;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.eclipse.hawkbit.eventbus.event.TargetCreatedEvent;
import org.eclipse.hawkbit.eventbus.event.TargetDeletedEvent;
import org.eclipse.hawkbit.eventbus.event.TargetInfoUpdateEvent;
import org.eclipse.hawkbit.executor.AfterTransactionCommitExecutor;
import org.eclipse.hawkbit.repository.jpa.TargetRepository;
import org.eclipse.hawkbit.repository.jpa.model.JpaTargetInfo;
import org.eclipse.hawkbit.repository.model.Target;
import org.eclipse.hawkbit.repository.model.TargetInfo;
import org.eclipse.hawkbit.repository.model.TenantAwareBaseEntity;
import org.eclipse.hawkbit.tenancy.TenantAware;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.google.common.eventbus.EventBus;

/**
 * An aspect implementation which wraps the necessary repository services for
 * saving {@link TenantAwareBaseEntity}s to publish create or update events.
 *
 *
 *
 *
 */
@Service
@Aspect
public class EntityChangeEventListener {

    @Autowired
    private EventBus eventBus;

    @Autowired
    private TenantAware tenantAware;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private AfterTransactionCommitExecutor afterCommit;

    /**
     * In case the a {@link Target} is created a corresponding
     * {@link TargetInfo} is created as well. We need the {@link TargetInfo}
     * information in the target created event. So we are listening to the
     * {@link TargetInfo} creation to indicate if an Target has been created.
     * 
     * @param joinpoint
     *            the aspect join point
     * @return the object of the {@link ProceedingJoinPoint#proceed()}
     * @throws Throwable
     *             in case exception happens in the
     *             {@link ProceedingJoinPoint#proceed()}
     */
    @Around("execution(* org.eclipse.hawkbit.repository.jpa.TargetInfoRepository.save(..))")
    // Exception squid:S00112 - Is aspectJ proxy
    @SuppressWarnings({ "squid:S00112" })
    public Object targetCreated(final ProceedingJoinPoint joinpoint) throws Throwable {
        final boolean isNew = isTargetInfoNew(joinpoint.getArgs()[0]);
        final Object result = joinpoint.proceed();
        if (result instanceof TargetInfo) {
            if (isNew) {
                notifyTargetCreated(entityManager.merge(entityManager.merge(((TargetInfo) result).getTarget())));
            } else {
                notifyTargetInfoChanged((TargetInfo) result);
            }
        }
        return result;
    }

    /**
     * Proxy method around the delete method of the {@link TargetRepository} to
     * notify the {@link TargetDeletedEvent} in case targets has been deleted.
     * 
     * @param joinpoint
     *            the aspect join point
     * @return the object of the {@link ProceedingJoinPoint#proceed()}
     * @throws Throwable
     *             in case exception happens in the
     *             {@link ProceedingJoinPoint#proceed()}
     */
    @Around("execution(* org.eclipse.hawkbit.repository.jpa.TargetRepository.deleteByIdIn(..))")
    // Exception squid:S00112 - Is aspectJ proxy
    @SuppressWarnings({ "squid:S00112" })
    public Object targetDeletedById(final ProceedingJoinPoint joinpoint) throws Throwable {
        final String currentTenant = tenantAware.getCurrentTenant();
        final Object result = joinpoint.proceed();
        final Collection<Long> targetIds = (Collection<Long>) joinpoint.getArgs()[0];
        targetIds.forEach(targetId -> notifyTargetDeleted(currentTenant, targetId));
        return result;
    }

    /**
     * Proxy method around the delete method of the {@link TargetRepository} to
     * notify the {@link TargetDeletedEvent} in case targets has been deleted.
     * 
     * @param joinpoint
     *            the aspect join point
     * @return the object of the {@link ProceedingJoinPoint#proceed()}
     * @throws Throwable
     *             in case exception happens in the
     *             {@link ProceedingJoinPoint#proceed()}
     */
    @Around("execution(* org.eclipse.hawkbit.repository.jpa.TargetRepository.delete(..))")
    // Exception squid:S00112 - Is aspectJ proxy
    @SuppressWarnings({ "squid:S00112", "unchecked" })
    public Object targetDeleted(final ProceedingJoinPoint joinpoint) throws Throwable {
        final String currentTenant = tenantAware.getCurrentTenant();
        final Object result = joinpoint.proceed();
        final Object param = joinpoint.getArgs()[0];
        // delete by id
        if (param instanceof Long) {
            notifyTargetDeleted(currentTenant, (Long) param);
        } else if (param instanceof Target) {
            notifyTargetDeleted(currentTenant, ((Target) param).getId());
        } else if (param instanceof Iterable) {
            ((Iterable<Target>) param).forEach(target -> notifyTargetDeleted(currentTenant, target.getId()));
        }
        return result;
    }

    private void notifyTargetCreated(final Target t) {
        afterCommit.afterCommit(() -> eventBus.post(new TargetCreatedEvent(t)));

    }

    private void notifyTargetInfoChanged(final TargetInfo targetInfo) {
        afterCommit.afterCommit(() -> eventBus.post(new TargetInfoUpdateEvent(targetInfo)));
    }

    private void notifyTargetDeleted(final String tenant, final Long targetId) {
        afterCommit.afterCommit(() -> eventBus.post(new TargetDeletedEvent(tenant, targetId)));
    }

    private static boolean isTargetInfoNew(final Object targetInfo) {
        return ((JpaTargetInfo) targetInfo).isNew();
    }

}
