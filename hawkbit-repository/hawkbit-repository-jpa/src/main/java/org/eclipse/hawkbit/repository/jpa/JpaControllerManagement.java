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
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import javax.persistence.EntityManager;
import javax.persistence.Query;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;

import org.eclipse.hawkbit.repository.ControllerManagement;
import org.eclipse.hawkbit.repository.EntityFactory;
import org.eclipse.hawkbit.repository.QuotaManagement;
import org.eclipse.hawkbit.repository.RepositoryConstants;
import org.eclipse.hawkbit.repository.RepositoryProperties;
import org.eclipse.hawkbit.repository.TenantConfigurationManagement;
import org.eclipse.hawkbit.repository.builder.ActionStatusCreate;
import org.eclipse.hawkbit.repository.event.remote.TargetPollEvent;
import org.eclipse.hawkbit.repository.exception.CancelActionNotAllowedException;
import org.eclipse.hawkbit.repository.exception.EntityNotFoundException;
import org.eclipse.hawkbit.repository.exception.QuotaExceededException;
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
import org.eclipse.hawkbit.repository.model.SoftwareModuleMetadata;
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
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.DefaultTransactionDefinition;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.validation.annotation.Validated;

import com.google.common.base.Joiner;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

/**
 * JPA based {@link ControllerManagement} implementation.
 *
 */
@Transactional(readOnly = true)
@Validated
public class JpaControllerManagement implements ControllerManagement {
    private static final Logger LOG = LoggerFactory.getLogger(ControllerManagement.class);

    private final BlockingDeque<TargetPoll> queue;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private ActionRepository actionRepository;

    @Autowired
    private TargetRepository targetRepository;

    @Autowired
    private SoftwareModuleRepository softwareModuleRepository;

    @Autowired
    private ActionStatusRepository actionStatusRepository;

    @Autowired
    private QuotaManagement quotaManagement;

    @Autowired
    private TenantConfigurationManagement tenantConfigurationManagement;

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

    @Autowired
    private SoftwareModuleMetadataRepository softwareModuleMetadataRepository;

    @Autowired
    private PlatformTransactionManager txManager;

    @Autowired
    private TenantAware tenantAware;

    private final RepositoryProperties repositoryProperties;

    JpaControllerManagement(final ScheduledExecutorService executorService,
            final RepositoryProperties repositoryProperties) {

        if (!repositoryProperties.isEagerPollPersistence()) {
            executorService.scheduleWithFixedDelay(this::flushUpdateQueue,
                    repositoryProperties.getPollPersistenceFlushTime(),
                    repositoryProperties.getPollPersistenceFlushTime(), TimeUnit.MILLISECONDS);

            queue = new LinkedBlockingDeque<>(repositoryProperties.getPollPersistenceQueueSize());
        } else {
            queue = null;
        }

        this.repositoryProperties = repositoryProperties;
    }

    private <T> T runInNewTransaction(final String transactionName, final TransactionCallback<T> action) {
        final DefaultTransactionDefinition def = new DefaultTransactionDefinition();
        def.setName(transactionName);
        def.setReadOnly(false);
        def.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        return new TransactionTemplate(txManager, def).execute(action);
    }

    @Override
    public String getPollingTime() {
        return systemSecurityContext.runAsSystem(() -> tenantConfigurationManagement
                .getConfigurationValue(TenantConfigurationKey.POLLING_TIME_INTERVAL, String.class).getValue());
    }

    @Override
    public Optional<Action> getActionForDownloadByTargetAndSoftwareModule(final String controllerId,
            final long moduleId) {
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
    public boolean hasTargetArtifactAssigned(final long targetId, final String sha1Hash) {
        throwExceptionIfTargetDoesNotExist(targetId);
        return actionRepository.count(ActionSpecifications.hasTargetAssignedArtifact(targetId, sha1Hash)) > 0;
    }

    @Override
    public Optional<Action> findOldestActiveActionByTarget(final String controllerId) {
        if (!actionRepository.activeActionExistsForControllerId(controllerId)) {
            return Optional.empty();
        }

        // used in favorite to findFirstByTargetAndActiveOrderByIdAsc due to
        // DATAJPA-841 issue.
        return actionRepository.findFirstByTargetControllerIdAndActive(new Sort(Direction.ASC, "id"), controllerId,
                true);
    }

    @Override
    public Optional<Action> findActionWithDetails(final long actionId) {
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
            final Target result = targetRepository.save((JpaTarget) entityFactory.target().create()
                    .controllerId(controllerId).description("Plug and Play target: " + controllerId).name(controllerId)
                    .status(TargetUpdateStatus.REGISTERED).lastTargetQuery(System.currentTimeMillis())
                    .address(Optional.ofNullable(address).map(URI::toString).orElse(null)).build());

            afterCommit.afterCommit(
                    () -> eventPublisher.publishEvent(new TargetPollEvent(result, applicationContext.getId())));

            return result;
        }

        return updateTargetStatus(target, address);
    }

