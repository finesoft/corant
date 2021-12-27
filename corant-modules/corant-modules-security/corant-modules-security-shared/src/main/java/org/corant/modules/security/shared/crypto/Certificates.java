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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.MessageDigest;
import java.security.PrivateKey;
import java.security.SecureRandom;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Base64;
import java.util.Calendar;
import java.util.Date;
import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.oiw.OIWObjectIdentifiers;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.AlgorithmIdentifier;
import org.bouncycastle.asn1.x509.BasicConstraints;
import org.bouncycastle.asn1.x509.ExtendedKeyUsage;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.asn1.x509.KeyPurposeId;
import org.bouncycastle.asn1.x509.KeyUsage;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.cert.CertIOException;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.X509ExtensionUtils;
import org.bouncycastle.cert.X509v1CertificateBuilder;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.crypto.util.PrivateKeyFactory;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.DefaultDigestAlgorithmIdentifierFinder;
import org.bouncycastle.operator.DefaultSignatureAlgorithmIdentifierFinder;
import org.bouncycastle.operator.DigestCalculator;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.operator.bc.BcDigestCalculatorProvider;
import org.bouncycastle.operator.bc.BcRSAContentSignerBuilder;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.corant.shared.exception.CorantRuntimeException;
import org.jose4j.base64url.Base64Url;
import org.jose4j.lang.HashUtil;

/**
 * corant-modules-security-shared
 *
 * NOTE: Some codes come from keycloak, if there is infringement, please inform
 * me(finesoft@gmail.com).
 *
 * @author bingo 11:04:00
 *
 */
public class Certificates {

  /**
   * Creates the content signer for generation of Version 1
   * {@link java.security.cert.X509Certificate}.
   *
   * @param privateKey the private key
   *
   * @return the content signer
   */
  public static ContentSigner createSigner(PrivateKey privateKey) {
    try {
      AlgorithmIdentifier sigAlgId =
          new DefaultSignatureAlgorithmIdentifierFinder().find("SHA256WithRSAEncryption");
      AlgorithmIdentifier digAlgId = new DefaultDigestAlgorithmIdentifierFinder().find(sigAlgId);

      return new BcRSAContentSignerBuilder(sigAlgId, digAlgId)
          .build(PrivateKeyFactory.createKey(privateKey.getEncoded()));
    } catch (IOException | OperatorCreationException e) {
      throw new CorantRuntimeException(e, "Could not create content signer.");
    }
  }

  public static X509Certificate decodex509Certificate(String cert) {
    if (cert == null) {
      return null;
    }
    try (ByteArrayInputStream bis =
        new ByteArrayInputStream(Base64.getDecoder().decode(removeCertBeginEnd(cert)))) {
      return decodeX509Certificate(bis);
    } catch (IOException | GeneralSecurityException e) {
      throw new CorantRuntimeException(e);
    }
  }

  public static X509Certificate decodeX509Certificate(InputStream is)
      throws GeneralSecurityException {
    CertificateFactory cf = CertificateFactory.getInstance("X.509", "BC");
    return (X509Certificate) cf.generateCertificate(is);
  }

  /**
   * Generate version 1 self signed {@link java.security.cert.X509Certificate}..
   *
   * @param caKeyPair the CA key pair
   * @param subject the subject name
   *
   * @return the x509 certificate
   *
   */
  public static X509Certificate generateV1SelfSignedCertificate(KeyPair caKeyPair, String subject) {
    return generateV1SelfSignedCertificate(caKeyPair, subject,
        BigInteger.valueOf(System.currentTimeMillis()));
  }

  public static X509Certificate generateV1SelfSignedCertificate(KeyPair caKeyPair, String subject,
      BigInteger serialNumber) {
    try {
      X500Name subjectDN = new X500Name("CN=" + subject);
      Date validityStartDate = new Date(System.currentTimeMillis() - 100000);
      Calendar calendar = Calendar.getInstance();
      calendar.add(Calendar.YEAR, 10);
      Date validityEndDate = new Date(calendar.getTime().getTime());
      SubjectPublicKeyInfo subPubKeyInfo =
          SubjectPublicKeyInfo.getInstance(caKeyPair.getPublic().getEncoded());

      X509v1CertificateBuilder builder = new X509v1CertificateBuilder(subjectDN, serialNumber,
          validityStartDate, validityEndDate, subjectDN, subPubKeyInfo);
      X509CertificateHolder holder = builder.build(createSigner(caKeyPair.getPrivate()));

      return new JcaX509CertificateConverter().getCertificate(holder);
    } catch (Exception e) {
      throw new CorantRuntimeException(e, "Error creating X509v1Certificate.");
    }
  }

