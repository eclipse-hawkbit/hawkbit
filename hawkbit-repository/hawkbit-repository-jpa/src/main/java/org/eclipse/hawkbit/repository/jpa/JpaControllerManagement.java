/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.jpa;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.persistence.EntityManager;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;
import javax.validation.constraints.NotNull;

import org.eclipse.hawkbit.repository.ControllerManagement;
import org.eclipse.hawkbit.repository.RepositoryConstants;
import org.eclipse.hawkbit.repository.RepositoryProperties;
import org.eclipse.hawkbit.repository.TargetManagement;
import org.eclipse.hawkbit.repository.TenantConfigurationManagement;
import org.eclipse.hawkbit.repository.exception.EntityNotFoundException;
import org.eclipse.hawkbit.repository.exception.ToManyAttributeEntriesException;
import org.eclipse.hawkbit.repository.exception.ToManyStatusEntriesException;
import org.eclipse.hawkbit.repository.jpa.cache.CacheWriteNotify;
import org.eclipse.hawkbit.repository.jpa.model.JpaAction;
import org.eclipse.hawkbit.repository.jpa.model.JpaActionStatus;
import org.eclipse.hawkbit.repository.jpa.model.JpaActionStatus_;
import org.eclipse.hawkbit.repository.jpa.model.JpaDistributionSet;
import org.eclipse.hawkbit.repository.jpa.model.JpaSoftwareModule;
import org.eclipse.hawkbit.repository.jpa.model.JpaTarget;
import org.eclipse.hawkbit.repository.jpa.model.JpaTargetInfo;
import org.eclipse.hawkbit.repository.jpa.model.JpaTarget_;
import org.eclipse.hawkbit.repository.jpa.specifications.ActionSpecifications;
import org.eclipse.hawkbit.repository.model.Action;
import org.eclipse.hawkbit.repository.model.Action.Status;
import org.eclipse.hawkbit.repository.model.ActionStatus;
import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.repository.model.LocalArtifact;
import org.eclipse.hawkbit.repository.model.SoftwareModule;
import org.eclipse.hawkbit.repository.model.Target;
import org.eclipse.hawkbit.repository.model.TargetInfo;
import org.eclipse.hawkbit.repository.model.TargetUpdateStatus;
import org.eclipse.hawkbit.repository.model.TenantConfiguration;
import org.eclipse.hawkbit.security.HawkbitSecurityProperties;
import org.eclipse.hawkbit.security.SystemSecurityContext;
import org.eclipse.hawkbit.tenancy.configuration.TenantConfigurationKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

/**
 * JPA based {@link ControllerManagement} implementation.
 *
 */
@Transactional(readOnly = true, isolation = Isolation.READ_UNCOMMITTED)
@Validated
public class JpaControllerManagement implements ControllerManagement {
    private static final Logger LOG = LoggerFactory.getLogger(ControllerManagement.class);
    private static final Logger LOG_DOS = LoggerFactory.getLogger("server-security.dos");

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private ActionRepository actionRepository;

    @Autowired
    private TargetRepository targetRepository;

    @Autowired
    private TargetManagement targetManagement;

    @Autowired
    private TargetInfoRepository targetInfoRepository;

    @Autowired
    private SoftwareModuleRepository softwareModuleRepository;

    @Autowired
    private ActionStatusRepository actionStatusRepository;

    @Autowired
    private HawkbitSecurityProperties securityProperties;

    @Autowired
    private RepositoryProperties repositoryProperties;

    @Autowired
    private TenantConfigurationRepository tenantConfigurationRepository;

    @Autowired
    private TenantConfigurationManagement tenantConfigurationManagement;

    @Autowired
    private CacheWriteNotify cacheWriteNotify;

    @Autowired
    private SystemSecurityContext systemSecurityContext;

    @Override
    public String getPollingTime() {
        final TenantConfigurationKey configurationKey = TenantConfigurationKey.POLLING_TIME_INTERVAL;
        final Class<String> propertyType = String.class;
        JpaTenantConfigurationManagement.validateTenantConfigurationDataType(configurationKey, propertyType);
        final TenantConfiguration tenantConfiguration = tenantConfigurationRepository
                .findByKey(configurationKey.getKeyName());

        return systemSecurityContext.runAsSystem(() -> tenantConfigurationManagement
                .buildTenantConfigurationValueByKey(configurationKey, propertyType, tenantConfiguration).getValue());
    }

    @Override
    @Modifying
    @Transactional(isolation = Isolation.READ_UNCOMMITTED)
    public Target updateLastTargetQuery(final String controllerId, final URI address) {
        final Target target = targetRepository.findByControllerId(controllerId);
        if (target == null) {
            throw new EntityNotFoundException(controllerId);
        }

        return updateLastTargetQuery(target.getTargetInfo(), address).getTarget();
    }

