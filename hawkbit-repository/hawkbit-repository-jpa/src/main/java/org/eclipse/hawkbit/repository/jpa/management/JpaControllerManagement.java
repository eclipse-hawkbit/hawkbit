/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.repository.jpa.management;

import static org.eclipse.hawkbit.repository.jpa.executor.AfterTransactionCommitExecutor.afterCommit;
import static org.eclipse.hawkbit.repository.model.Action.Status.DOWNLOADED;
import static org.eclipse.hawkbit.repository.model.Action.Status.FINISHED;
import static org.eclipse.hawkbit.repository.model.Target.CONTROLLER_ATTRIBUTE_MAX_KEY_SIZE;
import static org.eclipse.hawkbit.repository.model.Target.CONTROLLER_ATTRIBUTE_MAX_VALUE_SIZE;

import java.net.URI;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalUnit;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;
import jakarta.validation.constraints.NotEmpty;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.ListUtils;
import org.eclipse.hawkbit.context.Auditor;
import org.eclipse.hawkbit.context.System;
import org.eclipse.hawkbit.context.Tenant;
import org.eclipse.hawkbit.ql.jpa.QLSupport;
import org.eclipse.hawkbit.repository.ConfirmationManagement;
import org.eclipse.hawkbit.repository.ControllerManagement;
import org.eclipse.hawkbit.repository.DeploymentManagement;
import org.eclipse.hawkbit.repository.DistributionSetManagement;
import org.eclipse.hawkbit.repository.MaintenanceScheduleHelper;
import org.eclipse.hawkbit.repository.QuotaManagement;
import org.eclipse.hawkbit.repository.RepositoryConstants;
import org.eclipse.hawkbit.repository.RepositoryProperties;
import org.eclipse.hawkbit.repository.SecurityTokenGeneratorHolder;
import org.eclipse.hawkbit.repository.SoftwareModuleManagement;
import org.eclipse.hawkbit.repository.UpdateMode;
import org.eclipse.hawkbit.repository.event.EventPublisherHolder;
import org.eclipse.hawkbit.repository.event.remote.CancelTargetAssignmentEvent;
import org.eclipse.hawkbit.repository.event.remote.TargetAttributesRequestedEvent;
import org.eclipse.hawkbit.repository.event.remote.TargetPollEvent;
import org.eclipse.hawkbit.repository.exception.CancelActionNotAllowedException;
import org.eclipse.hawkbit.repository.exception.EntityAlreadyExistsException;
import org.eclipse.hawkbit.repository.exception.EntityNotFoundException;
import org.eclipse.hawkbit.repository.exception.InvalidTargetAttributeException;
import org.eclipse.hawkbit.repository.exception.SoftwareModuleNotAssignedToTargetException;
import org.eclipse.hawkbit.repository.helper.TenantConfigHelper;
import org.eclipse.hawkbit.repository.jpa.Jpa;
import org.eclipse.hawkbit.repository.jpa.acm.AccessController;
import org.eclipse.hawkbit.repository.jpa.configuration.Constants;
import org.eclipse.hawkbit.repository.jpa.model.AbstractJpaBaseEntity_;
import org.eclipse.hawkbit.repository.jpa.model.JpaAction;
import org.eclipse.hawkbit.repository.jpa.model.JpaActionStatus;
import org.eclipse.hawkbit.repository.jpa.model.JpaActionStatus_;
import org.eclipse.hawkbit.repository.jpa.model.JpaAction_;
import org.eclipse.hawkbit.repository.jpa.model.JpaDistributionSet;
import org.eclipse.hawkbit.repository.jpa.model.JpaTarget;
import org.eclipse.hawkbit.repository.jpa.model.JpaTarget_;
import org.eclipse.hawkbit.repository.jpa.repository.ActionRepository;
import org.eclipse.hawkbit.repository.jpa.repository.ActionStatusRepository;
import org.eclipse.hawkbit.repository.jpa.repository.SoftwareModuleRepository;
import org.eclipse.hawkbit.repository.jpa.repository.TargetRepository;
import org.eclipse.hawkbit.repository.jpa.repository.TargetTypeRepository;
import org.eclipse.hawkbit.repository.jpa.specifications.ActionSpecifications;
import org.eclipse.hawkbit.repository.jpa.specifications.TargetSpecifications;
import org.eclipse.hawkbit.repository.jpa.specifications.TargetTypeSpecification;
import org.eclipse.hawkbit.repository.jpa.utils.DeploymentHelper;
import org.eclipse.hawkbit.repository.jpa.utils.QuotaHelper;
import org.eclipse.hawkbit.repository.model.Action;
import org.eclipse.hawkbit.repository.model.Action.ActionStatusCreate;
import org.eclipse.hawkbit.repository.model.Action.Status;
import org.eclipse.hawkbit.repository.model.ActionStatus;
import org.eclipse.hawkbit.repository.model.AutoConfirmationStatus;
import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.repository.model.DistributionSetAssignmentResult;
import org.eclipse.hawkbit.repository.model.SoftwareModule;
import org.eclipse.hawkbit.repository.model.Target;
import org.eclipse.hawkbit.repository.model.TargetType;
import org.eclipse.hawkbit.repository.model.TargetUpdateStatus;
import org.eclipse.hawkbit.repository.qfields.TargetFields;
import org.eclipse.hawkbit.tenancy.configuration.ControllerPollProperties;
import org.eclipse.hawkbit.tenancy.configuration.DurationHelper;
import org.eclipse.hawkbit.tenancy.configuration.PollingTime;
import org.eclipse.hawkbit.tenancy.configuration.TenantConfigurationProperties.TenantConfigurationKey;
import org.eclipse.hawkbit.util.IpUtil;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty;
import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;

