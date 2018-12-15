[![Build Status](https://api.travis-ci.org/symbiote-h2020/ResourceAccessProxyPluginStarter.svg?branch=develop)](https://api.travis-ci.org/symbiote-h2020/ResourceAccessProxyPluginStarter)
[![codecov.io](https://codecov.io/github/symbiote-h2020/ResourceAccessProxyPluginStarter/branch/staging/graph/badge.svg)](https://codecov.io/github/symbiote-h2020/ResourceAccessProxyPluginStarter)
[![](https://jitpack.io/v/symbiote-h2020/ResourceAccessProxyPluginStarter.svg)](https://jitpack.io/#symbiote-h2020/ResourceAccessProxyPluginStarter)

# Resource Access Proxy (RAP) Plugin

## Using RAP Plugin

The idea of RAP Plugin is to use it as dependency in implementation that connects your platform with SymbIoTe. 
Generic parts like RabbitMQ communication with RAP component is implemented in this library. 
That way a developer doesn't have to implement complex communication.

Example of project using RAP plugin starter is in following repository [https://github.com/symbiote-h2020/RapPluginExample](https://github.com/symbiote-h2020/RapPluginExample)

## Creating concrete plugin

### 1. Creating new SpringBoot project

Create new SpringBoot (1.5.18) project with no dependencies.

### 2. Adding symbIoTe dependencies to `build.gradle`

Add following dependencies for cutting edge version:

`compile('com.github.symbiote-h2020:ResourceAccessProxyPluginStarter:develop-SNAPSHOT') { changing = true }`

or add following for specific version:

`compile('com.github.symbiote-h2020:ResourceAccessProxyPluginStarter:{version}')`

Current version is `0.5.2`.

**NOTE:** The versions until 0.3.5 had different artifact name. Now the artifact name is ResourceAccessProxyPlugin**Starter**.

**NOTE:** The versions from 0.5.0 have new listener interfaces.

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
on changed resource. Subscriptions are supported by registering `NotificationResourceListener`. 

### 4. Registering RAP plugin consumers

There are following RAP plugin consumers:
- for reading resources there are:
  - **OLD** - `ReadingResourceListener`
  - **NEW** - `ResourceAccessListener`
- for activating actuator there are:
  - **OLD** - `ActuatingoResourceListener` 
  - **NEW** - `ActuatorAccessListener` 
- for invoking service there are:
  - **OLD** - `InvokingServiceResourceListener`
  - **NEW** - `ActuatorAccessListener`
- for beginning and ending subscription there is `NotificationResourceListener`

Old listeners and registratin methods are deprecated and can be removed in new
version.

Registering and unregistering resources is done by calling `register...` 
or `unregister...` methods
in `RapPlugin` class. `RapPlugin` class can be injected like any other bean.

### 5. Reading resources
When resource needs to be read the RAP component will send message to RAP plugin and
that message will cause calling method in registered listener. 

#### 5.1. Old listener `ReadingResourceListener`
It has following methods:

- `Observation readResource(String resourceId)` for reading one resource.
The argument is internal resource ID. It returns last observed value.
- `List<Observation> readResourceHistory(String resourceId)` for reading
historical observed values which are returned. Default value is to return last 
100 readings.

In the case that reading is not possible method should return throw 
`RapPluginException`.

Here is example of registering and handling faked values for resource with internal id `iid1`:

```java
rapPlugin.registerReadingResourceListener(new ReadingResourceListener() {
    
    @Override
    public List<Observation> readResourceHistory(String resourceId) {
        if("isen1".equals(resourceId))
            // This is the place to put reading history data of sensor.
            return new ArrayList<>(Arrays.asList(createObservation(resourceId), createObservation(resourceId), createObservation(resourceId)));
        else 
            throw new RapPluginException(404, "Sensor not found.");
    }
    
    @Override
    public Observation readResource(String resourceId) {
        if("isen1".equals(resourceId)) {
            // This is place to put reading data from sensor 
            return createObservation(resourceId);
        }
            
        throw new RapPluginException(404, "Sensor not found.");
    }
});
```
#### 5.2. New listener `ResourceAccessListener`
It has following methods:

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
For actuating resource there are listeners with only one method.

#### 6.1 Old listener `ActuatingResourceListener`
The method in this interface is: 
`void actuateResource(String resourceId, Map<String,Capability> parameters);`.

Arguments are: internal resource ID and actuation parameters map.
The key of map is capability name and capability is implemented in `Capability`
class. `Capability` class has name parameters map. Parameters map has key which
is parameter name and value is `Parameter` class. `Parameter` class has name and
value. Parameter value can be any object.

This method can throw `RapPluginException` when actuation can not be executed.

Here is example implementation:
```java
rapPlugin.registerActuatingResourceListener(new ActuatingResourceListener() {
    @Override
    public void actuateResource(String resourceId, Map<String,Capability> parameters) {
        System.out.println("Called actuation for resource " + resourceId);
        for(Capability capability: parameters.values()) {
            System.out.println("Capability: " + capability.getName());
            for(Parameter parameter: capability.getParameters().values()) {
                System.out.println(" " + parameter.getName() + " = " + parameter.getValue());
            }
        }
        
        if("iaid1".equals(resourceId)) {
            // This is place to put actuation code for resource with id
            System.out.println("iaid1 is actuated");
            return;
        } else {
            throw new RapPluginException(404, "Actuating entity not found.");
        }
    }
});
```

#### 6.2 New listener `ActuatorAccessListener`
The method in this interface is: 
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
For invoking service there is listeners with only one method.

#### 7.1 Old listener `InvokingServiceListener`
The method in this interface is: 
`Object invokeService(String resourceId, Map<String,Parameter> parameters);`.

Arguments are: internal resource ID and actuation parameters map.
Parameters map has key which
is parameter name and value is `Parameter` class. `Parameter` class has name and
value. Parameter value can be any object.

This method can throw `RapPluginException` when invoking service can not be executed.

Here is example implementation:
```java
rapPlugin.registerInvokingServiceListener(new InvokingServiceListener() {
    
    @Override
    public Object invokeService(String resourceId, Map<String, Parameter> parameters) {
        System.out.println("In invoking service of resource " + resourceId);
        for(Parameter p: parameters.values())
            System.out.println(" Parameter - name: " + p.getName() + " value: " + p.getValue());
        if("isrid1".equals(resourceId)) {
            return "ok";
        } else {
            throw new RapPluginException(404, "Service not found.");
        }
    }
});
```
#### 7.2 New listener `ServiceAccessListener`
The method in this interface is: 
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

## Running

You can run this component as any other spring boot application.

`./gradlew bootRun`

or

`java -jar build/libs/RapPluginExample-0.0.1-SNAPSHOT.jar`

Note: In order to function correctly you need to start RabbitMQ and RAP component before.

## Appendix
### createObservation method
```
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
