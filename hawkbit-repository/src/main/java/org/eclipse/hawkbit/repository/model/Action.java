/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.model;

import java.util.List;

public interface Action extends TenantAwareBaseEntity {

    /**
     * indicating that target action has no force time {@link #hasForcedTime()}.
     */
    public static final long NO_FORCE_TIME = 0L;

    /**
     * @return the distributionSet
     */
    DistributionSet getDistributionSet();

    /**
     * @param distributionSet
     *            the distributionSet to set
     */
    void setDistributionSet(DistributionSet distributionSet);

    /**
     * @return true when action is in state {@link Status#CANCELING} or
     *         {@link Status#CANCELED}, false otherwise
     */
    boolean isCancelingOrCanceled();

    void setActive(boolean active);

    Status getStatus();

    void setStatus(Status status);

    int getDownloadProgressPercent();

    void setDownloadProgressPercent(int downloadProgressPercent);

    boolean isActive();

    void setActionType(ActionType actionType);

    /**
     * @return the actionType
     */
    ActionType getActionType();

    List<ActionStatus> getActionStatus();

    void setTarget(Target target);

    Target getTarget();

    long getForcedTime();

    void setForcedTime(long forcedTime);

    RolloutGroup getRolloutGroup();

    void setRolloutGroup(RolloutGroup rolloutGroup);

    Rollout getRollout();

    void setRollout(Rollout rollout);

    /**
     * checks if the {@link #forcedTime} is hit by the given
     * {@code hitTimeMillis}, by means if the given milliseconds are greater
     * than the forcedTime.
     *
     * @param hitTimeMillis
     *            the milliseconds, mostly the
     *            {@link System#currentTimeMillis()}
     * @return {@code true} if this {@link #type} is in
     *         {@link ActionType#TIMEFORCED} and the given {@code hitTimeMillis}
     *         is greater than the {@link #forcedTime} otherwise {@code false}
     */
    boolean isHitAutoForceTime(long hitTimeMillis);

    /**
     * @return {@code true} if either the {@link #type} is
     *         {@link ActionType#FORCED} or {@link ActionType#TIMEFORCED} but
     *         then if the {@link #forcedTime} has been exceeded otherwise
     *         always {@code false}
     */
    boolean isForce();

    /**
     * @return true when action is forced, false otherwise
     */
    boolean isForced();

    /**
     * Action status as reported by the controller.
     *
     * Be aware that JPA is persisting the ordinal number of the enum by means
     * the ordered number in the enum. So don't re-order the enums within the
     * Status enum declaration!
     *
     */
    public enum Status {
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
         * Action has been presented to the target.
         */
        RETRIEVED,

        /**
         * Action needs download by this target which has now started.
         */
        DOWNLOAD,

        /**
         * Action is in waiting state, e.g. the action is scheduled in a rollout
         * but not yet activated.
         */
        SCHEDULED;
    }

    /**
     * The action type for this action relation.
     *
     */
    public enum ActionType {
        FORCED, SOFT, TIMEFORCED;
    }

}