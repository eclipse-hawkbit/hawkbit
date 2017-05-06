/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.management.event;

/**
 * Enum for events of type SaveAction which are thrown when pending actions are
 * performed
 */
public enum SaveActionWindowEvent {

    SAVED_ASSIGNMENTS, DISCARD_ALL_ASSIGNMENTS, DISCARD_ASSIGNMENTS, DISCARD_ALL_DISTRIBUTIONS, SHOW_HIDE_TAB, DISCARD_ALL_TARGETS, DISCARD_ASSIGNMENT, DISCARD_DELETE_TARGET, DISCARD_DELETE_DS

}
