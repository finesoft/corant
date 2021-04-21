/*
 * Copyright (c) 2013-2021, Bingo.Chen (finesoft@gmail.com).
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

import static org.corant.shared.util.Assertions.shouldNotNull;
import static org.corant.shared.util.Classes.defaultClassLoader;
import static org.corant.shared.util.Conversions.toLong;
import static org.corant.shared.util.Conversions.toObject;
import static org.corant.shared.util.Objects.defaultObject;
import static org.corant.shared.util.Strings.defaultString;
import static org.corant.shared.util.Strings.split;
import static org.corant.shared.util.Validates.isValidMacAddress;
import java.io.File;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Enumeration;
import java.util.Optional;
import java.util.Random;
import javax.management.MBeanServer;
import javax.management.ObjectName;
import com.sun.management.HotSpotDiagnosticMXBean;

/**
 * corant-shared
 *
 * @author bingo 下午6:31:17
 *
 */
public class Systems {

  private Systems() {}

  public static void dumpHeap(String filePath, boolean live) throws IOException {
    MBeanServer server = ManagementFactory.getPlatformMBeanServer();
    HotSpotDiagnosticMXBean mxBean = ManagementFactory.newPlatformMXBeanProxy(server,
        "com.sun.management:type=HotSpotDiagnostic", HotSpotDiagnosticMXBean.class);
    mxBean.dumpHeap(filePath, live);
  }

  public static long getAvailableMemory() {
    return Runtime.getRuntime().maxMemory() - Runtime.getRuntime().totalMemory()
        + Runtime.getRuntime().freeMemory();
  }

  public static int getCPUs() {
    return Runtime.getRuntime().availableProcessors();
  }

  public static long getCurrentPID() {
    return Long.parseLong(ManagementFactory.getRuntimeMXBean().getName().split("@")[0]);
  }

  public static String getFileEncoding() {
    return getSystemProperty("file.encoding");
  }

  public static String getFileSeparator() {
    return getSystemProperty("file.separator");
  }

