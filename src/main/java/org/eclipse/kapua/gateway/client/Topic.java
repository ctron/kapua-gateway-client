/*******************************************************************************
 * Copyright (c) 2017 Red Hat Inc and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat Inc - initial API and implementation
 *******************************************************************************/
package org.eclipse.kapua.gateway.client;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

public final class Topic {

    private final List<String> segments;

    private Topic(final List<String> segments) {
        this.segments = Collections.unmodifiableList(segments);
    }

    public List<String> getSegments() {
        return this.segments;
    }

    public Stream<String> stream() {
        return this.segments.stream();
    }

    public static Topic split(final String path) {
        if (path == null) {
            return null;
        }

        return new Topic(Arrays.asList(path.split("\\/+")));
    }

    public static Topic of(final String first, final String... strings) {
        if (first == null) {
            return null;
        }

        if (strings == null || strings.length <= 0) {
            return new Topic(Collections.singletonList(first));
        }

        final List<String> segments = new ArrayList<>(1 + strings.length);
        segments.add(first);
        segments.addAll(Arrays.asList(strings));
        return new Topic(segments);
    }

}