@Slf4j
@Transactional(readOnly = true)
@Validated
@Service
@ConditionalOnBooleanProperty(prefix = "hawkbit.jpa", name = { "enabled", "controller-management" }, matchIfMissing = true)
public class JpaControllerManagement extends JpaActionManagement implements ControllerManagement {

    private static final Pattern PATTERN = Pattern.compile("[a-zA-Z0-9_\\-!@#$%^&*()+=\\[\\]{}|;:'\",.<>/\\\\?\\s]*");

    // TODO - make it final
    private TargetRepository targetRepository;
    private final TargetTypeRepository targetTypeRepository;
    private final DeploymentManagement deploymentManagement;
    private final ConfirmationManagement confirmationManagement;
    private final SoftwareModuleRepository softwareModuleRepository;
    private final SoftwareModuleManagement<? extends SoftwareModule> softwareModuleManagement;
    private final DistributionSetManagement<? extends DistributionSet> distributionSetManagement;
    private final ControllerPollProperties controllerPollProperties;
    private final PlatformTransactionManager txManager;
    private final EntityManager entityManager;

    private final Duration minPollingTime;
    private final Duration maxPollingTime;
    private final BlockingDeque<TargetPoll> queue;

    @SuppressWarnings("squid:S00107")
    protected JpaControllerManagement(
            final ActionRepository actionRepository, final ActionStatusRepository actionStatusRepository, final QuotaManagement quotaManagement,
            final RepositoryProperties repositoryProperties,
            final TargetRepository targetRepository, final TargetTypeRepository targetTypeRepository,
            final DeploymentManagement deploymentManagement, final ConfirmationManagement confirmationManagement,
            final SoftwareModuleRepository softwareModuleRepository,
            final SoftwareModuleManagement<? extends SoftwareModule> softwareModuleManagement,
            final DistributionSetManagement<? extends DistributionSet> distributionSetManagement,
            final ControllerPollProperties controllerPollProperties,
            final PlatformTransactionManager txManager, final EntityManager entityManager,
            final ScheduledExecutorService executorService) {
        super(actionRepository, actionStatusRepository, quotaManagement, repositoryProperties);

        this.targetRepository = targetRepository;
        this.targetTypeRepository = targetTypeRepository;
        this.deploymentManagement = deploymentManagement;
        this.confirmationManagement = confirmationManagement;
        this.softwareModuleRepository = softwareModuleRepository;
        this.softwareModuleManagement = softwareModuleManagement;
        this.distributionSetManagement = distributionSetManagement;
        this.controllerPollProperties = controllerPollProperties;
        this.txManager = txManager;
        this.entityManager = entityManager;

        minPollingTime = controllerPollProperties.getMinPollingTime() == null
                ? Duration.of(0, ChronoUnit.SECONDS)
                : DurationHelper.fromString(controllerPollProperties.getMinPollingTime());
        maxPollingTime = controllerPollProperties.getMaxPollingTime() == null
                ? Duration.of(100, ChronoUnit.YEARS)
                : DurationHelper.fromString(controllerPollProperties.getMaxPollingTime());
        if (!repositoryProperties.isEagerPollPersistence()) {
            executorService.scheduleWithFixedDelay(this::flushUpdateQueue,
                    repositoryProperties.getPollPersistenceFlushTime(),
                    repositoryProperties.getPollPersistenceFlushTime(), TimeUnit.MILLISECONDS);
            queue = new LinkedBlockingDeque<>(repositoryProperties.getPollPersistenceQueueSize());
        } else {
            queue = null;
        }
    }

    @Override
    public int getWeightConsideringDefault(final Action action) {
        return super.getWeightConsideringDefault(action);
    }

    @Override
    protected void onActionStatusUpdate(final JpaActionStatus newActionStatus, final JpaAction action) {
        final Action.Status updatedActionStatus = newActionStatus.getStatus();
        final long timestamp = newActionStatus.getTimestamp();
        switch (updatedActionStatus) {
            case ERROR: {
                final JpaTarget target = action.getTarget();
                target.setUpdateStatus(TargetUpdateStatus.ERROR);
                handleErrorOnAction(action, target);
                break;
            }
            case FINISHED: {
                requestControllerAttributes(handleFinishedAndStoreInTargetStatus(timestamp, action));
                break;
            }
            case DOWNLOADED: {
                handleDownloadedActionStatus(action).ifPresent(controllerId ->
                        requestControllerAttributes(findByControllerId(controllerId)
                                .map(JpaTarget.class::cast)
                                .orElseThrow(() -> new EntityNotFoundException(Target.class, controllerId))));
                break;
            }
            default: {
                break;
            }
        }
    }

    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    @Retryable(retryFor = { ConcurrencyFailureException.class }, maxAttempts = Constants.TX_RT_MAX,
            backoff = @Backoff(delay = Constants.TX_RT_DELAY))
    public Action addCancelActionStatus(final ActionStatusCreate create) {
        final JpaAction action = actionRepository.getById(create.getActionId());
        if (!action.isCancelingOrCanceled()) {
            throw new CancelActionNotAllowedException("The action is not in canceling state.");
        }

        final JpaActionStatus actionStatus = buildJpaActionStatus(create);
        switch (actionStatus.getStatus()) {
            case CANCELED, FINISHED: {
                handleFinishedCancelation(actionStatus, action);
                break;
            }
            case ERROR, CANCEL_REJECTED: {
                // Cancellation rejected. Back to running.
                action.setStatus(Status.RUNNING);
                break;
            }
            default: {
                // information status entry - check for a potential DOS attack
                assertActionStatusQuota(create, action);
                assertActionStatusMessageQuota(actionStatus);
                break;
            }
        }

        actionStatus.setAction(actionRepository.save(action));
        actionStatusRepository.save(actionStatus);

        return action;
    }

