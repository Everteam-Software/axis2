======================================================
Apache Axis2 @axisVersion@ build (@TODAY@)
Binary Release

http://ws.apache.org/axis2
------------------------------------------------------

This is the Standard Binary Release of Axis2.

The lib directory contains;

1. axis2-adb-@axisVersion@.jar
2. axis2-adb-codegen-@axisVersion@.jar
3. axis2-ant-plugin-@axisVersion@.jar
4. axis2-codegen-@axisVersion@.jar
5. axis2-fastinfoset-@axisVersion@.jar
6. axis2-java2wsdl-@axisVersion@.jar
7. axis2-jaxbri-@axisVersion@.jar
8. axis2-jaxws-@axisVersion@.jar
9. axis2-jaxws-api-@axisVersion@.jar
10. axis2-jibx-@axisVersion@.jar
11. axis2-json-@axisVersion@.jar
12. axis2-jws-api-@axisVersion@.jar
13. axis2-kernel-@axisVersion@.jar
14. axis2-metadata-@axisVersion@.jar
15. axis2-saaj-@axisVersion@.jar
16. axis2-saaj-api-@axisVersion@.jar
17. axis2-soapmonitor-@axisVersion@.jar
18. axis2-spring-@axisVersion@.jar
19. axis2-xmlbeans-@axisVersion@.jar

and all 3rd party distributable dependencies of the above jars.

The repository/modules directory contains the deployable addressing module.

The webapp folder contains an ant build script to generate the axis2.war out of this distribution.
(This requires Ant 1.6.5 or later)

The samples directory contains all the Axis2 samples which demonstrates some of the key features of
Axis2. It also contains a few samples relevant to documents found in Axis2's Docs Distribution.

The bin directory contains a set of usefull scripts for the users.

The conf directory contains the axis2.xml file which allows to configure Axis2.

(Please note that this release does not include the other WS-* implementation modules, like
WS-Security, that are being developed within Axis2. Those can be downloaded from
http://ws.apache.org/axis2/modules/)
