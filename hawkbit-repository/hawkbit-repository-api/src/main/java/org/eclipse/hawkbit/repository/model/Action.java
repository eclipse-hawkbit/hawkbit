/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.model;

import java.time.ZonedDateTime;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import javax.validation.constraints.NotEmpty;

/**
 * Update operations to be executed by the target.
 */
public interface Action extends TenantAwareBaseEntity {

    /**
     * Maximum length of controllerId.
     */
    int MAINTENANCE_WINDOW_SCHEDULE_LENGTH = 128;

    /**
     * Maximum length of controllerId.
     */
    int MAINTENANCE_WINDOW_DURATION_LENGTH = 16;

    /**
     * Maximum length of controllerId.
     */
    int MAINTENANCE_WINDOW_TIMEZONE_LENGTH = 8;

    /**
     * Maximum length of external reference.
     */
    int EXTERNAL_REF_MAX_LENGTH = 512;

    /**
     * Minimum weight to indicate the priority of {@link Action}.
     */
    int WEIGHT_MIN = 0;
    /**
     * Maximum weight to indicate the priority of {@link Action}.
     */
    int WEIGHT_MAX = 1000;

    /**
     * @return the distributionSet
     */
    DistributionSet getDistributionSet();

    /**
     * @return <code>true</code> when action is in state
     *         {@link Status#CANCELING} or {@link Status#CANCELED}, false
     *         otherwise
     */
    default boolean isCancelingOrCanceled() {
        return Status.CANCELING == getStatus() || Status.CANCELED == getStatus();
    }

    /**
     * @return current {@link Status} of the {@link Action}.
     */
    Status getStatus();

    /**
     * @return <code>true</code> if {@link Action} is still running.
     */
    boolean isActive();

    /**
     * @return the {@link ActionType}
     */
    ActionType getActionType();

    /**
     * @return {@link Target} of this {@link Action}.
     */
    Target getTarget();

    /**
     * @return time in {@link TimeUnit#MILLISECONDS} after which
     *         {@link #isForced()} switches to <code>true</code> in case of
     *         {@link ActionType#TIMEFORCED}.
     */
    long getForcedTime();

    /**
     * @return priority of the {@link Action}.
     */
    Optional<Integer> getWeight();

    /**
     * @return rolloutGroup related to this {@link Action}.
     */
    RolloutGroup getRolloutGroup();

    /**
     * @return rollout related to this {@link Action}.
     */
    Rollout getRollout();

    /**
     * @return maintenance window schedule related to this {@link Action}.
     */
    String getMaintenanceWindowSchedule();

    /**
     * @return maintenance window duration related to this {@link Action}.
     */
    String getMaintenanceWindowDuration();

    /**
     * @return maintenance window time zone related to this {@link Action}.
     */
    String getMaintenanceWindowTimeZone();

    /**
     * @param externalRef
     *            associated with this action
     */
    void setExternalRef(@NotEmpty String externalRef);

    /**
     * @return externalRef of the action
     */
    String getExternalRef();

    /**
     * @return the username that initiated this action (directly or indirectly)
     */
    String getInitiatedBy();

    /**
     * @return the latest action status code. Performance optimization to not
     *         query the action status table for the last action status code.
     */
    Optional<Integer> getLastActionStatusCode();

    /**
     * checks if the {@link #getForcedTime()} is hit by the given
     * {@code hitTimeMillis}, by means if the given milliseconds are greater
     * than the forcedTime.
     *
     * @param hitTimeMillis
     *            the milliseconds, mostly the
     *            {@link System#currentTimeMillis()}
     * @return {@code true} if this {@link #getActionType()} is in
     *         {@link ActionType#TIMEFORCED} and the given {@code hitTimeMillis}
     *         is greater than the {@link #getForcedTime()} otherwise
     *         {@code false}
     */
    default boolean isHitAutoForceTime(final long hitTimeMillis) {
        if (ActionType.TIMEFORCED == getActionType()) {
            return hitTimeMillis >= getForcedTime();
        }
        return false;
    }

