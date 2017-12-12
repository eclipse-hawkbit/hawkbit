---
layout: documentation
title: Clustering
---

{% include base.html %}

# Create Feign REST Client
In this guide we describe how to create a [Feign](https://github.com/Netflix/feign) Rest Client based on a  [Spring Boot](http://projects.spring.io/spring-boot/) Application. hawkBit provides REST interfaces for [Management API](https://github.com/eclipse/hawkbit/tree/master/hawkbit-ddi-api) and [DDI API] (https://github.com/eclipse/hawkbit/tree/master/hawkbit-ddi-api). Using this interfaces you can create a feign client with the help of the [feign inheritance support] (http://projects.spring.io/spring-cloud/spring-cloud.html#spring-cloud-feign-inheritance).
Our [example](https://github.com/eclipse/hawkbit-examples) modules demonstrate how to create [Feign](https://github.com/Netflix/feign) client resources. Here you can find the [Management API client resources](hhttps://github.com/eclipse/hawkbit-examples/tree/master/hawkbit-example-mgmt-feign-client) and the [DDI client resources](https://github.com/eclipse/hawkbit-examples/tree/master/hawkbit-example-ddi-feign-client).
A small [simulator application](https://github.com/eclipse/hawkbit-examples/tree/master/hawkbit-example-mgmt-simulator) demonstrates how you can interact with the hawkBit via the [Management API
](http://www.eclipse.org/hawkbit/documentation/interfaces/management-api.html). 

### Example Managment API simulator

In the follow code section, you can a see a feign client resource example. The interface extend the orgin api inteface to declare the `@FeignClient`. The `@FeignClient`declares that a REST client with that interface should be created. 

```
@FeignClient(url = "${hawkbit.url:localhost:8080}/" + MgmtRestConstants.TARGET_V1_REQUEST_MAPPING)
public interface MgmtTargetClientResource extends MgmtTargetRestApi {
}
```

This interface can be autowired and use as a normal java interface:

```
public class CreateStartedRolloutExample {

    @Autowired
    private MgmtTargetClientResource targetResource;


    public void run() {
        // create ten targets
        targetResource.createTargets(new TargetBuilder().controllerId("00-FF-AA-0").name("00-FF-AA-0")
                .description("Targets used for rollout example").buildAsList(10));
    }

```

At [hawkbit-example-core-feign-client](https://github.com/eclipse/hawkbit-examples/tree/master/hawkbit-example-core-feign-client) is a spring configuration to auto configure some beans, which can be reused for a own feign client.

```
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