    @Override
    public SoftwareModule getSoftwareModule(final long id) {
        return softwareModuleRepository.findById(id).map(SoftwareModule.class::cast)
                .orElseThrow(() -> new EntityNotFoundException(SoftwareModule.class, id));
    }

    @Override
    public Map<Long, Map<String, String>> findTargetVisibleMetaDataBySoftwareModuleId(final Collection<Long> moduleId) {
        return System.asSystem(() -> softwareModuleManagement.findMetaDataBySoftwareModuleIdsAndTargetVisible(moduleId));
    }

    @Override
    @Transactional
    @Retryable(retryFor = { ConcurrencyFailureException.class }, maxAttempts = Constants.TX_RT_MAX,
            backoff = @Backoff(delay = Constants.TX_RT_DELAY))
    public ActionStatus addInformationalActionStatus(final ActionStatusCreate create) {
        final JpaAction action = actionRepository.getById(create.getActionId());
        assertActionStatusQuota(create, action);

        final JpaActionStatus actionStatus = buildJpaActionStatus(create);
        actionStatus.setAction(action);
        assertActionStatusMessageQuota(actionStatus);

        return actionStatusRepository.save(actionStatus);
    }

    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    @Retryable(retryFor = { ConcurrencyFailureException.class }, maxAttempts = Constants.TX_RT_MAX,
            backoff = @Backoff(delay = Constants.TX_RT_DELAY))
    public Action addUpdateActionStatus(final ActionStatusCreate statusCreate) {
        return addActionStatus(statusCreate);
    }

    @Override
    public Optional<Action> findActiveActionWithHighestWeight(final String controllerId) {
        return Stream.concat(
                        // get the highest action with weight
                        actionRepository.findAll(
                                ActionSpecifications.byTargetControllerIdAndActiveAndWeightIsNull(controllerId, false),
                                PageRequest.of(
                                        0, 1,
                                        Sort.by(Sort.Order.desc(JpaAction_.WEIGHT), Sort.Order.asc(AbstractJpaBaseEntity_.ID)))).stream(),
                        // get the oldest action without weight
                        actionRepository.findAll(
                                ActionSpecifications.byTargetControllerIdAndActiveAndWeightIsNull(controllerId, false),
                                PageRequest.of(0, 1, Sort.by(Sort.Order.asc(AbstractJpaBaseEntity_.ID)))).stream())
                .min(Comparator.comparingInt(this::getWeightConsideringDefault).reversed().thenComparing(Action::getId))
                .map(Action.class::cast);
    }

    @Override
    public List<Action> findActiveActionsWithHighestWeight(final String controllerId, final int maxActionCount) {
        return super.findActiveActionsWithHighestWeightConsideringDefault(controllerId, maxActionCount);
    }

    @Override
    public Optional<Action> findActionWithDetails(final long actionId) {
        return actionRepository.findWithDetailsById(actionId);
    }

    @Override
    public Page<ActionStatus> findActionStatusByAction(final long actionId, final Pageable pageable) {
        if (!actionRepository.existsById(actionId)) {
            throw new EntityNotFoundException(Action.class, actionId);
        }

        return actionStatusRepository.findByActionId(pageable, actionId);
    }

    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    @Retryable(retryFor = ConcurrencyFailureException.class, noRetryFor = EntityAlreadyExistsException.class, maxAttempts = Constants.TX_RT_MAX, backoff = @Backoff(delay = Constants.TX_RT_DELAY))
    public Target findOrRegisterTargetIfItDoesNotExist(final String controllerId, final URI address) {
        return findOrRegisterTargetIfItDoesNotExist0(controllerId, address, null, null);
    }

    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    @Retryable(retryFor = ConcurrencyFailureException.class, noRetryFor = EntityAlreadyExistsException.class, maxAttempts = Constants.TX_RT_MAX, backoff = @Backoff(delay = Constants.TX_RT_DELAY))
    public Target findOrRegisterTargetIfItDoesNotExist(final String controllerId, final URI address, final String name, final String type) {
        return findOrRegisterTargetIfItDoesNotExist0(controllerId, address, name, type);
    }

    private Target findOrRegisterTargetIfItDoesNotExist0(final String controllerId, final URI address, final String name, final String type) {
        final Specification<JpaTarget> spec = (targetRoot, query, cb) -> cb.equal(targetRoot.get(JpaTarget_.controllerId), controllerId);
        return targetRepository.findOne(spec)
                .map(target -> updateTarget(target, address, name, type))
                .orElseGet(() -> createTarget(controllerId, address, name, type));
    }

