package org.eclipse.hawkbit.ui.distributions.event;

import org.eclipse.hawkbit.repository.model.SoftwareModule;
import org.eclipse.hawkbit.ui.common.table.BaseEntityEvent;
import org.eclipse.hawkbit.ui.common.table.BaseEntityEventType;

/**
 *
 *
 *
 */
public class SoftwareModuleTableEvent extends BaseEntityEvent<SoftwareModule> {

    /**
     * SoftwareModule table components events.
     *
     */
    public enum SoftwareModuleComponentEvent {
        SELECT_ALL
    }

    private SoftwareModuleComponentEvent softwareModuleComponentEvent;

    /**
     * Constructor.
     * 
     * @param eventType
     *            the event type.
     * @param entity
     *            the entity
     */
    public SoftwareModuleTableEvent(final BaseEntityEventType eventType, final SoftwareModule entity) {
        super(eventType, entity);
    }

    /**
     * The component event.
     * 
     * @param SoftwareModuleComponentEvent
     *            the softwareModule component event.
     */
    public SoftwareModuleTableEvent(final SoftwareModuleComponentEvent softwareModuleComponentEvent) {
        super(null, null);
        this.softwareModuleComponentEvent = softwareModuleComponentEvent;
    }

    public SoftwareModuleComponentEvent getSoftwareModuleComponentEvent() {
        return softwareModuleComponentEvent;
    }

}
