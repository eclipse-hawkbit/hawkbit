# hawkBit JPA API

JPA vendor independent base classes and utilities shared by the JPA vendor modules
(`hawkbit-repository-jpa-eclipselink`, `hawkbit-repository-jpa-hibernate`) and, through them, by
`hawkbit-repository-jpa`.

Contains:

- `EntityInterceptor`, `model.EntityInterceptorListener`, `model.helper.EntityInterceptorHolder` - entity lifecycle
  interceptors: an entity listener calling all registered interceptors, held in a singleton holder so they can be
  contributed as Spring beans
- `model.EventAwareEntity` - callbacks implemented by entities that fire create / update / delete events
- `executor.AfterTransactionCommitExecutor` - registers runnables to be executed after a successful Spring transaction
  commit
- `utils.JpaExceptionTranslator` - single point of SQL exception translation, used by both EclipseLink and Hibernate
  integrations
- `utils.ExceptionMapper` - maps exceptions thrown by the management classes to hawkBit management exceptions
- `utils.MapAttributeConverter` - base JPA `AttributeConverter` mapping enums to DB values via a map
