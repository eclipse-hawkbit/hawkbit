---
layout: documentation
title: Getting Started
---

{% include base.html %}

# hawkBit sandbox

We offer a sandbox installation that is free for everyone to try out hawkBit. However, keep in mind that the sandbox database will be reset from time to time. It is also not possible to upload any artifacts into the sandbox. But you can use it to try out the Management UI, Management API and DDI API.

[hawkbit-sandbox](https://hawkbit.eu-gb.mybluemix.net/UI/)

# Compile, Run and Getting Started

#### Clone and build hawkBit
{% highlight bash %}
$ git clone https://github.com/eclipse/hawkbit.git
$ cd hawkbit
$ mvn clean install
{% endhighlight %}

#### Start hawkBit update server
[Example Application](https://github.com/eclipse/hawkbit/tree/master/examples/hawkbit-example-app)

{% highlight bash %}
$ java -jar ./hawkbit-runtime/hawkbit-update-server/target/hawkbit-update-server-#version#-SNAPSHOT.jar
{% endhighlight %}

#### Build hawkBit examples
{% highlight bash %}
$ git clone https://github.com/eclipse/hawkbit.git
$ cd hawkbit/examples
$ mvn clean install
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