  /**
   * Generates version 3 {@link java.security.cert.X509Certificate}.
   *
   * @param keyPair the key pair
   * @param caPrivateKey the CA private key
   * @param caCert the CA certificate
   * @param subject the subject name
   *
   * @return the x509 certificate
   *
   * @throws GeneralSecurityException the exception
   */
  public static X509Certificate generateV3Certificate(KeyPair keyPair, PrivateKey caPrivateKey,
      X509Certificate caCert, String subject) throws GeneralSecurityException {
    try {
      X500Name subjectDN = new X500Name("CN=" + subject);

      // Serial Number
      SecureRandom random = SecureRandom.getInstance("SHA1PRNG");
      BigInteger serialNumber = BigInteger.valueOf(Math.abs(random.nextInt()));

      // Validity
      Date notBefore = new Date(System.currentTimeMillis());
      Date notAfter = new Date(System.currentTimeMillis() + 1000L * 60 * 60 * 24 * 30 * 12 * 3);

      // SubjectPublicKeyInfo
      SubjectPublicKeyInfo subjPubKeyInfo = SubjectPublicKeyInfo
          .getInstance(ASN1Sequence.getInstance(keyPair.getPublic().getEncoded()));

      X509v3CertificateBuilder certGen =
          new X509v3CertificateBuilder(new X500Name(caCert.getSubjectDN().getName()), serialNumber,
              notBefore, notAfter, subjectDN, subjPubKeyInfo);

      DigestCalculator digCalc = new BcDigestCalculatorProvider()
          .get(new AlgorithmIdentifier(OIWObjectIdentifiers.idSHA1));
      X509ExtensionUtils x509ExtensionUtils = new X509ExtensionUtils(digCalc);

      // Subject Key Identifier
      certGen.addExtension(Extension.subjectKeyIdentifier, false,
          x509ExtensionUtils.createSubjectKeyIdentifier(subjPubKeyInfo));

      // Authority Key Identifier
      certGen.addExtension(Extension.authorityKeyIdentifier, false,
          x509ExtensionUtils.createAuthorityKeyIdentifier(subjPubKeyInfo));

      // Key Usage
      certGen.addExtension(Extension.keyUsage, false,
          new KeyUsage(KeyUsage.digitalSignature | KeyUsage.keyCertSign | KeyUsage.cRLSign));

      // Extended Key Usage
      KeyPurposeId[] EKU = new KeyPurposeId[2];
      EKU[0] = KeyPurposeId.id_kp_emailProtection;
      EKU[1] = KeyPurposeId.id_kp_serverAuth;

      certGen.addExtension(Extension.extendedKeyUsage, false, new ExtendedKeyUsage(EKU));

      // Basic Constraints
      certGen.addExtension(Extension.basicConstraints, true, new BasicConstraints(0));

      // Content Signer
      ContentSigner sigGen = new JcaContentSignerBuilder("SHA1WithRSAEncryption").setProvider("BC")
          .build(caPrivateKey);

      // Certificate
      return new JcaX509CertificateConverter().setProvider("BC")
          .getCertificate(certGen.build(sigGen));
    } catch (CertificateException | OperatorCreationException | CertIOException e) {
      throw new GeneralSecurityException("Error creating X509v3Certificate.", e);
    }
  }

  public static String x5t(X509Certificate certificate) throws CertificateEncodingException {
    return base64urlThumbprint(certificate, "SHA-1");
  }

  public static String x5tS256(X509Certificate certificate) throws CertificateEncodingException {
    return base64urlThumbprint(certificate, "SHA-256");
  }

  static String base64urlThumbprint(X509Certificate certificate, String hashAlg)
      throws CertificateEncodingException {
    MessageDigest msgDigest = HashUtil.getMessageDigest(hashAlg);
    byte[] digest = msgDigest.digest(certificate.getEncoded());
    return Base64Url.encode(digest);
  }

  static String removeCertBeginEnd(String pem) {
    String rpem = pem.replaceAll("-----BEGIN(.*?)CERTIFICATE-----", "");
    rpem = rpem.replaceAll("-----END(.*?)CERTIFICATE-----", "");
    rpem = rpem.replace("\r\n", "");
    rpem = rpem.replace("\n", "");
    rpem = rpem.replace("\\n", "");
    return rpem.trim();
  }
}
