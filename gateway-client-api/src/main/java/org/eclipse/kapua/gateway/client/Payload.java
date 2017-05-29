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

import static java.time.Instant.now;
import static java.util.Collections.singletonMap;
import static java.util.Collections.unmodifiableMap;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Payload data
 */
public class Payload {

    public static class Builder {

        private Instant timestamp;

        private Map<String, Object> values = new HashMap<>();

        public Builder() {
            this.timestamp = Instant.now();
        }

        public Instant timestamp() {
            return this.timestamp;
        }

        public Builder timestamp(final Instant timestamp) {
            this.timestamp = timestamp;
            return this;
        }

        public Map<String, Object> values() {
            return this.values;
        }

        public Builder values(final Map<String, Object> values) {
            this.values = values;
            return this;
        }

        public Builder put(final String key, final Object value) {
            this.values.put(key, value);
            return this;
        }

        public Payload build() {
            return new Payload(this.timestamp, this.values, true);
        }
    }

    private final Instant timestamp;
    private final Map<String, Object> values;

    private Payload(final Instant timestamp, final Map<String, Object> values, final boolean cloneValues) {
        this.timestamp = timestamp;
        this.values = unmodifiableMap(cloneValues ? new HashMap<>(values) : values);
    }

    public Instant getTimestamp() {
        return this.timestamp;
    }

    public Map<String, Object> getValues() {
        return this.values;
    }

    @Override
    public String toString() {
        return String.format("[Payload - timestamp: %s, values: %s]", this.timestamp, this.values);
    }

    public static Payload of(final String key, final Object value) {
        return new Payload(now(), singletonMap(key, value), false);
    }

    public static Payload of(final Map<String, Object> values) {
        return new Payload(now(), values, true);
    }

    public static Payload of(final Instant timestamp, final String key, final Object value) {
        return new Payload(timestamp, singletonMap(key, value), false);
    }

    public static Payload of(final Instant timestamp, final Map<String, Object> values) {
        return new Payload(timestamp, values, true);
    }
}
