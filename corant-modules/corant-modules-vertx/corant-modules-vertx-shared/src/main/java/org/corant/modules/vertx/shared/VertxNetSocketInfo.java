/*
 * Copyright (c) 2013-2023, Bingo.Chen (finesoft@gmail.com).
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
package org.corant.modules.vertx.shared;

import static org.corant.shared.util.Assertions.shouldNotNull;
import java.util.Objects;
import io.vertx.core.net.NetSocket;
import io.vertx.core.net.SocketAddress;

/**
 * corant-modules-vertx-shared
 *
 * @author bingo 16:32:06
 */
public class VertxNetSocketInfo {

  private final String writeHandlerId;

  private final SocketAddress localSocketAddress;

  private final SocketAddress realLocalSocketAddress;

  private final SocketAddress remoteSocketAddress;

  private final SocketAddress realRemoteSocketAddress;

  private final int hash;

  public VertxNetSocketInfo(NetSocket socket) {
    shouldNotNull(socket);
    localSocketAddress = socket.localAddress(false);
    realLocalSocketAddress = socket.localAddress(true);
    remoteSocketAddress = socket.remoteAddress(false);
    realRemoteSocketAddress = socket.remoteAddress(true);
    writeHandlerId = socket.writeHandlerID();
    hash = Objects.hash(localSocketAddress, realLocalSocketAddress, realRemoteSocketAddress,
        remoteSocketAddress, writeHandlerId);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }
    VertxNetSocketInfo other = (VertxNetSocketInfo) obj;
    return Objects.equals(localSocketAddress, other.localSocketAddress)
        && Objects.equals(realLocalSocketAddress, other.realLocalSocketAddress)
        && Objects.equals(realRemoteSocketAddress, other.realRemoteSocketAddress)
        && Objects.equals(remoteSocketAddress, other.remoteSocketAddress)
        && Objects.equals(writeHandlerId, other.writeHandlerId);
  }

  public SocketAddress getLocalSocketAddress() {
    return localSocketAddress;
  }

  public SocketAddress getRealLocalSocketAddress() {
    return realLocalSocketAddress;
  }

  public SocketAddress getRealRemoteSocketAddress() {
    return realRemoteSocketAddress;
  }

  public SocketAddress getRemoteSocketAddress() {
    return remoteSocketAddress;
  }

  public String getWriteHandlerId() {
    return writeHandlerId;
  }

  @Override
  public int hashCode() {
    return hash;
  }

  @Override
  public String toString() {
    return "NetSocketInfo [localSocketAddress=" + localSocketAddress + ", remoteSocketAddress="
        + remoteSocketAddress + ", writeHandlerId=" + writeHandlerId + "]";
  }
}
