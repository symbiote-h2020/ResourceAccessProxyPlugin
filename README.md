[![Build Status](https://api.travis-ci.org/symbiote-h2020/ResourceAccessProxyPluginStarter.svg?branch=develop)](https://api.travis-ci.org/symbiote-h2020/ResourceAccessProxyPluginStarter)
[![codecov.io](https://codecov.io/github/symbiote-h2020/ResourceAccessProxyPluginStarter/branch/staging/graph/badge.svg)](https://codecov.io/github/symbiote-h2020/ResourceAccessProxyPluginStarter)
[![](https://jitpack.io/v/symbiote-h2020/ResourceAccessProxyPluginStarter.svg)](https://jitpack.io/#symbiote-h2020/ResourceAccessProxyPluginStarter)

<!-- TOC -->

- [RELEASE NOTES](#release-notes)
  - [1.1.0](#110)
  - [1.0.4](#104)
  - [1.0.3](#103)
  - [0.5.0](#050)
  - [0.3.5](#035)
- [Resource Access Proxy (RAP) Plugin](#resource-access-proxy-rap-plugin)
  - [Using RAP Plugin](#using-rap-plugin)
  - [Creating concrete plugin](#creating-concrete-plugin)
    - [1. Creating new SpringBoot project](#1-creating-new-springboot-project)
    - [2. Adding symbIoTe dependencies to `build.gradle`](#2-adding-symbiote-dependencies-to-buildgradle)
    - [3. Setting configuration](#3-setting-configuration)
    - [4. Registering RAP plugin consumers](#4-registering-rap-plugin-consumers)
    - [5. Reading resources](#5-reading-resources)
    - [6. Triggering actuator](#6-triggering-actuator)
    - [7. Invoking service](#7-invoking-service)
    - [8. Notifications/Subscriptions](#8-notificationssubscriptions)
  - [Running](#running)
  - [Appendix](#appendix)
    - [createObservation method](#createobservation-method)

<!-- /TOC -->

# RELEASE NOTES
## 1.1.0
Added support for notification subscriptions and removed old API for listeners.

## 1.0.4
Updated dependency to SymbIoTeLibraries:6.0.0

## 1.0.3
Supports use with Java 8 and 11. Uses new version of SpringBoot 2.2.x and new version of SpringCloud Greenwich.SR1. Current deprecated classes and methods will be removed in 1.1.x

## 0.5.0
This version has new listener interfaces. The old ones are deprecated.

## 0.3.5
Versions until 0.3.5 had different artifact name. Now the artifact name is ResourceAccessProxyPlugin**Starter**

# Resource Access Proxy (RAP) Plugin

## Using RAP Plugin

The idea of RAP Plugin is to use it as dependency in implementation that connects your platform with SymbIoTe. 
Generic parts like RabbitMQ communication with RAP component is implemented in this library.
That way a developer doesn't have to implement complex communication.

Example of project using RAP plugin starter is in following repository [https://github.com/symbiote-h2020/RapPluginExample](https://github.com/symbiote-h2020/RapPluginExample)

## Creating concrete plugin

### 1. Creating new SpringBoot project

Create new SpringBoot (2.2.7) project with no dependencies.

### 2. Adding symbIoTe dependencies to `build.gradle`

Add following dependencies for cutting edge version:

`compile('com.github.symbiote-h2020:ResourceAccessProxyPluginStarter:develop-SNAPSHOT') { changing = true }`

or add following for specific version:

`compile('com.github.symbiote-h2020:ResourceAccessProxyPluginStarter:{version}')`

Current version is `1.0.4`.

This is dependency from jitpack repository. In order to use
jitpack you need to put in `build.gradle` following lines as
well:

```
repositories {
	...
	maven { url "https://jitpack.io" } // this is important to add
}
```

### 3. Setting configuration

Configuration needs to be put in `application.properties` or YML file. An example is here:

```
spring.application.name=RapPluginExample

rabbit.host=localhost
rabbit.username=guest
rabbit.password=guest
rabbit.replyTimeout=60000

rap-plugin.filtersSupported=false
rap-plugin.notificationsSupported=false
```

The first line is defining the name of this specific RAP plugin.

**NOTE:** When you register resource in the registration JSON 
there is `pluginId` which needs to be this name. In this case 
it needs to be `RapPluginExample`.

The second group of lines is configuration of RabbitMQ server. The RAP plugin communicates
with RAP component by using RabbitMQ server. For connecting to RabbitMQ server RAP plugin
needs to know host, username and password. The default values are in this example.

The third group of lines are configuration of RAP plugin. 

If `filtersSupported` is `false` then
the RAP component will filter responses from RAP plugin. Then the RAP plugin needs to return
all observations when history is asked. If this value is `true` then the RAP is responsible
for filtering data. The data is send in new listeners.

If `notificationsSupported` is `false` it means that RAP plugin can not stream data. If it
is `true` then it can accept subscriptions and RAP plugin is responsible for pushing data
on changed resource. Subscriptions are supported by registering `SubscriptionListener`.

### 4. Registering RAP plugin consumers

There are following RAP plugin consumers:

- for reading resources there is `ResourceAccessListener`
- for activating actuator there is `ActuatorAccessListener`
- for invoking service there is `ServiceAccessListener`
- for beginning and ending subscription there is `SubscriptionListener`

Registering and unregistering resources is done by calling `register...` 
or `unregister...` methods
in `RapPlugin` class. `RapPlugin` class can be injected like any other bean.

### 5. Reading resources

When resource needs to be read the RAP component will send message to RAP plugin and
that message will cause calling method in registered listener. 

Listener `ResourceAccessListener` has following methods:

- `String getResource(List<ResourceInfo> resourceInfo)` for reading last observation.
The argument `resourceInfo` represents resource. Internal id can be extracted by using
`Utils.getInternalResourceId(resourceInfo)` 
It returns last observed value converted to JSON ans returned as `String`.
- `String getResourceHistory(List<ResourceInfo> resourceInfo, int top, Query filterQuery)` 
for reading historical observed values.
The arguments are `resourceInfo` that represents resource, `top` is value 
of how many observations needs to be returned and `filterQuery` are other filters 
(if property `rap-plugin.filtersSupported` is `false` it will always be `null`). 
It returns observations that are convertes to JSON and returned as `String`.

In the case that reading is not possible method should throw 
`RapPluginException`.

Here is example of registering and handling faked values for resource with
internal id `rp_isen1` or `isen1`:

```java
rapPlugin.registerReadingResourceListener(new ResourceAccessListener() {

    @Override
    public String getResourceHistory(List<ResourceInfo> resourceInfo, int top, Query filterQuery) {
        LOG.debug("reading resource history with info {}", resourceInfo);

        String resourceId = Utils.getInternalResourceId(resourceInfo);

        if("rp_isen1".equals(resourceId) || "isen1".equals(resourceId)) {
            // This is the place to put reading history data of sensor.
            List<Observation> observations = new LinkedList<>();
            for (int i = 0; i < top; i++) {
                observations.add(createObservation(resourceId));
            }

            try {
                return mapper.writeValueAsString(observations);
            } catch (JsonProcessingException e) {
                throw new RapPluginException(500, "Can not convert observations to JSON", e);
            }
        } else { 
            throw new RapPluginException(404, "Sensor not found.");
        }
    }

    @Override
    public String getResource(List<ResourceInfo> resourceInfo) {
        LOG.debug("reading resource with info {}", resourceInfo);

        String resourceId = Utils.getInternalResourceId(resourceInfo);

        if("rp_isen1".equals(resourceId) || "isen1".equals(resourceId)) {
            // This is place to put reading data from sensor 
            try {
                return mapper.writeValueAsString(createObservation(resourceId));
            } catch (JsonProcessingException e) {
                throw new RapPluginException(500, "Can not convert observation to JSON", e);
            }
        }

        throw new RapPluginException(404, "Sensor not found.");
    }
});
```

### 6. Triggering actuator

For actuating resource there is listener interface `ActuatorAccessListener` with only one method:

`void actuateResource(String internalId, Map<String, Map<String, Value>> capabilities)`.

Arguments are: internal resource ID and actuation capability map.
The key of map is capability name and value is parameters map. 
Parameters map has key which
is parameter name and value is `Value` interface. `Value` interface has 4 implementations: 
`PrimitiveValue`, `PrimitiveValueArray`, `ComplexValue` and `ComplexValueArray`.

This method can throw `RapPluginException` when actuation can not be executed.

Here is example implementation:

```java
rapPlugin.registerActuatingResourceListener(new ActuatorAccessListener() {
    @Override
    public void actuateResource(String internalId, Map<String, Map<String, Value>> capabilities) {
        System.out.println("Called actuation for resource " + internalId);
        // print capabilities
        for (Entry<String, Map<String, Value>> capabilityEntry: capabilities.entrySet()) {
            System.out.println("Capability: " + capabilityEntry.getKey());

            for(Entry<String, Value> parameterEntry: capabilityEntry.getValue().entrySet()) {
                System.out.print(" " + parameterEntry.getKey() + " = ");
                PrimitiveValue primitiveValue = parameterEntry.getValue().asPrimitive();
                if(primitiveValue.isString()) {
                    System.out.println(primitiveValue.asString());
                } else if (primitiveValue.isInt()) {
                    System.out.println(primitiveValue.asInt());
                } else {
                    System.out.println(primitiveValue.toString());
                }
            }
        }

        if("rp_iaid1".equals(internalId) || "iaid1".equals(internalId)) {
            // This is place to put actuation code for resource with id
            System.out.println("iaid1 is actuated");
            return;
        } else {
            throw new RapPluginException(404, "Actuating entity not found.");
        }
    }
});
```

### 7. Invoking service

For invoking service there is listener interface `ServiceAccessListener` with only one method:

`String invokeService(String internalId, Map<String, Value> parameters)`.

Arguments are: internal resource ID and actuation parameters map.
Parameters map has key which
is parameter name and value is `Value` interface (the same as in case of actuation,
see details there).

This method can throw `RapPluginException` when invoking service can not be executed.

Here is example implementation:

```java
rapPlugin.registerInvokingServiceListener(new ServiceAccessListener() {

    @Override
    public String invokeService(String internalId, Map<String, Value> parameters) {
    System.out.println("In invoking service of resource " + internalId);

    // print parameters
    for(Entry<String, Value> parameterEntry: parameters.entrySet()) {
        System.out.println(" Parameter - name: " + parameterEntry.getKey() + " value: " + 
                parameterEntry.getValue().asPrimitive().asString());
    }

    try {
        if("rp_isrid1".equals(internalId)) {
            return mapper.writeValueAsString("ok");
        } else if ("isrid1".equals(internalId)) {
            return mapper.writeValueAsString("some json");
        } else {
            throw new RapPluginException(404, "Service not found.");
        }
    } catch (JsonProcessingException e) {
        throw new RapPluginException(500, "Can not convert service response to JSON", e);
    }
});
```

### 8. Notifications/Subscriptions

For handling subscriptions there is `SubscriptionListener` interface. It has two methods:

- `public void subscribeResource(String resourceId)`
  - called when subscription for resource with internal id is started
- `public void unsubscribeResource(String resourceId)`
  - called whrn subscription for resource is terminated

Plugin implementor must handle when to send notifications.
It could be on some event or periodic. The notification can be send to
subscriber by using method `sendNotification` in `rapPlugin` object.
This method accepts `Observation` object. This method sends observation to RAP and RAP forwards it to users over websocket.

Here is complete example of periodic subscription:

```java
rapPlugin.registerNotificationResourceListener(new SubscriptionListener() {

  private Set<String> subscriptionSet = Collections.synchronizedSet(new HashSet<>()); // internal ids of subscribed resources
  private volatile Thread subscriptionThread; // currently active thread

  @Override
  public void subscribeResource(String resourceId) {
    LOG.info("Subscribing to {}", resourceId);
    synchronized (subscriptionSet) {
      subscriptionSet.add(resourceId);
      if (subscriptionThread == null) {
        subscriptionThread = new Thread(() -> { // thread that send notifications
          while (!subscriptionSet.isEmpty()) {
            sendPush(); // send notification for all subscribed resources
            try {
              Thread.sleep(5000); // notifications are send every 5 sec
            } catch (InterruptedException e) {
              e.printStackTrace();
            }
          }
          subscriptionThread = null;
        });
        subscriptionThread.start();
      }
    }
  }

  private void sendPush() {
    LOG.info("Sending notifications!!!!");
    ObjectMapper mapper = new ObjectMapper();
    synchronized (subscriptionSet) {
      for (String id : subscriptionSet) {
        Observation observation = createObservation(id);
        rapPlugin.sendNotification(observation);
        LOG.info("Notification for resource {}: {}", id, observation);
      }
    }
  }

  @Override
  public void unsubscribeResource(String resourceId) {
    LOG.info("Unsubscribe to {}", resourceId);
    synchronized (subscriptionSet) {
      subscriptionSet.remove(resourceId);
    }
  }
});
```

## Running

You can run this component as any other spring boot application.

`./gradlew bootRun`

or

`java -jar build/libs/RapPluginExample-0.0.1-SNAPSHOT.jar`

Note: In order to function correctly you need to start RabbitMQ and RAP component before.

## Appendix

### createObservation method

```java
public Observation createObservation(String sensorId) {
    Location loc = new WGS84Location(48.2088475, 16.3734492, 158, "Stephansdome", Arrays.asList("City of Wien"));

    TimeZone zoneUTC = TimeZone.getTimeZone("UTC");
    DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
    dateFormat.setTimeZone(zoneUTC);
    Date date = new Date();
    String timestamp = dateFormat.format(date);

    long ms = date.getTime() - 1000;
    date.setTime(ms);
    String samplet = dateFormat.format(date);

    ObservationValue obsval = 
            new ObservationValue(
                "7", 
                new Property("Temperature", "TempIRI", Arrays.asList("Air temperature")), 
                new UnitOfMeasurement("C", "degree Celsius", "C_IRI", null));
    ArrayList<ObservationValue> obsList = new ArrayList<>();
    obsList.add(obsval);

    Observation obs = new Observation(sensorId, loc, timestamp, samplet , obsList);

    try {
        LOG.debug("Observation: \n{}", new ObjectMapper().writeValueAsString(obs));
    } catch (JsonProcessingException e) {
        LOG.error("Can not convert observation to JSON", e);
    }

    return obs;
}
```
