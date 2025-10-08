/**
 * Copyright (c) 2025 Bosch Digital GmbH, Germany. All rights reserved.
 */
package org.eclipse.hawkbit.repository.jpa.actions;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.hawkbit.repository.TenantConfigurationManagement;
import org.eclipse.hawkbit.repository.exception.AssignmentQuotaExceededException;
import org.eclipse.hawkbit.repository.jpa.Jpa;
import org.eclipse.hawkbit.repository.jpa.utils.QuotaHelper;
import org.eclipse.hawkbit.repository.model.Action;
import org.eclipse.hawkbit.repository.model.Target;
import org.eclipse.hawkbit.security.SystemSecurityContext;
import org.eclipse.hawkbit.tenancy.configuration.TenantConfigurationProperties;

import java.io.Serializable;
import java.util.function.ToLongFunction;

@Slf4j
public class TargetAssignmentsChecker {

    private final SystemSecurityContext systemSecurityContext;
    private final TenantConfigurationManagement tenantConfigurationManagement;
    private final EntityManager entityManager;

    public TargetAssignmentsChecker(final EntityManager entityManager,
                                    final SystemSecurityContext systemSecurityContext, final TenantConfigurationManagement tenantConfigurationManagement) {
        this.entityManager = entityManager;
        this.systemSecurityContext = systemSecurityContext;
        this.tenantConfigurationManagement = tenantConfigurationManagement;
    }

    public void checkActionsPerTargetQuota(final Long id, final long requested, final int quota, final ToLongFunction<Long> func) {
        int actionsPurgePercentage = getActionsPurgePercentage();

        try {
            QuotaHelper.assertAssignmentQuota(id, requested, quota, Action.class, Target.class, func);
        } catch (final AssignmentQuotaExceededException quotaExceededException) {
            if (actionsPurgePercentage > 0 && actionsPurgePercentage < 100) {
                int numberOfActions = (int) ((actionsPurgePercentage / 100.0) * quota);
                log.info("Actions purge percentage {}, will delete {} oldest actions for target {}",
                        actionsPurgePercentage, numberOfActions, id);
                deleteLastTargetActions(id, numberOfActions);
            } else {
                throw quotaExceededException;
            }
        }
    }

    /**
     * Deletes the first n target actions of a target
     * @param targetId - target id
     * @param numberOfActions - number of actions to be deleted
     */
    public void deleteLastTargetActions(long targetId, int numberOfActions) {
        // Workaround for the case where JPQL or Criteria API do not support LIMIT
        log.info("Deleting last {} actions of target {}", numberOfActions, targetId);
        final String SQL = "DELETE FROM sp_action WHERE id IN(" +
                "SELECT id FROM (" +
                    "SELECT id FROM sp_action" +
                    " WHERE target=" + Jpa.nativeQueryParamPrefix() + "target" +
                    " ORDER BY id ASC" +
                    " LIMIT " + numberOfActions
                + ") AS sub"
                +")";
        Query query = entityManager.createNativeQuery(SQL);
        query.setParameter("target", targetId);
        query.executeUpdate();
    }

    private int getActionsPurgePercentage() {
        return getConfigValue(TenantConfigurationProperties.TenantConfigurationKey.ACTIONS_PURGE_PERCENTAGE_ON_QUOTA_HIT, Integer.class);
    }

    private <T extends Serializable> T getConfigValue(final String key, final Class<T> valueType) {
        return systemSecurityContext.runAsSystem(() -> tenantConfigurationManagement.getConfigurationValue(key, valueType).getValue());
    }

}
