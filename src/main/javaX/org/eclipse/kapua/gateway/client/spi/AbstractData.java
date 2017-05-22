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
package org.eclipse.kapua.gateway.client.spi;

import static java.util.Objects.requireNonNull;

import java.util.concurrent.CompletionStage;

import org.eclipse.kapua.gateway.client.Data;
import org.eclipse.kapua.gateway.client.ErrorHandler;
import org.eclipse.kapua.gateway.client.MessageHandler;
import org.eclipse.kapua.gateway.client.Payload;
import org.eclipse.kapua.gateway.client.Topic;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AbstractData implements Data {

    private static final Logger logger = LoggerFactory.getLogger(AbstractData.class);

    private AbstractApplication application;
    private Topic topic;

    public AbstractData(final AbstractApplication application, final Topic topic) {
        this.application = application;
        this.topic = topic;
    }

    @Override
    public void send(final Payload payload) throws Exception {
        this.application.publish(this.topic, payload);
    }

    @Override
    public void subscribe(final MessageHandler handler, final ErrorHandler<? extends Throwable> errorHandler) throws Exception {
        requireNonNull(handler);
        requireNonNull(errorHandler);

        logger.debug("Setting subscription for: {}", this.topic);

        final CompletionStage<?> future = application.subscribe(this.topic, handler, errorHandler );
        future.toCompletableFuture().get();
    }
}
