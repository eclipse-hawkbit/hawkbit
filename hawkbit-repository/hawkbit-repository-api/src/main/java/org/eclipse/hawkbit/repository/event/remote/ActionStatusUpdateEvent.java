package org.eclipse.hawkbit.repository.event.remote;

import java.util.List;

import org.eclipse.hawkbit.repository.event.TenantAwareEvent;
import org.eclipse.hawkbit.repository.model.Action;
import org.eclipse.hawkbit.repository.model.Action.Status;

/**
 * Information that represents status of a target for a given distributionSetId.
 */
public class ActionStatusUpdateEvent implements TenantAwareEvent {
    private final Long distributionSetId;
    private final String targetControllerId;
    private final String tenant;
    private final List<String> messages;
    private final Status status;

    public ActionStatusUpdateEvent(String tenant, Long distributionSetId, String targetControllerId, Status status,
            List<String> messages) {
        this.distributionSetId = distributionSetId;
        this.targetControllerId = targetControllerId;
        this.tenant = tenant;
        this.messages = messages;
        this.status = status;
    }

    /**
     * @return distributionSetId to which the current status belongs to.
     */
    public Long getDistributionSetId() {
        return distributionSetId;
    }

    /**
     * @return targetId for which the status is available.
     */
    public String getTargetControllerId() {
        return targetControllerId;
    }

    /**
     * @return the tenant under which the execution is happening.
     */
    public String getTenant() {
        return tenant;
    }

    /**
     * @return list of messages associated with the status.
     */
    public List<String> getMessages() {
        return messages;
    }

    /**
     * @return the status of the target in the context of distributionSetId.
     */
    public Status getStatus() {
        return status;
    }

}
