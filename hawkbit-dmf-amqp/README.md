# hawkBit device management federation API - implementation

This is the AMQP implementation for the device management federation API. The implementation uses the spring-amqp project.

# Integration Test

This modules contains some integration tests for the device management federation API implementation which uses a RabbitMQ. If there is no RabbitMQ running on the system, all tests will be marked as skipped. You can disable this rule and the tests will fail if there is no RabbitMQ running. n order to disable the rule at runtime, set an environment variable RABBITMQ_SERVER_REQUIRED=true.
The default RabbitMQ hostname is localhost. To set another hostname, set the property spring.rabbitmq.host to the new hostname.
The default RabbitMQ username is guest. To set another username, set the property spring.rabbitmq.username to the new username.
The default RabbitMQ password is guest. To set another password, set the property spring.rabbitmq.password to the new password.