/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository;

import java.net.URI;
import java.util.List;
import java.util.Map;

import javax.persistence.EntityManager;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;
import javax.validation.constraints.NotNull;

import org.eclipse.hawkbit.im.authentication.SpPermission.SpringEvalExpressions;
import org.eclipse.hawkbit.repository.exception.EntityAlreadyExistsException;
import org.eclipse.hawkbit.repository.exception.EntityNotFoundException;
import org.eclipse.hawkbit.repository.exception.ToManyAttributeEntriesException;
import org.eclipse.hawkbit.repository.exception.ToManyStatusEntriesException;
import org.eclipse.hawkbit.repository.model.Action;
import org.eclipse.hawkbit.repository.model.Action.Status;
import org.eclipse.hawkbit.repository.model.ActionStatus;
import org.eclipse.hawkbit.repository.model.ActionStatus_;
import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.repository.model.SoftwareModule;
import org.eclipse.hawkbit.repository.model.Target;
import org.eclipse.hawkbit.repository.model.TargetInfo;
import org.eclipse.hawkbit.repository.model.TargetUpdateStatus;
import org.eclipse.hawkbit.repository.model.Target_;
import org.hibernate.validator.constraints.NotEmpty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.bind.RelaxedPropertyResolver;
import org.springframework.context.EnvironmentAware;
import org.springframework.core.env.Environment;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

/**
 * Service layer for all operations of the controller API (with access
 * permissions only for the controller).
 *
 *
 *
 */
@Transactional(readOnly = true)
@Validated
@Service
public class ControllerManagement implements EnvironmentAware {
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
    private DeploymentManagement deploymentManagement;

    @Autowired
    private TargetInfoRepository targetInfoRepository;

    @Autowired
    private SoftwareModuleRepository softwareModuleRepository;

    @Autowired
    private ActionStatusRepository actionStatusRepository;

    private Integer maxCount = 1000;

    private Integer maxAttributes = 100;

    /**
     * Refreshes the time of the last time the controller has been connected to
     * the server.
     *
     * @param targetid
     *            of the target to to update
     * @param address
     *            the client address of the target, might be {@code null}
     * @return the updated target
     *
     * @throws EntityNotFoundException
     *             if target with given ID could not be found
     */
    @Modifying
    @Transactional
    @PreAuthorize(SpringEvalExpressions.IS_CONTROLLER)
    public Target updateLastTargetQuery(@NotEmpty final String targetid, final URI address) {
        final Target target = targetRepository.findByControllerId(targetid);
        if (target == null) {
            throw new EntityNotFoundException(targetid);
        }

        return updateLastTargetQuery(target.getTargetInfo(), address).getTarget();
    }

    /**
     * Retrieves last {@link UpdateAction} for a download of an artifact of
     * given module and target.
     *
     * @param targetId
     *            to look for
     * @param module
     *            that should be assigned to the target
     * @return last {@link UpdateAction} for given combination
     *
     * @throws EntityNotFoundException
     *             if action for given combination could not be found
     */
    @PreAuthorize(SpringEvalExpressions.IS_CONTROLLER)
    public Action getActionForDownloadByTargetAndSoftwareModule(@NotNull final String targetId,
            @NotNull final SoftwareModule module) {
        final List<Action> action = actionRepository.findActionByTargetAndSoftwareModule(targetId, module);

        if (action.isEmpty() || action.get(0).isCancelingOrCanceled()) {
            throw new EntityNotFoundException(
                    "No assigment found for module " + module.getId() + " to target " + targetId);
        }

        return action.get(0);
    }

    /**
     * Refreshes the time of the last time the controller has been connected to
     * the server.
     *
     * @param target
     *            to update
     * @param address
     *            the client address of the target, might be {@code null}
     * @return the updated target
     *
     */
    @Modifying
    @Transactional
    @PreAuthorize(SpringEvalExpressions.IS_CONTROLLER)
    public TargetInfo updateLastTargetQuery(@NotNull final TargetInfo target, final URI address) {
        return updateTargetStatus(target, null, System.currentTimeMillis(), address);
    }

