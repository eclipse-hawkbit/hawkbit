/**
 * Copyright (c) 2020 Bosch.IO GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.push;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BooleanSupplier;

import org.eclipse.hawkbit.repository.event.entity.EntityIdEvent;
import org.eclipse.hawkbit.repository.event.remote.DistributionSetDeletedEvent;
import org.eclipse.hawkbit.repository.event.remote.DistributionSetTagDeletedEvent;
import org.eclipse.hawkbit.repository.event.remote.DistributionSetTypeDeletedEvent;
import org.eclipse.hawkbit.repository.event.remote.RolloutDeletedEvent;
import org.eclipse.hawkbit.repository.event.remote.SoftwareModuleDeletedEvent;
import org.eclipse.hawkbit.repository.event.remote.SoftwareModuleTypeDeletedEvent;
import org.eclipse.hawkbit.repository.event.remote.TargetDeletedEvent;
import org.eclipse.hawkbit.repository.event.remote.TargetFilterQueryDeletedEvent;
import org.eclipse.hawkbit.repository.event.remote.TargetTagDeletedEvent;
import org.eclipse.hawkbit.repository.event.remote.TargetTypeDeletedEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.DistributionSetCreatedEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.DistributionSetTagCreatedEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.DistributionSetTagUpdatedEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.DistributionSetTypeCreatedEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.DistributionSetTypeUpdatedEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.DistributionSetUpdatedEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.RolloutCreatedEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.SoftwareModuleCreatedEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.SoftwareModuleTypeCreatedEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.SoftwareModuleTypeUpdatedEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.SoftwareModuleUpdatedEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.TargetCreatedEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.TargetFilterQueryCreatedEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.TargetFilterQueryUpdatedEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.TargetTagCreatedEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.TargetTagUpdatedEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.TargetTypeCreatedEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.TargetTypeUpdatedEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.TargetUpdatedEvent;
import org.eclipse.hawkbit.ui.SpPermissionChecker;
import org.eclipse.hawkbit.ui.push.event.ActionChangedEvent;
import org.eclipse.hawkbit.ui.push.event.RolloutChangedEvent;
import org.eclipse.hawkbit.ui.push.event.RolloutGroupChangedEvent;

/**
 * The default hawkbit event permission checker.
 */
public class HawkbitEventPermissionChecker implements UIEventPermissionChecker {
    private final SpPermissionChecker permChecker;
    private final Map<Collection<Class<? extends EntityIdEvent>>, BooleanSupplier> eventPermissionRules;

    /**
     * Constructor.
     * 
     * @param permChecker
     *            Permission Checker
     */
    public HawkbitEventPermissionChecker(final SpPermissionChecker permChecker) {
        this.permChecker = permChecker;
        this.eventPermissionRules = new HashMap<>();

        initEventsPermissionRules();
    }

    private void initEventsPermissionRules() {
        eventPermissionRules.put(
                Arrays.asList(TargetCreatedEvent.class, TargetUpdatedEvent.class, TargetDeletedEvent.class,
                        TargetTagCreatedEvent.class, TargetTagUpdatedEvent.class, TargetTagDeletedEvent.class,
                        ActionChangedEvent.class, TargetFilterQueryCreatedEvent.class,
                        TargetFilterQueryUpdatedEvent.class, TargetFilterQueryDeletedEvent.class,
                        TargetTypeCreatedEvent.class, TargetTypeUpdatedEvent.class, TargetTypeDeletedEvent.class),
                permChecker::hasTargetReadPermission);

        eventPermissionRules.put(Arrays.asList(DistributionSetCreatedEvent.class, DistributionSetUpdatedEvent.class,
                DistributionSetDeletedEvent.class, SoftwareModuleCreatedEvent.class, SoftwareModuleUpdatedEvent.class,
                SoftwareModuleDeletedEvent.class, DistributionSetTagCreatedEvent.class,
                DistributionSetTagUpdatedEvent.class, DistributionSetTagDeletedEvent.class,
                DistributionSetTypeCreatedEvent.class, DistributionSetTypeUpdatedEvent.class,
                DistributionSetTypeDeletedEvent.class, SoftwareModuleTypeCreatedEvent.class,
                SoftwareModuleTypeUpdatedEvent.class, SoftwareModuleTypeDeletedEvent.class),
                permChecker::hasReadRepositoryPermission);

        eventPermissionRules.put(Arrays.asList(RolloutCreatedEvent.class, RolloutChangedEvent.class,
                RolloutDeletedEvent.class, RolloutGroupChangedEvent.class), permChecker::hasRolloutReadPermission);
    }

    @Override
    public boolean isEventAllowed(final Class<? extends EntityIdEvent> eventClass) {
        return eventPermissionRules.entrySet().stream().filter(entry -> entry.getKey().contains(eventClass)).findAny()
                .map(entry -> entry.getValue().getAsBoolean()).orElse(false);
    }
}