    @Override
    public Action getActionForDownloadByTargetAndSoftwareModule(final String controllerId,
            final SoftwareModule module) {
        final List<Action> action = actionRepository.findActionByTargetAndSoftwareModule(controllerId,
                (JpaSoftwareModule) module);

        if (action.isEmpty() || action.get(0).isCancelingOrCanceled()) {
            throw new EntityNotFoundException(
                    "No assigment found for module " + module.getId() + " to target " + controllerId);
        }

        return action.get(0);
    }

    @Override
    public boolean hasTargetArtifactAssigned(final String controllerId, final LocalArtifact localArtifact) {
        final Target target = targetRepository.findByControllerId(controllerId);
        if (target == null) {
            return false;
        }
        return actionRepository.count(ActionSpecifications.hasTargetAssignedArtifact(target, localArtifact)) > 0;
    }

    @Override
    public List<Action> findActionByTargetAndActive(final Target target) {
        return actionRepository.findByTargetAndActiveOrderByIdAsc((JpaTarget) target, true);
    }

    @Override
    public List<SoftwareModule> findSoftwareModulesByDistributionSet(final DistributionSet distributionSet) {
        return new ArrayList<>(softwareModuleRepository.findByAssignedTo((JpaDistributionSet) distributionSet));
    }

    @Override
    public Action findActionWithDetails(final Long actionId) {
        return actionRepository.findById(actionId);
    }

    @Override
    @Modifying
    @Transactional(isolation = Isolation.READ_UNCOMMITTED)
    public Target findOrRegisterTargetIfItDoesNotexist(final String controllerId, final URI address) {
        final Specification<JpaTarget> spec = (targetRoot, query, cb) -> cb
                .equal(targetRoot.get(JpaTarget_.controllerId), controllerId);

        JpaTarget target = targetRepository.findOne(spec);

        if (target == null) {
            target = new JpaTarget(controllerId);
            target.setDescription("Plug and Play target: " + controllerId);
            target.setName(controllerId);
            return targetManagement.createTarget(target, TargetUpdateStatus.REGISTERED, System.currentTimeMillis(),
                    address);
        }

        return updateLastTargetQuery(target.getTargetInfo(), address).getTarget();
    }

    @Override
    @Modifying
    @Transactional(isolation = Isolation.READ_UNCOMMITTED)
    public TargetInfo updateTargetStatus(final TargetInfo targetInfo, final TargetUpdateStatus status,
            final Long lastTargetQuery, final URI address) {
        final JpaTargetInfo mtargetInfo = (JpaTargetInfo) entityManager.merge(targetInfo);
        if (status != null) {
            mtargetInfo.setUpdateStatus(status);
        }
        if (lastTargetQuery != null) {
            mtargetInfo.setLastTargetQuery(lastTargetQuery);
        }
        if (address != null) {
            mtargetInfo.setAddress(address.toString());
        }
        return targetInfoRepository.save(mtargetInfo);
    }

    @Override
    @Modifying
    @Transactional(isolation = Isolation.READ_UNCOMMITTED)
    public Action addCancelActionStatus(final ActionStatus actionStatus) {
        final JpaAction action = (JpaAction) actionStatus.getAction();

        checkForToManyStatusEntries(action);
        action.setStatus(actionStatus.getStatus());

        switch (actionStatus.getStatus()) {
        case WARNING:
        case ERROR:
        case RUNNING:
            break;
        case CANCELED:
        case FINISHED:
            handleFinishedCancelation(actionStatus, action);
            break;
        case RETRIEVED:
            actionStatus.addMessage(RepositoryConstants.SERVER_MESSAGE_PREFIX + "Cancellation request retrieved.");
            break;
        default:
            // do nothing
        }
        actionRepository.save(action);
        actionStatusRepository.save((JpaActionStatus) actionStatus);

        return action;
    }

    private void handleFinishedCancelation(final ActionStatus actionStatus, final JpaAction action) {
        // in case of successful cancellation we also report the success at
        // the canceled action itself.
        actionStatus.addMessage(
                RepositoryConstants.SERVER_MESSAGE_PREFIX + "Cancellation completion is finished sucessfully.");
        DeploymentHelper.successCancellation(action, actionRepository, targetManagement, targetInfoRepository,
                entityManager);
    }

