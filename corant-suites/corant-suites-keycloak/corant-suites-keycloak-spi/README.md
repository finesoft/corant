

# Config keycloak(6.0.1 Wildfly 8.0) jms module 
  Open standalong.xml 
  # 1. config extensions to add jms module.
  ```
  <extensions>
        .....
        <extension module="org.wildfly.extension.messaging-activemq"/> <!--add jms module-->
  </extensions>
  ```
  # 2. config subsystem
  ```
  <subsystem xmlns="urn:jboss:domain:messaging-activemq:2.0">
    <server name="default">
        <security-setting name="#">
            <role name="guest" send="true" consume="true" create-non-durable-queue="true" delete-non-durable-queue="true"/>
        </security-setting>
        <address-setting name="#" dead-letter-address="jms.queue.DLQ" expiry-address="jms.queue.ExpiryQueue" max-size-bytes="10485760" page-size-bytes="2097152" message-counter-history-day-limit="10"/>
        <http-connector name="http-connector" socket-binding="http" endpoint="http-acceptor"/>
        <http-connector name="http-connector-throughput" socket-binding="http" endpoint="http-acceptor-throughput">
            <param name="batch-delay" value="50"/>
        </http-connector>
        <remote-connector name="remote-artemis" socket-binding="remote-artemis"/>
        <in-vm-connector name="in-vm" server-id="0"/>
        <http-acceptor name="http-acceptor" http-listener="default"/>
        <http-acceptor name="http-acceptor-throughput" http-listener="default">
            <param name="batch-delay" value="50"/>
            <param name="direct-deliver" value="false"/>
        </http-acceptor>
        <in-vm-acceptor name="in-vm" server-id="0"/>
        <jms-queue name="ExpiryQueue" entries="java:/jms/queue/ExpiryQueue"/>
        <jms-queue name="KeycloakEventTopic" entries="topic/KeycloakEventTopic java:jboss/exported/jms/topic/KeycloakEventTopic"/>
        <jms-queue name="DLQ" entries="java:/jms/queue/DLQ"/>
        <connection-factory name="InVmConnectionFactory" entries="java:/ConnectionFactory" connectors="in-vm"/>
        <connection-factory name="RemoteConnectionFactory" entries="java:jboss/exported/jms/RemoteConnectionFactory" connectors="http-connector"/>
        <pooled-connection-factory name="activemq-ra" entries="java:/JmsXA java:jboss/DefaultJMSConnectionFactory" connectors="in-vm" transaction="xa"/>
        <pooled-connection-factory name="remote-artemis" entries="java:/jms/remoteArtemis java:jboss/exported/jms/remoteArtemis" connectors="remote-artemis" user="sa" password="cn.cps"/>
    </server>
</subsystem>
  ```
  # 3. config socket binding
   ```
  <socket-binding-group name="standard-sockets" default-interface="public" port-offset="${jboss.socket.binding.port-offset:0}">
      ....
      <outbound-socket-binding name="remote-artemis"> <!--name is subsystem/server/remote-connector-->
            <remote-destination host="localhost" port="61616"/><!--host and port is outside jms server host and port-->
        </outbound-socket-binding>    
   </socket-binding>
    ```
