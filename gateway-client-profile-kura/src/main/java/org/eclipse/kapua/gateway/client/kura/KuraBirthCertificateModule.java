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
package org.eclipse.kapua.gateway.client.kura;

import java.nio.ByteBuffer;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;

import org.eclipse.kapua.gateway.client.Client;
import org.eclipse.kapua.gateway.client.Module;
import org.eclipse.kapua.gateway.client.ModuleContext;
import org.eclipse.kapua.gateway.client.kura.internal.Metrics;
import org.eclipse.kapua.gateway.client.kura.payload.KuraPayloadProto.KuraPayload;
import org.eclipse.kapua.gateway.client.mqtt.MqttClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class KuraBirthCertificateModule implements Module {

    private static final Logger logger = LoggerFactory.getLogger(KuraBirthCertificateModule.class);

    @FunctionalInterface
    public interface Provider {

        public void provideData(Map<String, String> values);

        public static final Provider JVM = new Provider() {

            @Override
            public void provideData(final Map<String, String> values) {
                values.put("jvm_name", System.getProperty("java.vm.name"));
                values.put("jvm_version", System.getProperty("java.version"));

                values.put("os", System.getProperty("os.name"));
                values.put("os_version", System.getProperty("os.version"));
                values.put("os_arch", System.getProperty("os.arch"));
            }

        };

        public static final Provider RUNTIME = new Provider() {

            @Override
            public void provideData(final Map<String, String> values) {
                values.put("available_processors", Integer.toString(Runtime.getRuntime().availableProcessors()));
                values.put("total_memory", Long.toString(Runtime.getRuntime().totalMemory()));
            }

        };
    }

    public static class Builder {

        private final String accountName;

        private final Set<Provider> providers = new HashSet<>();

        private Builder(final String accountName) {
            this.accountName = accountName;
        }

        public Builder defaultProviders() {
            this.providers.add(Provider.JVM);
            this.providers.add(Provider.RUNTIME);
            return this;
        }

        public Builder provider(final Provider provider) {
            Objects.requireNonNull(provider);
            this.providers.add(provider);
            return this;
        }

        public Builder providers(final Collection<Provider> providers) {
            Objects.requireNonNull(providers);
            this.providers.addAll(providers);
            return this;
        }

        public Set<Provider> providers() {
            return Collections.unmodifiableSet(this.providers);
        }

        public KuraBirthCertificateModule build() {
            return new KuraBirthCertificateModule(this.accountName, providers());
        }
    }

    public static Builder newBuilder(final String accountName) {
        return new Builder(accountName);
    }

    private final Set<String> applications = new TreeSet<>();

    private MqttClient client;

    private final String accountName;

    private final Set<Provider> providers;

    private KuraBirthCertificateModule(final String accountName, final Set<Provider> providers) {
        this.accountName = accountName;
        this.providers = new HashSet<>(providers);
    }

    @Override
    public void applicationAdded(final String applicationId) {
        logger.info("Application added: {}", applicationId);
        if (this.applications.add(applicationId)) {
            sendBirthCertificate();
        }
    }

    @Override
    public void applicationRemoved(final String applicationId) {
        logger.info("Application removed: {}", applicationId);
        if (this.applications.remove(applicationId)) {
            sendBirthCertificate();
        }
    }

    @Override
    public void connected() {
        sendBirthCertificate();
    }

    @Override
    public void initialize(final ModuleContext context) {
        final Client client = context.getClient();
        if (!(client instanceof MqttClient)) {
            throw new IllegalStateException(String.format("%s can only be used with an %s based instance", KuraBirthCertificateModule.class.getSimpleName(), MqttClient.class.getName()));
        }
        this.client = (MqttClient) client;
    }

    private void sendBirthCertificate() {
        logger.debug("Sending birth certificate");

        final Map<String, String> values = new HashMap<>();

        for (final Provider provider : this.providers) {
            provider.provideData(values);
        }

        StringBuffer sb = new StringBuffer( "" );
        String del = "";
        for( String t: this.applications ){
            sb.append(del).append( t );
            del = ",";
        }
        values.put("application_ids", sb.toString());//String.join(",", this.applications));

        // build payload

        final KuraPayload.Builder builder = KuraPayload.newBuilder();
        Metrics.buildMetrics(builder, values);
        final ByteBuffer buffer = ByteBuffer.wrap(builder.build().toByteArray());

        // publish MQTT payload

        final String clientId = this.client.getMqttClientId();

        try {
            this.client.publishMqtt(String.format("$EDC/%s/%s/MQTT/BIRTH", this.accountName, clientId), buffer);
        } catch (final Exception e) {
            logger.warn("Failed to publish birth certificate", e);
        }
    }

}
