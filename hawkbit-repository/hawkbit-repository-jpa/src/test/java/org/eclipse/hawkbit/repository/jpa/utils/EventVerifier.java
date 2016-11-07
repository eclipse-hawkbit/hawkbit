package org.eclipse.hawkbit.repository.jpa.utils;

import static org.fest.assertions.api.Assertions.assertThat;

import java.util.concurrent.atomic.AtomicInteger;

import org.eclipse.hawkbit.repository.event.remote.RemoteTenantAwareEvent;
import org.eclipse.hawkbit.repository.test.util.TestApplicationContextProvider;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;
import org.springframework.context.ApplicationListener;
import org.springframework.context.ConfigurableApplicationContext;

public class EventVerifier implements TestRule {

    private final AtomicInteger counter = new AtomicInteger();

    @Override
    public Statement apply(final Statement rootStatement, final Description description) {
        return new Statement() {
            @Override
            public void evaluate() throws Throwable {
                beforeTest(description);
                rootStatement.evaluate();
                afterTest(description);
            }
        };
    }

    private void beforeTest(final Description description) {

        if (hasEventCountAnnotation(description)) {

            final ConfigurableApplicationContext context = TestApplicationContextProvider.getContext();
            context.addApplicationListener(new ApplicationListener<RemoteTenantAwareEvent>() {

                @Override
                public void onApplicationEvent(final RemoteTenantAwareEvent event) {

                    if (shouldCountEvent(description, event)) {
                        counter.incrementAndGet();
                    }
                }
            });
        }
    }

    private void afterTest(final Description description) {
        if (hasEventCountAnnotation(description)) {
            final ExpectEvent annotation = description.getAnnotation(ExpectEvent.class);
            assertThat(counter.intValue()).as("Expected events").isEqualTo(annotation.count());
        }
    }

    private boolean hasEventCountAnnotation(final Description description) {
        return description.getAnnotation(ExpectEvent.class) != null;
    }

    private boolean shouldCountEvent(final Description description, final RemoteTenantAwareEvent event) {
        final ExpectEvent annotation = description.getAnnotation(ExpectEvent.class);
        return event.getClass().isAssignableFrom(annotation.type());
    }
}
