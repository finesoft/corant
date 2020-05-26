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
package org.corant.shared.util;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;
import java.util.Random;

/**
 *
 * @author bingo 上午12:30:55
 *
 */
public class MacAddrUtils {

  private MacAddrUtils() {
    super();
  }

  public static long getIp4LongValue() {
    try {
      InetAddress inetAddress = InetAddress.getLocalHost();
      byte[] ip = inetAddress.getAddress();
      return Math.abs(ip[0] << 24 | ip[1] << 16 | ip[2] << 8 | ip[3]);
    } catch (Exception ex) {
      ex.printStackTrace();
      return 0;
    }
  }

  public static byte[] getMacAddress() throws SocketException {
    Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces();
    if (en != null) {
      while (en.hasMoreElements()) {
        NetworkInterface nint = en.nextElement();
        if (!nint.isLoopback()) {
          // Pick the first valid non loopback address we find
          byte[] address = nint.getHardwareAddress();
          if (isValidAddress(address)) {
            return address;
          }
        }
      }
    }
    // Could not find a mac address
    return new byte[0];
  }

  public static byte[] getSecureMungedAddress(Random rd) {
    byte[] address = null;
    try {
      address = getMacAddress();
    } catch (SocketException e) {
      // address will be set below
    }

    if (!isValidAddress(address)) {
      address = constructDummyMulticastAddress(rd);
    }

    byte[] mungedBytes = new byte[6];
    rd.nextBytes(mungedBytes);
    for (int i = 0; i < 6; ++i) {
      mungedBytes[i] ^= address[i];
    }

    return mungedBytes;
  }

  public static boolean isValidAddress(byte[] address) {
    if (address == null || address.length != 6) {
      return false;
    }
    for (byte b : address) {
      if (b != 0x00) {
        return true; // If any of the bytes are non zero assume a good address
      }
    }
    return false;
  }

  private static byte[] constructDummyMulticastAddress(Random rd) {
    byte[] dummy = new byte[6];
    rd.nextBytes(dummy);
    /*
     * Set the broadcast bit to indicate this is not a _real_ mac address
     */
    dummy[0] |= (byte) 0x01;
    return dummy;
  }
}
