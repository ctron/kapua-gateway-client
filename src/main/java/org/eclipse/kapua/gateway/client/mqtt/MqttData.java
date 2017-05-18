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
import java.util.Objects;
import java.util.concurrent.Future;

import org.eclipse.kapua.gateway.client.BinaryPayloadCodec;
import org.eclipse.kapua.gateway.client.Data;
import org.eclipse.kapua.gateway.client.ErrorHandler;
import org.eclipse.kapua.gateway.client.MessageHandler;
import org.eclipse.kapua.gateway.client.Payload;
import org.eclipse.kapua.gateway.client.Topic;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MqttData implements Data {

    private static final Logger logger = LoggerFactory.getLogger(MqttData.class);

    private final MqttConnection connection;
    private final BinaryPayloadCodec codec;
    private final String topic;

    public MqttData(final MqttConnection connection, final MqttNamespace namespace, final BinaryPayloadCodec codec, final String clientId, final String applicationId, final Topic topic) {
        this.connection = connection;
        this.codec = codec;
        this.topic = namespace.dataTopic(clientId, applicationId, topic);
    }
    
    @Override
    public void send(Payload payload) throws Exception {
        logger.debug("Publishing values - {} -> {}", this.topic, payload.getValues());

        final ByteBuffer buffer = this.codec.encode(payload, null);
        buffer.flip();

        this.connection.publish(this.topic, buffer);
    }

    @Override
    public void subscribe(MessageHandler handler, ErrorHandler<? extends Throwable> errorHandler) throws Exception {
        Objects.requireNonNull(handler);

        logger.debug("Setting subscription for: {}", this.topic);

        Future<?> future = this.connection.subscribe(this.topic, new MqttMessageHandler() {

            @Override
            public void handleMessage(final String topic, final ByteBuffer payload) throws Exception {
                logger.debug("Received message for: {}", topic);
                try {
                    MqttData.this.handleMessage(handler, payload);
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
        future.get();
    }

    protected void handleMessage(final MessageHandler handler, final ByteBuffer buffer) throws Exception {
        final Payload payload = this.codec.decode(buffer);
        logger.debug("Received: {}", payload);
        handler.handleMessage(payload);
    }
}