    @Override
    public Action getActionForDownloadByTargetAndSoftwareModule(final String controllerId, final long moduleId) {
        throwExceptionIfTargetDoesNotExist(controllerId);
        throwExceptionIfSoftwareModuleDoesNotExist(moduleId);

        // it used to perform 3-table join query
        // @Query("Select a from JpaAction a join a.distributionSet ds join ds.modules modul where a.target.controllerId = :target and modul.id = :module order by a.id desc")
        //        final List<Action> actions = actionRepository.findActionByTargetAndSoftwareModule(controllerId, moduleId);
        // TODO AC - we could fetch distribution sets in order to skip calls to serarch for modules
        return actionRepository
                .findAll(ActionSpecifications.byTargetControllerIdAndActive(controllerId, true))
                .stream()
                .filter(action -> !action.isCancelingOrCanceled())
                .filter(action -> action.getDistributionSet().getModules()
                        .stream()
                        .anyMatch(module -> module.getId() == moduleId))
                .map(Action.class::cast)
                .findFirst()
                .orElseThrow(() -> new SoftwareModuleNotAssignedToTargetException(moduleId, controllerId));
    }

    @Override
    public String getPollingTime(final Target target) {
        return System.asSystem(() -> {
            final PollingTime pollingTime = new PollingTime(
                    TenantConfigHelper.getTenantConfigurationManagement()
                            .getConfigurationValue(TenantConfigurationKey.POLLING_TIME, String.class).getValue());
            if (!ObjectUtils.isEmpty(pollingTime.getOverrides()) && target instanceof JpaTarget jpaTarget) {
                for (final PollingTime.Override override : pollingTime.getOverrides()) {
                    try {
                        if (QLSupport.getInstance().entityMatcher(override.qlStr(), TargetFields.class).match(jpaTarget)) {
                            return override.pollingInterval().getFormattedIntervalWithDeviation(minPollingTime, maxPollingTime);
                        }
                    } catch (final Exception e) {
                        log.warn("Error while evaluating polling override for target {}: {}", jpaTarget.getId(), e.getMessage());
                    }
                }
            }
            // returns default - no overrides or not applicable for the target
            return pollingTime.getPollingInterval().getFormattedIntervalWithDeviation(minPollingTime, maxPollingTime);
        });
    }