    /**
     * Check if action type is either forced or time-forced <i>and</i> force-time is
     * exceeded.
     *
     * @return {@code true} if either the {@link #getActionType()} is
     *         {@link ActionType#FORCED} or {@link ActionType#TIMEFORCED} but then
     *         if the {@link #getForcedTime()} has been exceeded otherwise always
     *         {@code false}
     */
    default boolean isForcedOrTimeForced() {
        switch (getActionType()) {
        case FORCED:
            return true;
        case TIMEFORCED:
            return isHitAutoForceTime(System.currentTimeMillis());
        default:
            return false;
        }
    }

    /**
     * @return true when action is forced, false otherwise
     */
    default boolean isForced() {
        return ActionType.FORCED == getActionType();
    }

    /**
     * @return true when action is downloadonly, false otherwise
     */
    default boolean isDownloadOnly() {
        return ActionType.DOWNLOAD_ONLY == getActionType();
    }

    /**
     * Action status as reported by the controller.
     *
     * Be aware that JPA is persisting the ordinal number of the enum by means
     * the ordered number in the enum. So don't re-order the enums within the
     * Status enum declaration!
     *
     */
    enum Status {
        /**
         * Action is finished successfully for this target.
         */
        FINISHED,

        /**
         * Action has failed for this target.
         */
        ERROR,

        /**
         * Action is still running but with warnings.
         */
        WARNING,

        /**
         * Action is still running for this target.
         */
        RUNNING,

        /**
         * Action has been canceled for this target.
         */
        CANCELED,

        /**
         * Action is in canceling state and waiting for controller confirmation.
         */
        CANCELING,

        /**
         * Action has been send to the target.
         */
        RETRIEVED,

        /**
         * Action requests download by this target which has now started.
         */
        DOWNLOAD,

        /**
         * Action is in waiting state, e.g. the action is scheduled in a rollout
         * but not yet activated.
         */
        SCHEDULED,

        /**
         * Cancellation has been rejected by the controller.
         */
        CANCEL_REJECTED,

        /**
         * Action has been downloaded by the target and waiting for update to
         * start.
         */
        DOWNLOADED,

        /**
         * Action is waiting to be confirmed by the user
         */
        WAIT_FOR_CONFIRMATION
    }

    /**
     * The action type for this action relation.
     *
     */
    enum ActionType {
        /**
         * Forced action execution. Target is advised to executed immediately.
         */
        FORCED,

        /**
         * Soft action execution. Target is advised to execute when it fits.
         */
        SOFT,

        /**
         * {@link #SOFT} action execution until
         * {@link Action#isHitAutoForceTime(long)} is reached, {@link #FORCED}
         * after that.
         */
        TIMEFORCED,

        /**
         * Target is only advised to download, but not install
         */
        DOWNLOAD_ONLY
    }

    /**
     * Returns the start time of next available maintenance window for the
     * {@link Action} as {@link ZonedDateTime}. If a maintenance window is
     * already active, the start time of currently active window is returned.
     *
     * @return the start time as { @link Optional<ZonedDateTime>}.
     */
    Optional<ZonedDateTime> getMaintenanceWindowStartTime();

    /**
     * The method checks whether the action has a maintenance schedule defined
     * for it. A maintenance schedule defines a set of maintenance windows
     * during which actual update can be performed. A valid schedule defines at
     * least one maintenance window.
     *
     * @return true if action has a maintenance schedule, else false.
     */
    boolean hasMaintenanceSchedule();

    /**
     * The method checks whether the maintenance schedule has already lapsed for
     * the action, i.e. there are no more windows available for maintenance.
     * Controller manager uses the method to check if the maintenance schedule
     * has lapsed, and automatically cancels the action if it is lapsed.
     *
     * @return true if maintenance schedule has lapsed, else false.
     */
    boolean isMaintenanceScheduleLapsed();

    /**
     * The method checks whether a maintenance window is available for the
     * action to proceed. If it is available, a 'true' value is returned. The
     * maintenance window is considered available: 1) If there is no maintenance
     * schedule at all, in which case device can start update any time after
     * download is finished; or 2) the current time is within a scheduled
     * maintenance window start and end time.
     *
     * @return true if maintenance window is available, else false.
     */
    boolean isMaintenanceWindowAvailable();

    /**
     * Checks if the action is waiting for confirmation.
     * @return true if the action is waiting for confirmation, else false
     */
    boolean isWaitingConfirmation();

}
