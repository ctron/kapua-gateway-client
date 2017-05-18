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

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ScheduledExecutorService;
import java.util.function.Consumer;

import org.eclipse.kapua.gateway.client.utils.TransportAsync;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractClient implements Client {

    private static final Logger logger = LoggerFactory.getLogger(AbstractClient.class);

    public static abstract class Builder<T extends Builder<T>> implements Client.Builder {

        protected abstract T builder();

        private final Set<Module> modules = new HashSet<>();

        public T module(final Module module) {
            Objects.requireNonNull(module);

            this.modules.add(module);
            return builder();
        }

        public Set<Module> modules() {
            return this.modules;
        }
    }

    protected final ScheduledExecutorService executor;
    private final Set<Module> modules;

    private final TransportAsync transport;

    public AbstractClient(final ScheduledExecutorService executor, final Set<Module> modules) {
        this.executor = executor;
        this.modules = new HashSet<>(modules);

        this.transport = new TransportAsync(executor);

        fireModuleEvent(module -> module.initialize(new ModuleContext() {

            @Override
            public Client getClient() {
                return AbstractClient.this;
            }
        }));
    }

    @Override
    public Transport transport() {
        return this.transport;
    }

    private void fireModuleEvent(final Consumer<Module> consumer) {
        for (final Module module : this.modules) {
            try {
                consumer.accept(module);
            } catch (final Exception e) {
                logger.info("Failed to process module event", e);
            }
        }
    }

    protected void notifyAddApplication(final String applicationId) {
        fireModuleEvent(module -> module.applicationAdded(applicationId));
    }

    protected void notifyRemoveApplication(final String applicationId) {
        fireModuleEvent(module -> module.applicationRemoved(applicationId));
    }

    protected void notifyConnected() {
        fireModuleEvent(Module::connected);
        this.transport.handleConnected();
    }

    protected void notifyDisconnected() {
        fireModuleEvent(Module::disconnected);
        this.transport.handleDisconnected();
    }
}
