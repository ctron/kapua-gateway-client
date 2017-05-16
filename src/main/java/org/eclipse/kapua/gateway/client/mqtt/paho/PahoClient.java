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
package org.eclipse.kapua.gateway.client.mqtt.paho;

import static java.util.Objects.requireNonNull;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import org.eclipse.kapua.gateway.client.Application;
import org.eclipse.kapua.gateway.client.BinaryPayloadCodec;
import org.eclipse.kapua.gateway.client.Data;
import org.eclipse.kapua.gateway.client.Topic;
import org.eclipse.kapua.gateway.client.Transport;
import org.eclipse.kapua.gateway.client.Credentials.UserAndPassword;
import org.eclipse.kapua.gateway.client.internal.Buffers;
import org.eclipse.kapua.gateway.client.internal.Module;
import org.eclipse.kapua.gateway.client.internal.TransportAsync;
import org.eclipse.kapua.gateway.client.mqtt.MqttClient;
import org.eclipse.kapua.gateway.client.mqtt.MqttNamespace;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttMessageListener;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttAsyncClient;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClientPersistence;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PahoClient extends MqttClient {

    private static final Logger logger = LoggerFactory.getLogger(PahoClient.class);

    private final class PahoApplication implements Application {

        private final String applicationId;

        private final TransportAsync transport = new TransportAsync(PahoClient.this.executor);

        private PahoApplication(final String applicationId) {
            this.applicationId = applicationId;
        }

        @Override
        public void close() throws Exception {
            internalCloseApplication(this.applicationId, this);
        }

        @Override
        public Data data(final Topic topic) {
            return new PahoData(PahoClient.this, PahoClient.this.namespace, PahoClient.this.codec, PahoClient.this.clientId, this.applicationId, topic);
        }

        @Override
        public Transport transport() {
            return this.transport;
        }
    }

    public static class Builder extends MqttClient.Builder<Builder> {

        private Supplier<MqttClientPersistence> persistenceProvider = MemoryPersistence::new;
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

        public Builder persistentProvider(final Supplier<MqttClientPersistence> provider) {
            if (provider != null) {
                this.persistenceProvider = provider;
            } else {
                this.persistenceProvider = MemoryPersistence::new;
            }
            return builder();
        }

        public Supplier<MqttClientPersistence> persistentProvider() {
            return this.persistenceProvider;
        }

        @Override
        public PahoClient build() throws Exception {

            final String broker = nonEmptyText(broker(), "broker");
            final String clientId = nonEmptyText(clientId(), "clientId");

            final MqttClientPersistence persistence = requireNonNull(this.persistenceProvider.get(), "Persistence provider returned 'null' persistence");
            final MqttNamespace namespace = requireNonNull(namespace(), "Namespace must be set");
            final BinaryPayloadCodec codec = requireNonNull(codec(), "Codec must be set");

            MqttAsyncClient client = new MqttAsyncClient(broker, clientId, persistence);
            ScheduledExecutorService executor = createExecutor(clientId);
            try {
                final PahoClient result = new PahoClient(modules(), clientId, executor, namespace, codec, client, persistence, createConnectOptions(this));
                client = null;
                executor = null;
                return result;
            } finally {
                if (executor != null) {
                    executor.shutdown();
                }
                if (client != null) {
                    try {
                        client.disconnectForcibly(0);
                    } finally {
                        client.close();
                    }
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
        return Executors.newSingleThreadScheduledExecutor(new ThreadFactory() {

            @Override
            public Thread newThread(final Runnable r) {
                return new Thread(r, clientId);
            }
        });
    }

    private static MqttConnectOptions createConnectOptions(final Builder builder) {
        final MqttConnectOptions result = new MqttConnectOptions();

        // we need to handle re-connect ourselves in order to get proper events
        result.setAutomaticReconnect(false);

        final Object credentials = builder.credentials();
        if (credentials instanceof UserAndPassword) {
            final UserAndPassword userAndPassword = (UserAndPassword) credentials;
            result.setUserName(userAndPassword.getUsername());
            result.setPassword(userAndPassword.getPassword());
        } else if (credentials == null) {
            // ignore
        } else {
            throw new IllegalArgumentException(String.format("Unsupported credentials type: %s", credentials.getClass().getName()));
        }

        return result;
    }

    private final String clientId;
    private final MqttNamespace namespace;
    private final BinaryPayloadCodec codec;
    private final MqttConnectOptions connectOptions;
    private MqttAsyncClient client;

    private final Map<String, PahoApplication> applications = new HashMap<>();
    private final Map<String, IMqttMessageListener> subscriptions = new HashMap<>();

    private PahoClient(final Set<Module> modules, final String clientId, final ScheduledExecutorService executor, final MqttNamespace namespace, final BinaryPayloadCodec codec,
            final MqttAsyncClient client,
            final MqttClientPersistence persistence,
            final MqttConnectOptions connectOptions) {
        super(executor, clientId, modules);

        this.clientId = clientId;
        this.namespace = namespace;
        this.codec = codec;
        this.connectOptions = connectOptions;
        this.client = client;

        this.client.setCallback(new MqttCallback() {

            @Override
            public void messageArrived(final String topic, final MqttMessage message) throws Exception {
            }

            @Override
            public void deliveryComplete(final IMqttDeliveryToken token) {
            }

            @Override
            public void connectionLost(final Throwable cause) {
                handleDisconnected();
            }
        });

        this.executor.execute(this::connect);
    }

    protected void connect() {
        try {
            this.client.connect(this.connectOptions, null, new IMqttActionListener() {

                @Override
                public void onSuccess(final IMqttToken asyncActionToken) {
                    handleConnected();
                }

                @Override
                public void onFailure(final IMqttToken asyncActionToken, final Throwable exception) {
                    handleDisconnected();
                }
            });
        } catch (final MqttException e) {
            logger.warn("Failed to call connect", e);
        }
    }

    @Override
    public void close() {

        final MqttAsyncClient client;

        synchronized (this) {
            client = this.client;
            if (client == null) {
                return;
            }
            this.client = null;
        }

        try {
            // disconnect first

            try {
                client.disconnect().waitForCompletion();
            } catch (final MqttException e) {
            }

            // now try to close (and free the resources)

            try {
                client.close();
            } catch (final MqttException e) {
            }
        } finally {
            this.executor.shutdown();
        }
    }

    protected void handleConnected() {
        notifyConnected();
        synchronized (this) {
            handleResubscribe();
            this.applications.values().stream().forEach(app -> app.transport.handleConnected());
        }
    }

    private void handleResubscribe() {
        for (final Map.Entry<String, IMqttMessageListener> entry : this.subscriptions.entrySet()) {
            try {
                this.client.subscribe(entry.getKey(), 1, null, null, entry.getValue());
            } catch (final MqttException e) {
                logger.warn("Failed to re-subscribe to '{}'", entry.getKey());
            }
        }
    }

    protected void handleDisconnected() {
        try {
            notifyDisconnected();
            synchronized (this) {
                this.applications.values().stream().forEach(app -> app.transport.handleDisconnected());
            }
        } finally {
            this.executor.schedule(this::connect, 1, TimeUnit.SECONDS);
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
        final PahoApplication result = new PahoApplication(applicationId);

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
        // FIXME: try to optimize, remove buffer copy

        this.client.publish(topic, Buffers.toByteArray(payload), 1, false);
    }

    @Override
    public void publishMqttPayload(final String topic, final ByteBuffer payload) throws Exception {
        publish(topic, payload);
    }

    IMqttToken subscribe(final String topic, final IMqttMessageListener messageListener) throws MqttException {
        synchronized (this) {
            this.subscriptions.put(topic, messageListener);
            return this.client.subscribe(topic, 1, null, null, messageListener);
        }
    }

}
