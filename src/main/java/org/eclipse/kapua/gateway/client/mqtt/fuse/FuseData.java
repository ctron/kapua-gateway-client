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

import java.nio.ByteBuffer;
import java.util.Objects;

import org.eclipse.kapua.gateway.client.BinaryPayloadCodec;
import org.eclipse.kapua.gateway.client.Data;
import org.eclipse.kapua.gateway.client.ErrorHandler;
import org.eclipse.kapua.gateway.client.MessageHandler;
import org.eclipse.kapua.gateway.client.Payload;
import org.eclipse.kapua.gateway.client.Topic;
import org.eclipse.kapua.gateway.client.mqtt.MqttNamespace;
import org.fusesource.hawtbuf.Buffer;
import org.fusesource.mqtt.client.Future;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class FuseData implements Data {

    private static final Logger logger = LoggerFactory.getLogger(FuseData.class);

    private final FuseClient client;

    private final MqttNamespace namespace;

    private final BinaryPayloadCodec codec;

    private final String topic;

    public FuseData(final FuseClient client, final MqttNamespace namespace, final BinaryPayloadCodec codec, final String clientId, final String applicationId, final Topic topic) {
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

        Future<?> future = this.client.subscribe(this.topic, new FuseMessageHandler() {

            @Override
            public void handleMessage(final String topic, final Buffer payload) throws Exception {
                logger.debug("Received message for: {}", topic);
                try {
                    FuseData.this.handleMessage(handler, payload);
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
        future.await();
    }

    protected void handleMessage(final MessageHandler handler, final Buffer buffer) throws Exception {
        final Payload payload = this.codec.decode(buffer.toByteBuffer());
        logger.debug("Received: {}", payload);
        handler.handleMessage(payload);
    }

}