    /**
     * Retrieves all {@link Action}s which are active and assigned to a
     * {@link Target}.
     *
     * @param target
     *            the target to retrieve the actions from
     * @return a list of actions assigned to given target which are active
     */
    @PreAuthorize(SpringEvalExpressions.IS_CONTROLLER)
    public List<Action> findActionByTargetAndActive(final Target target) {
        return actionRepository.findByTargetAndActiveOrderByIdAsc(target, true);
    }

    /**
     * Retrieves all {@link SoftwareModule}s which are assigned to the given
     * {@link DistributionSet}.
     *
     * @param distributionSet
     *            the distribution set which should be assigned to the returned
     *            {@link SoftwareModule}s
     * @return a list of {@link SoftwareModule}s assigned to given
     *         {@code distributionSet}
     */
    @PreAuthorize(SpringEvalExpressions.IS_CONTROLLER)
    public List<SoftwareModule> findSoftwareModulesByDistributionSet(final DistributionSet distributionSet) {
        return softwareModuleRepository.findByAssignedTo(distributionSet);
    }

    /**
     * Get the {@link Action} entity for given actionId with all lazy
     * attributes.
     *
     * @param actionId
     *            to be id of the action
     * @return the corresponding {@link Action}
     */
    @PreAuthorize(SpringEvalExpressions.IS_CONTROLLER)
    public Action findActionWithDetails(@NotNull final Long actionId) {
        return actionRepository.findById(actionId);
    }

    /**
     * register new target in the repository (plug-and-play).
     *
     * @param targetid
     *            reference
     * @param address
     *            the client IP address of the target, might be {@code null}
     * @return target reference
     */
    @Modifying
    @Transactional
    @PreAuthorize(SpringEvalExpressions.IS_CONTROLLER)
    public Target findOrRegisterTargetIfItDoesNotexist(@NotEmpty final String targetid, final URI address) {
        final Specification<Target> spec = (targetRoot, query, cb) -> cb.equal(targetRoot.get(Target_.controllerId),
                targetid);

        Target target = targetRepository.findOne(spec);

        if (target == null) {
            target = new Target(targetid);
            target.setDescription("Plug and Play target: " + targetid);
            target.setName(targetid);
            return targetManagement.createTarget(target, TargetUpdateStatus.REGISTERED, System.currentTimeMillis(),
                    address);
        }

        return updateLastTargetQuery(target.getTargetInfo(), address).getTarget();
    }

