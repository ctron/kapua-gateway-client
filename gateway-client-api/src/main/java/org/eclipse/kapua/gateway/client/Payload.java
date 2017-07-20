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
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Payload data
 */
public class Payload {

    public static class Builder {

       // private Instant timestamp;
    	Date timestamp;

        private Map<String, Object> values = new HashMap<String, Object>();

        public Builder() {
            this.timestamp = new Date();
        }

        public Date timestamp() {
            return this.timestamp;
        }

        public Builder timestamp(final Date timestamp) {
            Objects.requireNonNull(timestamp);

            this.timestamp = timestamp;
            return this;
        }

        public Map<String, ?> values() {
            return this.values;
        }

        public Builder values(final Map<String, ?> values) {
            Objects.requireNonNull(values);

            this.values.clear();
            this.values.putAll(values);

            return this;
        }

        public Builder put(final String key, final Object value) {
            Objects.requireNonNull(key);

            this.values.put(key, value);
            return this;
        }

        public Payload build() {
            return new Payload(this.timestamp, this.values, true);
        }
    }

    private final Date timestamp;
    private final Map<String, ?> values;

    private Payload(final Date timestamp, final Map<String, ?> values, final boolean cloneValues) {
        this.timestamp = timestamp;
        this.values = unmodifiableMap(cloneValues ? new HashMap<>(values) : values);
    }

    public Date getTimestamp() {
        return this.timestamp;
    }

    public Map<String, ?> getValues() {
        return this.values;
    }

    @Override
    public String toString() {
        return String.format("[Payload - timestamp: %s, values: %s]", this.timestamp, this.values);
    }

    public static Payload of(final String key, final Object value) {
        Objects.requireNonNull(key);

        return new Payload(new Date(), singletonMap(key, value), false);
    }

    public static Payload of(final Map<String, ?> values) {
        Objects.requireNonNull(values);

        return new Payload(new Date(), values, true);
    }

    public static Payload of(final Date timestamp, final String key, final Object value) {
        Objects.requireNonNull(timestamp);
        Objects.requireNonNull(key);

        return new Payload(timestamp, singletonMap(key, value), false);
    }

    public static Payload of(final Date timestamp, final Map<String, ?> values) {
        Objects.requireNonNull(timestamp);
        Objects.requireNonNull(values);

        return new Payload(timestamp, values, true);
    }
}
