# Eclipse Kapua™ Gateway Client SDK [![Build status](https://api.travis-ci.org/ctron/kapua-gateway-client.svg)](https://travis-ci.org/ctron/kapua-gateway-client) [![Maven Central](https://img.shields.io/maven-central/v/de.dentrassi.kapua/kapua-gateway-client.svg "Maven Central Status")](https://search.maven.org/#search%7Cga%7C1%7Cg%3A%22de.dentrassi.kapua%22)

This project provides an SDK for connecting to [Eclipse Kapua](https://eclipse.org/kapua)  as a gateway.

**Note:** This is not part of the Eclipse Kapua project.

This project should provide a simple to use SDK for pushing telemetry data into Kapua
and consuming command messages out of Kapua.

**Note:** This is a work on progress and should not be considered production ready.

## How to use

The following quick steps should provide you with a working example.

### Add to your Maven project

```xml
<dependency>
  <groupId>de.dentrassi.kapua</groupId>
  <artifactId>kapua-gateway-client-provider-mqtt-fuse</artifactId>
  <version><!-- replace with current version --></version>
</dependency>
<dependency>
  <groupId>de.dentrassi.kapua</groupId>
  <artifactId>kapua-gateway-client-profile-kura</artifactId>
  <version><!-- replace with current version --></version>
</dependency>
```

### Example client

```java

import static org.eclipse.kapua.gateway.client.Credentials.userAndPassword;
import static org.eclipse.kapua.gateway.client.Errors.ignore;

import org.eclipse.kapua.gateway.client.mqtt.fuse.FuseClient;
import org.eclipse.kapua.gateway.client.profile.kura.KuraMqttProfile;

try (Client client = KuraMqttProfile.newProfile(FuseClient.Builder::new)
  .accountName("kapua-sys")
  .clientId("foo-bar-1")
  .brokerUrl("tcp://localhost:1883")
  .credentials(userAndPassword("kapua-broker", "kapua-password"))
  .build()) {

  try (Application application = client.buildApplication("app1").build()) {

    // subscribe to a topic

    application.data(Topic.of("my", "receiver")).subscribe(message -> {
      System.out.format("Received: %s%n", message);
    });

    // cache sender instance

    Sender<RuntimeException> sender = application
      .data(Topic.of("my", "sender"))
      .errors(ignore());

    int i = 0;
    while (true) {
      // send
      sender.send(Payload.of("counter", i++));
      Thread.sleep(1000);
    }
  }
}
```