    /**
     * Flush the update queue by means to persisting
     * {@link Target#getLastTargetQuery()}.
     */
    private void flushUpdateQueue() {
        LOG.debug("Run flushUpdateQueue.");

        final int size = queue.size();
        if (size <= 0) {
            return;
        }

        LOG.debug("{} events in flushUpdateQueue.", size);

        final Set<TargetPoll> events = Sets.newHashSetWithExpectedSize(queue.size());
        final int drained = queue.drainTo(events);

        if (drained <= 0) {
            return;
        }

        try {
            events.stream().collect(Collectors.groupingBy(TargetPoll::getTenant)).forEach((tenant, polls) -> {
                final TransactionCallback<Void> createTransaction = status -> updateLastTargetQueries(tenant, polls);
                tenantAware.runAsTenant(tenant, () -> runInNewTransaction("flushUpdateQueue", createTransaction));
            });
        } catch (final RuntimeException ex) {
            LOG.error("Failed to persist UpdateQueue content.", ex);
            return;
        }

        LOG.debug("{} events persisted.", drained);
    }

    private Void updateLastTargetQueries(final String tenant, final List<TargetPoll> polls) {
        LOG.debug("Persist {} targetqueries.", polls.size());

        final List<List<String>> pollChunks = Lists.partition(
                polls.stream().map(TargetPoll::getControllerId).collect(Collectors.toList()),
                Constants.MAX_ENTRIES_IN_STATEMENT);

        pollChunks.forEach(chunk -> {
            setLastTargetQuery(tenant, System.currentTimeMillis(), chunk);
            chunk.forEach(controllerId -> afterCommit.afterCommit(() -> eventPublisher
                    .publishEvent(new TargetPollEvent(controllerId, tenant, applicationContext.getId()))));
        });

        return null;
    }

    /**
     * Sets {@link Target#getLastTargetQuery()} by native SQL in order to avoid
     * raising opt lock revision as this update is not mission critical and in
     * fact only written by {@link ControllerManagement}, i.e. the target
     * itself.
     */
    private void setLastTargetQuery(final String tenant, final long currentTimeMillis, final List<String> chunk) {
        final Map<String, String> paramMapping = Maps.newHashMapWithExpectedSize(chunk.size());

        for (int i = 0; i < chunk.size(); i++) {
            paramMapping.put("cid" + i, chunk.get(i));
        }

        final Query updateQuery = entityManager.createNativeQuery(
                "UPDATE sp_target t SET t.last_target_query = #last_target_query WHERE t.controller_id IN ("
                        + formatQueryInStatementParams(paramMapping.keySet()) + ") AND t.tenant = #tenant");

        paramMapping.entrySet().forEach(entry -> updateQuery.setParameter(entry.getKey(), entry.getValue()));
        updateQuery.setParameter("last_target_query", currentTimeMillis);
        updateQuery.setParameter("tenant", tenant);

        final int updated = updateQuery.executeUpdate();
        if (updated < chunk.size()) {
            LOG.error("Targets polls could not be applied completely ({} instead of {}).", updated, chunk.size());
        }
    }

    private static String formatQueryInStatementParams(final Collection<String> paramNames) {
        return "#" + Joiner.on(",#").join(paramNames);
    }

    /**
     * Stores target directly to DB in case either {@link Target#getAddress()}
     * or {@link Target#getUpdateStatus()} changes or the buffer queue is full.
     * 
     */
    private Target updateTargetStatus(final JpaTarget toUpdate, final URI address) {
        boolean storeEager = isStoreEager(toUpdate, address);

        if (TargetUpdateStatus.UNKNOWN.equals(toUpdate.getUpdateStatus())) {
            toUpdate.setUpdateStatus(TargetUpdateStatus.REGISTERED);
            storeEager = true;
        }

        if (storeEager || !queue.offer(new TargetPoll(toUpdate))) {
            toUpdate.setAddress(address.toString());
            toUpdate.setLastTargetQuery(System.currentTimeMillis());

            afterCommit.afterCommit(
                    () -> eventPublisher.publishEvent(new TargetPollEvent(toUpdate, applicationContext.getId())));

            return targetRepository.save(toUpdate);
        }

        return toUpdate;
    }

    private boolean isStoreEager(final JpaTarget toUpdate, final URI address) {
        if (repositoryProperties.isEagerPollPersistence()) {
            return true;
        } else if (toUpdate.getAddress() == null) {
            return true;
        } else {
            return !toUpdate.getAddress().equals(address);
        }
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
            checkForTooManyStatusEntries(action);
            checkForTooManyStatusMessages(actionStatus);
            break;
        }

        actionStatus.setAction(actionRepository.save(action));
        actionStatusRepository.save(actionStatus);

