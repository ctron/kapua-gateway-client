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
package org.eclipse.kapua.gateway.client.mqtt.fuse;

import static java.util.Objects.requireNonNull;

import java.net.URI;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import org.eclipse.kapua.gateway.client.Application;
import org.eclipse.kapua.gateway.client.BinaryPayloadCodec;
import org.eclipse.kapua.gateway.client.Credentials.UserAndPassword;
import org.eclipse.kapua.gateway.client.Data;
import org.eclipse.kapua.gateway.client.Module;
import org.eclipse.kapua.gateway.client.Topic;
import org.eclipse.kapua.gateway.client.Transport;
import org.eclipse.kapua.gateway.client.mqtt.MqttClient;
import org.eclipse.kapua.gateway.client.mqtt.MqttNamespace;
import org.eclipse.kapua.gateway.client.utils.TransportAsync;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.fusesource.hawtbuf.Buffer;
import org.fusesource.hawtbuf.UTF8Buffer;
import org.fusesource.mqtt.client.Callback;
import org.fusesource.mqtt.client.CallbackConnection;
import org.fusesource.mqtt.client.ExtendedListener;
import org.fusesource.mqtt.client.Future;
import org.fusesource.mqtt.client.MQTT;
import org.fusesource.mqtt.client.Promise;
import org.fusesource.mqtt.client.QoS;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FuseClient extends MqttClient {

    private static final Logger logger = LoggerFactory.getLogger(FuseClient.class);

    private final class FuseApplication implements Application {

        private final String applicationId;

        private final TransportAsync transport = new TransportAsync(FuseClient.this.executor);

        private FuseApplication(final String applicationId) {
            this.applicationId = applicationId;
        }

        @Override
        public void close() throws Exception {
            internalCloseApplication(this.applicationId, this);
        }

        @Override
        public Data data(final Topic topic) {
            return new FuseData(FuseClient.this, FuseClient.this.namespace, FuseClient.this.codec, FuseClient.this.clientId, this.applicationId, topic);
        }

        @Override
        public Transport transport() {
            return this.transport;
        }
    }

    public static class Builder extends MqttClient.Builder<Builder> {

        private String broker;

        @Override
        protected Builder builder() {
            return this;
        }

        public Builder broker(final String broker) {
            this.broker = broker;
            return this;
        }

        public String broker() {
            return this.broker;
        }

        @Override
        public FuseClient build() throws Exception {

            final String broker = nonEmptyText(broker(), "broker");
            final String clientId = nonEmptyText(clientId(), "clientId");

            final MqttNamespace namespace = requireNonNull(namespace(), "Namespace must be set");
            final BinaryPayloadCodec codec = requireNonNull(codec(), "Codec must be set");

            final MQTT mqtt = new MQTT();
            mqtt.setCleanSession(false);
            mqtt.setHost(URI.create(broker));
            mqtt.setClientId(clientId);

            final Object credentials = credentials();
            if (credentials == null) {
                // none
            } else if (credentials instanceof UserAndPassword) {
                final UserAndPassword userAndPassword = (UserAndPassword) credentials;
                mqtt.setUserName(userAndPassword.getUsername());
                mqtt.setPassword(userAndPassword.getPasswordAsString());
            } else {
                throw new IllegalStateException(String.format("Unknown credentials type: %s", credentials.getClass().getName()));
            }

            CallbackConnection connection = mqtt.callbackConnection();
            ScheduledExecutorService executor = createExecutor(clientId);
            try {
                final FuseClient result = new FuseClient(modules(), clientId, executor, namespace, codec, connection);
                connection = null;
                executor = null;
                return result;
            } finally {
                if (executor != null) {
                    executor.shutdown();
                }
            }
        }
    }

    private static String nonEmptyText(final String string, final String fieldName) {
        if (string == null || string.isEmpty()) {
            throw new IllegalArgumentException(String.format("'%s' must not be null or empty", fieldName));
        }
        return string;
    }

    public static ScheduledExecutorService createExecutor(final String clientId) {
        return Executors.newSingleThreadScheduledExecutor(r -> new Thread(r, clientId));
    }

    private ExtendedListener listener = new ExtendedListener() {

        @Override
        public void onPublish(final UTF8Buffer topic, final Buffer body, final Runnable ack) {
            onPublish(topic, body, new Callback<Callback<Void>>() {

                @Override
                public void onSuccess(Callback<Void> value) {
                    ack.run();
                }

                @Override
                public void onFailure(Throwable value) {
                }

            });
        }

        @Override
        public void onFailure(Throwable value) {
        }

        @Override
        public void onDisconnected() {
            handleDisconnected();
        }

        @Override
        public void onConnected() {
            handleConnected();
        }

        @Override
        public void onPublish(final UTF8Buffer topic, final Buffer body, final Callback<Callback<Void>> ack) {
            handleMessageArrived(topic.toString(), body, ack);
        }
    };

    private final String clientId;
    private final MqttNamespace namespace;
    private final BinaryPayloadCodec codec;
    private final CallbackConnection connection;

    private final Map<String, FuseApplication> applications = new HashMap<>();

    private final Map<String, FuseMessageHandler> subscriptions = new HashMap<>();

    private FuseClient(final Set<Module> modules, final String clientId, final ScheduledExecutorService executor, final MqttNamespace namespace, final BinaryPayloadCodec codec,
            final CallbackConnection connection) {

        super(executor, clientId, modules);

        this.clientId = clientId;
        this.namespace = namespace;
        this.codec = codec;
        this.connection = connection;

        connection.listener(this.listener);
        connection.connect(new Promise<>());
    }

    @Override
    public void close() {
        connection.disconnect(null);
        executor.shutdown();
    }

    protected void handleConnected() {
        logger.debug("Connected");
        
        notifyConnected();
        synchronized (this) {
            this.applications.values().stream().forEach(app -> app.transport.handleConnected());
        }
    }

    protected void handleDisconnected() {
        logger.debug("Disconnected");
        
        notifyDisconnected();
        synchronized (this) {
            this.applications.values().stream().forEach(app -> app.transport.handleDisconnected());
        }
    }

    @Override
    public Application.Builder buildApplication(final String applicationId) {
        return new Application.Builder() {

            @Override
            public Application build() {
                return internalBuildApplication(applicationId);
            }
        };
    }

    protected Application internalBuildApplication(final String applicationId) {
        final FuseApplication result = new FuseApplication(applicationId);

        synchronized (this) {
            this.applications.put(applicationId, result);
            notifyAddApplication(applicationId);
        }

        return result;
    }

    protected void internalCloseApplication(final String applicationId, final Application application) {
        synchronized (this) {
            this.applications.remove(applicationId, application);
            notifyRemoveApplication(applicationId);
        }
    }

    void publish(final String topic, final ByteBuffer payload) throws MqttException {
        this.connection.publish(Buffer.utf8(topic), new Buffer(payload), QoS.AT_LEAST_ONCE, false, null);
    }

    @Override
    public void publishMqttPayload(final String topic, final ByteBuffer payload) throws Exception {
        publish(topic, payload);
    }

    Future<?> subscribe(final String topic, final FuseMessageHandler messageListener) throws MqttException {
        synchronized (this) {
            this.subscriptions.put(topic, messageListener);

            final Promise<byte[]> promise = new Promise<>();
            connection.subscribe(new org.fusesource.mqtt.client.Topic[] {
                    new org.fusesource.mqtt.client.Topic(topic, QoS.AT_LEAST_ONCE)
            }, promise);

            return promise;
        }
    }

    protected void handleMessageArrived(final String topic, final Buffer payload, final Callback<Callback<Void>> ack) {
        final FuseMessageHandler handler = this.subscriptions.get(topic);
        if (handler != null) {
            try {
                handler.handleMessage(topic, payload);
                ack.onSuccess(null);
            } catch (Exception e) {
                ack.onFailure(e);
            }
        }
    }

}
