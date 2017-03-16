# hawkBit device management federation API - implementation

This is the AMQP implementation for the device management federation API. The implementation uses the spring-amqp project.

# Integration Test

This modules contains some integration tests for the device management federation API implementation which uses a RabbitMQ. If there is no RabbitMQ run on the system, the test will marked as skipped. You can disable this rule and the tests will fail if there is no broker running. To disable the rule at runtime, set an environment variable RABBITMQ_SERVER_REQUIRED to true.
The default RabbitMQ host name is localhost. To set a another host name, set the property spring.rabbitmq.host to the new host name.
