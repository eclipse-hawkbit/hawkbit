# hawkBit device management federation API - implementation

This is the AMQP implementation for the device management federation API. The implementation uses the spring-amqp
project.

# Integration Test

This modules contains some integration tests for the device management federation API implementation which uses a
RabbitMQ (base class `AbstractAmqpIntegrationTest` in `hawkbit-dmf-rabbitmq-test`, annotated with Spring AMQP's
`@RabbitAvailable`). The tests expect a RabbitMQ broker on `localhost:5672` (user/password `guest`/`guest`) with the
management plugin on port `15672` (used to create a virtual host per test class). If there is no RabbitMQ running on
the system, all tests will be marked as skipped. In order to make the tests fail instead, set the environment variable
`RABBITMQ_SERVER_REQUIRED=true`.

The broker connection can be changed with the environment variables supported by `@RabbitAvailable`:
`RABBITMQ_TEST_HOSTNAME`, `RABBITMQ_TEST_PORT`, `RABBITMQ_TEST_USER`, `RABBITMQ_TEST_PASSWORD`.

A local broker can be started, for instance, with:

```
$ docker run -d -p 5672:5672 -p 15672:15672 rabbitmq:3-management
```
