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
package org.corant.modules.security.shared.crypto;

import static org.corant.shared.util.Assertions.shouldNotBlank;
import static org.corant.shared.util.Strings.defaultString;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.InputStream;
import java.security.GeneralSecurityException;
import java.security.Key;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.MessageDigest;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Security;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.Locale;
import java.util.Optional;
import java.util.logging.Logger;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.corant.shared.ubiquity.Tuple.Triple;
import org.corant.shared.util.Resources;
import org.corant.shared.util.Resources.URLResource;

/**
 * corant-modules-security-shared
 *
 * @author bingo 19:23:17
 *
 */
public class Keys {

  static {
    if (Security.getProvider("BC") == null) {
      Security.addProvider(new BouncyCastleProvider());
    }
  }

  static final Logger LOGGER = Logger.getLogger(Keys.class.getName());

  public static Triple<String, String, String> createJsonWebKeySet()
      throws GeneralSecurityException {
    KeyPair keyPair = generateKeyPair(2048, "RSA");
    String pubKey = Base64.getEncoder().encodeToString(keyPair.getPublic().getEncoded());
    String priKey = Base64.getEncoder().encodeToString(keyPair.getPrivate().getEncoded());
    String keyId = createKeyId(keyPair.getPrivate(), "SHA-256");
    return Triple.of(pubKey, priKey, keyId);
  }

  public static String createKeyId(Key key, String algo) throws GeneralSecurityException {
    return Base64.getEncoder().encodeToString(
        MessageDigest.getInstance(defaultString(algo, "SHA-256")).digest(key.getEncoded()));
  }

  public static PrivateKey decodePrivateKey(byte[] der, String algo)
      throws GeneralSecurityException {
    PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(der);
    KeyFactory kf = KeyFactory.getInstance(defaultString(algo, "RSA"), "BC");
    return kf.generatePrivate(spec);
  }

  public static PrivateKey decodePrivateKey(InputStream is, String algo) throws Exception {
    DataInputStream dis = new DataInputStream(is);
    byte[] keyBytes = new byte[dis.available()];
    dis.readFully(keyBytes);
    return decodePrivateKey(keyBytes, algo);
  }

  public static PrivateKey decodePrivateKey(String pem, String algo)
      throws GeneralSecurityException {
    return pem == null ? null : decodePrivateKey(pemToDer(pem), algo);
  }

  public static PublicKey decodePublicKey(byte[] der, String algo) throws GeneralSecurityException {
    X509EncodedKeySpec spec = new X509EncodedKeySpec(der);
    KeyFactory kf = KeyFactory.getInstance(defaultString(algo, "RSA"), "BC");
    return kf.generatePublic(spec);
  }

  public static PublicKey decodePublicKey(InputStream is, String algo) throws Exception {
    DataInputStream dis = new DataInputStream(is);
    byte[] keyBytes = new byte[dis.available()];
    dis.readFully(keyBytes);
    return decodePublicKey(keyBytes, algo);
  }

  public static PublicKey decodePublicKey(String pem, String algo) throws GeneralSecurityException {
    return pem == null ? null : decodePublicKey(pemToDer(pem), algo);
  }

  public static X509Certificate decodex509Certificate(String cert) throws GeneralSecurityException {
    if (cert == null) {
      return null;
    }
    try (ByteArrayInputStream bis = new ByteArrayInputStream(pemToDer(cert))) {
      return decodeX509Certificate(bis);
    } catch (Exception e) {
      throw new GeneralSecurityException(e);
    }
  }

  public static X509Certificate decodeX509Certificate(InputStream is)
      throws GeneralSecurityException {
    CertificateFactory cf = CertificateFactory.getInstance("X.509", "BC");
    return (X509Certificate) cf.generateCertificate(is);
  }

  public static KeyPair generateKeyPair(int keySize, String algo) throws GeneralSecurityException {
    KeyPairGenerator keyPairGenerator =
        KeyPairGenerator.getInstance(defaultString(algo, "RSA"), "BC");
    keyPairGenerator.initialize(keySize);
    return keyPairGenerator.genKeyPair();
  }

  public static KeyPair loadKeyPairFromKeystore(String path, String storePassword,
      String keyPassword, String keyAlias, String algo) throws GeneralSecurityException {
    Optional<URLResource> resource = Resources.tryFrom(shouldNotBlank(path)).findAny();
    if (resource.isPresent()) {
      try (InputStream is = resource.get().openStream()) {
        KeyStore keyStore = KeyStore.getInstance(defaultString(algo, "PKCS12"), "BC");
        keyStore.load(is, storePassword.toCharArray());
        PrivateKey privateKey = (PrivateKey) keyStore.getKey(keyAlias, keyPassword.toCharArray());
        if (privateKey == null) {
          throw new GeneralSecurityException(
              "Couldn't load key with alias '" + keyAlias + "' from keystore");
        }
        PublicKey publicKey = keyStore.getCertificate(keyAlias).getPublicKey();
        return new KeyPair(publicKey, privateKey);
      } catch (Exception e) {
        throw new GeneralSecurityException(e);
      }
    }
    return null;
  }

  public static KeyStore loadKeyStore(String path, String password) throws Exception {
    Optional<URLResource> resource = Resources.tryFrom(shouldNotBlank(path)).findAny();
    if (resource.isPresent()) {
      KeyStore trustStore = KeyStore.getInstance(KeyStore.getDefaultType());
      try (InputStream is = resource.get().openStream()) {
        trustStore.load(is, password.toCharArray());
      }
      return trustStore;
    }
    return null;
  }

  public static byte[] pemToDer(String pem) {
    String usePem = removeBeginEnd(pem);
    return Base64.getDecoder().decode(usePem);
  }

  public static String toPem(Key key) {
    String name = key.getClass().getSimpleName().toUpperCase(Locale.ENGLISH);
    if (name.endsWith("KEY")) {
      name = name.substring(0, name.length() - 3).concat(" KEY");
    }
    StringBuilder sb = new StringBuilder();
    sb.append("-----BEGIN ").append(name).append("-----\n");
    sb.append(Base64.getEncoder().encodeToString(key.getEncoded())).append("\n");
    sb.append("-----END ").append(name).append("-----\n");
    return sb.toString();
  }

  static String removeBeginEnd(String pem) {
    String rpem = pem.replaceAll("-----BEGIN (.*)-----", "");
    rpem = rpem.replaceAll("-----END (.*)----", "");
    rpem = rpem.replaceAll("\r\n", "");
    rpem = rpem.replaceAll("\n", "");
    return rpem.trim();
  }

}
