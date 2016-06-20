package org.eclipse.hawkbit.repository;

import org.eclipse.hawkbit.repository.model.ActionStatus;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for the repository.
 *
 */
@ConfigurationProperties("hawkbit.server.repository")
public class RepositoryProperties {

    /**
     * Set to <code>true</code> if the repository has to reject
     * {@link ActionStatus} entries for actions that are closed. Note: if this
     * is enforced you have to make sure that the feedback channel from the
     * devices i in order.
     */
    private boolean rejectActionStatusForClosedAction = false;

    public boolean isRejectActionStatusForClosedAction() {
        return rejectActionStatusForClosedAction;
    }

    public void setRejectActionStatusForClosedAction(final boolean rejectActionStatusForClosedAction) {
        this.rejectActionStatusForClosedAction = rejectActionStatusForClosedAction;
    }

}
