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

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executor;

import org.eclipse.kapua.gateway.client.Application;
import org.eclipse.kapua.gateway.client.ErrorHandler;
import org.eclipse.kapua.gateway.client.MessageHandler;
import org.eclipse.kapua.gateway.client.Payload;
import org.eclipse.kapua.gateway.client.Topic;
import org.eclipse.kapua.gateway.client.Transport;
import org.eclipse.kapua.gateway.client.utils.TransportAsync;

public abstract class AbstractApplication implements Application {

    private final AbstractClient client;
    protected final Set<Topic> subscriptions = new HashSet<>();
    protected final String applicationId;
    protected final TransportAsync transport;
    private boolean closed;

    public AbstractApplication(final AbstractClient client, final String applicationId, final Executor executor) {
        this.client = client;
        this.applicationId = applicationId;
        this.transport = new TransportAsync(executor);
    }

    protected synchronized void handleConnected() {
        if (closed) {
            return;
        }
        this.transport.handleConnected();
    }

    protected synchronized void handleDisconnected() {
        if (closed) {
            return;
        }
        this.transport.handleDisconnected();
    }

    protected void checkClosed() {
        if (closed) {
            throw new IllegalStateException("Application is closed");
        }
    }

    @Override
    public synchronized Transport transport() {
        checkClosed();
        return this.transport;
    }

    @Override
    public abstract AbstractData data(Topic topic);

    @Override
    public void close() throws Exception {
        synchronized (this) {
            if (closed) {
                return;
            }
            closed = true;
        }

        client.internalCloseApplication(applicationId, subscriptions, this);
    }

    protected abstract void publish(Topic topic, Payload payload) throws Exception;

    public CompletionStage<?> subscribe(Topic topic, MessageHandler handler, ErrorHandler<? extends Throwable> errorHandler) throws Exception {
        recordSubscription(topic);
        return internalSubscribe(topic, handler, errorHandler);
    }

    private void recordSubscription(final Topic topic) {
        subscriptions.add(topic);
    }

    protected abstract CompletionStage<?> internalSubscribe(Topic topic, MessageHandler handler, ErrorHandler<? extends Throwable> errorHandler) throws Exception;
}