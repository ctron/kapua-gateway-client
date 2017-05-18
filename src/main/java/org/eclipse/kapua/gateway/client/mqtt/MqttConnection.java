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
package org.eclipse.kapua.gateway.client.mqtt;

import java.nio.ByteBuffer;
import java.util.concurrent.Future;

public interface MqttConnection {

    public void publish(String topic, ByteBuffer buffer) throws Exception;

    public Future<?> subscribe(String topic, MqttMessageHandler messageHandler) throws Exception;

}
