# spring-boot-sample-jetty-jsp
Spring Boot Jetty JSP Sample - embedded

This application that tries to overcome limitations specified in [Spring Boot Documentation 26.3.4 JSP limitations](http://docs.spring.io/spring-boot/docs/current/reference/html/boot-features-developing-web-applications.html#boot-features-jsp-limitations uses maven-shade-plugin) and [Support JSP with Embedded Jetty #367](https://github.com/spring-projects/spring-boot/issues/367).
Extracts from a jar file all JSP files and points Jetty to their location.
One thing to remember is that this project creates temporary directory that needs to be kept alive [http://www.rudder-project.org/redmine/issues/4473](http://www.rudder-project.org/redmine/issues/4473). To avoid that issue you can specify alternative temporary directory for Java (read link's content)
