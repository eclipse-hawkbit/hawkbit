# hawkBit JPA EclipseLink Vendor integration

Implementation of [EclipseLink](http://www.eclipse.org/eclipselink/) JPA vendor.

Since there seem to be bug in eclipselink static weaver or eclipselink-maven-plugin - don't weave properly the abstract classes when no non-abstract entity into the maven module - we use to have fake entity in the module to make it work.