    @Override
    public String getPollingTimeForAction(final Target target, final Action action) {
        final String pollingTime = getPollingTime(target);
        if (!action.hasMaintenanceSchedule() || action.isMaintenanceScheduleLapsed()) {
            return pollingTime;
        } else {
            // the count to be used for reducing polling interval -> the configured value of {@link TenantConfigurationKey#MAINTENANCE_WINDOW_POLL_COUNT}
            final int maintenanceWindowPollCount = TenantConfigHelper.getAsSystem(
                    TenantConfigurationKey.MAINTENANCE_WINDOW_POLL_COUNT, Integer.class);
            return new EventTimer(pollingTime, controllerPollProperties.getMinPollingTime(), ChronoUnit.SECONDS)
                    .timeToNextEvent(maintenanceWindowPollCount, action.getMaintenanceWindowStartTime().orElse(null));
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
    @Transactional
    @Retryable(retryFor = { ConcurrencyFailureException.class }, maxAttempts = Constants.TX_RT_MAX,
            backoff = @Backoff(delay = Constants.TX_RT_DELAY))
    public void registerRetrieved(final long actionId, final String message) {
        handleRegisterRetrieved(actionId, message);
    }

    @Override
    @Transactional
    @Retryable(retryFor = { ConcurrencyFailureException.class }, maxAttempts = Constants.TX_RT_MAX,
            backoff = @Backoff(delay = Constants.TX_RT_DELAY))
    public Target updateControllerAttributes(final String controllerId, final Map<String, String> data, final UpdateMode mode) {
        // Constraints on attribute keys & values are not validated by EclipseLink. Hence, they are validated here.
        if (data.entrySet().stream().anyMatch(e -> !isAttributeEntryValid(e))) {
            throw new InvalidTargetAttributeException();
        }

        final JpaTarget target = targetRepository.getByControllerId(controllerId);

        // get the modifiable attribute map
        final Map<String, String> controllerAttributes = target.getControllerAttributes();
        final UpdateMode updateMode = mode != null ? mode : UpdateMode.MERGE;
        switch (updateMode) {
            case REMOVE:
                // remove the addressed attributes
                data.keySet().forEach(controllerAttributes::remove);
                break;
            case REPLACE:
                // clear the attributes before adding the new attributes
                controllerAttributes.clear();
                copy(data, controllerAttributes);
                target.setRequestControllerAttributes(false);
                break;
            case MERGE:
                // just merge the attributes in
                copy(data, controllerAttributes);
                target.setRequestControllerAttributes(false);
                break;
            default:
                // unknown update mode
                throw new IllegalStateException("The update mode " + updateMode + " is not supported.");
        }
        assertTargetAttributesQuota(target);

        return targetRepository.save(target);
    }

    @Override
    public Optional<Target> find(final long targetId) {
        return targetRepository.findById(targetId).map(Target.class::cast);
    }

    @Override
    public Optional<Target> findByControllerId(final String controllerId) {
        return targetRepository.findByControllerId(controllerId).map(Target.class::cast);
    }

    @Override
    public List<String> getActionHistoryMessages(final long actionId, final int messageCount) {
        // Just return empty list in case messageCount is zero.
        if (messageCount == 0) {
            return Collections.emptyList();
        }

        // For negative and large value of messageCount, limit the number of messages.
        final int limit = messageCount < 0 || messageCount >= RepositoryConstants.MAX_ACTION_HISTORY_MSG_COUNT
                ? RepositoryConstants.MAX_ACTION_HISTORY_MSG_COUNT
                : messageCount;

        final PageRequest pageable = PageRequest.of(0, limit, Sort.by(Direction.DESC, "timestamp"));
        final Page<String> messages = actionStatusRepository.findMessagesByActionIdAndMessageNotLike(
                actionId, RepositoryConstants.SERVER_MESSAGE_PREFIX + "%", pageable);

        log.debug("Retrieved {} message(s) from action history for action {}.", messages.getNumberOfElements(), actionId);

        return messages.getContent();
    }

    @Override
    @Modifying
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public Action cancelAction(final Action action) {
        log.debug("cancelAction({})", action.getId());
        if (action.isCancelingOrCanceled()) {
            throw new CancelActionNotAllowedException("Actions in canceling or canceled state cannot be canceled");
        }

        if (action.isActive()) {
            log.debug("action ({}) was still active. Change to {}.", action, Status.CANCELING);
            final JpaAction jpaAction = (JpaAction) action;

            jpaAction.setStatus(Status.CANCELING);
            // document that the status has been retrieved
            actionStatusRepository.save(
                    new JpaActionStatus(jpaAction, Status.CANCELING, java.lang.System.currentTimeMillis(), "manual cancelation requested"));
            final Action saveAction = actionRepository.save(jpaAction);

            cancelAssignDistributionSetEvent(jpaAction);
            return saveAction;
        } else {
            throw new CancelActionNotAllowedException("Action [id: " + action.getId() + "] is not active and cannot be canceled");
        }
    }

    @Override
    public void updateActionExternalRef(final long actionId, @NotEmpty final String externalRef) {
        // if access control for target repository is present check that caller has UPDATE access to the target of the action
        targetRepository.getAccessController().ifPresent(
                accessController -> accessController.assertOperationAllowed(
                        AccessController.Operation.UPDATE,
                        actionRepository.getById(actionId).getTarget()));
        actionRepository.updateExternalRef(actionId, externalRef);
    }

    @Override
    public void deleteExistingTarget(@NotEmpty final String controllerId) {
        final JpaTarget target = targetRepository.getByControllerId(controllerId);
        targetRepository.deleteById(target.getId());
        entityManager.flush();
    }

    @Override
    public Optional<Action> findInstalledActionByTarget(final Target target) {
        final JpaTarget jpaTarget = (JpaTarget) target;
        return Optional.ofNullable(jpaTarget.getInstalledDistributionSet())
                .flatMap(installedDistributionSet -> actionRepository.findFirstByTargetIdAndDistributionSetIdAndStatusOrderByIdDesc(
                        jpaTarget.getId(), installedDistributionSet.getId(), FINISHED));
    }

    @Override
    public AutoConfirmationStatus activateAutoConfirmation(final String controllerId, final String initiator, final String remark) {
        return confirmationManagement.activateAutoConfirmation(controllerId, initiator, remark);
    }

    @Override
    public void deactivateAutoConfirmation(final String controllerId) {
        confirmationManagement.deactivateAutoConfirmation(controllerId);
    }

    @Override
    public boolean updateOfflineAssignedVersion(@NotEmpty final String controllerId, final String distributionName, final String version) {
        List<DistributionSetAssignmentResult> distributionSetAssignmentResults =
                System.asSystem(() -> Auditor.asAuditor(controllerId, () -> deploymentManagement.offlineAssignedDistributionSets(
                        List.of(Map.entry(controllerId, distributionSetManagement.findByNameAndVersion(distributionName, version).getId())))));

        return distributionSetAssignmentResults.stream()
                .findFirst()
                .map(result -> result.getAlreadyAssigned() == 0)
                .orElseThrow();
    }

    private Optional<TargetType> findTargetType(String targetTypeName) {
        return targetTypeRepository.findOne(TargetTypeSpecification.hasName(targetTypeName)).map(TargetType.class::cast);
    }

    // for testing
    void setTargetRepository(final TargetRepository targetRepositorySpy) {
        this.targetRepository = targetRepositorySpy;
    }

    private static boolean isAddressChanged(final URI addressToUpdate, final URI address) {
        return addressToUpdate == null || !addressToUpdate.equals(address);
    }

    private static boolean isNameChanged(final String nameToUpdate, final String name) {
        return StringUtils.hasText(name) && !nameToUpdate.equals(name);
    }

    private static boolean isTypeChanged(final TargetType targetTypeToUpdate, final String type) {
        return (type != null) && (targetTypeToUpdate == null || !targetTypeToUpdate.getName().equals(type));
    }

    private static boolean isStatusUnknown(final TargetUpdateStatus statusToUpdate) {
        return TargetUpdateStatus.UNKNOWN == statusToUpdate;
    }

    private static boolean isAttributeEntryValid(final Map.Entry<String, String> e) {
        return isAttributeKeyValid(e.getKey()) && isAttributeValueValid(e.getValue());
    }

    private static boolean isAttributeKeyValid(final String key) {
        return key != null && key.length() <= CONTROLLER_ATTRIBUTE_MAX_KEY_SIZE && PATTERN.matcher(key).matches();
    }

    private static boolean isAttributeValueValid(final String value) {
        return value == null || (value.length() <= CONTROLLER_ATTRIBUTE_MAX_VALUE_SIZE && PATTERN.matcher(value).matches());
    }

    private static void copy(final Map<String, String> src, final Map<String, String> trg) {
        if (src == null || src.isEmpty()) {
            return;
        }
        src.forEach((key, value) -> {
            if (value != null) {
                trg.put(key, value);
            } else {
                trg.remove(key);
            }
        });
    }

    private void throwExceptionIfTargetDoesNotExist(final String controllerId) {
        if (!targetRepository.exists(TargetSpecifications.hasControllerId(controllerId))) {
            throw new EntityNotFoundException(Target.class, controllerId);
        }
    }

    private void throwExceptionIfTargetDoesNotExist(final Long targetId) {
        if (!targetRepository.existsById(targetId)) {
            throw new EntityNotFoundException(Target.class, targetId);
        }
    }

    private void throwExceptionIfSoftwareModuleDoesNotExist(final Long moduleId) {
        if (!softwareModuleRepository.existsById(moduleId)) {
            throw new EntityNotFoundException(SoftwareModule.class, moduleId);
        }
    }

    private Target createTarget(final String controllerId, final URI address, final String name, final String type) {
        log.debug("Creating target for thing ID \"{}\".", controllerId);
        final JpaTarget jpaTarget = new JpaTarget();
        jpaTarget.setControllerId(controllerId);
        jpaTarget.setDescription("Plug and Play target: " + controllerId);
        jpaTarget.setName((StringUtils.hasText(name) ? name : controllerId));
        jpaTarget.setSecurityToken(SecurityTokenGeneratorHolder.getInstance().generateToken());
        jpaTarget.setUpdateStatus(TargetUpdateStatus.REGISTERED);
        jpaTarget.setLastTargetQuery(java.lang.System.currentTimeMillis());
        jpaTarget.setAddress(Optional.ofNullable(address).map(URI::toString).orElse(null));

        if (StringUtils.hasText(type)) {
            final Optional<TargetType> targetTypeOptional = findTargetType(type);
            if (targetTypeOptional.isPresent()) {
                log.debug("Setting target type for thing ID \"{}\" to \"{}\".", controllerId, type);
                jpaTarget.setTargetType(targetTypeOptional.get());
            } else {
                log.error("Target type with the provided name \"{}\" was not found. Creating target for thing ID" +
                        " \"{}\" without target type assignment", type, controllerId);
            }
        }

        final Target result = targetRepository.save(jpaTarget);
        afterCommit(() -> EventPublisherHolder.getInstance().getEventPublisher().publishEvent(new TargetPollEvent(result)));
        return result;
    }

    /**
     * Flush the update queue by means to persisting
     * {@link Target#getLastTargetQuery()}.
     */
    private void flushUpdateQueue() {
        log.debug("Run flushUpdateQueue.");

        final int size = queue.size();
        if (size == 0) {
            return;
        }

        log.debug("{} events in flushUpdateQueue.", size);

        final Set<TargetPoll> events = new HashSet<>(queue.size());
        final int drained = queue.drainTo(events);

        if (drained <= 0) {
            return;
        }

        try {
            events.stream().collect(Collectors.groupingBy(TargetPoll::getTenant)).forEach((tenant, polls) -> {
                final TransactionCallback<Void> createTransaction = status -> updateLastTargetQueries(tenant, polls);
                org.eclipse.hawkbit.context.System.asSystemAsTenant(
                        tenant,
                        () -> DeploymentHelper.runInNewTransaction(txManager, "flushUpdateQueue", createTransaction));
            });
        } catch (final RuntimeException ex) {
            log.error("Failed to persist UpdateQueue content.", ex);
            return;
        }

        log.debug("{} events persisted.", drained);
    }

    private Void updateLastTargetQueries(final String tenant, final List<TargetPoll> polls) {
        log.debug("Persist {} targetqueries.", polls.size());

        final List<List<String>> pollChunks = ListUtils.partition(
                polls.stream().map(TargetPoll::getControllerId).toList(),
                Constants.MAX_ENTRIES_IN_STATEMENT);

        pollChunks.forEach(chunk -> {
            setLastTargetQuery(tenant, java.lang.System.currentTimeMillis(), chunk);
            chunk.forEach(controllerId -> afterCommit(() -> EventPublisherHolder.getInstance().getEventPublisher()
                    .publishEvent(new TargetPollEvent(controllerId, tenant))));
        });

        return null;
    }

    /**
     * Sets {@link Target#getLastTargetQuery()} by native SQL in order to avoid raising opt lock revision as this update is not mission-critical
     * and in fact only written by {@link ControllerManagement}, i.e. the target itself.
     */
    private void setLastTargetQuery(final String tenant, final long currentTimeMillis, final List<String> chunk) {
        final Query updateQuery = entityManager.createNativeQuery(
                "UPDATE sp_target SET last_target_query = " + Jpa.nativeQueryParamPrefix() + "last_target_query " +
                        "WHERE controller_id IN (" + Jpa.formatNativeQueryInClause("cid", chunk) + ")" +
                        " AND tenant = " + Jpa.nativeQueryParamPrefix() + "tenant");

        updateQuery.setParameter("last_target_query", currentTimeMillis);
        Jpa.setNativeQueryInParameter(updateQuery, "cid", chunk);
        updateQuery.setParameter("tenant", tenant);

        final int updated = updateQuery.executeUpdate();
        if (updated < chunk.size()) {
            log.error("Targets polls could not be applied completely ({} instead of {}).", updated, chunk.size());
        }
    }

    /**
     * Stores target directly to DB in case either {@link Target#getAddress()} or {@link Target#getUpdateStatus()} or {@link Target#getName()}
     * changes or the buffer queue is full.
     */
    @SuppressWarnings("java:S3776") // it's just complex
    private Target updateTarget(final JpaTarget toUpdate, final URI address, final String name, final String type) {
        if (isStoreEager(toUpdate, address, name, type) || !queue.offer(new TargetPoll(toUpdate))) {
            if (isAddressChanged(IpUtil.addressToUri(toUpdate.getAddress()), address)) {
                toUpdate.setAddress(address.toString());
            }
            if (isNameChanged(toUpdate.getName(), name)) {
                toUpdate.setName(name);
            }

            if (isTypeChanged(toUpdate.getTargetType(), type)) {
                if (StringUtils.hasText(type)) {
                    final Optional<TargetType> targetTypeOptional = findTargetType(type);
                    if (targetTypeOptional.isPresent()) {
                        log.debug("Updating target type for thing ID \"{}\" to \"{}\".", toUpdate.getControllerId(), type);
                        toUpdate.setTargetType(targetTypeOptional.get());
                    } else {
                        log.error(
                                "Target type with the provided name \"{}\" was not found. Target type for thing ID \"{}\" will not be updated",
                                type, toUpdate.getControllerId());
                    }
                } else {
                    log.debug("Removing target type assignment for thing ID \"{}\".", toUpdate.getControllerId());
                    toUpdate.setTargetType(null); //unassign target type if "" target type name was provided
                }
            }
            if (isStatusUnknown(toUpdate.getUpdateStatus())) {
                toUpdate.setUpdateStatus(TargetUpdateStatus.REGISTERED);
            }
            toUpdate.setLastTargetQuery(java.lang.System.currentTimeMillis());
            afterCommit(() -> EventPublisherHolder.getInstance().getEventPublisher().publishEvent(new TargetPollEvent(toUpdate)));
            return targetRepository.save(toUpdate);
        }
        return toUpdate;
    }

    private boolean isStoreEager(final JpaTarget toUpdate, final URI address, final String name, final String type) {
        return repositoryProperties.isEagerPollPersistence() || isAddressChanged(IpUtil.addressToUri(toUpdate.getAddress()), address)
                || isNameChanged(toUpdate.getName(), name) || isTypeChanged(toUpdate.getTargetType(), type)
                || isStatusUnknown(toUpdate.getUpdateStatus());
    }

    private void handleFinishedCancelation(final JpaActionStatus actionStatus, final JpaAction action) {
        // in case of successful cancellation we also report the success at the canceled action itself.
        actionStatus.addMessage(RepositoryConstants.SERVER_MESSAGE_PREFIX + "Cancellation completion is finished successfully.");
        DeploymentHelper.successCancellation(action, actionRepository, targetRepository);
    }

    /**
     * Handles the case where the {@link Action.Status#DOWNLOADED} status is
     * reported by the device. In case the update is finished, a controllerId
     * will be returned to trigger a request for attributes.
     *
     * @param action updated action
     * @return a present controllerId in case the attributes needs to be
     *         requested.
     */
    private Optional<String> handleDownloadedActionStatus(final JpaAction action) {
        if (!isDownloadOnly(action)) {
            return Optional.empty();
        }

        final JpaTarget target = action.getTarget();
        action.setActive(false);
        action.setStatus(DOWNLOADED);
        target.setUpdateStatus(TargetUpdateStatus.IN_SYNC);
        targetRepository.save(target);

        return Optional.of(target.getControllerId());
    }

    private void requestControllerAttributes(final JpaTarget target) {
        target.setRequestControllerAttributes(true);

        EventPublisherHolder.getInstance().getEventPublisher()
                .publishEvent(new TargetAttributesRequestedEvent(Tenant.currentTenant(), target.getId(),
                        JpaTarget.class, target.getControllerId(), target.getAddress() != null ? target.getAddress() : null));
    }

    private void handleErrorOnAction(final JpaAction mergedAction, final JpaTarget mergedTarget) {
        mergedAction.setActive(false);
        mergedAction.setStatus(Status.ERROR);
        mergedTarget.setAssignedDistributionSet(null);

        targetRepository.save(mergedTarget);
    }

    /**
     * Handles the case where the {@link Action.Status#FINISHED} status is reported by the device. In case the update is finished,
     * a controllerId will be returned to trigger a request for attributes.
     *
     * @param action updated action
     * @return a present controllerId in case the attributes needs to be requested.
     */
    private JpaTarget handleFinishedAndStoreInTargetStatus(final long timestamp, final JpaAction action) {
        final JpaTarget target = action.getTarget();
        action.setActive(false);
        action.setStatus(Status.FINISHED);
        if (target.getInstallationDate() == null || target.getInstallationDate() < timestamp) {
            final JpaDistributionSet ds = entityManager.merge(action.getDistributionSet());

            target.setInstalledDistributionSet(ds);
            target.setInstallationDate(timestamp);

            // Target reported an installation of a DOWNLOAD_ONLY assignment, the assigned DS has to be adapted
            // because the currently assigned DS can be unequal to the currently installed DS (the downloadOnly DS)
            if (isDownloadOnly(action)) {
                target.setAssignedDistributionSet(action.getDistributionSet());
            }

            // check if the assigned set is equal to the installed set (not
            // necessarily the case as another update might be pending already).
            if (target.getAssignedDistributionSet() != null
                    && target.getAssignedDistributionSet().getId().equals(target.getInstalledDistributionSet().getId())) {
                target.setUpdateStatus(TargetUpdateStatus.IN_SYNC);
            }

            entityManager.detach(ds);
        }

        return target;
    }

    private void assertTargetAttributesQuota(final JpaTarget target) {
        final int limit = quotaManagement.getMaxAttributeEntriesPerTarget();
        QuotaHelper.assertAssignmentQuota(target.getId(), target.getControllerAttributes().size(), limit, "Attribute",
                Target.class.getSimpleName(), null);
    }

    /**
     * Registers retrieved status for given {@link Target} and {@link Action} if
     * it does not exist yet.
     *
     * @param actionId to the handle status for
     * @param message for the status
     */
    private void handleRegisterRetrieved(final Long actionId, final String message) {
        final JpaAction action = actionRepository.getById(actionId);
        // do a manual query with CriteriaBuilder to avoid unnecessary field queries and an extra
        // count query made by spring-data when using pageable requests, we don't need an extra count
        // query, we just want to check if the last action status is a retrieved or not.
        final CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        final CriteriaQuery<Object[]> queryActionStatus = cb.createQuery(Object[].class);
        final Root<JpaActionStatus> actionStatusRoot = queryActionStatus.from(JpaActionStatus.class);
        final CriteriaQuery<Object[]> query = queryActionStatus
                .multiselect(actionStatusRoot.get(AbstractJpaBaseEntity_.id), actionStatusRoot.get(JpaActionStatus_.status))
                .where(cb.equal(actionStatusRoot.get(JpaActionStatus_.action).get(AbstractJpaBaseEntity_.id), actionId))
                .orderBy(cb.desc(actionStatusRoot.get(AbstractJpaBaseEntity_.id)));
        final List<Object[]> resultList = entityManager.createQuery(query).setFirstResult(0).setMaxResults(1).getResultList();

        // if the latest status is not in retrieve state then we add a retrieved state again, we want
        // to document a deployment retrieved status and a cancel retrieved status, but multiple
        // retrieves after the other we don't want to store to protect to overflood action status in
        // case controller retrieves a action multiple times.
        if (resultList.isEmpty() || (Status.RETRIEVED != resultList.get(0)[1])) {
            // document that the status has been retrieved
            actionStatusRepository.save(new JpaActionStatus(action, Status.RETRIEVED, java.lang.System.currentTimeMillis(), message));

            // don't change the action status itself in case the action is in
            // canceling state otherwise
            // we modify the action status and the controller won't get the
            // cancel job anymore.
            if (!action.isCancelingOrCanceled()) {
                action.setStatus(Status.RETRIEVED);
            }
        }
    }

    private void cancelAssignDistributionSetEvent(final Action action) {
        afterCommit(() -> EventPublisherHolder.getInstance().getEventPublisher().publishEvent(new CancelTargetAssignmentEvent(action)));
    }

    /**
     * EventTimer to handle reduction of polling interval based on maintenance
     * window start time. Class models the next polling time as an event to be
     * raised and time to next polling as a timer. The event, in this case the
     * polling, should happen when timer expires. Class makes use of java.time
     * package to manipulate and calculate timer duration.
     */
    private static class EventTimer {

        private final String defaultEventInterval;
        private final Duration defaultEventIntervalDuration;

        private final String minimumEventInterval;
        private final Duration minimumEventIntervalDuration;

        private final TemporalUnit timeUnit;

        /**
         * Constructor.
         *
         * @param defaultEventInterval default timer value to use for interval between events.
         *         This puts an upper bound for the timer value
         * @param minimumEventInterval for loading {@link DistributionSet#getModules()}. This
         *         puts a lower bound to the timer value
         * @param timeUnit representing the unit of time to be used for timer.
         */
        EventTimer(final String defaultEventInterval, final String minimumEventInterval, final TemporalUnit timeUnit) {
            this.defaultEventInterval = defaultEventInterval;
            this.defaultEventIntervalDuration = MaintenanceScheduleHelper.convertToISODuration(defaultEventInterval);

            this.minimumEventInterval = minimumEventInterval;
            this.minimumEventIntervalDuration = MaintenanceScheduleHelper.convertToISODuration(minimumEventInterval);

            this.timeUnit = timeUnit;
        }

        /**
         * This method calculates the time interval until the next event based
         * on the desired number of events before the time when interval is
         * reset to default. The return value is bounded by
         * {@link EventTimer#defaultEventInterval} and
         * {@link EventTimer#minimumEventInterval}.
         *
         * @param eventCount number of events desired until the interval is reset to
         *         default. This is not guaranteed as the interval between
         *         events cannot be less than the minimum interval
         * @param timerResetTime time when exponential forwarding should reset to default
         * @return String in HH:mm:ss format for time to next event.
         */
        String timeToNextEvent(final int eventCount, final ZonedDateTime timerResetTime) {
            final ZonedDateTime currentTime = ZonedDateTime.now();

            // If there is no reset time, or if we already past the reset time,
            // return the default interval.
            if (timerResetTime == null || currentTime.compareTo(timerResetTime) > 0) {
                return defaultEventInterval;
            }

            // Calculate the interval timer based on desired event count.
            final Duration currentIntervalDuration = Duration.of(currentTime.until(timerResetTime, timeUnit), timeUnit)
                    .dividedBy(eventCount);

            // Need not return interval greater than the default.
            if (currentIntervalDuration.compareTo(defaultEventIntervalDuration) > 0) {
                return defaultEventInterval;
            }

            // Should not return interval less than minimum.
            if (currentIntervalDuration.compareTo(minimumEventIntervalDuration) < 0) {
                return minimumEventInterval;
            }

            return String.format("%02d:%02d:%02d", currentIntervalDuration.toHours(),
                    currentIntervalDuration.toMinutes() % 60, currentIntervalDuration.getSeconds() % 60);
        }
    }

    @Data
    private static class TargetPoll {

        private final String tenant;
        private final String controllerId;

        TargetPoll(final Target target) {
            this.tenant = target.getTenant();
            this.controllerId = target.getControllerId();
        }
    }
}