  public static long getFreeMemory() {
    return Runtime.getRuntime().freeMemory();
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

  public static String getJavaHomeDir() {
    return getSystemProperty("java.home");
  }

  public static String getJavaSpecificationName() {
    return getSystemProperty("java.specification.name");
  }

  public static String getJavaSpecificationVendor() {
    return getSystemProperty("java.specification.vendor");
  }

  public static String getJavaSpecificationVersion() {
    return getSystemProperty("java.specification.version");
  }

  public static String getJavaVendor() {
    return getSystemProperty("java.vendor");
  }

  public static String getJavaVendorURL() {
    return getSystemProperty("java.vendor.url");
  }

  public static String getJavaVersion() {
    return getSystemProperty("java.version");
  }

  public static int getJavaVersionNumber() {
    String javaVersion = getJavaVersion();
    final int lastDashNdx = javaVersion.lastIndexOf('-');
    if (lastDashNdx != -1) {
      javaVersion = javaVersion.substring(0, lastDashNdx);
    }
    if (javaVersion.startsWith("1.")) {
      final int index = javaVersion.indexOf('.', 2);
      return Integer.parseInt(javaVersion.substring(2, index));
    } else {
      final int index = javaVersion.indexOf('.');
      return Integer.parseInt(index == -1 ? javaVersion : javaVersion.substring(0, index));
    }
  }

  public static String getJvmInfo() {
    return getSystemProperty("java.vm.info");
  }

  public static String getJvmName() {
    return getSystemProperty("java.vm.name");
  }

  public static String getJvmSpecificationName() {
    return getSystemProperty("java.vm.specification.name");
  }

  public static String getJvmSpecificationVendor() {
    return getSystemProperty("java.vm.specification.vendor");
  }

  public static String getJvmSpecificationVersion() {
    return getSystemProperty("java.vm.specification.version");
  }

  public static String getJvmVendor() {
    return getSystemProperty("java.vm.vendor");
  }

  public static String getJvmVersion() {
    return getSystemProperty("java.vm.version");
  }

  public static String getLineSeparator() {
    return getSystemProperty("line.separator");
  }

  public static byte[] getMacAddress() throws SocketException {
    Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces();
    while (en.hasMoreElements()) {
      NetworkInterface nint = en.nextElement();
      if (!nint.isLoopback()) {
        // Pick the first valid non loopback address we find
        byte[] address = nint.getHardwareAddress();
        if (isValidMacAddress(address)) {
          return address;
        }
      }
    }
    // Could not find a mac address
    return Bytes.EMPTY_ARRAY;
  }

  public static long getMaxMemory() {
    return Runtime.getRuntime().maxMemory();
  }

  public static String getOsArchitecture() {
    return getSystemProperty("os.arch");
  }

  public static String getOsName() {
    return getSystemProperty("os.name");
  }

  public static String getOsVersion() {
    return getSystemProperty("os.version");
  }

  public static String getPathSeparator() {
    return getSystemProperty("path.separator");
  }

  public static Optional<Long> getPhysicalMemory() {
    try {
      Object attribute = ManagementFactory.getPlatformMBeanServer().getAttribute(
          new ObjectName("java.lang", "type", "OperatingSystem"), "TotalPhysicalMemorySize");
      if (attribute != null) {
        return Optional.of(toLong(attribute.toString()));
      }
    } catch (Exception e) {
    }
    return Optional.empty();
  }

  public static byte[] getSecureMungedAddress(Random rd) {
    byte[] address = null;
    try {
      address = getMacAddress();
    } catch (SocketException e) {
      // address will be set below
    }

    if (!isValidMacAddress(address)) {
      address = constructDummyMulticastAddress(rd);
    }

    byte[] mungedBytes = new byte[6];
    rd.nextBytes(mungedBytes);
    for (int i = 0; i < 6; ++i) {
      mungedBytes[i] ^= address[i];
    }

    return mungedBytes;
  }

  public static String[] getSystemClasspath() {
    return split(getSystemProperty("java.class.path"), File.pathSeparator);
  }

  public static String getSystemProperty(final String name) {
    return getSystemProperty(name, (String) null);
  }

  public static <T> T getSystemProperty(final String name, Class<T> type) {
    return toObject(getSystemProperty(name), type);
  }

  public static <T> T getSystemProperty(final String name, Class<T> type, T defaultValue) {
    return defaultObject(getSystemProperty(name, type), defaultValue);
  }

  public static String getSystemProperty(final String name, final String defaultValue) {
    shouldNotNull(name);
    String value = null;
    try {
      if (System.getSecurityManager() == null) {
        value = System.getProperty(name);
      } else {
        value = AccessController
            .doPrivileged((PrivilegedAction<String>) () -> System.getProperty(name));
      }
    } catch (final Exception ignore) {
    }
    return defaultString(value, defaultValue);
  }

  public static String getTempDir() {
    return getSystemProperty("java.io.tmpdir");
  }

  public static long getTotalMemory() {
    return Runtime.getRuntime().totalMemory();
  }

  public static long getUsedMemory() {
    return Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
  }

  public static String getUserCountry() {
    return defaultObject(getSystemProperty("user.country"),
        () -> getSystemProperty("user.country"));
  }

  public static String getUserHome() {
    return getSystemProperty("user.home");
  }

  public static String getUserLanguage() {
    return getSystemProperty("user.language");
  }

  public static String getUserName() {
    return getSystemProperty("user.name");
  }

  public static String getWorkingDir() {
    return getSystemProperty("user.dir");
  }

  public static boolean isAix() {
    return detectOS("AIX");
  }

  public static boolean isAndroid() {
    try {
      Class.forName("android.app.Application", false, defaultClassLoader());
      return true;
    } catch (Exception e) {
      return false;
    }
  }

  public static boolean isHpUx() {
    return detectOS("HP-UX");
  }

  public static boolean isIrix() {
    return detectOS("Irix");
  }

  public static boolean isLinux() {
    return detectOS("Linux") || detectOS("LINUX");
  }

  public static boolean isMac() {
    return detectOS("Mac");
  }

  public static boolean isMacOsX() {
    return detectOS("Mac OS X");
  }

  public static boolean isOs2() {
    return detectOS("OS/2");
  }

  public static boolean isSolaris() {
    return detectOS("Solaris");
  }

  public static boolean isSunOS() {
    return detectOS("SunOS");
  }

  public static boolean isWindows() {
    return detectOS("Windows");
  }

  public static boolean isWindows10() {
    return detectOS("Windows", "10");
  }

  public static boolean isWindows2000() {
    return detectOS("Windows", "5.0");
  }

  public static boolean isWindows95() {
    return detectOS("Windows", "4.0");
  }

  public static boolean isWindows98() {
    return detectOS("Windows", "4.1");
  }

  public static boolean isWindowsME() {
    return detectOS("Windows", "4.9");
  }

  public static boolean isWindowsNT() {
    return detectOS("Windows NT");
  }

  public static boolean isWindowsServer() {
    return detectOS("Windows Server");
  }

  public static boolean isWindowsServer2016() {
    return detectOS("Windows Server", "2016");
  }

  public static boolean isWindowsServer2019() {
    return detectOS("Windows Server", "2019");
  }

  public static boolean isWindowsXP() {
    return detectOS("Windows", "5.1");
  }

  static boolean detectOS(final String osNamePrefix) {
    String osn = getSystemProperty("os.name");
    return osn != null && osn.startsWith(osNamePrefix);
  }

  static boolean detectOS(final String osNamePrefix, final String osVersionPrefix) {
    String osn = getSystemProperty("os.name");
    String osv = getSystemProperty("os.version");
    return osn != null && osv != null && osn.startsWith(osNamePrefix)
        && osv.startsWith(osVersionPrefix);
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