        return action;
    }

    private void checkForTooManyStatusMessages(final JpaActionStatus actionStatus) {
        if (actionStatus.getMessages().size() > quotaManagement.getMaxMessagesPerActionStatus()) {
            throw new QuotaExceededException("ActionStatus messages", actionStatus.getMessages().size(),
                    quotaManagement.getMaxStatusEntriesPerAction());
        }

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
            checkForTooManyStatusEntries(action);
            checkForTooManyStatusMessages(actionStatus);
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

    private void checkForTooManyStatusEntries(final JpaAction action) {
        if (quotaManagement.getMaxStatusEntriesPerAction() > 0) {

            final Long statusCount = actionStatusRepository.countByAction(action);

            if (statusCount >= quotaManagement.getMaxStatusEntriesPerAction()) {
                throw new QuotaExceededException(ActionStatus.class, statusCount,
                        quotaManagement.getMaxStatusEntriesPerAction());
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
            throw new QuotaExceededException("Controller attribues", target.getControllerAttributes().size(),
                    quotaManagement.getMaxAttributeEntriesPerTarget());
        }

        target.setRequestControllerAttributes(false);

        return targetRepository.save(target);
    }

    @Override
    @Transactional
    @Retryable(include = {
            ConcurrencyFailureException.class }, maxAttempts = Constants.TX_RT_MAX, backoff = @Backoff(delay = Constants.TX_RT_DELAY))
    public Action registerRetrieved(final long actionId, final String message) {
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

        checkForTooManyStatusEntries(action);
        checkForTooManyStatusMessages(statusMessage);

        return actionStatusRepository.save(statusMessage);
    }

    private JpaAction getActionAndThrowExceptionIfNotFound(final Long actionId) {
        return actionRepository.findById(actionId)
                .orElseThrow(() -> new EntityNotFoundException(Action.class, actionId));
    }

    @Override
    public Optional<Target> getByControllerId(final String controllerId) {
        return targetRepository.findByControllerId(controllerId);
    }

    @Override
    public Optional<Target> get(final long targetId) {
        return Optional.ofNullable(targetRepository.findOne(targetId));
    }

    @Override
    public Page<ActionStatus> findActionStatusByAction(final Pageable pageReq, final long actionId) {
        if (!actionRepository.exists(actionId)) {
            throw new EntityNotFoundException(Action.class, actionId);
        }

        return actionStatusRepository.findByActionId(pageReq, actionId);
    }

    @Override
    public List<String> getActionHistoryMessages(final long actionId, final int messageCount) {
        // Just return empty list in case messageCount is zero.
        if (messageCount == 0) {
            return Collections.emptyList();
        }

        // For negative and large value of messageCount, limit the number of
        // messages.
        final int limit = messageCount < 0 || messageCount >= RepositoryConstants.MAX_ACTION_HISTORY_MSG_COUNT
                ? RepositoryConstants.MAX_ACTION_HISTORY_MSG_COUNT : messageCount;

        final PageRequest pageable = new PageRequest(0, limit, new Sort(Direction.DESC, "occurredAt"));
        final Page<String> messages = actionStatusRepository.findMessagesByActionIdAndMessageNotLike(pageable, actionId,
                RepositoryConstants.SERVER_MESSAGE_PREFIX + "%");

        LOG.debug("Retrieved {} message(s) from action history for action {}.", messages.getNumberOfElements(),
                actionId);

        return messages.getContent();
    }

    @Override
    public Optional<SoftwareModule> getSoftwareModule(final long id) {
        return Optional.ofNullable(softwareModuleRepository.findOne(id));
    }

    @Override
    public Map<Long, List<SoftwareModuleMetadata>> findTargetVisibleMetaDataBySoftwareModuleId(
            final Collection<Long> moduleId) {

        return softwareModuleMetadataRepository
                .findBySoftwareModuleIdInAndTargetVisible(new PageRequest(0, RepositoryConstants.MAX_META_DATA_COUNT),
                        moduleId, true)
                .getContent().stream().collect(Collectors.groupingBy(o -> (Long) o[0],
                        Collectors.mapping(o -> (SoftwareModuleMetadata) o[1], Collectors.toList())));
    }

    private static class TargetPoll {

        private final String tenant;
        private final String controllerId;

        TargetPoll(final Target target) {
            this.tenant = target.getTenant();
            this.controllerId = target.getControllerId();
        }

        public String getTenant() {
            return tenant;
        }

        public String getControllerId() {
            return controllerId;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((controllerId == null) ? 0 : controllerId.hashCode());
            result = prime * result + ((tenant == null) ? 0 : tenant.hashCode());
            return result;
        }

        @Override
        public boolean equals(final Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final TargetPoll other = (TargetPoll) obj;
            if (controllerId == null) {
                if (other.controllerId != null) {
                    return false;
                }
            } else if (!controllerId.equals(other.controllerId)) {
                return false;
            }
            if (tenant == null) {
                if (other.tenant != null) {
                    return false;
                }
            } else if (!tenant.equals(other.tenant)) {
                return false;
            }
            return true;
        }

    }
}
