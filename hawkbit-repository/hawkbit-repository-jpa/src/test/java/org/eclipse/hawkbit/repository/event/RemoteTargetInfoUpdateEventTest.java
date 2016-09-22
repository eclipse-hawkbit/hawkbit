/**
 * Copyright (c) 2011-2016 Bosch Software Innovations GmbH, Germany. All rights reserved.
 */
package org.eclipse.hawkbit.repository.event;

import org.eclipse.hawkbit.repository.jpa.AbstractJpaIntegrationTest;

import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

/**
 *
 */
@Features("Component Tests - Repository")
@Stories("Entity Events")
public class RemoteTargetInfoUpdateEventTest extends AbstractJpaIntegrationTest {

    // @Test
    // @Description("Verifies that the target info reloading by remote events
    // works")
    // public void reloadTargetInfoByRemoteEvent() {
    // final Target createdTarget =
    // targetManagement.createTarget(entityFactory.generateTarget("12345"));
    // final TargetInfoUpdateEvent infoUpdateEvent = new
    // TargetInfoUpdateEvent(createdTarget.getTenant(),
    // createdTarget.getId(), JpaTarget.class,
    // EventPublisherHolder.getInstance().getApplicationId());
    // final TargetInfo jpaTargetInfo = infoUpdateEvent.getEntity();
    // assertThat(jpaTargetInfo).isEqualTo(createdTarget.getTargetInfo());
    // }
}
