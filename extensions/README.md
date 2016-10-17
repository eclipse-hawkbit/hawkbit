# hawkBit Extensions

hawkBit extensions are implementations to extend the functionality of hawkBit which are maintained by the hawkBit community. The extensions can be used to integrate in a hawkBit application to exchange or extend hawkBit functionality. Extensions should work with the hawkBit example application. All extensions provide a `README.md` which explains the use of the extension and how to use it.

hawkBit extensions are implementation which are not included in the default implementations of hawkBit's security and auto-configuration mechanism or extending functionality by e.g. integrating third-party services to hawkBit. 

hawkBit makes use of the spring-bean and configuration mechanism which allows an flexible configuration mechanism and exchanging beans at spring-configurations. Many beans are `@Conditional` annotated in hawkBit so they can be overwritten. Extensions can also leverage and implemented functionalities based on the hawkBit event mechanism by subscribing to events and implement additional functionality. 

### hawkBit Extension follows
* Containing a `README.md` which explains the extension in detail and how to use it
* Working with the hawkBit example application
* Following the maven-artifact-id `hawkbit-extension-<name>`
