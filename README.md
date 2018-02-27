[![Build Status](https://api.travis-ci.org/symbiote-h2020/ResourceAccessProxyPlugin.svg?branch=develop)](https://api.travis-ci.org/symbiote-h2020/ResourceAccessProxyPlugin)
[![codecov.io](https://codecov.io/github/symbiote-h2020/ResourceAccessProxyPlugin/branch/staging/graph/badge.svg)](https://codecov.io/github/symbiote-h2020/ResourceAccessProxyPlugin)
[![](https://jitpack.io/v/symbiote-h2020/ResourceAccessProxyPlugin.svg)](https://jitpack.io/#symbiote-h2020/ResourceAccessProxyPlugin)

# Resource Access Proxy (RAP) Plugin

## Using RAP Plugin

The idea of RAP Plugin is to use it as dependency in implementation that connects your platform with SymbIoTe. 
Generic parts like RabbitMQ communication with RAP component is implemented in this library. 
That way a developer doesn't have to implement complex communication. 

## Creating concrete plugin

### 1. Creating new SpringBoot project

Create new SpringBoot project with no dependencies.

### 2. Adding symbIoTe dependencies to `build.gradle`

Add following dependencies for cutting edge version:

`compile('com.github.symbiote-h2020:ResourceAccessProxyPlugin:develop-SNAPSHOT') { changing = true }`

or add following for specific version:

`compile('com.github.symbiote-h2020:ResourceAccessProxyPlugin:{version}')`

Current version is `1.0.0`.

This is dependency from jitpack repository. 
In order to use jitpack you need to put in `build.gradle` 
following lines as well:

```
repositories {
	jcenter()
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
- for activating actuator and calling service there is `WritingToResourceListener`
- for beginning and ending subscription there is `NotificationResourceListener`

Registering and unregistering resources is done by calling `register...` or `unregister...` methods
in `RapPlugin` class. `RapPlugin` class can be injected like any other bean.

### 5. Reading resources
When resource needs to be read the RAP component will send message to RAP plugin and
that message will cause calling method in `ReadingResourceListener` class.
It has following methods:

- `List<Observation> readResource(String resourceId)` for reading one resource.
The argument is internal resource ID. It returns list of observed values.
- `List<Observation> readResourceHistory(String resourceId)` for reading
historical observed values which are returned.

In the case that reading is not possible method should return `null`.

Here is example of registering and handling faked values for resource with internal id `iid1`:

```java
rapPlugin.registerReadingResourceListener(new ReadingResourceListener() {
    
    @Override
    public List<Observation> readResourceHistory(String resourceId) {
        if("iid1".equals(resourceId))
            return new ArrayList<>(Arrays.asList(createObservation(resourceId), createObservation(resourceId), createObservation(resourceId)));

        return null;
    }
    
    @Override
    public List<Observation> readResource(String resourceId) {
        if("iid1".equals(resourceId)) {
            Observation o = createObservation(resourceId);
            return new ArrayList<>(Arrays.asList(o));
        }
            
        return null;            
    }
});
```
 
### 6. Triggering actuator and/or calling service
For both actions is used the same listener `WritingToResourceListener`. There 
is only one method in this interface: 
`Result<Object> writeResource(String resourceId, List<InputParameter> parameters)`.
Arguments are: internal resource ID and service/actuation parameters.
Parameters are implemented in `InputParameter` class. Return value is different
for actuation and service call:
- actuation - `null` is usual value, but it can be `Result` with message.
- service call - must have return value that is put in `Result` object.

Here is example of both implementations of listener:
```java
rapPlugin.registerWritingToResourceListener(new WritingToResourceListener() {
    
    @Override
    public Result<Object> writeResource(String resourceId, List<InputParameter> parameters) {
        LOG.debug("writing to resource {} body:{}", resourceId, parameters);
        if("2000".equals(resourceId)) { // actuation
            Optional<InputParameter> lightParameter = parameters.stream().filter(p -> p.getName().equals("light")).findFirst();
            if(lightParameter.isPresent()) {
                String value = lightParameter.get().getValue();
                if("on".equals(value)) {
                    LOG.debug("Turning on light {}", resourceId);
                    return new Result<>(false, null, "Turning on light " + resourceId);
                } else if("off".equals(value)) {
                    LOG.debug("Turning off light {}", resourceId);
                    return new Result<>(false, null, "Turning off light " + resourceId);
                }
            }
        } else if("3000".equals(resourceId)) { // service call
            Optional<InputParameter> lightParameter = parameters.stream().filter(p -> p.getName().equals("trasholdTemperature")).findFirst();
            if(lightParameter.isPresent()) {
                String value = lightParameter.get().getValue();
                LOG.debug("Setting trashold on resource {} to {}", resourceId, value);
                return new Result<>(false, null, "Setting trashold on resource " + resourceId + " to " + value);
                }
            }
            return null;
        }
    });
}
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
    Location loc = new WGS84Location(15.9, 45.8, 145, "Spansko", Arrays.asList("City of Zagreb"));
    
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
                    new Property("Temperature", Arrays.asList("Air temperature")), 
                    new UnitOfMeasurement("C", "degree Celsius", null));
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
