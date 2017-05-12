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
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.persistence.EntityManager;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;

import org.eclipse.hawkbit.repository.ControllerManagement;
import org.eclipse.hawkbit.repository.EntityFactory;
import org.eclipse.hawkbit.repository.QuotaManagement;
import org.eclipse.hawkbit.repository.RepositoryConstants;
import org.eclipse.hawkbit.repository.RepositoryProperties;
import org.eclipse.hawkbit.repository.TargetManagement;
import org.eclipse.hawkbit.repository.TenantConfigurationManagement;
import org.eclipse.hawkbit.repository.builder.ActionStatusCreate;
import org.eclipse.hawkbit.repository.event.remote.DownloadProgressEvent;
import org.eclipse.hawkbit.repository.event.remote.TargetPollEvent;
import org.eclipse.hawkbit.repository.exception.CancelActionNotAllowedException;
import org.eclipse.hawkbit.repository.exception.EntityNotFoundException;
import org.eclipse.hawkbit.repository.exception.ToManyAttributeEntriesException;
import org.eclipse.hawkbit.repository.exception.TooManyStatusEntriesException;
import org.eclipse.hawkbit.repository.jpa.builder.JpaActionStatusCreate;
import org.eclipse.hawkbit.repository.jpa.configuration.Constants;
import org.eclipse.hawkbit.repository.jpa.executor.AfterTransactionCommitExecutor;
import org.eclipse.hawkbit.repository.jpa.model.JpaAction;
import org.eclipse.hawkbit.repository.jpa.model.JpaActionStatus;
import org.eclipse.hawkbit.repository.jpa.model.JpaActionStatus_;
import org.eclipse.hawkbit.repository.jpa.model.JpaAction_;
import org.eclipse.hawkbit.repository.jpa.model.JpaDistributionSet;
import org.eclipse.hawkbit.repository.jpa.model.JpaTarget;
import org.eclipse.hawkbit.repository.jpa.model.JpaTarget_;
import org.eclipse.hawkbit.repository.jpa.specifications.ActionSpecifications;
import org.eclipse.hawkbit.repository.model.Action;
import org.eclipse.hawkbit.repository.model.Action.Status;
import org.eclipse.hawkbit.repository.model.ActionStatus;
import org.eclipse.hawkbit.repository.model.SoftwareModule;
import org.eclipse.hawkbit.repository.model.Target;
import org.eclipse.hawkbit.repository.model.TargetUpdateStatus;
import org.eclipse.hawkbit.security.SystemSecurityContext;
import org.eclipse.hawkbit.tenancy.TenantAware;
import org.eclipse.hawkbit.tenancy.configuration.TenantConfigurationProperties.TenantConfigurationKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

/**
 * JPA based {@link ControllerManagement} implementation.
 *
 */
