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

import java.util.concurrent.Semaphore;
import java.util.function.Consumer;

public interface Transport {

    public interface TransportEvents {

        public void connected(Runnable runnable);

        public void disconnected(Runnable runnable);
    }

    public void state(Consumer<Boolean> stateChange);

    public default void events(final Consumer<TransportEvents> events) {
        class TransportEventsImpl implements TransportEvents {

            private Runnable connected;
            private Runnable disconnected;

            @Override
            public void connected(final Runnable runnable) {
                this.connected = runnable;
            }

            @Override
            public void disconnected(final Runnable runnable) {
                this.disconnected = runnable;
            }

        }

        final TransportEventsImpl impl = new TransportEventsImpl();

        events.accept(impl);

        state(state -> {
            if (state) {
                impl.connected.run();
            } else {
                impl.disconnected.run();
            }
        });
    }

    public static void waitForConnection(final Transport transport) throws InterruptedException {

        final Semaphore sem = new Semaphore(0);

        transport.state(state -> {
            if (state) {
                sem.release();
            }
        });

        sem.acquire();
    }
}