    @Override
    @Modifying
    @Transactional(isolation = Isolation.READ_UNCOMMITTED)
    public Action addUpdateActionStatus(@NotNull final ActionStatus actionStatus) {
        final JpaAction action = (JpaAction) actionStatus.getAction();

        // if action is already closed we accept further status updates if
        // permitted so by configuration. This is especially useful if the
        // action status feedback channel order from the device cannot be
        // guaranteed. However, if an action is closed we do not accept further
        // close messages.
        if (actionIsNotActiveButIntermediateFeedbackStillAllowed(actionStatus, action)) {
            LOG.debug("Update of actionStatus {} for action {} not possible since action not active anymore.",
                    actionStatus.getId(), action.getId());
            return action;
        }
        return handleAddUpdateActionStatus((JpaActionStatus) actionStatus, action);
    }

    private boolean actionIsNotActiveButIntermediateFeedbackStillAllowed(final ActionStatus actionStatus,
            final JpaAction action) {
        return !action.isActive() && (repositoryProperties.isRejectActionStatusForClosedAction()
                || (Status.ERROR.equals(actionStatus.getStatus()) || Status.FINISHED.equals(actionStatus.getStatus())));
    }

    /**
     * Sets {@link TargetUpdateStatus} based on given {@link ActionStatus}.
     *
     * @param actionStatus
     * @param action
     * @return
     */
    private Action handleAddUpdateActionStatus(final JpaActionStatus actionStatus, final JpaAction action) {
        LOG.debug("addUpdateActionStatus for action {}", action.getId());

        final JpaAction mergedAction = entityManager.merge(action);
        JpaTarget mergedTarget = (JpaTarget) mergedAction.getTarget();
        // check for a potential DOS attack
        checkForToManyStatusEntries(action);

        switch (actionStatus.getStatus()) {
        case ERROR:
            mergedTarget = DeploymentHelper.updateTargetInfo(mergedTarget, TargetUpdateStatus.ERROR, false,
                    targetInfoRepository, entityManager);
            handleErrorOnAction(mergedAction, mergedTarget);
            break;
        case FINISHED:
            handleFinishedAndStoreInTargetStatus(mergedTarget, mergedAction);
            break;
        case CANCELED:
        case WARNING:
        case RUNNING:
            handleIntermediateFeedback(mergedAction, mergedTarget);
            break;
        default:
            break;
        }

        actionStatusRepository.save(actionStatus);

        LOG.debug("addUpdateActionStatus {} for target {} is finished.", action.getId(), mergedTarget.getId());

        return actionRepository.save(mergedAction);
    }

    private void handleIntermediateFeedback(final JpaAction mergedAction, final JpaTarget mergedTarget) {
        // we change the target state only if the action is still running
        // otherwise this is considered as late feedback that does not have
        // an impact on the state anymore.
        if (mergedAction.isActive()) {
            DeploymentHelper.updateTargetInfo(mergedTarget, TargetUpdateStatus.PENDING, false, targetInfoRepository,
                    entityManager);
        }
    }

    private void handleErrorOnAction(final JpaAction mergedAction, final JpaTarget mergedTarget) {
        mergedAction.setActive(false);
        mergedAction.setStatus(Status.ERROR);
        mergedTarget.setAssignedDistributionSet(null);
        targetManagement.updateTarget(mergedTarget);
    }

    private void checkForToManyStatusEntries(final JpaAction action) {
        if (securityProperties.getDos().getMaxStatusEntriesPerAction() > 0) {

            final Long statusCount = actionStatusRepository.countByAction(action);

            if (statusCount >= securityProperties.getDos().getMaxStatusEntriesPerAction()) {
                LOG_DOS.error(
                        "Potential denial of service (DOS) attack identfied. More status entries in the system than permitted ({})!",
                        securityProperties.getDos().getMaxStatusEntriesPerAction());
                throw new ToManyStatusEntriesException(
                        String.valueOf(securityProperties.getDos().getMaxStatusEntriesPerAction()));
            }
        }
    }

    private void handleFinishedAndStoreInTargetStatus(final JpaTarget target, final JpaAction action) {
        action.setActive(false);
        action.setStatus(Status.FINISHED);
        final JpaTargetInfo targetInfo = (JpaTargetInfo) target.getTargetInfo();
        final JpaDistributionSet ds = (JpaDistributionSet) entityManager.merge(action.getDistributionSet());
        targetInfo.setInstalledDistributionSet(ds);
        if (target.getAssignedDistributionSet() != null && targetInfo.getInstalledDistributionSet() != null && target
                .getAssignedDistributionSet().getId().equals(targetInfo.getInstalledDistributionSet().getId())) {
            targetInfo.setUpdateStatus(TargetUpdateStatus.IN_SYNC);
            targetInfo.setInstallationDate(System.currentTimeMillis());
        } else {
            targetInfo.setUpdateStatus(TargetUpdateStatus.PENDING);
            targetInfo.setInstallationDate(System.currentTimeMillis());
        }
        targetInfoRepository.save(targetInfo);
        entityManager.detach(ds);
    }

