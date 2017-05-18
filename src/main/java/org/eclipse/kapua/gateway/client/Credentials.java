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

public final class Credentials {

    private Credentials() {
    }

    public static class UserAndPassword {

        private final String username;
        private final char[] password;

        private UserAndPassword(final String username, final char[] password) {
            this.username = username;
            this.password = password;
        }

        public String getUsername() {
            return this.username;
        }

        public char[] getPassword() {
            return this.password;
        }
        
        public String getPasswordAsString() {
            return String.valueOf(password);
        }
    }

    public static UserAndPassword userAndPassword(final String username, final String password) {
        return new UserAndPassword(username, password.toCharArray());
    }

    public static UserAndPassword userAndPassword(final String username, final char[] password) {
        return new UserAndPassword(username, password);
    }
}
