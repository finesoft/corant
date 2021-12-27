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

import static org.corant.shared.util.Assertions.shouldBeTrue;
import java.io.IOException;
import java.io.InputStream;
import java.security.InvalidAlgorithmParameterException;
import java.security.Key;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.Provider;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.Security;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.security.spec.AlgorithmParameterSpec;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.Locale;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.corant.shared.exception.CorantRuntimeException;
import org.corant.shared.resource.Resource;
import org.corant.shared.util.Texts;

/**
 * corant-modules-security-shared
 *
 * <p>
 * The format of all public key certificates adopts the X.509 standard, and the storing syntax of
 * all private key information adopts PKCS8.
 *
 * @author bingo 19:23:17
 *
 */
public class Keys {

  static SecureRandom secureRandom = new SecureRandom();

  static {
    if (Security.getProvider("BC") == null) {
      Security.addProvider(new BouncyCastleProvider());
    }
  }

  public static byte[] decodeBase64(String data) {
    return Base64.getDecoder().decode(data);
  }

  public static PrivateKey decodePrivateKey(byte[] encodedBytes, String algo) {
    try {
      PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(encodedBytes);
      KeyFactory kf = KeyFactory.getInstance(algo);
      return kf.generatePrivate(keySpec);
    } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
      throw new CorantRuntimeException(e);
    }
  }

  public static PrivateKey decodePrivateKey(String pemEncoded, String algo) {
    return decodePrivateKey(decodeBase64(removePemKeyBeginEnd(pemEncoded)), algo);
  }

  public static PublicKey decodePublicKey(byte[] encodedBytes, String algo) {
    try {
      X509EncodedKeySpec spec = new X509EncodedKeySpec(encodedBytes);
      KeyFactory kf = KeyFactory.getInstance(algo);
      return kf.generatePublic(spec);
    } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
      throw new CorantRuntimeException(e);
    }
  }

  public static PublicKey decodePublicKey(String pemEncoded, String algo) {
    return decodePublicKey(decodeBase64(removePemKeyBeginEnd(pemEncoded)), algo);
  }

  public static SecretKeySpec decodeSecretKeySpec(byte[] bytes, String algo) {
    return new SecretKeySpec(bytes, algo);
  }

  public static SecretKeySpec decodeSecretKeySpec(String pemEncoded, String algo) {
    return decodeSecretKeySpec(decodeBase64(removePemKeyBeginEnd(pemEncoded)), algo);
  }

  public static String encodeBase64(byte[] data) {
    return Base64.getEncoder().encodeToString(data);
  }

  public static KeyPair generateKeyPair(String algo) {
    return generateKeyPair(null, algo, (Integer) null, null);
  }

  public static KeyPair generateKeyPair(String algo, AlgorithmParameterSpec spec) {
    return generateKeyPair(null, algo, spec, null);
  }

  public static KeyPair generateKeyPair(String algo, AlgorithmParameterSpec spec,
      SecureRandom secureRandom) {
    return generateKeyPair(null, algo, spec, secureRandom);
  }

  public static KeyPair generateKeyPair(String algo, Integer keySize) {
    return generateKeyPair(null, algo, keySize, null);
  }

  public static KeyPair generateKeyPair(String algo, Integer keySize, SecureRandom secureRandom) {
    return generateKeyPair(null, algo, keySize, secureRandom);
  }

  public static KeyPair generateKeyPair(String provider, String algo, AlgorithmParameterSpec spec,
      SecureRandom secureRandom) {
    try {
      KeyPairGenerator keyPairGenerator =
          provider != null ? KeyPairGenerator.getInstance(algo, provider)
              : KeyPairGenerator.getInstance(algo);
      if (spec != null) {
        if (secureRandom != null) {
          keyPairGenerator.initialize(spec, secureRandom);
        } else {
          keyPairGenerator.initialize(spec);
        }
      }
      return keyPairGenerator.genKeyPair();
    } catch (NoSuchProviderException | NoSuchAlgorithmException
        | InvalidAlgorithmParameterException e) {
      throw new CorantRuntimeException(e);
    }
  }

  public static KeyPair generateKeyPair(String provider, String algo, Integer keySize,
      SecureRandom secureRandom) {
    try {
      KeyPairGenerator keyPairGenerator =
          provider != null ? KeyPairGenerator.getInstance(algo, provider)
              : KeyPairGenerator.getInstance(algo);
      if (keySize != null) {
        if (secureRandom != null) {
          keyPairGenerator.initialize(keySize, secureRandom);
        } else {
          keyPairGenerator.initialize(keySize);
        }
      }
      return keyPairGenerator.genKeyPair();
    } catch (NoSuchProviderException | NoSuchAlgorithmException e) {
      throw new CorantRuntimeException(e);
    }
  }

  public static SecretKey generateSecretKey(Object provider, String algo,
      AlgorithmParameterSpec spec, SecureRandom secureRandom) {
    try {
      KeyGenerator generator;
      if (provider instanceof Provider) {
        generator = KeyGenerator.getInstance(algo, (Provider) provider);
      } else if (provider instanceof String) {
        generator = KeyGenerator.getInstance(algo, (String) provider);
      } else {
        generator = KeyGenerator.getInstance(algo);
      }
      if (secureRandom != null) {
        generator.init(spec, secureRandom);
      } else {
        generator.init(spec);
      }
      return generator.generateKey();
    } catch (NoSuchAlgorithmException | NoSuchProviderException
        | InvalidAlgorithmParameterException e) {
      throw new CorantRuntimeException(e);
    }
  }

  public static SecretKey generateSecretKey(Object provider, String algo, Integer keyBitSize,
      SecureRandom secureRandom) {
    try {
      KeyGenerator generator;
      if (provider instanceof Provider) {
        generator = KeyGenerator.getInstance(algo, (Provider) provider);
      } else if (provider instanceof String) {
        generator = KeyGenerator.getInstance(algo, (String) provider);
      } else {
        generator = KeyGenerator.getInstance(algo);
      }
      if (secureRandom != null) {
        generator.init(keyBitSize, secureRandom);
      } else {
        generator.init(keyBitSize);
      }
      return generator.generateKey();
    } catch (NoSuchAlgorithmException | NoSuchProviderException e) {
      throw new CorantRuntimeException(e);
    }
  }

  public static SecretKey generateSecretKey(String algo, AlgorithmParameterSpec spec) {
    return generateSecretKey(null, algo, spec, null);
  }

  public static SecretKey generateSecretKey(String algo, Integer keyBitSize) {
    return generateSecretKey(null, algo, keyBitSize, null);
  }

  public static SecretKey generateSecretKeySpec(String algo, int keyBitSize) {
    shouldBeTrue(keyBitSize % 8 == 0);
    byte[] secretBytes = new byte[keyBitSize / 8];
    secureRandom.nextBytes(secretBytes);
    return new SecretKeySpec(secretBytes, algo);
  }

  public static KeyPair readKeyPairFromKeystore(Resource resource, String provider, String algo,
      String storePassword, String keyPassword, String keyAlias) {
    try (InputStream is = resource.openInputStream()) {
      KeyStore keyStore =
          provider != null ? KeyStore.getInstance(algo, provider) : KeyStore.getInstance(algo);
      keyStore.load(is, storePassword.toCharArray());
      PrivateKey privateKey = (PrivateKey) keyStore.getKey(keyAlias, keyPassword.toCharArray());
      if (privateKey == null) {
        throw new CorantRuntimeException("Couldn't load key with alias '%s' from keystore",
            keyAlias);
      }
      PublicKey publicKey = keyStore.getCertificate(keyAlias).getPublicKey();
      return new KeyPair(publicKey, privateKey);
    } catch (NoSuchProviderException | IOException | KeyStoreException | NoSuchAlgorithmException
        | CertificateException | UnrecoverableKeyException e) {
      throw new CorantRuntimeException(e);
    }
  }

  public static KeyStore readKeyStore(Resource resource, String password) {
    try (InputStream is = resource.openInputStream()) {
      KeyStore trustStore = KeyStore.getInstance(KeyStore.getDefaultType());
      trustStore.load(is, password.toCharArray());
      return trustStore;
    } catch (NoSuchAlgorithmException | IOException | KeyStoreException | CertificateException e) {
      throw new CorantRuntimeException(e);
    }
  }

  public static PrivateKey readPrivateKey(Resource resource, String algo) {
    try (InputStream is = resource.openInputStream()) {
      return decodePrivateKey(Texts.fromInputStream(is), algo);
    } catch (IOException e) {
      throw new CorantRuntimeException(e);
    }
  }

  public static PublicKey readPublicKey(Resource resource, String algo) {
    try (InputStream is = resource.openInputStream()) {
      return decodePublicKey(Texts.fromInputStream(is), algo);
    } catch (IOException e) {
      throw new CorantRuntimeException(e);
    }
  }

  public static SecretKeySpec readSecretKeySpec(Resource resource, String algo) {
    try (InputStream is = resource.openInputStream()) {
      return decodeSecretKeySpec(Texts.fromInputStream(is), algo);
    } catch (IOException e) {
      throw new CorantRuntimeException(e);
    }
  }

  public static String removePemKeyBeginEnd(String pem) {
    String rpem = pem.replaceAll("-----BEGIN (.*)-----", "");
    rpem = rpem.replaceAll("-----END (.*)----", "");
    rpem = rpem.replace("\r\n", "");
    rpem = rpem.replace("\n", "");
    return rpem.trim();
  }

  public static String toPem(Key key) {
    String name = key.getClass().getSimpleName().toUpperCase(Locale.ENGLISH);
    if (name.endsWith("KEY")) {
      name = name.substring(0, name.length() - 3).concat(" KEY");
    }
    StringBuilder sb = new StringBuilder();
    sb.append("-----BEGIN ").append(name).append("-----\n");
    sb.append(encodeBase64(key.getEncoded())).append("\n");
    sb.append("-----END ").append(name).append("-----\n");
    return sb.toString();
  }
}