    @Override
    @Modifying
    @Transactional(isolation = Isolation.READ_UNCOMMITTED)
    public Target updateControllerAttributes(final String controllerId, final Map<String, String> data) {
        final JpaTarget target = targetRepository.findByControllerId(controllerId);

        if (target == null) {
            throw new EntityNotFoundException(controllerId);
        }

        final JpaTargetInfo targetInfo = (JpaTargetInfo) target.getTargetInfo();
        targetInfo.getControllerAttributes().putAll(data);

        if (targetInfo.getControllerAttributes().size() > securityProperties.getDos()
                .getMaxAttributeEntriesPerTarget()) {
            LOG_DOS.info("Target tries to insert more than the allowed number of entries ({}). DOS attack anticipated!",
                    securityProperties.getDos().getMaxAttributeEntriesPerTarget());
            throw new ToManyAttributeEntriesException(
                    String.valueOf(securityProperties.getDos().getMaxAttributeEntriesPerTarget()));
        }

        targetInfo.setLastTargetQuery(System.currentTimeMillis());
        targetInfo.setRequestControllerAttributes(false);
        return targetRepository.save(target);
    }

    @Override
    @Modifying
    @Transactional(isolation = Isolation.READ_UNCOMMITTED)
    public Action registerRetrieved(final Action action, final String message) {
        return handleRegisterRetrieved((JpaAction) action, message);
    }

    /**
     * Registers retrieved status for given {@link Target} and {@link Action} if
     * it does not exist yet.
     *
     * @param action
     *            to the handle status for
     * @param message
     *            for the status
     * @return the updated action in case the status has been changed to
     *         {@link Status#RETRIEVED}
     */
    private Action handleRegisterRetrieved(final JpaAction action, final String message) {
        // do a manual query with CriteriaBuilder to avoid unnecessary field
        // queries and an extra
        // count query made by spring-data when using pageable requests, we
        // don't need an extra count
        // query, we just want to check if the last action status is a retrieved
        // or not.
        final CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        final CriteriaQuery<Object[]> queryActionStatus = cb.createQuery(Object[].class);
        final Root<JpaActionStatus> actionStatusRoot = queryActionStatus.from(JpaActionStatus.class);
        final CriteriaQuery<Object[]> query = queryActionStatus
                .multiselect(actionStatusRoot.get(JpaActionStatus_.id), actionStatusRoot.get(JpaActionStatus_.status))
                .where(cb.equal(actionStatusRoot.get(JpaActionStatus_.action), action))
                .orderBy(cb.desc(actionStatusRoot.get(JpaActionStatus_.id)));
        final List<Object[]> resultList = entityManager.createQuery(query).setFirstResult(0).setMaxResults(1)
                .getResultList();

        // if the latest status is not in retrieve state then we add a retrieved
        // state again, we want
        // to document a deployment retrieved status and a cancel retrieved
        // status, but multiple
        // retrieves after the other we don't want to store to protect to
        // overflood action status in
        // case controller retrieves a action multiple times.
        if (resultList.isEmpty() || !Status.RETRIEVED.equals(resultList.get(0)[1])) {
            // document that the status has been retrieved
            actionStatusRepository
                    .save(new JpaActionStatus(action, Status.RETRIEVED, System.currentTimeMillis(), message));

            // don't change the action status itself in case the action is in
            // canceling state otherwise
            // we modify the action status and the controller won't get the
            // cancel job anymore.
            if (!action.isCancelingOrCanceled()) {
                final JpaAction actionMerge = entityManager.merge(action);
                actionMerge.setStatus(Status.RETRIEVED);
                return actionRepository.save(actionMerge);
            }
        }
        return action;
    }

    @Override
    @Modifying
    @Transactional(isolation = Isolation.READ_UNCOMMITTED)
    public ActionStatus addInformationalActionStatus(final ActionStatus statusMessage) {
        return actionStatusRepository.save((JpaActionStatus) statusMessage);
    }

    @Override
    public String getSecurityTokenByControllerId(final String controllerId) {
        final Target target = targetRepository.findByControllerId(controllerId);
        return target != null ? target.getSecurityToken() : null;
    }

    @Override
    @Modifying
    @Transactional(isolation = Isolation.READ_UNCOMMITTED)
    public TargetInfo updateLastTargetQuery(final TargetInfo target, final URI address) {
        return updateTargetStatus(target, null, System.currentTimeMillis(), address);
    }

    @Override
    public void downloadProgressPercent(final long statusId, final int progressPercent,
            final long shippedBytesSinceLast, final long shippedBytesOverall) {
        cacheWriteNotify.downloadProgressPercent(statusId, progressPercent, shippedBytesSinceLast, shippedBytesOverall);
    }

}
