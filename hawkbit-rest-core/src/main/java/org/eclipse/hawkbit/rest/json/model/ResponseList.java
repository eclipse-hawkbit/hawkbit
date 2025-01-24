/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.rest.json.model;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

import org.springframework.hateoas.RepresentationModel;

/**
 * List that extends RepresentationModel to ensure that links in content are in HAL format.
 *
 * @param <T> of the response content
 */
public class ResponseList<T> extends RepresentationModel<ResponseList<T>> implements List<T> {

    private final List<T> content;

    /**
     * @param content to delegate
     */
    public ResponseList(final List<T> content) {
        this.content = content;
    }

    @Override
    public int size() {
        return content.size();
    }

    @Override
    public boolean isEmpty() {
        return content.isEmpty();
    }

    @Override
    public boolean contains(final Object o) {
        return content.contains(o);
    }

    @Override
    public Iterator<T> iterator() {
        return content.iterator();
    }

    @Override
    public Object[] toArray() {
        return content.toArray();
    }

    @Override
    public <C> C[] toArray(final C[] a) {
        return content.toArray(a);
    }

    @Override
    public boolean add(final T e) {
        return content.add(e);
    }

    @Override
    public boolean remove(final Object o) {
        return content.remove(o);
    }

    @Override
    public boolean containsAll(final Collection<?> c) {
        return new HashSet<>(content).containsAll(c);
    }

    @Override
    public boolean addAll(final Collection<? extends T> c) {
        return content.addAll(c);
    }

    @Override
    public boolean addAll(final int index, final Collection<? extends T> c) {
        return content.addAll(index, c);
    }

    @Override
    public boolean removeAll(final Collection<?> c) {
        return content.removeAll(c);
    }

    @Override
    public boolean retainAll(final Collection<?> c) {
        return content.retainAll(c);
    }

    @Override
    public void clear() {
        content.clear();
    }

    @Override
    public T get(final int index) {
        return content.get(index);
    }

    @Override
    public T set(final int index, final T element) {
        return content.set(index, element);
    }

    @Override
    public void add(final int index, final T element) {
        content.add(index, element);
    }

    @Override
    public T remove(final int index) {
        return content.remove(index);
    }

    @Override
    public int indexOf(final Object o) {
        return content.indexOf(o);
    }

    @Override
    public int lastIndexOf(final Object o) {
        return content.lastIndexOf(o);
    }

    @Override
    public ListIterator<T> listIterator() {
        return content.listIterator();
    }

    @Override
    public ListIterator<T> listIterator(final int index) {
        return content.listIterator(index);
    }

    @Override
    public List<T> subList(final int fromIndex, final int toIndex) {
        return content.subList(fromIndex, toIndex);
    }

    @Override
    public String toString() {
        return "ResponseList [content=" + content + "]";
    }

    @Override
    public boolean equals(final Object o) {
        return content.equals(o);
    }

    @Override
    public int hashCode() {
        return content.hashCode();
    }

}
