/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.eventbus;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * Marks an class as an event subscriber to listen on event on the event bus
 * without explicit register this class to the event bus.
 *
 * <pre>
 * &#064;EventSubscriber
 * public class MySubscriber {
 *     &#064;Subscribe
 *     public void listen(MyEvent event) {
 *         System.out.println(&quot;event received: &quot; + event);
 *     }
 * }
 * </pre>
 * 
 *
 *
 *
 */
@Target({ TYPE })
@Retention(RUNTIME)
public @interface EventSubscriber {

}
