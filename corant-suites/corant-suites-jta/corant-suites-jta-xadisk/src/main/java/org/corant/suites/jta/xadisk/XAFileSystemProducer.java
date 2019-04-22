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
package org.corant.suites.jta.xadisk;

import java.util.Optional;
import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;
import org.corant.shared.normal.Defaults;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.xadisk.bridge.proxies.interfaces.XAFileSystem;
import org.xadisk.bridge.proxies.interfaces.XAFileSystemProxy;
import org.xadisk.filesystem.standalone.StandaloneFileSystemConfiguration;

/**
 * corant-suites-jta-xadisk
 *
 * Unfinished yet
 *
 * @author bingo 上午1:08:34
 *
 */
@ApplicationScoped
public class XAFileSystemProducer {

  protected static final String defaultDiskHome = Defaults.corantUserDir("xadisk").toString();

  @Inject
  @ConfigProperty(name = "jta.xadisk.disk-home")
  private Optional<String> diskHome;

  @Inject
  @ConfigProperty(name = "jta.xadisk.instance-id", defaultValue = "instance-1")
  private String instanceId;

  @Inject
  @ConfigProperty(name = "jta.xadisk.work-manager-core-pool-size", defaultValue = "10")
  private Integer workManagerCorePoolSize;

  @Inject
  @ConfigProperty(name = "jta.xadisk.work-manager-max-pool-size", defaultValue = "1024")
  private Integer workManagerMaxPoolSize;

  @Inject
  @ConfigProperty(name = "jta.xadisk.work-manager-keep-alive-time", defaultValue = "60")
  private Long workManagerKeepAliveTime;

  @Inject
  @ConfigProperty(name = "jta.xadisk.direct-buffer-pool-size", defaultValue = "1000")
  private Integer directBufferPoolSize;

  @Inject
  @ConfigProperty(name = "jta.xadisk.nondirect-buffer-pool-size", defaultValue = "1000")
  private Integer nonDirectBufferPoolSize;

  @Inject
  @ConfigProperty(name = "jta.xadisk.buffer-size", defaultValue = "4096")
  private Integer bufferSize;

  @Inject
  @ConfigProperty(name = "jta.xadisk.transaction-log-file-max-size", defaultValue = "1000000000")
  private Long transactionLogFileMaxSize;

  @Inject
  @ConfigProperty(name = "jta.xadisk.cumulative-buffer-size-for-disk-write",
      defaultValue = "1000000")
  private Integer cumulativeBufferSizeForDiskWrite;

  @Inject
  @ConfigProperty(name = "jta.xadisk.direct-buffer-idle-time", defaultValue = "100")
  private Integer directBufferIdleTime;

  @Inject
  @ConfigProperty(name = "jta.xadisk.nondirect-buffer-idle-time", defaultValue = "100")
  private Integer nonDirectBufferIdleTime;

  @Inject
  @ConfigProperty(name = "jta.xadisk.buffer-pool-reliever-interval", defaultValue = "60")
  private Integer bufferPoolRelieverInterval;

  @Inject
  @ConfigProperty(name = "jta.xadisk.max-nonpooled-buffer-size", defaultValue = "1000000")
  private Long maxNonPooledBufferSize;

  @Inject
  @ConfigProperty(name = "jta.xadisk.dead-lock-detector-interval", defaultValue = "30")
  private Integer deadLockDetectorInterval;

  @Inject
  @ConfigProperty(name = "jta.xadisk.lock-timeout", defaultValue = "10000")
  private Integer lockTimeOut;

  @Inject
  @ConfigProperty(name = "jta.xadisk.max-concurrent-event-deliveries", defaultValue = "20")
  private Integer maximumConcurrentEventDeliveries;

  @Inject
  @ConfigProperty(name = "jta.xadisk.transaction-timeout", defaultValue = "60")
  private Integer transactionTimeout;

  @Inject
  @ConfigProperty(name = "jta.xadisk.enable-remote-invocations", defaultValue = "false")
  private Boolean enableRemoteInvocations;

  @Inject
  @ConfigProperty(name = "jta.xadisk.server-host", defaultValue = "127.0.0.1")
  private String serverHost;

  @Inject
  @ConfigProperty(name = "jta.xadisk.server-port", defaultValue = "9999")
  private Integer serverPort;

  @Inject
  @ConfigProperty(name = "jta.xadisk.synchronize-directory-changes", defaultValue = "true")
  private Boolean synchronizeDirectoryChanges;

  @Inject
  @ConfigProperty(name = "jta.xadisk.enable-cluster-mode", defaultValue = "false")
  private Boolean enableClusterMode;

  @Inject
  @ConfigProperty(name = "jta.xadisk.cluster-master-host")
  private Optional<String> clusterMasterHost;

  @Inject
  @ConfigProperty(name = "jta.xadisk.cluster-master-port")
  private Optional<Integer> clusterMasterPort;

  StandaloneFileSystemConfiguration config;

  @PostConstruct
  void onPostConstruct() {
    config = new StandaloneFileSystemConfiguration(diskHome.orElse(defaultDiskHome), instanceId);
    config.setBufferPoolRelieverInterval(bufferPoolRelieverInterval);
    config.setBufferSize(bufferSize);
    clusterMasterHost.ifPresent(config::setClusterMasterAddress);
    clusterMasterPort.ifPresent(config::setClusterMasterPort);
    config.setCumulativeBufferSizeForDiskWrite(cumulativeBufferSizeForDiskWrite);
    config.setDeadLockDetectorInterval(deadLockDetectorInterval);
    config.setDirectBufferIdleTime(directBufferIdleTime);
    config.setDirectBufferPoolSize(directBufferPoolSize);
    config.setEnableClusterMode(enableClusterMode);
    config.setEnableRemoteInvocations(enableRemoteInvocations);
    config.setInstanceId(instanceId);
    config.setLockTimeOut(lockTimeOut);
    config.setMaximumConcurrentEventDeliveries(maximumConcurrentEventDeliveries);
    config.setMaxNonPooledBufferSize(maxNonPooledBufferSize);
    config.setNonDirectBufferIdleTime(nonDirectBufferIdleTime);
    config.setNonDirectBufferPoolSize(nonDirectBufferPoolSize);
    config.setServerAddress(serverHost);
    config.setServerPort(serverPort);
    config.setSynchronizeDirectoryChanges(synchronizeDirectoryChanges);
    config.setTransactionLogFileMaxSize(transactionLogFileMaxSize);
    config.setTransactionTimeout(transactionTimeout);
    config.setWorkManagerCorePoolSize(workManagerCorePoolSize);
    config.setWorkManagerKeepAliveTime(workManagerKeepAliveTime);
    config.setWorkManagerMaxPoolSize(workManagerMaxPoolSize);
  }

  @Produces
  @ApplicationScoped
  XAFileSystem produceNative() {
    return XAFileSystemProxy.bootNativeXAFileSystem(config);
  }
}
