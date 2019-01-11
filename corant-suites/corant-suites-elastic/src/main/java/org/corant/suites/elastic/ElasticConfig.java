/*
 * Copyright (c) 2013-2018, Bingo.Chen (finesoft@gmail.com).
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package org.corant.suites.elastic;

import java.time.Duration;
import java.util.Optional;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;

/**
 * corant-suites-elastic
 *
 * @author bingo 上午11:54:10
 *
 */
@ApplicationScoped
public class ElasticConfig {

  public static final String PREFIX = "elastic.";

  @Inject
  @ConfigProperty(name = "elastic.cluster.name", defaultValue = "elasticsearch")
  private String clusterName;

  @Inject
  @ConfigProperty(name = "elastic.cluster.nodes")
  private Optional<String> clusterNodes;

  /**
   * A bind port range. Defaults to 9300-9400
   */
  @Inject
  @ConfigProperty(name = "elastic.transport.tcp.port")
  private String transportTcpPort;

  /**
   * The port that other nodes in the cluster should use when communicating with this node. Useful
   * when a cluster node is behind a proxy or firewall and the transport.tcp.port is not directly
   * addressable from the outside. Defaults to the actual port assigned via transport.tcp.port.
   */
  @Inject
  @ConfigProperty(name = "elastic.transport.publish_port")
  private String transportPublishPort;

  /**
   * The host address to bind the transport service to. Defaults to transport.host (if set) or
   * network.bind_host.
   */
  @Inject
  @ConfigProperty(name = "elastic.transport.bind_host")
  private String transportBindHost;


  /**
   * The host address to publish for nodes in the cluster to connect to. Defaults to transport.host
   * (if set) or network.publish_host.
   */
  @Inject
  @ConfigProperty(name = "elastic.transport.publish_host")
  private String transportPublishHost;

  /**
   * Used to set the transport.bind_host and the transport.publish_host Defaults to transport.host
   * or network.host.
   */
  @Inject
  @ConfigProperty(name = "elastic.transport.host")
  private String transportHost;

  /**
   * The socket connect timeout setting (in time setting format). Defaults to 30s.
   */
  @Inject
  @ConfigProperty(name = "elastic.transport.tcp.connect_timeout")
  private int transportTcpConnectionTimeout;

  /**
   * Set to true to enable compression (DEFLATE) between all nodes. Defaults to false.
   */
  @Inject
  @ConfigProperty(name = "elastic.transport.tcp.compress")
  private Boolean transportTcpCompress;

  /**
   * Schedule a regular application-level ping message to ensure that transport connections between
   * nodes are kept alive. Defaults to 5s in the transport client and -1 (disabled) elsewhere. It is
   * preferable to correctly configure TCP keep-alives instead of using this feature, because TCP
   * keep-alives apply to all kinds of long-lived connection and not just to transport connections.
   */
  @Inject
  @ConfigProperty(name = "elastic.transport.ping_schedule")
  private Duration transportPingSchedule;

  /**
   * 
   * @return the clusterName
   */
  public String getClusterName() {
    return clusterName;
  }

  /**
   * 
   * @return the clusterNodes
   */
  public Optional<String> getClusterNodes() {
    return clusterNodes;
  }

  /**
   * 
   * @return the transportBindHost
   */
  public String getTransportBindHost() {
    return transportBindHost;
  }

  /**
   * 
   * @return the transportHost
   */
  public String getTransportHost() {
    return transportHost;
  }

  /**
   * 
   * @return the transportPingSchedule
   */
  public Duration getTransportPingSchedule() {
    return transportPingSchedule;
  }

  /**
   * 
   * @return the transportPublishHost
   */
  public String getTransportPublishHost() {
    return transportPublishHost;
  }

  /**
   * 
   * @return the transportPublishPort
   */
  public String getTransportPublishPort() {
    return transportPublishPort;
  }

  /**
   * 
   * @return the transportTcpCompress
   */
  public Boolean getTransportTcpCompress() {
    return transportTcpCompress;
  }

  /**
   * 
   * @return the transportTcpConnectionTimeout
   */
  public int getTransportTcpConnectionTimeout() {
    return transportTcpConnectionTimeout;
  }

  /**
   * 
   * @return the transportTcpPort
   */
  public String getTransportTcpPort() {
    return transportTcpPort;
  }

  /**
   *
   * @param clusterName the clusterName to set
   */
  public void setClusterName(String clusterName) {
    this.clusterName = clusterName;
  }

  /**
   *
   * @param clusterNodes the clusterNodes to set
   */
  public void setClusterNodes(Optional<String> clusterNodes) {
    this.clusterNodes = clusterNodes;
  }

  /**
   *
   * @param transportBindHost the transportBindHost to set
   */
  public void setTransportBindHost(String transportBindHost) {
    this.transportBindHost = transportBindHost;
  }

  /**
   *
   * @param transportHost the transportHost to set
   */
  public void setTransportHost(String transportHost) {
    this.transportHost = transportHost;
  }

  /**
   *
   * @param transportPingSchedule the transportPingSchedule to set
   */
  public void setTransportPingSchedule(Duration transportPingSchedule) {
    this.transportPingSchedule = transportPingSchedule;
  }

  /**
   *
   * @param transportPublishHost the transportPublishHost to set
   */
  public void setTransportPublishHost(String transportPublishHost) {
    this.transportPublishHost = transportPublishHost;
  }

  /**
   *
   * @param transportPublishPort the transportPublishPort to set
   */
  public void setTransportPublishPort(String transportPublishPort) {
    this.transportPublishPort = transportPublishPort;
  }

  /**
   *
   * @param transportTcpCompress the transportTcpCompress to set
   */
  public void setTransportTcpCompress(Boolean transportTcpCompress) {
    this.transportTcpCompress = transportTcpCompress;
  }

  /**
   *
   * @param transportTcpConnectionTimeout the transportTcpConnectionTimeout to set
   */
  public void setTransportTcpConnectionTimeout(int transportTcpConnectionTimeout) {
    this.transportTcpConnectionTimeout = transportTcpConnectionTimeout;
  }

  /**
   *
   * @param transportTcpPort the transportTcpPort to set
   */
  public void setTransportTcpPort(String transportTcpPort) {
    this.transportTcpPort = transportTcpPort;
  }


}
