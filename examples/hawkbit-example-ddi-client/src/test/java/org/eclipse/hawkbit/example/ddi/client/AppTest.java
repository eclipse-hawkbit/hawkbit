package org.eclipse.hawkbit.example.ddi.client;

import org.eclipse.hawkbit.ddi.client.DdiClient;
import org.junit.Test;

/**
 * Unit test for simple App.
 */
public class AppTest {

    @Test
    public void AppTest() {

        final DdiClient ddiClient = new DdiClient("localhost:8080", "mytest", "mytest", "desc", "DEFAULT");
        ddiClient.startDdiClient();
    }

}
