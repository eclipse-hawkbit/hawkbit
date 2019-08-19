package org.eclipse.hawkbit.dmf.hono;

import org.springframework.cloud.stream.annotation.Input;
import org.springframework.messaging.SubscribableChannel;

public interface HonoInputSink {
    String DEVICE_CREATED = "device-created";
    String DEVICE_DELETED = "device-deleted";
    String DEVICE_UPDATED = "device-updated";

    @Input(DEVICE_CREATED)
    SubscribableChannel onDeviceCreated();

    @Input(DEVICE_UPDATED)
    SubscribableChannel onDeviceUpdated();

    @Input(DEVICE_DELETED)
    SubscribableChannel onDeviceDeleted();
}
