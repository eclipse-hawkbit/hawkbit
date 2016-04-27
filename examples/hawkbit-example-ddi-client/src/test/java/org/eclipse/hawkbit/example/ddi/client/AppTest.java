package org.eclipse.hawkbit.example.ddi.client;

import org.eclipse.hawkbit.ddi.client.DdiExampleClient;
import org.eclipse.hawkbit.ddi.client.strategy.SaveArtifactsToLocalTempDirectories;
import org.junit.Test;

/**
 * Unit test for simple App.
 */
public class AppTest {

    @Test
    public void AppTest() {
        final DdiExampleClient ddiClient = new DdiExampleClient("http://localhost:8080/", "Einstein17", "DEFAULT",
                new SaveArtifactsToLocalTempDirectories());
        final Thread thread = new Thread(ddiClient);
        thread.run();

        System.out.println("next steps........................");
    }

}
