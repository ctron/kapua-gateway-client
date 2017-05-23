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
package de.dentrassi.kapua.smarthome.internal;

import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(name = "Eclipse Kapua Client Configuration", description = "Configuration for the Eclipe Kapua client, pointing towards the Kapua installation.")
public @interface Configuration {

    public String accountName() default "kapua-sys";

    public String clientId();

    public String brokerUrl();

    public String user();

    public String password();
}
