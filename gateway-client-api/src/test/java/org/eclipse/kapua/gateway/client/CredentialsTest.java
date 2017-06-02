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

import org.eclipse.kapua.gateway.client.Credentials.UserAndPassword;
import org.junit.Assert;
import org.junit.Test;

public class CredentialsTest {

    @Test
    public void testNull1() {
        final UserAndPassword c = Credentials.userAndPassword(null, (char[]) null);
        Assert.assertNull(c.getUsername());
        Assert.assertNull(c.getPassword());
        Assert.assertNull(c.getPasswordAsString());
    }

    @Test
    public void testNull2() {
        final UserAndPassword c = Credentials.userAndPassword(null, (String) null);
        Assert.assertNull(c.getUsername());
        Assert.assertNull(c.getPassword());
        Assert.assertNull(c.getPasswordAsString());
    }
}
