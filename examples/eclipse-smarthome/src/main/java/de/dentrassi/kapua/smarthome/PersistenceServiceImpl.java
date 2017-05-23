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
package de.dentrassi.kapua.smarthome;

import static org.eclipse.kapua.gateway.client.profile.kura.KuraMqttProfile.newProfile;

import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;

import org.eclipse.kapua.gateway.client.Application;
import org.eclipse.kapua.gateway.client.Client;
import org.eclipse.kapua.gateway.client.Credentials;
import org.eclipse.kapua.gateway.client.Errors;
import org.eclipse.kapua.gateway.client.Payload;
import org.eclipse.kapua.gateway.client.Sender;
import org.eclipse.kapua.gateway.client.Topic;
import org.eclipse.kapua.gateway.client.mqtt.fuse.FuseClient;
import org.eclipse.kapua.gateway.client.profile.kura.KuraMqttProfile;
import org.eclipse.smarthome.core.items.Item;
import org.eclipse.smarthome.core.library.types.DecimalType;
import org.eclipse.smarthome.core.library.types.OnOffType;
import org.eclipse.smarthome.core.library.types.PlayPauseType;
import org.eclipse.smarthome.core.library.types.StringType;
import org.eclipse.smarthome.core.library.types.UpDownType;
import org.eclipse.smarthome.core.persistence.PersistenceService;
import org.eclipse.smarthome.core.types.State;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.dentrassi.kapua.smarthome.internal.Configuration;

@Component
@Designate(ocd=Configuration.class)
public class PersistenceServiceImpl implements PersistenceService {

    private static final Logger logger = LoggerFactory.getLogger(PersistenceServiceImpl.class);

    private Client client;
    private Application app;

    @Override
    public String getName() {
        return "kapua";
    }

    @Override
    public void store(final Item item) {
        store(item, null);
    }

    @Override
    public void store(final Item item, final String alias) {
        logger.debug("Store - item: {}, alias: {}", item, alias);

        final String key = alias == null ? item.getName() : alias;
        if (key == null || key.isEmpty()) {
            return;
        }

        final Map<String, Object> data = new HashMap<>();
        final State state = item.getState();
        if (state instanceof DecimalType) {
            data.put("value", ((DecimalType) state).longValue());
        } else if (state instanceof StringType) {
            data.put("value", state.toString());
        } else if (state instanceof OnOffType) {
            data.put("value", state.equals(OnOffType.ON));
        } else if (state instanceof UpDownType) {
            data.put("value", state.equals(UpDownType.UP));
        } else if (state instanceof PlayPauseType) {
            data.put("value", state.equals(PlayPauseType.PLAY));
        } else {
            return;
        }

        final Sender<RuntimeException> sender;

        synchronized (this) {
            if (app == null) {
                logger.debug("Not configured, discarding event");
                return;
            }
            sender = app.data(Topic.of(key)).errors(Errors.ignore());
            sender.send(Payload.of(data));
        }
    }

    @Activate
    protected synchronized void activate(final Map<String, Object> properties) throws Exception {
        if (logger.isWarnEnabled()) {
            logger.warn("Create new Kapua client: {}", new TreeMap<>(properties));
        }

        final String broker = getOrDefault(properties, "brokerUrl", null);
        if (broker == null || broker.isEmpty()) {
            throw new IllegalStateException("'brokerUrl' is not set");
        }

        final KuraMqttProfile<FuseClient.Builder> profile = newProfile(FuseClient.Builder::new);
        profile.accountName(getOrDefault(properties, "accoutName", "kapua-sys"));
        profile.clientId(getOrDefault(properties, "clientId", UUID.randomUUID().toString()));
        profile.brokerUrl(broker);

        final String user = getOrDefault(properties, "user",null);
        final String password = getOrDefault(properties, "password", null);

        if (!user.isEmpty() && !password.isEmpty()) {
            profile.credentials(Credentials.userAndPassword(user, password));
        }

        this.client = profile.build();

        this.app = client.buildApplication("smarthome").build();
    }

    private static String getOrDefault(final Map<String, ?> properties, final String key, final String defaultValue) {
        if (properties == null) {
            return defaultValue;
        }

        final Object value = properties.get(key);
        if (value == null) {
            return defaultValue;
        }

        return value.toString();
    }

    @Deactivate
    protected synchronized void deactivate() throws Exception {
        if (client != null) {
            try {
                this.client.close();
            } finally {
                client = null;
                app = null;
            }
        }
    }

    @Modified
    protected synchronized void modified(final Map<String, Object> properties) throws Exception {
        deactivate();
        activate(properties);
    }
}
