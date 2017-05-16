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

import java.util.Optional;

@FunctionalInterface
public interface ErrorHandler<X extends Throwable> {

    public void handleError(Throwable e, Optional<Payload> message) throws X;

    public static ErrorHandler<RuntimeException> ignore() {
        return new ErrorHandler<RuntimeException>() {

            @Override
            public void handleError(final Throwable e, final Optional<Payload> payload) throws RuntimeException {
            }
        };
    }
}
