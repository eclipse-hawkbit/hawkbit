package org.eclipse.hawkbit.ui.push;

import java.lang.reflect.Method;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.event.ApplicationListenerMethodAdapter;
import org.springframework.context.event.EventListener;

import com.google.common.eventbus.Subscribe;

/**
 * An {@link BeanPostProcessor} implementation which registers the event
 * listener for the push strategy
 * 
 *
 */

public class DelayedEventBusPushStrategyProcessor implements BeanPostProcessor {

    private static final Logger LOGGER = LoggerFactory.getLogger(DelayedEventBusPushStrategyProcessor.class);

    @Autowired
    private ConfigurableApplicationContext applicationContext;

    @Override
    public Object postProcessBeforeInitialization(final Object bean, final String beanName) {
        return bean;
    }

    @Override
    public Object postProcessAfterInitialization(final Object bean, final String beanName) {
        final Class<? extends Object> beanClass = bean.getClass();

        if (!beanClass.isAssignableFrom(DelayedEventBusPushStrategy.class)) {
            return bean;
        }

        LOGGER.trace("Found bean {} with {} annotation ", bean.getClass().getName(),
                DelayedEventBusPushStrategy.class.getSimpleName());
        final Method[] declaredMethods = beanClass.getDeclaredMethods();
        for (final Method method : declaredMethods) {
            final EventListener eventlistener = method.getAnnotation(EventListener.class);
            if (eventlistener != null) {
                LOGGER.trace("Found method {} for bean {} with {} annotation", method.getName(),
                        bean.getClass().getName(), Subscribe.class.getSimpleName());
                applicationContext.addApplicationListener(
                        new ApplicationListenerMethodAdapter(beanName, DelayedEventBusPushStrategy.class, method));
            }
        }
        return bean;
    }

}