@Transactional(readOnly = true)
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
    private SoftwareModuleRepository softwareModuleRepository;

    @Autowired
    private TargetManagement targetManagement;

    @Autowired
    private ActionStatusRepository actionStatusRepository;

    @Autowired
    private QuotaManagement quotaManagement;

    @Autowired
    private RepositoryProperties repositoryProperties;

    @Autowired
    private TenantConfigurationManagement tenantConfigurationManagement;

    @Autowired
    private TenantAware tenantAware;

    @Autowired
    private SystemSecurityContext systemSecurityContext;

    @Autowired
    private EntityFactory entityFactory;

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private AfterTransactionCommitExecutor afterCommit;

    @Override
    public String getPollingTime() {
        return systemSecurityContext.runAsSystem(() -> tenantConfigurationManagement
                .getConfigurationValue(TenantConfigurationKey.POLLING_TIME_INTERVAL, String.class).getValue());
    }

    @Override
    @Transactional
    @Retryable(include = {
            ConcurrencyFailureException.class }, maxAttempts = Constants.TX_RT_MAX, backoff = @Backoff(delay = Constants.TX_RT_DELAY))
    public Target updateLastTargetQuery(final String controllerId, final URI address) {
        final JpaTarget target = (JpaTarget) targetRepository.findByControllerId(controllerId)
                .orElseThrow(() -> new EntityNotFoundException(Target.class, controllerId));

        return updateTargetStatus(target, null, System.currentTimeMillis(), address);
    }

    @Override
    public Optional<Action> getActionForDownloadByTargetAndSoftwareModule(final String controllerId,
            final Long moduleId) {
        throwExceptionIfTargetDoesNotExist(controllerId);
        throwExceptionIfSoftwareModuleDoesNotExist(moduleId);

        final List<Action> action = actionRepository.findActionByTargetAndSoftwareModule(controllerId, moduleId);

        if (action.isEmpty() || action.get(0).isCancelingOrCanceled()) {
            return Optional.empty();
        }

        return Optional.ofNullable(action.get(0));
    }

    private void throwExceptionIfTargetDoesNotExist(final String controllerId) {
        if (!targetRepository.existsByControllerId(controllerId)) {
            throw new EntityNotFoundException(Target.class, controllerId);
        }
    }

    private void throwExceptionIfTargetDoesNotExist(final Long targetId) {
        if (!targetRepository.exists(targetId)) {
            throw new EntityNotFoundException(Target.class, targetId);
        }
    }

    private void throwExceptionIfSoftwareModuleDoesNotExist(final Long moduleId) {
        if (!softwareModuleRepository.exists(moduleId)) {
            throw new EntityNotFoundException(SoftwareModule.class, moduleId);
        }
    }

    @Override
    public boolean hasTargetArtifactAssigned(final String controllerId, final String sha1Hash) {
        throwExceptionIfTargetDoesNotExist(controllerId);
        return actionRepository.count(ActionSpecifications.hasTargetAssignedArtifact(controllerId, sha1Hash)) > 0;
    }

    @Override
    public boolean hasTargetArtifactAssigned(final Long targetId, final String sha1Hash) {
        throwExceptionIfTargetDoesNotExist(targetId);
        return actionRepository.count(ActionSpecifications.hasTargetAssignedArtifact(targetId, sha1Hash)) > 0;
    }

    @Override
    public Optional<Action> findOldestActiveActionByTarget(final String controllerId) {
        throwExceptionIfTargetDoesNotExist(controllerId);

        // used in favorite to findFirstByTargetAndActiveOrderByIdAsc due to
        // DATAJPA-841 issue.
        return actionRepository.findFirstByTargetControllerIdAndActive(new Sort(Direction.ASC, "id"), controllerId,
                true);
    }

    @Override
    public Optional<Action> findActionWithDetails(final Long actionId) {
        return actionRepository.getById(actionId);
    }

    @Override
    @Transactional
    @Retryable(include = {
            ConcurrencyFailureException.class }, maxAttempts = Constants.TX_RT_MAX, backoff = @Backoff(delay = Constants.TX_RT_DELAY))
    public Target findOrRegisterTargetIfItDoesNotexist(final String controllerId, final URI address) {
        final Specification<JpaTarget> spec = (targetRoot, query, cb) -> cb
                .equal(targetRoot.get(JpaTarget_.controllerId), controllerId);

        final JpaTarget target = targetRepository.findOne(spec);

        if (target == null) {
            final Target result = targetManagement.createTarget(entityFactory.target().create()
                    .controllerId(controllerId).description("Plug and Play target: " + controllerId).name(controllerId)
                    .status(TargetUpdateStatus.REGISTERED).lastTargetQuery(System.currentTimeMillis())
                    .address(Optional.ofNullable(address).map(URI::toString).orElse(null)));

            afterCommit.afterCommit(
                    () -> eventPublisher.publishEvent(new TargetPollEvent(result, applicationContext.getId())));

            return result;
        }

        return updateTargetStatus(target, null, System.currentTimeMillis(), address);
    }

    private Target updateTargetStatus(final JpaTarget toUpdate, final TargetUpdateStatus status,
            final Long lastTargetQuery, final URI address) {
        if (status != null) {
            toUpdate.setUpdateStatus(status);
        }
        if (lastTargetQuery != null) {
            toUpdate.setLastTargetQuery(lastTargetQuery);
        }

        if (TargetUpdateStatus.UNKNOWN.equals(toUpdate.getUpdateStatus())) {
            toUpdate.setUpdateStatus(TargetUpdateStatus.REGISTERED);
        }

        if (address != null) {
            toUpdate.setAddress(address.toString());
            afterCommit.afterCommit(
                    () -> eventPublisher.publishEvent(new TargetPollEvent(toUpdate, applicationContext.getId())));
        }

        return targetRepository.save(toUpdate);
    }

    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    @Retryable(include = {
            ConcurrencyFailureException.class }, maxAttempts = Constants.TX_RT_MAX, backoff = @Backoff(delay = Constants.TX_RT_DELAY))
    public Action addCancelActionStatus(final ActionStatusCreate c) {
        final JpaActionStatusCreate create = (JpaActionStatusCreate) c;

        final JpaAction action = getActionAndThrowExceptionIfNotFound(create.getActionId());

        if (!action.isCancelingOrCanceled()) {
            throw new CancelActionNotAllowedException("The action is not in canceling state.");
        }

        final JpaActionStatus actionStatus = create.build();

        switch (actionStatus.getStatus()) {
        case CANCELED:
        case FINISHED:
            handleFinishedCancelation(actionStatus, action);
            break;
        case ERROR:
        case CANCEL_REJECTED:
            // Cancellation rejected. Back to running.
            action.setStatus(Status.RUNNING);
            break;
        default:
            // information status entry - check for a potential DOS attack
            checkForToManyStatusEntries(action);
            break;
        }

        actionStatus.setAction(actionRepository.save(action));
        actionStatusRepository.save(actionStatus);

        return action;
    }

    private void handleFinishedCancelation(final JpaActionStatus actionStatus, final JpaAction action) {
        // in case of successful cancellation we also report the success at
        // the canceled action itself.
        actionStatus.addMessage(
                RepositoryConstants.SERVER_MESSAGE_PREFIX + "Cancellation completion is finished sucessfully.");
        DeploymentHelper.successCancellation(action, actionRepository, targetRepository);
    }

    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    @Retryable(include = {
            ConcurrencyFailureException.class }, maxAttempts = Constants.TX_RT_MAX, backoff = @Backoff(delay = Constants.TX_RT_DELAY))
    public Action addUpdateActionStatus(final ActionStatusCreate c) {
        final JpaActionStatusCreate create = (JpaActionStatusCreate) c;
        final JpaAction action = getActionAndThrowExceptionIfNotFound(create.getActionId());
        final JpaActionStatus actionStatus = create.build();

        // if action is already closed we accept further status updates if
        // permitted so by configuration. This is especially useful if the
        // action status feedback channel order from the device cannot be
        // guaranteed. However, if an action is closed we do not accept further
        // close messages.
        if (actionIsNotActiveButIntermediateFeedbackStillAllowed(actionStatus, action.isActive())) {
            LOG.debug("Update of actionStatus {} for action {} not possible since action not active anymore.",
                    actionStatus.getStatus(), action.getId());
            return action;
        }
        return handleAddUpdateActionStatus(actionStatus, action);
    }

    private boolean actionIsNotActiveButIntermediateFeedbackStillAllowed(final ActionStatus actionStatus,
            final boolean actionActive) {
        return !actionActive && (repositoryProperties.isRejectActionStatusForClosedAction()
                || (Status.ERROR.equals(actionStatus.getStatus()) || Status.FINISHED.equals(actionStatus.getStatus())));
    }

    /**
     * Sets {@link TargetUpdateStatus} based on given {@link ActionStatus}.
     */
    private Action handleAddUpdateActionStatus(final JpaActionStatus actionStatus, final JpaAction action) {
        LOG.debug("addUpdateActionStatus for action {}", action.getId());

        switch (actionStatus.getStatus()) {
        case ERROR:
            final JpaTarget target = DeploymentHelper.updateTargetInfo((JpaTarget) action.getTarget(),
                    TargetUpdateStatus.ERROR, false);
            handleErrorOnAction(action, target);
            break;
        case FINISHED:
            handleFinishedAndStoreInTargetStatus(action);
            break;
        default:
            // information status entry - check for a potential DOS attack
            checkForToManyStatusEntries(action);
            break;
        }

        actionStatus.setAction(action);
        actionStatusRepository.save(actionStatus);

        LOG.debug("addUpdateActionStatus for action {} isfinished.", action.getId());

        return actionRepository.save(action);
    }

    private void handleErrorOnAction(final JpaAction mergedAction, final JpaTarget mergedTarget) {
        mergedAction.setActive(false);
        mergedAction.setStatus(Status.ERROR);
        mergedTarget.setAssignedDistributionSet(null);

        targetRepository.save(mergedTarget);
    }

    private void checkForToManyStatusEntries(final JpaAction action) {
        if (quotaManagement.getMaxStatusEntriesPerAction() > 0) {

            final Long statusCount = actionStatusRepository.countByAction(action);

            if (statusCount >= quotaManagement.getMaxStatusEntriesPerAction()) {
                LOG_DOS.error(
                        "Potential denial of service (DOS) attack identfied. More status entries in the system than permitted ({})!",
                        quotaManagement.getMaxStatusEntriesPerAction());
                throw new TooManyStatusEntriesException(String.valueOf(quotaManagement.getMaxStatusEntriesPerAction()));
            }
        }
    }

    private void handleFinishedAndStoreInTargetStatus(final JpaAction action) {
        final JpaTarget target = (JpaTarget) action.getTarget();
        action.setActive(false);
        action.setStatus(Status.FINISHED);
        final JpaDistributionSet ds = (JpaDistributionSet) entityManager.merge(action.getDistributionSet());

        target.setInstalledDistributionSet(ds);
        target.setInstallationDate(System.currentTimeMillis());

        // check if the assigned set is equal to the installed set (not
        // necessarily the case as another update might be pending already).
        if (target.getAssignedDistributionSet() != null
                && target.getAssignedDistributionSet().getId().equals(target.getInstalledDistributionSet().getId())) {
            target.setUpdateStatus(TargetUpdateStatus.IN_SYNC);
        }

        targetRepository.save(target);

        entityManager.detach(ds);
    }

    @Override
    @Transactional
    @Retryable(include = {
            ConcurrencyFailureException.class }, maxAttempts = Constants.TX_RT_MAX, backoff = @Backoff(delay = Constants.TX_RT_DELAY))
    public Target updateControllerAttributes(final String controllerId, final Map<String, String> data) {
        final JpaTarget target = (JpaTarget) targetRepository.findByControllerId(controllerId)
                .orElseThrow(() -> new EntityNotFoundException(Target.class, controllerId));

        target.getControllerAttributes().putAll(data);

        if (target.getControllerAttributes().size() > quotaManagement.getMaxAttributeEntriesPerTarget()) {
            LOG_DOS.info("Target tries to insert more than the allowed number of entries ({}). DOS attack anticipated!",
                    quotaManagement.getMaxAttributeEntriesPerTarget());
            throw new ToManyAttributeEntriesException(
                    String.valueOf(quotaManagement.getMaxAttributeEntriesPerTarget()));
        }

        target.setRequestControllerAttributes(false);

        return targetRepository.save(target);
    }

    @Override
    @Transactional
    @Retryable(include = {
            ConcurrencyFailureException.class }, maxAttempts = Constants.TX_RT_MAX, backoff = @Backoff(delay = Constants.TX_RT_DELAY))
    public Action registerRetrieved(final Long actionId, final String message) {
        return handleRegisterRetrieved(actionId, message);
    }

    /**
     * Registers retrieved status for given {@link Target} and {@link Action} if
     * it does not exist yet.
     *
     * @param actionId
     *            to the handle status for
     * @param message
     *            for the status
     * @return the updated action in case the status has been changed to
     *         {@link Status#RETRIEVED}
     */
    private Action handleRegisterRetrieved(final Long actionId, final String message) {
        final JpaAction action = getActionAndThrowExceptionIfNotFound(actionId);
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
                .where(cb.equal(actionStatusRoot.get(JpaActionStatus_.action).get(JpaAction_.id), actionId))
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
                action.setStatus(Status.RETRIEVED);
                return actionRepository.save(action);
            }
        }
        return action;
    }

    @Override
    @Transactional
    @Retryable(include = {
            ConcurrencyFailureException.class }, maxAttempts = Constants.TX_RT_MAX, backoff = @Backoff(delay = Constants.TX_RT_DELAY))
    public ActionStatus addInformationalActionStatus(final ActionStatusCreate c) {
        final JpaActionStatusCreate create = (JpaActionStatusCreate) c;
        final JpaAction action = getActionAndThrowExceptionIfNotFound(create.getActionId());
        final JpaActionStatus statusMessage = create.build();
        statusMessage.setAction(action);

        checkForToManyStatusEntries(action);

        return actionStatusRepository.save(statusMessage);
    }

    private JpaAction getActionAndThrowExceptionIfNotFound(final Long actionId) {
        return actionRepository.findById(actionId)
                .orElseThrow(() -> new EntityNotFoundException(Action.class, actionId));
    }

    @Override
    public void downloadProgress(final Long statusId, final Long requestedBytes, final Long shippedBytesSinceLast,
            final Long shippedBytesOverall) {
        eventPublisher.publishEvent(new DownloadProgressEvent(tenantAware.getCurrentTenant(), shippedBytesSinceLast,
                applicationContext.getId()));
    }

    @Override
    public Optional<Target> findByControllerId(final String controllerId) {
        return targetRepository.findByControllerId(controllerId);
    }

    @Override
    public Optional<Target> findByTargetId(final Long targetId) {
        return Optional.ofNullable(targetRepository.findOne(targetId));
    }

    @Override
    public Page<ActionStatus> findActionStatusByAction(final Pageable pageReq, final Long actionId) {
        if (!actionRepository.exists(actionId)) {
            throw new EntityNotFoundException(Action.class, actionId);
        }

        return actionStatusRepository.findByActionId(pageReq, actionId);
    }

}
