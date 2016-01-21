/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.rest.resource.model.action;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

import org.springframework.hateoas.ResourceSupport;

/**
 * A list representation of the ActionStatus because Spring MVC cannot handle
 * plain lists interfaces as request body.
 *
 *
 *
 *
 */
public class ActionStatussRest extends ResourceSupport implements List<ActionStatusRest> {

    private final List<ActionStatusRest> delegate = new ArrayList<>();

    /**
     * @return
     * @see java.util.List#size()
     */
    @Override
    public int size() {
        return delegate.size();
    }

    /**
     * @return
     * @see java.util.List#isEmpty()
     */
    @Override
    public boolean isEmpty() {
        return delegate.isEmpty();
    }

    /**
     * @param o
     * @return
     * @see java.util.List#contains(java.lang.Object)
     */
    @Override
    public boolean contains(final Object o) {
        return delegate.contains(o);
    }

    /**
     * @return
     * @see java.util.List#iterator()
     */
    @Override
    public Iterator<ActionStatusRest> iterator() {
        return delegate.iterator();
    }

    /**
     * @return
     * @see java.util.List#toArray()
     */
    @Override
    public Object[] toArray() {
        return delegate.toArray();
    }

    /**
     * @param a
     * @return
     * @see java.util.List#toArray(java.lang.Object[])
     */
    @Override
    public <T> T[] toArray(final T[] a) {
        return delegate.toArray(a);
    }

    /**
     * @param e
     * @return
     * @see java.util.List#add(java.lang.Object)
     */
    @Override
    public boolean add(final ActionStatusRest e) {
        return delegate.add(e);
    }

    /**
     * @param o
     * @return
     * @see java.util.List#remove(java.lang.Object)
     */
    @Override
    public boolean remove(final Object o) {
        return delegate.remove(o);
    }

    /**
     * @param c
     * @return
     * @see java.util.List#containsAll(java.util.Collection)
     */
    @Override
    public boolean containsAll(final Collection<?> c) {
        return delegate.containsAll(c);
    }

    /**
     * @param c
     * @return
     * @see java.util.List#addAll(java.util.Collection)
     */
    @Override
    public boolean addAll(final Collection<? extends ActionStatusRest> c) {
        return delegate.addAll(c);
    }

    /**
     * @param index
     * @param c
     * @return
     * @see java.util.List#addAll(int, java.util.Collection)
     */
    @Override
    public boolean addAll(final int index, final Collection<? extends ActionStatusRest> c) {
        return delegate.addAll(index, c);
    }

    /**
     * @param c
     * @return
     * @see java.util.List#removeAll(java.util.Collection)
     */
    @Override
    public boolean removeAll(final Collection<?> c) {
        return delegate.removeAll(c);
    }

    /**
     * @param c
     * @return
     * @see java.util.List#retainAll(java.util.Collection)
     */
    @Override
    public boolean retainAll(final Collection<?> c) {
        return delegate.retainAll(c);
    }

    /**
     *
     * @see java.util.List#clear()
     */
    @Override
    public void clear() {
        delegate.clear();
    }

    /**
     * @param o
     * @return
     * @see java.util.List#equals(java.lang.Object)
     */
    @Override
    public boolean equals(final Object o) {
        return delegate.equals(o);
    }

    /**
     * @return
     * @see java.util.List#hashCode()
     */
    @Override
    public int hashCode() {
        return delegate.hashCode();
    }

    /**
     * @param index
     * @return
     * @see java.util.List#get(int)
     */
    @Override
    public ActionStatusRest get(final int index) {
        return delegate.get(index);
    }

    /**
     * @param index
     * @param element
     * @return
     * @see java.util.List#set(int, java.lang.Object)
     */
    @Override
    public ActionStatusRest set(final int index, final ActionStatusRest element) {
        return delegate.set(index, element);
    }

    /**
     * @param index
     * @param element
     * @see java.util.List#add(int, java.lang.Object)
     */
    @Override
    public void add(final int index, final ActionStatusRest element) {
        delegate.add(index, element);
    }

    /**
     * @param index
     * @return
     * @see java.util.List#remove(int)
     */
    @Override
    public ActionStatusRest remove(final int index) {
        return delegate.remove(index);
    }

    /**
     * @param o
     * @return
     * @see java.util.List#indexOf(java.lang.Object)
     */
    @Override
    public int indexOf(final Object o) {
        return delegate.indexOf(o);
    }

    /**
     * @param o
     * @return
     * @see java.util.List#lastIndexOf(java.lang.Object)
     */
    @Override
    public int lastIndexOf(final Object o) {
        return delegate.lastIndexOf(o);
    }

    /**
     * @return
     * @see java.util.List#listIterator()
     */
    @Override
    public ListIterator<ActionStatusRest> listIterator() {
        return delegate.listIterator();
    }

    /**
     * @param index
     * @return
     * @see java.util.List#listIterator(int)
     */
    @Override
    public ListIterator<ActionStatusRest> listIterator(final int index) {
        return delegate.listIterator(index);
    }

    /**
     * @param fromIndex
     * @param toIndex
     * @return
     * @see java.util.List#subList(int, int)
     */
    @Override
    public List<ActionStatusRest> subList(final int fromIndex, final int toIndex) {
        return delegate.subList(fromIndex, toIndex);
    }

}
