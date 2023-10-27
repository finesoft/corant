# Corant

[![License](https://img.shields.io/:license-Apache2-blue.svg)](http://www.apache.org/licenses/LICENSE-2.0)
[![Codacy Badge](https://api.codacy.com/project/badge/Grade/7cfd816c14d94bbd9d34198fa0a306ab)](https://app.codacy.com/manual/finesoft/corant?utm_source=github.com&utm_medium=referral&utm_content=finesoft/corant&utm_campaign=Badge_Grade_Dashboard) [![Join the chat at https://gitter.im/corant-project/community](https://badges.gitter.im/corant-project/community.svg)](https://gitter.im/corant-project/community?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge)

Microservice stack with CDI MicroProfile.

Corant is a microservice development stack. Use Maven for builds, base on JDK 11 or above
(JDK 8 will no longer be supported, only supported for versions before [1.3](https://github.com/finesoft/corant/tree/1.3)), use CDI as container.
The following projects are under the corant project.
# Corant projects
### 1. corant-boms
```text
Maven dependence descriptions that are used in corant.
```
### 2. corant-parent
```text
Maven build descriptions
```
### 3. corant-devops
```text
Corant build and testing project. include maven build plugins and framework testing.
``` 
### 4. corant-shared
```text
The global shared project, contains utility classes for resource handling, 
string handling, reflection, object conversion, assertion, etc. Most projects rely on this.    
``` 
### 5. corant-config
```text
The application configuration processing that implemented the microprofile-config specification.
``` 
### 6. corant-kernel
```text
Initialize the application and the container, provide related operations such as boot/startup/shutdown, 
and related pre-processing and post-processing.
``` 
### 7. corant-context 
```text
Container, context and concurrent processing module, providing some convenient context processing classes 
and managed executor service. This module will be fully compatible with microprofile-context-propagation 
in the future.
```
### 8. corant-modules
```text
Extension project. This project includes many subprojects, integrate JEE or other open source
 components to make integration and development easier.
Currently we have supported specifications JTA/JPA/JMS/JNDI/JAXRS/SERVLET/JCACHE/MICROPROFILES ,etc.,
supported components or frameworks include ElasticSearch/Mongodb/Redis/Undertow, etc.,
and also provide some common development practices such as DDD, Dynamic SQL/NoSQL query framework, etc.
``` 


# Getting started
Learn how to create a Hello World app quickly and efficiently.
## 1. Prerequisites
* maven 2.2.1 (or newer)
* JDK 8 or 11+
## 2. Create the project
* Define the version of corant in `pom.xml`.Use the latest version(required jdk 11+).Jdk 8 for the version: [1.3](https://github.com/finesoft/corant/tree/1.3)
```
<properties>
     <version.corant>XXXX-SNAPSHOT</version.corant>
</properties>
```
* Now add `corant-kernel` dependency. Initialize the application and container.
```
 <dependency>
    <groupId>org.corant</groupId>
    <artifactId>corant-kernel</artifactId>
    <version>${version.corant}</version>
 </dependency>
``` 
* Add HTTP server. We prefer to use Undertow.
```
<dependency>
    <groupId>org.corant</groupId>
    <artifactId>corant-modules-webserver-undertow</artifactId>
    <version>${version.corant}</version>
</dependency>
```
This is the bare minimum required to run an app.We don't need to include a web.xml file as like application servers Corant supports annotation scanning.
```java
@ApplicationScoped
public class App  {
  public static void main(String[] args) {
    Corant.startup(args);
  }
}
```
## 3. Add JAX-RS component
* Add dependency.
```
<dependency>
    <groupId>org.corant</groupId>
    <artifactId>corant-modules-jaxrs-resteasy</artifactId>
    <version>${version.corant}</version>
</dependency>
```
* Extends the `javax.ws.rs.core.Application`.
```java
@ApplicationScoped
@ApplicationPath("/")
public class App extends Application {
  public static void main(String[] args) {
    Corant.startup(args);
  }
}
```
* Add a REST endpoint.
```java
@Path("/app")
@ApplicationScoped
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class HelloWorldEndpoint {
  @Path("/greeting")
  @GET
  public Response hello() {
    return Response.ok("Hello World!").build();
  }
}
```
* Run the app and visit http://localhost:8080/app/greeting and you should see the following.
```
Hello World!
```
Congratulations you have created a simple and lightweight Java EE app.

[For some demo projects, please check](https://github.com/sushuaihao/corant-demo)

Notices: 
This codebase is only used for learning or reference, not in the production environment; 
If you use the codebase in a production environment or redistributeit, any problems arising therefrom are irrelevant to us, 
we do not assume any legal responsibility.
