[![Build Status](https://api.travis-ci.org/symbiote-h2020/ResourceAccessProxyPlugin.svg?branch=develop)](https://api.travis-ci.org/symbiote-h2020/ResourceAccessProxyPlugin)
[![codecov.io](https://codecov.io/github/symbiote-h2020/ResourceAccessProxyPlugin/branch/staging/graph/badge.svg)](https://codecov.io/github/symbiote-h2020/ResourceAccessProxyPlugin)
[![](https://jitpack.io/v/symbiote-h2020/ResourceAccessProxyPlugin.svg)](https://jitpack.io/#symbiote-h2020/ResourceAccessProxyPlugin)

# Resource Access Proxy (RAP) Plugin

## Using RAP Plugin

The idea of RAP Plugin is to use it as dependency in implementation that connects your platform with SymbIoTe. 
Generic parts like RabbitMQ communication with RAP component is implemented in this library. 
That way a developer doesn't have to implement complex communication.

Example of project using RAP plugin starter is in following repository [https://github.com/symbiote-h2020/RapPluginExample](https://github.com/symbiote-h2020/RapPluginExample)

## Creating concrete plugin

### 1. Creating new SpringBoot project

Create new SpringBoot project with no dependencies.

### 2. Adding symbIoTe dependencies to `build.gradle`

Add following dependencies for cutting edge version:

`compile('com.github.symbiote-h2020:ResourceAccessProxyPlugin:develop-SNAPSHOT') { changing = true }`

or add following for specific version:

`compile('com.github.symbiote-h2020:ResourceAccessProxyPlugin:{version}')`

Current version is `0.3.1`.

This is dependency from jitpack repository. 
In order to use jitpack you need to put in `build.gradle` 
following lines as well:

```
repositories {
	...
	maven { url "https://jitpack.io" } // this is important to add
}
```

### 3. Setting configuration

Configuration needs to be put in `application.properties` or YMl file. An example is here:

```
spring.application.name=RapPluginExample

rabbit.host=localhost
rabbit.username=guest
rabbit.password=guest

rap-plugin.filtersSupported=false
rap-plugin.notificationsSupported=false
```

The first line is defining the name of this specific RAP plugin.

The second group of lines is configuration of RabbitMQ server. The RAP plugin communicates
with RAP component by using RabbitMQ server. For connecting to RabbitMQ server RAP plugin
needs to know hots, username and password. The default values are in this example.

The third group of lines are configuration of RAP plugin. 

If `filtersSupported` is `false` then
the RAP component will filter responses from RAP plugin. Then the RAP plugin needs to return
all observations when history is asked. If this value is `true` then the RAP is responsible
for filtering data.

If `notificationsSupported` is `false` it means that RAP plugin can not stream data. If it
is `true` then it can accept subscriptions and RAP plugin is responsible for pushing data
on changed resource. Subscriptions are supported by registering `NotificationResourceListener`. 

### 4. Registering RAP plugin consumers

There are following RAP plugin consumers:
- for reading resources there is `ReadingResourceListener`
- for activating actuator there is `ActuatingoResourceListener`
- for invoking service there is `InvokingServiceResourceListener`
- for beginning and ending subscription there is `NotificationResourceListener`

Registering and unregistering resources is done by calling `register...` 
or `unregister...` methods
in `RapPlugin` class. `RapPlugin` class can be injected like any other bean.

### 5. Reading resources
When resource needs to be read the RAP component will send message to RAP plugin and
that message will cause calling method in `ReadingResourceListener` class.
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
 
### 6. Triggering actuator
For actuating resource there is `ActuatingResourceListener`. There 
is only one method in this interface: 
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

### 7. Invoking service
For invoking service there is `InvokingServiceListener`. There 
is only one method in this interface: 
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