    /**
     * Update selective the target status of a given {@code target}.
     *
     * @param targetInfo
     *            the target to update the target status
     * @param status
     *            the status to be set of the target. Might be {@code null} if
     *            the target status should not be updated
     * @param lastTargetQuery
     *            the last target query to be set of the target. Might be
     *            {@code null} if the target lastTargetQuery should not be
     *            updated
     * @param address
     *            the client address of the target, might be {@code null}
     * @return the updated TargetInfo
     */
    @Modifying
    @Transactional
    @PreAuthorize(SpringEvalExpressions.IS_CONTROLLER)
    public TargetInfo updateTargetStatus(@NotNull final TargetInfo targetInfo, final TargetUpdateStatus status,
            final Long lastTargetQuery, final URI address) {
        final TargetInfo mtargetInfo = entityManager.merge(targetInfo);
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

    /**
     * Adds an {@link ActionStatus} for a {@link UpdateAction} and cancels the
     * {@link UpdateAction} if necessary.
     * 
     * @param actionStatus
     *            to be updated
     * @param action
     *            the status is for
     * @return the persisted {@link Action}
     * 
     */
    @Modifying
    @Transactional
    @PreAuthorize(SpringEvalExpressions.IS_CONTROLLER)
    public Action addCancelActionStatus(@NotNull final ActionStatus actionStatus, final Action action) {

        checkForToManyStatusEntries(action);
        action.setStatus(actionStatus.getStatus());

        switch (actionStatus.getStatus()) {
        case WARNING:
        case ERROR:
        case RUNNING:
            break;
        case CANCELED:
        case FINISHED:
            // in case of successful cancelation we also report the success at
            // the canceled action itself.
            actionStatus.addMessage("Cancelation completion is finished sucessfully.");
            deploymentManagement.successCancellation(action);
            break;
        case RETRIEVED:
            actionStatus.addMessage("Cancelation request retrieved");
            break;
        default:
        }
        actionRepository.save(action);
        actionStatusRepository.save(actionStatus);

        return action;
    }

    /**
     * Updates an {@link ActionStatus} for a {@link UpdateAction}.
     *
     * @param actionStatus
     *            to be updated
     * @param action
     *            the update is for
     * @return the persisted {@link Action}
     *
     * @throws EntityAlreadyExistsException
     *             if a given entity already exists
     * @throws ToManyStatusEntriesException
     *             if more than the allowed number of status entries are
     *             inserted
     */
    @Modifying
    @Transactional
    @PreAuthorize(SpringEvalExpressions.IS_CONTROLLER)
    public Action addUpdateActionStatus(@NotNull final ActionStatus actionStatus, final Action action) {

        if (!action.isActive()) {
            LOG.debug("Update of actionStatus {} for action {} not possible since action not active anymore.",
                    actionStatus.getId(), action.getId());
            return action;
        }
        return handleAddUpdateActionStatus(actionStatus, action);
    }

    /**
     * Sets {@link TargetUpdateStatus} based on given {@link ActionStatus}.
     *
     * @param actionStatus
     * @param action
     * @return
     */
    public Action handleAddUpdateActionStatus(final ActionStatus actionStatus, final Action action) {
        LOG.debug("addUpdateActionStatus for action {}", action.getId());

        final Action mergedAction = entityManager.merge(action);
        Target mergedTarget = mergedAction.getTarget();
        // check for a potential DOS attack
        checkForToManyStatusEntries(action);

        switch (actionStatus.getStatus()) {
        case ERROR:
            mergedTarget = deploymentManagement.updateTargetInfo(mergedTarget, TargetUpdateStatus.ERROR, false);
            // set action inactive
            mergedAction.setActive(false);
            mergedAction.setStatus(Status.ERROR);
            mergedTarget.setAssignedDistributionSet(null);
            targetManagement.updateTarget(mergedTarget);
            break;
        case FINISHED:
            // set action inactive
            mergedAction.setActive(false);
            mergedAction.setStatus(Status.FINISHED);
            handleFinishedAndStoreInTargetStatus(mergedTarget, mergedAction);
            break;
        case CANCELED:
        case WARNING:
        case RUNNING:
            deploymentManagement.updateTargetInfo(mergedTarget, TargetUpdateStatus.PENDING, false);
            break;
        default:
            break;
        }

        actionStatusRepository.save(actionStatus);

        LOG.debug("addUpdateActionStatus {} for target {} is finished.", action.getId(), mergedTarget.getId());

        return actionRepository.save(mergedAction);
    }

    private void checkForToManyStatusEntries(final Action action) {
        if (maxCount > 0) {

            final Long statusCount = actionStatusRepository.countByAction(action);

            if (statusCount >= maxCount) {
                LOG_DOS.error(
                        "Potential denial of service (DOS) attack identfied. More status entries in the system than permitted ({})!",
                        maxCount);
                throw new ToManyStatusEntriesException(String.valueOf(maxCount));
            }
        }
    }

    private void handleFinishedAndStoreInTargetStatus(final Target target, final Action action) {
        final TargetInfo targetInfo = target.getTargetInfo();
        final DistributionSet ds = entityManager.merge(action.getDistributionSet());
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

    /**
     * Updates attributes of the controller.
     *
     * @param targetid
     *            to update
     * @param data
     *            to insert
     *
     * @return updated {@link Target}
     *
     * @throws EntityNotFoundException
     *             if target that has to be updated could not be found
     * @throws ToManyAttributeEntriesException
     *             if maximum
     */
    @Modifying
    @NotNull
    @Transactional
    @PreAuthorize(SpringEvalExpressions.IS_CONTROLLER)
    public Target updateControllerAttributes(@NotEmpty final String targetid, @NotNull final Map<String, String> data) {
        final Target target = targetRepository.findByControllerId(targetid);

        if (target == null) {
            throw new EntityNotFoundException(targetid);
        }

        target.getTargetInfo().getControllerAttributes().putAll(data);

        if (target.getTargetInfo().getControllerAttributes().size() > maxAttributes) {
            LOG_DOS.info("Target tries to insert more than the allowed number of entries ({}). DOS attack anticipated!",
                    maxAttributes);
            throw new ToManyAttributeEntriesException(String.valueOf(maxAttributes));
        }

        target.getTargetInfo().setLastTargetQuery(System.currentTimeMillis());
        target.getTargetInfo().setRequestControllerAttributes(false);
        return targetRepository.save(target);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.springframework.context.EnvironmentAware#setEnvironment(org.
     * springframework.core.env. Environment)
     */
    @Override
    public void setEnvironment(final Environment environment) {
        final RelaxedPropertyResolver env = new RelaxedPropertyResolver(environment, "hawkbit.server.");
        maxCount = env.getProperty("security.dos.maxStatusEntriesPerAction", Integer.class, 1000);
        maxAttributes = env.getProperty("security.dos.maxAttributeEntriesPerTarget", Integer.class, 100);
    }

    /**
     * Registers retrieved status for given {@link Target} and {@link Action} if
     * it does not exist yet.
     *
     * @param action
     *            to the handle status for
     * @param target
     *            to the handle status for
     * @param message
     *            for the status
     * @return the update action in case the status has been changed to
     *         {@link Status#RETRIEVED}
     */
    @Modifying
    @Transactional
    @PreAuthorize(SpringEvalExpressions.IS_CONTROLLER)
    public Action registerRetrieved(final Action action, final String message) {
        return handleRegisterRetrieved(action, message);
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
    public Action handleRegisterRetrieved(final Action action, final String message) {
        // do a manual query with CriteriaBuilder to avoid unnecessary field
        // queries and an extra
        // count query made by spring-data when using pageable requests, we
        // don't need an extra count
        // query, we just want to check if the last action status is a retrieved
        // or not.
        final CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        final CriteriaQuery<Object[]> queryActionStatus = cb.createQuery(Object[].class);
        final Root<ActionStatus> actionStatusRoot = queryActionStatus.from(ActionStatus.class);
        final CriteriaQuery<Object[]> query = queryActionStatus
                .multiselect(actionStatusRoot.get(ActionStatus_.id), actionStatusRoot.get(ActionStatus_.status))
                .where(cb.equal(actionStatusRoot.get(ActionStatus_.action), action))
                .orderBy(cb.desc(actionStatusRoot.get(ActionStatus_.id)));
        final List<Object[]> resultList = entityManager.createQuery(query).setFirstResult(0).setMaxResults(1)
                .getResultList();

        // if the latest status is not in retrieve state then we add a retrieved
        // state again, we want
        // to document a deployment retrieved status and a cancel retrieved
        // status, but multiple
        // retrieves after the other we don't want to store to protect to
        // overflood action status in
        // case controller retrieves a action multiple times.
        if (resultList.isEmpty() || resultList.get(0)[1] != Status.RETRIEVED) {
            // document that the status has been retrieved
            actionStatusRepository
                    .save(new ActionStatus(action, Status.RETRIEVED, System.currentTimeMillis(), message));

            // don't change the action status itself in case the action is in
            // canceling state otherwise
            // we modify the action status and the controller won't get the
            // cancel job anymore.
            if (!action.isCancelingOrCanceled()) {
                final Action actionMerge = entityManager.merge(action);
                actionMerge.setStatus(Status.RETRIEVED);
                return actionRepository.save(actionMerge);
            }
        }
        return action;
    }

    /**
     * @param statusMessage
     */
    @PreAuthorize(SpringEvalExpressions.IS_CONTROLLER)
    @Modifying
    @Transactional
    public void addActionStatusMessage(final ActionStatus statusMessage) {
        actionStatusRepository.save(statusMessage);
    }

    /**
     * An direct access to the security token of an
     * {@link Target#getSecurityToken()} without authorization. This is
     * necessary to be able to access the security-token without any
     * security-context information because the security-token is used for
     * authentication.
     *
     * @param controllerId
     *            the ID of the controller to retrieve the security token for
     * @return the security context of the target, in case no target exists for
     *         the given controllerId {@code null} is returned
     */
    @Transactional
    public String getSecurityTokenByControllerId(final String controllerId) {
        final Target target = targetRepository.findByControllerId(controllerId);
        return target != null ? target.getSecurityToken() : null;
    }
}
