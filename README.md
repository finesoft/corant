# corant

[![License](https://img.shields.io/:license-Apache2-blue.svg)](http://www.apache.org/licenses/LICENSE-2.0)
[![Codacy Badge](https://api.codacy.com/project/badge/Grade/7cfd816c14d94bbd9d34198fa0a306ab)](https://app.codacy.com/manual/finesoft/corant?utm_source=github.com&utm_medium=referral&utm_content=finesoft/corant&utm_campaign=Badge_Grade_Dashboard) [![Join the chat at https://gitter.im/corant-project/community](https://badges.gitter.im/corant-project/community.svg)](https://gitter.im/corant-project/community?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge)

Microservice stack with CDI MicroProfile.

Corant is a microservice development stack. Use Maven for builds, base on JDK 8 or above, use CDI as container.
The following projects are under the corant project.

# 1. corant-boms 
    Maven dependence descriptions that are used in corant.
# 2. corant-devops 
    Corant build and testing project. include maven build plugins and framework testing.
# 3. corant-shared 
    The global shared project, contains utility classes for resource handling, 
    string handling, reflection, object conversion, assertion, etc. Most projects rely on this.    
# 4. corant-config 
    The application configuration processing that implemented the microprofile-config specification.
# 5. corant-kernel 
    Initialize the application and the container, provide related operations such as boot/startup/shutdown, and related pre-processing and post-processing.
# 6. corant-context 
   Container and context processing module, providing some convenient context processing classes.   
# 7. corant-suites 
    Integration project. This project includes many subprojects that integrate JEE or other open source components
    to make integration and development easier. Currently we have supported specifications such as JTA/JPA/JMS/JNDI/JAXRS/SERVLET/JCACHE/MICROPROFILES ,etc., 
    supported components or  frameworks include ElasticSearch/Mongodb/Redis/Undertow, etc., and also provide some common development practices such as DDD, Dynamic SQL/NoSQL query framework, etc.


