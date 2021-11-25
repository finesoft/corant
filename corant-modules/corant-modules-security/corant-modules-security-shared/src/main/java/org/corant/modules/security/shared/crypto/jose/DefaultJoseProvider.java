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
package org.corant.modules.security.shared.crypto.jose;

import static org.corant.shared.util.Assertions.shouldBeTrue;
import java.io.IOException;
import java.net.Proxy;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.interfaces.ECKey;
import java.security.interfaces.ECPublicKey;
import java.security.interfaces.RSAKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.ECParameterSpec;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import javax.net.ssl.SSLSocketFactory;
import org.corant.modules.security.shared.crypto.Keys;
import org.corant.shared.exception.CorantRuntimeException;
import org.corant.shared.ubiquity.Tuple.Pair;
import org.jose4j.http.Get;
import org.jose4j.jwk.EcJwkGenerator;
import org.jose4j.jwk.EllipticCurveJsonWebKey;
import org.jose4j.jwk.HttpsJwks;
import org.jose4j.jwk.JsonWebKey;
import org.jose4j.jwk.JsonWebKeySet;
import org.jose4j.jwk.OctetSequenceJsonWebKey;
import org.jose4j.jwk.RsaJsonWebKey;
import org.jose4j.jwk.RsaJwkGenerator;
import org.jose4j.lang.JoseException;

/**
 * corant-modules-security-shared
 *
 * @author bingo 下午7:51:27
 *
 */
public class DefaultJoseProvider implements JoseProvider {

  protected ProtectionLevel protectionLevel;

  protected JoseSignatureProvider signatureProvider;

  protected JoseEncryptionProvider encryptionProvider;

  public DefaultJoseProvider(ProtectionLevel protectionLevel,
      JoseSignatureProvider signatureProvider, JoseEncryptionProvider encryptionProvider) {
    this.signatureProvider = signatureProvider;
    this.encryptionProvider = encryptionProvider;
    this.protectionLevel = protectionLevel;
  }

  protected DefaultJoseProvider() {}

  public static EllipticCurveJsonWebKey generateECJsonWebKey(ECParameterSpec spec) {
    return generateECJsonWebKey(spec, null, null);
  }

  public static EllipticCurveJsonWebKey generateECJsonWebKey(ECParameterSpec spec, String provider,
      SecureRandom secureRandom) {
    try {
      return EcJwkGenerator.generateJwk(spec, provider, secureRandom);
    } catch (JoseException e) {
      throw new CorantRuntimeException(e);
    }
  }

  public static OctetSequenceJsonWebKey generateOctetJsonWebKey(String algo, String provider,
      int keyBitSize, SecureRandom secureRandom) {
    return new OctetSequenceJsonWebKey(
        Keys.generateSecretKey(provider, algo, keyBitSize, secureRandom));
  }

  public static RsaJsonWebKey generateRSAJsonWebKey(int bits) {
    return generateRSAJsonWebKey(bits, null, null);
  }

  public static RsaJsonWebKey generateRSAJsonWebKey(int bits, String provider,
      SecureRandom secureRandom) {
    try {
      return RsaJwkGenerator.generateJwk(bits, provider, secureRandom);
    } catch (JoseException e) {
      throw new CorantRuntimeException(e);
    }
  }

  public static List<JsonWebKey> readJsonWebKeyFromHttp(String url, Proxy proxy,
      SSLSocketFactory sslSocketFactory) {
    Get simpleHttpGet = new Get();
    if (proxy != null) {
      simpleHttpGet.setHttpProxy(proxy);
    }
    if (sslSocketFactory != null) {
      simpleHttpGet.setSslSocketFactory(sslSocketFactory);
    }
    HttpsJwks hj = new HttpsJwks(url);
    hj.setSimpleHttpGet(simpleHttpGet);
    try {
      hj.refresh();
      return hj.getJsonWebKeys();
    } catch (JoseException | IOException e) {
      throw new CorantRuntimeException(e);
    }
  }

  public static EllipticCurveJsonWebKey wrapECJsonWebKey(ECPublicKey publicKey) {
    return new EllipticCurveJsonWebKey(publicKey);
  }

  public static JsonWebKeySet wrapJsonWebKeySet(PublicKey... keys) {
    shouldBeTrue(keys.length > 0
        && Arrays.stream(keys).allMatch(k -> k instanceof RSAKey || k instanceof ECKey));
    List<JsonWebKey> set = new ArrayList<>();
    for (PublicKey key : keys) {
      if (key instanceof RSAKey) {
        set.add(wrapRSAJsonWebKey((RSAPublicKey) key));
      } else {
        set.add(wrapECJsonWebKey((ECPublicKey) key));
      }
    }
    return new JsonWebKeySet(set);
  }

  public static RsaJsonWebKey wrapRSAJsonWebKey(RSAPublicKey key) {
    return new RsaJsonWebKey(key);
  }

  @Override
  public Map<String, Object> decode(String data, boolean verify) {
    if (protectionLevel == ProtectionLevel.SIGN) {
      return signatureProvider.parse(data, verify);
    } else if (protectionLevel == ProtectionLevel.ENCRYPT) {
      return encryptionProvider.decrypt(data, null, verify);
    } else {
      return encryptionProvider.decrypt(data,
          Pair.of(signatureProvider.getVerificationKey(), signatureProvider.getAlgorithmName()),
          verify);
    }
  }

  @Override
  public String encode(String claimsJson) {
    if (protectionLevel == ProtectionLevel.SIGN) {
      return signatureProvider.sign(claimsJson);
    } else if (protectionLevel == ProtectionLevel.ENCRYPT) {
      return encryptionProvider.encrypt(claimsJson, false);
    } else {
      return encryptionProvider.encrypt(signatureProvider.sign(claimsJson), true);
    }
  }

  @Override
  public JoseEncryptionProvider getEncryptionProvider() {
    return encryptionProvider;
  }

  @Override
  public ProtectionLevel getProtectionLevel() {
    return protectionLevel;
  }

  @Override
  public JoseSignatureProvider getSignatureProvider() {
    return signatureProvider;
  }

}
