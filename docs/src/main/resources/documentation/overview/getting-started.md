---
layout: documentation
title: Getting Started
---

{% include base.html %}

# hawkBit sandbox

We offer a sandbox installation that is free for everyone to try out hawkBit. However, keep in mind that the sandbox database will be reset from time to time. It is also not possible to upload any artifacts into the sandbox. But you can use it to try out the Management UI, Management API and DDI API.

[hawkbit-sandbox](https://hawkbit.eu-gb.mybluemix.net/UI/)

# Compile, Run and Getting Started

We are not providing an off the shelf installation ready hawkBit update server. However, we recommend to check out the [Example Application](examples/hawkbit-example-app) for a runtime ready Spring Boot based update server that is empowered by hawkBit. In addition we have [guide](https://github.com/eclipse/hawkbit/wiki/Run-hawkBit) for setting up a complete landscape.

#### Clone and build hawkBit
{% highlight bash %}
$ git clone https://github.com/eclipse/hawkbit.git
$ cd hawkbit
$ mvn clean install
{% endhighlight %}

#### Start hawkBit example app
[Example Application](https://github.com/eclipse/hawkbit/tree/master/examples/hawkbit-example-app)

{% highlight bash %}
$ java -jar ./examples/hawkbit-example-app/target/hawkbit-example-app-#version#.jar
{% endhighlight %}

#### Start hawkBit device simulator
[Device Simulator](https://github.com/eclipse/hawkbit/tree/master/examples/hawkbit-device-simulator)
{% highlight bash %}
$ java -jar ./examples/hawkbit-device-simulator/target/hawkbit-device-simulator-#version#.jar
{% endhighlight %}

#### Generate Getting Started data
[Example Management API Client](https://github.com/eclipse/hawkbit/tree/master/examples/hawkbit-example-mgmt-simulator)
{% highlight bash %}
$ java -jar ./examples/hawkbit-example-mgmt-simulator/target/hawkbit-example-mgmt-simulator-#version#.jar
{% endhighlight %}