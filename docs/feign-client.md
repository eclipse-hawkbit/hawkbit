# Feign Client

In this guide we describe how to create a **Feign REST Client** based on a Spring Boot Application.

---

### Create Feign REST Client

hawkBit provides REST interfaces for **Management API** and **DDI API**. Using these interfaces you can create a Feign client with the help of the *Feign inheritance support*.  
Our example modules demonstrate how to create Feign client resources. Here you can find the [Management API client resources](https://github.com/eclipse-hawkbit/hawkbit/tree/master/hawkbit-mgmt-api) and the [DDI client resources](https://github.com/eclipse-hawkbit/hawkbit/tree/master/hawkbit-ddi-api).  
A small simulator application demonstrates how you can interact with hawkBit via the [Management API](http://www.eclipse.org/hawkbit/documentation/interfaces/management-api.html).

---

### Example Management API Simulator

In the following code section, you can see a Feign client resource example.  
The interface extends the origin API interface to declare the `@FeignClient`.  
The `@FeignClient` declares that a REST client with that interface should be created.

This interface can be autowired and used as a normal Java interface:

```java
@FeignClient(url = "${hawkbit.url:localhost:8080}/" + MgmtRestConstants.TARGET_V1_REQUEST_MAPPING)
public interface MgmtTargetClientResource extends MgmtTargetRestApi {
}

public class CreateStartedRolloutExample {

    @Autowired
    private MgmtTargetClientResource targetResource;

    public void run() {

        // create ten targets
        targetResource.createTargets(new TargetBuilder().controllerId("00-FF-AA-0").name("00-FF-AA-0")
                .description("Targets used for rollout example").buildAsList(10));
    }
}
```

Example projects:  
- [hawkbit-example-mgmt-feign-client](https://github.com/eclipse-hawkbit/hawkbit-examples/tree/master/hawkbit-example-mgmt-feign-client)  
- [hawkbit-example-ddi-feign-client](https://github.com/eclipse-hawkbit/hawkbit-examples/tree/master/hawkbit-example-ddi-feign-client)  
- [hawkbit-example-mgmt-simulator](https://github.com/eclipse-hawkbit/hawkbit-examples/tree/master/hawkbit-example-mgmt-simulator)  

---

### Feign Client Configuration

At [`hawkbit-example-core-feign-client`](https://github.com/eclipse-hawkbit/hawkbit-examples/tree/master/hawkbit-example-core-feign-client) there is a Spring configuration to auto configure some beans, which can be reused for your own Feign client:

```java
@Configuration
@ConditionalOnClass(Feign.class)
@Import(FeignClientsConfiguration.class)
public class FeignClientConfiguration {

    @Bean
    public ApplicationJsonRequestHeaderInterceptor jsonHeaderInterceptor() {
        return new ApplicationJsonRequestHeaderInterceptor();
    }

    @Bean
    public Contract feignContract() {
        return new IgnoreMultipleConsumersProducersSpringMvcContract();
    }
}
``` 
