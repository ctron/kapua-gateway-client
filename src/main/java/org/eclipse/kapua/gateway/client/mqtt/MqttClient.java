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
package org.eclipse.kapua.gateway.client.mqtt;

import java.nio.ByteBuffer;
import java.util.Set;
import java.util.concurrent.ScheduledExecutorService;

import org.eclipse.kapua.gateway.client.AbstractClient;
import org.eclipse.kapua.gateway.client.BinaryPayloadCodec;
import org.eclipse.kapua.gateway.client.Module;
import org.eclipse.kapua.gateway.client.Credentials.UserAndPassword;

public abstract class MqttClient extends AbstractClient {

    public abstract static class Builder<T extends Builder<T>> extends AbstractClient.Builder<T> {

        private MqttNamespace namespace;
        private BinaryPayloadCodec codec;
        private UserAndPassword userAndPassword;
        private String clientId;

        public T codec(final BinaryPayloadCodec codec) {
            this.codec = codec;
            return builder();
        }

        public BinaryPayloadCodec codec() {
            return this.codec;
        }

        public T namespace(final MqttNamespace namespace) {
            this.namespace = namespace;
            return builder();
        }

        public MqttNamespace namespace() {
            return this.namespace;
        }

        public T clientId(final String clientId) {
            this.clientId = clientId;
            return builder();
        }

        public String clientId() {
            return this.clientId;
        }

        public T credentials(final UserAndPassword userAndPassword) {
            this.userAndPassword = userAndPassword;
            return builder();
        }

        public Object credentials() {
            return this.userAndPassword;
        }
    }

    private final String clientId;

    public MqttClient(final ScheduledExecutorService executor, final String clientId, final Set<Module> modules) {
        super(executor, modules);
        this.clientId = clientId;
    }

    public abstract void publishMqttPayload(String topic, ByteBuffer payload) throws Exception;

    public String getMqttClientId() {
        return this.clientId;
    }

}
