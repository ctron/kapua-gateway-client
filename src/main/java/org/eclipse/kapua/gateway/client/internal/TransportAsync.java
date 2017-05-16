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
package org.eclipse.kapua.gateway.client.internal;

import java.util.concurrent.Executor;
import java.util.function.Consumer;

import org.eclipse.kapua.gateway.client.Transport;

public class TransportAsync implements Transport {

    private final Executor executor;
    private Consumer<Boolean> listener;
    private boolean state;

    public TransportAsync(final Executor executor) {
        this.executor = executor;
    }

    private void fireEvent(final boolean state, final Consumer<Boolean> listener) {
        if (listener == null) {
            return;
        }
        this.executor.execute(() -> listener.accept(state));
    }

    public synchronized void handleConnected() {
        if (!this.state) {
            this.state = true;
            fireEvent(true, this.listener);
        }
    }

    public synchronized void handleDisconnected() {
        if (this.state) {
            this.state = false;
            fireEvent(false, this.listener);
        }
    }

    @Override
    public void state(final Consumer<Boolean> listener) {
        synchronized (this) {
            this.listener = listener;
            fireEvent(this.state, listener);
        }
    }
}
