package org.eclipse.hawkbit.example.ddi.client;

import org.eclipse.hawkbit.ddi.client.DdiExampleClient;
import org.junit.Test;

/**
 * Unit test for simple App.
 */
public class AppTest {

    @Test
    public void AppTest() {

        final DdiExampleClient ddiClient = new DdiExampleClient("http://localhost:8080/", "mytest", "mytest", "desc",
                "DEFAULT");
        ddiClient.startDdiClient();
    }

}
