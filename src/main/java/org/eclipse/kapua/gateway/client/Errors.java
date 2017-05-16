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

public final class Errors {

    private static final ErrorHandler<RuntimeException> IGNORE = Errors::ignore;

    private Errors() {
    }

    public static ErrorHandler<RuntimeException> ignore() {
        return IGNORE;
    }

    public static void ignore(final Throwable e, final Optional<Payload> payload) {
    }
}
