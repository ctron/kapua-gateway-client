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

import java.nio.ByteBuffer;
import java.util.Objects;

import org.eclipse.kapua.gateway.client.BinaryPayloadCodec;
import org.eclipse.kapua.gateway.client.Data;
import org.eclipse.kapua.gateway.client.ErrorHandler;
import org.eclipse.kapua.gateway.client.MessageHandler;
import org.eclipse.kapua.gateway.client.Payload;
import org.eclipse.kapua.gateway.client.Topic;
import org.eclipse.kapua.gateway.client.mqtt.MqttNamespace;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class PahoData implements Data {

    private static final Logger logger = LoggerFactory.getLogger(PahoData.class);

    private final PahoClient client;

    private final MqttNamespace namespace;

    private final BinaryPayloadCodec codec;

    private final String topic;

    public PahoData(final PahoClient client, final MqttNamespace namespace, final BinaryPayloadCodec codec, final String clientId, final String applicationId, final Topic topic) {
        this.client = client;
        this.namespace = namespace;
        this.codec = codec;
        this.topic = this.namespace.dataTopic(clientId, applicationId, topic);
    }

    @Override
    public void send(final Payload payload) throws Exception {
        logger.debug("Publishing values - {} -> {}", this.topic, payload.getValues());

        final ByteBuffer buffer = this.codec.encode(payload, null);
        buffer.flip();

        this.client.publish(this.topic, buffer);
    }

    @Override
    public void subscribe(final MessageHandler handler, final ErrorHandler<?> errorHandler) throws Exception {
        Objects.requireNonNull(handler);

        logger.debug("Setting subscription for: {}", this.topic);

        final IMqttToken token = this.client.subscribe(this.topic, new PahoMessageHandler() {

            @Override
            public void handleMessage(final String topic, final MqttMessage message) throws Exception {
                logger.debug("Received message for: {}", topic);
                try {
                    PahoData.this.handleMessage(handler, message);
                } catch (final Exception e) {
                    try {
                        errorHandler.handleError(e, null);
                    } catch (final Exception e1) {
                        throw e1;
                    } catch (final Throwable e1) {
                        throw new Exception(e1);
                    }
                }
            }
        });
        token.waitForCompletion();
    }

    protected void handleMessage(final MessageHandler handler, final MqttMessage message) throws Exception {
        final Payload payload = this.codec.decode(ByteBuffer.wrap(message.getPayload()));
        logger.debug("Received: {}", payload);
        handler.handleMessage(payload);
    }

}