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
package org.eclipse.kapua.gateway.client.profile;

import org.eclipse.kapua.gateway.client.Client;
import org.eclipse.kapua.gateway.client.Credentials.UserAndPassword;
import org.eclipse.kapua.gateway.client.kura.KuraBinaryPayloadCodec;
import org.eclipse.kapua.gateway.client.kura.KuraBirthCertificateModule;
import org.eclipse.kapua.gateway.client.kura.KuraNamespace;
import org.eclipse.kapua.gateway.client.mqtt.paho.PahoClient;

public class KuraPahoMqttProfile {

    public static KuraPahoMqttProfile newProfile() {
        return new KuraPahoMqttProfile();
    }

    private String accountName;
    private String brokerUrl;
    private String clientId;
    private UserAndPassword userAndPassword;

    private KuraPahoMqttProfile() {
    }

    public KuraPahoMqttProfile accountName(final String accountName) {
        this.accountName = accountName;
        return this;
    }

    public KuraPahoMqttProfile brokerUrl(final String brokerUrl) {
        this.brokerUrl = brokerUrl;
        return this;
    }

    public KuraPahoMqttProfile clientId(final String clientId) {
        this.clientId = clientId;
        return this;
    }

    public KuraPahoMqttProfile credentials(final UserAndPassword userAndPassword) {
        this.userAndPassword = userAndPassword;
        return this;
    }

    public Client build() throws Exception {
        validate();

        return new PahoClient.Builder()
                .clientId(this.clientId)
                .broker(this.brokerUrl)
                .credentials(this.userAndPassword)
                .codec(
                        new KuraBinaryPayloadCodec.Builder()
                                .build())
                .namespace(
                        new KuraNamespace.Builder()
                                .accountName(this.accountName)
                                .build())
                .module(
                        KuraBirthCertificateModule.newBuilder(this.accountName)
                                .defaultProviders()
                                .build())
                .build();
    }

    private void validate() {
        hasString(this.accountName, "accountName");
        hasString(this.brokerUrl, "brokerUrl");
        hasString(this.clientId, "clientId");
    }

    private static void hasString(final String value, final String name) {
        if (value == null || value.isEmpty()) {
            throw new IllegalStateException(String.format("'%s' must be set", value));
        }
    }
}
