JSON Object Signing and Encryption (JOSE)
=========================================

Created
:   2015-01-23
Last Updated
:   2020-11-02
Available Formats
:   [![](/_img/icons/text-xml.png)\
    XML](jose.xml) [![](/_img/icons/text-html.png)\
    HTML](jose.xhtml) [![](/_img/icons/text-plain.png)\
    Plain text](jose.txt)

**Registries included below**

-   [JSON Web Signature and Encryption Header
    Parameters](#web-signature-encryption-header-parameters)
-   [JSON Web Signature and Encryption
    Algorithms](#web-signature-encryption-algorithms)
-   [JSON Web Encryption Compression
    Algorithms](#web-encryption-compression-algorithms)
-   [JSON Web Key Types](#web-key-types)
-   [JSON Web Key Elliptic Curve](#web-key-elliptic-curve)
-   [JSON Web Key Parameters](#web-key-parameters)
-   [JSON Web Key Use](#web-key-use)
-   [JSON Web Key Operations](#web-key-operations)
-   [JSON Web Key Set Parameters](#web-key-set-parameters)

JSON Web Signature and Encryption Header Parameters
---------------------------------------------------

Registration Procedure(s)
:   Specification Required

Expert(s)
:   Jeff Hodges, Joe Hildebrand, Sean Turner

Reference
:   [[RFC7515](https://www.iana.org/go/rfc7515)]
Note
:   Registration requests should be sent to the mailing list 
        described in [RFC7515].
            

Available Formats
:   [![](/_img/icons/text-csv.png)\
    CSV](web-signature-encryption-header-parameters.csv)

  Header Parameter Name   Header Parameter Description           Header Parameter Usage Location(s)   Change Controller   Reference
  ----------------------- -------------------------------------- ------------------------------------ ------------------- ---------------------------------------------------------------
  alg                     Algorithm                              JWS                                  [[IESG](#IESG)]     [[RFC7515, Section 4.1.1](https://www.iana.org/go/rfc7515)]
  jku                     JWK Set URL                            JWS                                  [[IESG](#IESG)]     [[RFC7515, Section 4.1.2](https://www.iana.org/go/rfc7515)]
  jwk                     JSON Web Key                           JWS                                  [[IESG](#IESG)]     [[RFC7515, Section 4.1.3](https://www.iana.org/go/rfc7515)]
  kid                     Key ID                                 JWS                                  [[IESG](#IESG)]     [[RFC7515, Section 4.1.4](https://www.iana.org/go/rfc7515)]
  x5u                     X.509 URL                              JWS                                  [[IESG](#IESG)]     [[RFC7515, Section 4.1.5](https://www.iana.org/go/rfc7515)]
  x5c                     X.509 Certificate Chain                JWS                                  [[IESG](#IESG)]     [[RFC7515, Section 4.1.6](https://www.iana.org/go/rfc7515)]
  x5t                     X.509 Certificate SHA-1 Thumbprint     JWS                                  [[IESG](#IESG)]     [[RFC7515, Section 4.1.7](https://www.iana.org/go/rfc7515)]
  x5t\#S256               X.509 Certificate SHA-256 Thumbprint   JWS                                  [[IESG](#IESG)]     [[RFC7515, Section 4.1.8](https://www.iana.org/go/rfc7515)]
  typ                     Type                                   JWS                                  [[IESG](#IESG)]     [[RFC7515, Section 4.1.9](https://www.iana.org/go/rfc7515)]
  cty                     Content Type                           JWS                                  [[IESG](#IESG)]     [[RFC7515, Section 4.1.10](https://www.iana.org/go/rfc7515)]
  crit                    Critical                               JWS                                  [[IESG](#IESG)]     [[RFC7515, Section 4.1.11](https://www.iana.org/go/rfc7515)]
  alg                     Algorithm                              JWE                                  [[IESG](#IESG)]     [[RFC7516, Section 4.1.1](https://www.iana.org/go/rfc7516)]
  enc                     Encryption Algorithm                   JWE                                  [[IESG](#IESG)]     [[RFC7516, Section 4.1.2](https://www.iana.org/go/rfc7516)]
  zip                     Compression Algorithm                  JWE                                  [[IESG](#IESG)]     [[RFC7516, Section 4.1.3](https://www.iana.org/go/rfc7516)]
  jku                     JWK Set URL                            JWE                                  [[IESG](#IESG)]     [[RFC7516, Section 4.1.4](https://www.iana.org/go/rfc7516)]
  jwk                     JSON Web Key                           JWE                                  [[IESG](#IESG)]     [[RFC7516, Section 4.1.5](https://www.iana.org/go/rfc7516)]
  kid                     Key ID                                 JWE                                  [[IESG](#IESG)]     [[RFC7516, Section 4.1.6](https://www.iana.org/go/rfc7516)]
  x5u                     X.509 URL                              JWE                                  [[IESG](#IESG)]     [[RFC7516, Section 4.1.7](https://www.iana.org/go/rfc7516)]
  x5c                     X.509 Certificate Chain                JWE                                  [[IESG](#IESG)]     [[RFC7516, Section 4.1.8](https://www.iana.org/go/rfc7516)]
  x5t                     X.509 Certificate SHA-1 Thumbprint     JWE                                  [[IESG](#IESG)]     [[RFC7516, Section 4.1.9](https://www.iana.org/go/rfc7516)]
  x5t\#S256               X.509 Certificate SHA-256 Thumbprint   JWE                                  [[IESG](#IESG)]     [[RFC7516, Section 4.1.10](https://www.iana.org/go/rfc7516)]
  typ                     Type                                   JWE                                  [[IESG](#IESG)]     [[RFC7516, Section 4.1.11](https://www.iana.org/go/rfc7516)]
  cty                     Content Type                           JWE                                  [[IESG](#IESG)]     [[RFC7516, Section 4.1.12](https://www.iana.org/go/rfc7516)]
  crit                    Critical                               JWE                                  [[IESG](#IESG)]     [[RFC7516, Section 4.1.13](https://www.iana.org/go/rfc7516)]
  epk                     Ephemeral Public Key                   JWE                                  [[IESG](#IESG)]     [[RFC7518, Section 4.6.1.1](https://www.iana.org/go/rfc7518)]
  apu                     Agreement PartyUInfo                   JWE                                  [[IESG](#IESG)]     [[RFC7518, Section 4.6.1.2](https://www.iana.org/go/rfc7518)]
  apv                     Agreement PartyVInfo                   JWE                                  [[IESG](#IESG)]     [[RFC7518, Section 4.6.1.3](https://www.iana.org/go/rfc7518)]
  iv                      Initialization Vector                  JWE                                  [[IESG](#IESG)]     [[RFC7518, Section 4.7.1.1](https://www.iana.org/go/rfc7518)]
  tag                     Authentication Tag                     JWE                                  [[IESG](#IESG)]     [[RFC7518, Section 4.7.1.2](https://www.iana.org/go/rfc7518)]
  p2s                     PBES2 Salt Input                       JWE                                  [[IESG](#IESG)]     [[RFC7518, Section 4.8.1.1](https://www.iana.org/go/rfc7518)]
  p2c                     PBES2 Count                            JWE                                  [[IESG](#IESG)]     [[RFC7518, Section 4.8.1.2](https://www.iana.org/go/rfc7518)]
  iss                     Issuer                                 JWE                                  [[IESG](#IESG)]     [[RFC7519, Section 4.1.1](https://www.iana.org/go/rfc7519)]
  sub                     Subject                                JWE                                  [[IESG](#IESG)]     [[RFC7519, Section 4.1.2](https://www.iana.org/go/rfc7519)]
  aud                     Audience                               JWE                                  [[IESG](#IESG)]     [[RFC7519, Section 4.1.3](https://www.iana.org/go/rfc7519)]
  b64                     Base64url-Encode Payload               JWS                                  [[IESG](#IESG)]     [[RFC7797, Section 3](https://www.iana.org/go/rfc7797)]
  ppt                     PASSporT extension identifier          JWS                                  [[IESG](#IESG)]     [[RFC8225, Section 8.1](https://www.iana.org/go/rfc8225)]
  url                     URL                                    JWE, JWS                             [[IESG](#IESG)]     [[RFC8555, Section 6.4.1](https://www.iana.org/go/rfc8555)]
  nonce                   Nonce                                  JWE, JWS                             [[IESG](#IESG)]     [[RFC8555, Section 6.5.2](https://www.iana.org/go/rfc8555)]

JSON Web Signature and Encryption Algorithms
--------------------------------------------

Registration Procedure(s)
:   Specification Required

Expert(s)
:   Jeff Hodges, Joe Hildebrand, Sean Turner

Reference
:   [[RFC7518](https://www.iana.org/go/rfc7518)]
Note
:   Registration requests should be sent to the mailing list 
        described in [RFC7518].
            

Available Formats
:   [![](/_img/icons/text-csv.png)\
    CSV](web-signature-encryption-algorithms.csv)

  Algorithm Name       Algorithm Description                                              Algorithm Usage Location(s)   JOSE Implementation Requirements   Change Controller                                                                 Reference                                                                    Algorithm Analysis Document(s)
  -------------------- ------------------------------------------------------------------ ----------------------------- ---------------------------------- --------------------------------------------------------------------------------- ---------------------------------------------------------------------------- --------------------------------------------------------------------------------------------------------
  HS256                HMAC using SHA-256                                                 alg                           Required                           [[IESG](#IESG)]                                                                   [[RFC7518, Section 3.2](https://www.iana.org/go/rfc7518)]                    n/a
  HS384                HMAC using SHA-384                                                 alg                           Optional                           [[IESG](#IESG)]                                                                   [[RFC7518, Section 3.2](https://www.iana.org/go/rfc7518)]                    n/a
  HS512                HMAC using SHA-512                                                 alg                           Optional                           [[IESG](#IESG)]                                                                   [[RFC7518, Section 3.2](https://www.iana.org/go/rfc7518)]                    n/a
  RS256                RSASSA-PKCS1-v1\_5 using SHA-256                                   alg                           Recommended                        [[IESG](#IESG)]                                                                   [[RFC7518, Section 3.3](https://www.iana.org/go/rfc7518)]                    n/a
  RS384                RSASSA-PKCS1-v1\_5 using SHA-384                                   alg                           Optional                           [[IESG](#IESG)]                                                                   [[RFC7518, Section 3.3](https://www.iana.org/go/rfc7518)]                    n/a
  RS512                RSASSA-PKCS1-v1\_5 using SHA-512                                   alg                           Optional                           [[IESG](#IESG)]                                                                   [[RFC7518, Section 3.3](https://www.iana.org/go/rfc7518)]                    n/a
  ES256                ECDSA using P-256 and SHA-256                                      alg                           Recommended+                       [[IESG](#IESG)]                                                                   [[RFC7518, Section 3.4](https://www.iana.org/go/rfc7518)]                    n/a
  ES384                ECDSA using P-384 and SHA-384                                      alg                           Optional                           [[IESG](#IESG)]                                                                   [[RFC7518, Section 3.4](https://www.iana.org/go/rfc7518)]                    n/a
  ES512                ECDSA using P-521 and SHA-512                                      alg                           Optional                           [[IESG](#IESG)]                                                                   [[RFC7518, Section 3.4](https://www.iana.org/go/rfc7518)]                    n/a
  PS256                RSASSA-PSS using SHA-256 and MGF1 with SHA-256                     alg                           Optional                           [[IESG](#IESG)]                                                                   [[RFC7518, Section 3.5](https://www.iana.org/go/rfc7518)]                    n/a
  PS384                RSASSA-PSS using SHA-384 and MGF1 with SHA-384                     alg                           Optional                           [[IESG](#IESG)]                                                                   [[RFC7518, Section 3.5](https://www.iana.org/go/rfc7518)]                    n/a
  PS512                RSASSA-PSS using SHA-512 and MGF1 with SHA-512                     alg                           Optional                           [[IESG](#IESG)]                                                                   [[RFC7518, Section 3.5](https://www.iana.org/go/rfc7518)]                    n/a
  none                 No digital signature or MAC performed                              alg                           Optional                           [[IESG](#IESG)]                                                                   [[RFC7518, Section 3.6](https://www.iana.org/go/rfc7518)]                    n/a
  RSA1\_5              RSAES-PKCS1-v1\_5                                                  alg                           Recommended-                       [[IESG](#IESG)]                                                                   [[RFC7518, Section 4.2](https://www.iana.org/go/rfc7518)]                    n/a
  RSA-OAEP             RSAES OAEP using default parameters                                alg                           Recommended+                       [[IESG](#IESG)]                                                                   [[RFC7518, Section 4.3](https://www.iana.org/go/rfc7518)]                    n/a
  RSA-OAEP-256         RSAES OAEP using SHA-256 and MGF1 with SHA-256                     alg                           Optional                           [[IESG](#IESG)]                                                                   [[RFC7518, Section 4.3](https://www.iana.org/go/rfc7518)]                    n/a
  A128KW               AES Key Wrap using 128-bit key                                     alg                           Recommended                        [[IESG](#IESG)]                                                                   [[RFC7518, Section 4.4](https://www.iana.org/go/rfc7518)]                    n/a
  A192KW               AES Key Wrap using 192-bit key                                     alg                           Optional                           [[IESG](#IESG)]                                                                   [[RFC7518, Section 4.4](https://www.iana.org/go/rfc7518)]                    n/a
  A256KW               AES Key Wrap using 256-bit key                                     alg                           Recommended                        [[IESG](#IESG)]                                                                   [[RFC7518, Section 4.4](https://www.iana.org/go/rfc7518)]                    n/a
  dir                  Direct use of a shared symmetric key                               alg                           Recommended                        [[IESG](#IESG)]                                                                   [[RFC7518, Section 4.5](https://www.iana.org/go/rfc7518)]                    n/a
  ECDH-ES              ECDH-ES using Concat KDF                                           alg                           Recommended+                       [[IESG](#IESG)]                                                                   [[RFC7518, Section 4.6](https://www.iana.org/go/rfc7518)]                    n/a
  ECDH-ES+A128KW       ECDH-ES using Concat KDF and "A128KW" wrapping                     alg                           Recommended                        [[IESG](#IESG)]                                                                   [[RFC7518, Section 4.6](https://www.iana.org/go/rfc7518)]                    n/a
  ECDH-ES+A192KW       ECDH-ES using Concat KDF and "A192KW" wrapping                     alg                           Optional                           [[IESG](#IESG)]                                                                   [[RFC7518, Section 4.6](https://www.iana.org/go/rfc7518)]                    n/a
  ECDH-ES+A256KW       ECDH-ES using Concat KDF and "A256KW" wrapping                     alg                           Recommended                        [[IESG](#IESG)]                                                                   [[RFC7518, Section 4.6](https://www.iana.org/go/rfc7518)]                    n/a
  A128GCMKW            Key wrapping with AES GCM using 128-bit key                        alg                           Optional                           [[IESG](#IESG)]                                                                   [[RFC7518, Section 4.7](https://www.iana.org/go/rfc7518)]                    n/a
  A192GCMKW            Key wrapping with AES GCM using 192-bit key                        alg                           Optional                           [[IESG](#IESG)]                                                                   [[RFC7518, Section 4.7](https://www.iana.org/go/rfc7518)]                    n/a
  A256GCMKW            Key wrapping with AES GCM using 256-bit key                        alg                           Optional                           [[IESG](#IESG)]                                                                   [[RFC7518, Section 4.7](https://www.iana.org/go/rfc7518)]                    n/a
  PBES2-HS256+A128KW   PBES2 with HMAC SHA-256 and "A128KW" wrapping                      alg                           Optional                           [[IESG](#IESG)]                                                                   [[RFC7518, Section 4.8](https://www.iana.org/go/rfc7518)]                    n/a
  PBES2-HS384+A192KW   PBES2 with HMAC SHA-384 and "A192KW" wrapping                      alg                           Optional                           [[IESG](#IESG)]                                                                   [[RFC7518, Section 4.8](https://www.iana.org/go/rfc7518)]                    n/a
  PBES2-HS512+A256KW   PBES2 with HMAC SHA-512 and "A256KW" wrapping                      alg                           Optional                           [[IESG](#IESG)]                                                                   [[RFC7518, Section 4.8](https://www.iana.org/go/rfc7518)]                    n/a
  A128CBC-HS256        AES\_128\_CBC\_HMAC\_SHA\_256 authenticated encryption algorithm   enc                           Required                           [[IESG](#IESG)]                                                                   [[RFC7518, Section 5.2.3](https://www.iana.org/go/rfc7518)]                  n/a
  A192CBC-HS384        AES\_192\_CBC\_HMAC\_SHA\_384 authenticated encryption algorithm   enc                           Optional                           [[IESG](#IESG)]                                                                   [[RFC7518, Section 5.2.4](https://www.iana.org/go/rfc7518)]                  n/a
  A256CBC-HS512        AES\_256\_CBC\_HMAC\_SHA\_512 authenticated encryption algorithm   enc                           Required                           [[IESG](#IESG)]                                                                   [[RFC7518, Section 5.2.5](https://www.iana.org/go/rfc7518)]                  n/a
  A128GCM              AES GCM using 128-bit key                                          enc                           Recommended                        [[IESG](#IESG)]                                                                   [[RFC7518, Section 5.3](https://www.iana.org/go/rfc7518)]                    n/a
  A192GCM              AES GCM using 192-bit key                                          enc                           Optional                           [[IESG](#IESG)]                                                                   [[RFC7518, Section 5.3](https://www.iana.org/go/rfc7518)]                    n/a
  A256GCM              AES GCM using 256-bit key                                          enc                           Recommended                        [[IESG](#IESG)]                                                                   [[RFC7518, Section 5.3](https://www.iana.org/go/rfc7518)]                    n/a
  EdDSA                EdDSA signature algorithms                                         alg                           Optional                           [[IESG](#IESG)]                                                                   [[RFC8037, Section 3.1](https://www.iana.org/go/rfc8037)]                    [[RFC8032](https://www.iana.org/go/rfc8032)]
  RS1                  RSASSA-PKCS1-v1\_5 with SHA-1                                      JWK                           Prohibited                         [[W3C\_Web\_Cryptography\_Working\_Group](#W3C_Web_Cryptography_Working_Group)]   [[https://www.w3.org/TR/WebCryptoAPI](https://www.w3.org/TR/WebCryptoAPI)]   [[draft-irtf-cfrg-webcrypto-algorithms](https://www.iana.org/go/draft-irtf-cfrg-webcrypto-algorithms)]
  RSA-OAEP-384         RSA-OAEP using SHA-384 and MGF1 with SHA-384                       alg                           Optional                           [[W3C\_Web\_Cryptography\_Working\_Group](#W3C_Web_Cryptography_Working_Group)]   [[https://www.w3.org/TR/WebCryptoAPI](https://www.w3.org/TR/WebCryptoAPI)]   n/a
  RSA-OAEP-512         RSA-OAEP using SHA-512 and MGF1 with SHA-512                       alg                           Optional                           [[W3C\_Web\_Cryptography\_Working\_Group](#W3C_Web_Cryptography_Working_Group)]   [[https://www.w3.org/TR/WebCryptoAPI](https://www.w3.org/TR/WebCryptoAPI)]   n/a
  A128CBC              AES CBC using 128 bit key                                          JWK                           Prohibited                         [[W3C\_Web\_Cryptography\_Working\_Group](#W3C_Web_Cryptography_Working_Group)]   [[https://www.w3.org/TR/WebCryptoAPI](https://www.w3.org/TR/WebCryptoAPI)]   [[draft-irtf-cfrg-webcrypto-algorithms](https://www.iana.org/go/draft-irtf-cfrg-webcrypto-algorithms)]
  A192CBC              AES CBC using 192 bit key                                          JWK                           Prohibited                         [[W3C\_Web\_Cryptography\_Working\_Group](#W3C_Web_Cryptography_Working_Group)]   [[https://www.w3.org/TR/WebCryptoAPI](https://www.w3.org/TR/WebCryptoAPI)]   [[draft-irtf-cfrg-webcrypto-algorithms](https://www.iana.org/go/draft-irtf-cfrg-webcrypto-algorithms)]
  A256CBC              AES CBC using 256 bit key                                          JWK                           Prohibited                         [[W3C\_Web\_Cryptography\_Working\_Group](#W3C_Web_Cryptography_Working_Group)]   [[https://www.w3.org/TR/WebCryptoAPI](https://www.w3.org/TR/WebCryptoAPI)]   [[draft-irtf-cfrg-webcrypto-algorithms](https://www.iana.org/go/draft-irtf-cfrg-webcrypto-algorithms)]
  A128CTR              AES CTR using 128 bit key                                          JWK                           Prohibited                         [[W3C\_Web\_Cryptography\_Working\_Group](#W3C_Web_Cryptography_Working_Group)]   [[https://www.w3.org/TR/WebCryptoAPI](https://www.w3.org/TR/WebCryptoAPI)]   [[draft-irtf-cfrg-webcrypto-algorithms](https://www.iana.org/go/draft-irtf-cfrg-webcrypto-algorithms)]
  A192CTR              AES CTR using 192 bit key                                          JWK                           Prohibited                         [[W3C\_Web\_Cryptography\_Working\_Group](#W3C_Web_Cryptography_Working_Group)]   [[https://www.w3.org/TR/WebCryptoAPI](https://www.w3.org/TR/WebCryptoAPI)]   [[draft-irtf-cfrg-webcrypto-algorithms](https://www.iana.org/go/draft-irtf-cfrg-webcrypto-algorithms)]
  A256CTR              AES CTR using 256 bit key                                          JWK                           Prohibited                         [[W3C\_Web\_Cryptography\_Working\_Group](#W3C_Web_Cryptography_Working_Group)]   [[https://www.w3.org/TR/WebCryptoAPI](https://www.w3.org/TR/WebCryptoAPI)]   [[draft-irtf-cfrg-webcrypto-algorithms](https://www.iana.org/go/draft-irtf-cfrg-webcrypto-algorithms)]
  HS1                  HMAC using SHA-1                                                   JWK                           Prohibited                         [[W3C\_Web\_Cryptography\_Working\_Group](#W3C_Web_Cryptography_Working_Group)]   [[https://www.w3.org/TR/WebCryptoAPI](https://www.w3.org/TR/WebCryptoAPI)]   [[draft-irtf-cfrg-webcrypto-algorithms](https://www.iana.org/go/draft-irtf-cfrg-webcrypto-algorithms)]
  ES256K               ECDSA using secp256k1 curve and SHA-256                            alg                           Optional                           [[IESG](#IESG)]                                                                   [[RFC8812, Section 3.2](https://www.iana.org/go/rfc8812)]                    [[SEC2](http://www.secg.org/sec2-v2.pdf)]

JSON Web Encryption Compression Algorithms
------------------------------------------

Registration Procedure(s)
:   Specification Required

Expert(s)
:   Jeff Hodges, Joe Hildebrand, Sean Turner

Reference
:   [[RFC7518](https://www.iana.org/go/rfc7518)]
Note
:   Registration requests should be sent to the mailing list 
        described in [RFC7518].
            

Available Formats
:   [![](/_img/icons/text-csv.png)\
    CSV](web-encryption-compression-algorithms.csv)

  Compression Algorithm Value   Compression Algorithm Description   Change Controller   Reference
  ----------------------------- ----------------------------------- ------------------- ----------------------------------------------
  DEF                           DEFLATE                             [[IESG](#IESG)]     [[RFC7516](https://www.iana.org/go/rfc7516)]

JSON Web Key Types
------------------

Registration Procedure(s)
:   Specification Required

Expert(s)
:   Jeff Hodges, Joe Hildebrand, Sean Turner

Reference
:   [[RFC7518](https://www.iana.org/go/rfc7518)][[RFC7638](https://www.iana.org/go/rfc7638)]
Note
:   Registration requests should be sent to the mailing list 
        described in [RFC7518].
            

Available Formats
:   [![](/_img/icons/text-csv.png)\
    CSV](web-key-types.csv)

  "kty" Parameter Value   Key Type Description     JOSE Implementation Requirements   Change Controller   Reference
  ----------------------- ------------------------ ---------------------------------- ------------------- -----------------------------------------------------------
  EC                      Elliptic Curve           Recommended+                       [[IESG](#IESG)]     [[RFC7518, Section 6.2](https://www.iana.org/go/rfc7518)]
  RSA                     RSA                      Required                           [[IESG](#IESG)]     [[RFC7518, Section 6.3](https://www.iana.org/go/rfc7518)]
  oct                     Octet sequence           Required                           [[IESG](#IESG)]     [[RFC7518, Section 6.4](https://www.iana.org/go/rfc7518)]
  OKP                     Octet string key pairs   Optional                           [[IESG](#IESG)]     [[RFC8037, Section 2](https://www.iana.org/go/rfc8037)]

JSON Web Key Elliptic Curve
---------------------------

Registration Procedure(s)
:   Specification Required

Expert(s)
:   Jeff Hodges, Joe Hildebrand, Sean Turner

Reference
:   [[RFC7518](https://www.iana.org/go/rfc7518)][[RFC7638](https://www.iana.org/go/rfc7638)]
Note
:   Registration requests should be sent to the mailing list 
        described in [RFC7518].
            

Available Formats
:   [![](/_img/icons/text-csv.png)\
    CSV](web-key-elliptic-curve.csv)

  Curve Name   Curve Description                       JOSE Implementation Requirements   Change Controller   Reference
  ------------ --------------------------------------- ---------------------------------- ------------------- ---------------------------------------------------------------
  P-256        P-256 Curve                             Recommended+                       [[IESG](#IESG)]     [[RFC7518, Section 6.2.1.1](https://www.iana.org/go/rfc7518)]
  P-384        P-384 Curve                             Optional                           [[IESG](#IESG)]     [[RFC7518, Section 6.2.1.1](https://www.iana.org/go/rfc7518)]
  P-521        P-521 Curve                             Optional                           [[IESG](#IESG)]     [[RFC7518, Section 6.2.1.1](https://www.iana.org/go/rfc7518)]
  Ed25519      Ed25519 signature algorithm key pairs   Optional                           [[IESG](#IESG)]     [[RFC8037, Section 3.1](https://www.iana.org/go/rfc8037)]
  Ed448        Ed448 signature algorithm key pairs     Optional                           [[IESG](#IESG)]     [[RFC8037, Section 3.1](https://www.iana.org/go/rfc8037)]
  X25519       X25519 function key pairs               Optional                           [[IESG](#IESG)]     [[RFC8037, Section 3.2](https://www.iana.org/go/rfc8037)]
  X448         X448 function key pairs                 Optional                           [[IESG](#IESG)]     [[RFC8037, Section 3.2](https://www.iana.org/go/rfc8037)]
  secp256k1    SECG secp256k1 curve                    Optional                           [[IESG](#IESG)]     [[RFC8812, Section 3.1](https://www.iana.org/go/rfc8812)]

JSON Web Key Parameters
-----------------------

Registration Procedure(s)
:   Specification Required

Expert(s)
:   Jeff Hodges, Joe Hildebrand, Sean Turner

Reference
:   [[RFC7517](https://www.iana.org/go/rfc7517)][[RFC7638](https://www.iana.org/go/rfc7638)]
Note
:   Registration requests should be sent to the mailing list 
        described in [RFC7517].
            

Available Formats
:   [![](/_img/icons/text-csv.png)\
    CSV](web-key-parameters.csv)

  Parameter Name   Parameter Description                  Used with "kty" Value(s)   Parameter Information Class   Change Controller                                                                 Reference
  ---------------- -------------------------------------- -------------------------- ----------------------------- --------------------------------------------------------------------------------- ----------------------------------------------------------------------------
  kty              Key Type                               \*                         Public                        [[IESG](#IESG)]                                                                   [[RFC7517, Section 4.1](https://www.iana.org/go/rfc7517)]
  use              Public Key Use                         \*                         Public                        [[IESG](#IESG)]                                                                   [[RFC7517, Section 4.2](https://www.iana.org/go/rfc7517)]
  key\_ops         Key Operations                         \*                         Public                        [[IESG](#IESG)]                                                                   [[RFC7517, Section 4.3](https://www.iana.org/go/rfc7517)]
  alg              Algorithm                              \*                         Public                        [[IESG](#IESG)]                                                                   [[RFC7517, Section 4.4](https://www.iana.org/go/rfc7517)]
  kid              Key ID                                 \*                         Public                        [[IESG](#IESG)]                                                                   [[RFC7517, Section 4.5](https://www.iana.org/go/rfc7517)]
  x5u              X.509 URL                              \*                         Public                        [[IESG](#IESG)]                                                                   [[RFC7517, Section 4.6](https://www.iana.org/go/rfc7517)]
  x5c              X.509 Certificate Chain                \*                         Public                        [[IESG](#IESG)]                                                                   [[RFC7517, Section 4.7](https://www.iana.org/go/rfc7517)]
  x5t              X.509 Certificate SHA-1 Thumbprint     \*                         Public                        [[IESG](#IESG)]                                                                   [[RFC7517, Section 4.8](https://www.iana.org/go/rfc7517)]
  x5t\#S256        X.509 Certificate SHA-256 Thumbprint   \*                         Public                        [[IESG](#IESG)]                                                                   [[RFC7517, Section 4.9](https://www.iana.org/go/rfc7517)]
  crv              Curve                                  EC                         Public                        [[IESG](#IESG)]                                                                   [[RFC7518, Section 6.2.1.1](https://www.iana.org/go/rfc7518)]
  x                X Coordinate                           EC                         Public                        [[IESG](#IESG)]                                                                   [[RFC7518, Section 6.2.1.2](https://www.iana.org/go/rfc7518)]
  y                Y Coordinate                           EC                         Public                        [[IESG](#IESG)]                                                                   [[RFC7518, Section 6.2.1.3](https://www.iana.org/go/rfc7518)]
  d                ECC Private Key                        EC                         Private                       [[IESG](#IESG)]                                                                   [[RFC7518, Section 6.2.2.1](https://www.iana.org/go/rfc7518)]
  n                Modulus                                RSA                        Public                        [[IESG](#IESG)]                                                                   [[RFC7518, Section 6.3.1.1](https://www.iana.org/go/rfc7518)]
  e                Exponent                               RSA                        Public                        [[IESG](#IESG)]                                                                   [[RFC7518, Section 6.3.1.2](https://www.iana.org/go/rfc7518)]
  d                Private Exponent                       RSA                        Private                       [[IESG](#IESG)]                                                                   [[RFC7518, Section 6.3.2.1](https://www.iana.org/go/rfc7518)]
  p                First Prime Factor                     RSA                        Private                       [[IESG](#IESG)]                                                                   [[RFC7518, Section 6.3.2.2](https://www.iana.org/go/rfc7518)]
  q                Second Prime Factor                    RSA                        Private                       [[IESG](#IESG)]                                                                   [[RFC7518, Section 6.3.2.3](https://www.iana.org/go/rfc7518)]
  dp               First Factor CRT Exponent              RSA                        Private                       [[IESG](#IESG)]                                                                   [[RFC7518, Section 6.3.2.4](https://www.iana.org/go/rfc7518)]
  dq               Second Factor CRT Exponent             RSA                        Private                       [[IESG](#IESG)]                                                                   [[RFC7518, Section 6.3.2.5](https://www.iana.org/go/rfc7518)]
  qi               First CRT Coefficient                  RSA                        Private                       [[IESG](#IESG)]                                                                   [[RFC7518, Section 6.3.2.6](https://www.iana.org/go/rfc7518)]
  oth              Other Primes Info                      RSA                        Private                       [[IESG](#IESG)]                                                                   [[RFC7518, Section 6.3.2.7](https://www.iana.org/go/rfc7518)]
  k                Key Value                              oct                        Private                       [[IESG](#IESG)]                                                                   [[RFC7518, Section 6.4.1](https://www.iana.org/go/rfc7518)]
  crv              The subtype of key pair                OKP                        Public                        [[IESG](#IESG)]                                                                   [[RFC8037, Section 2](https://www.iana.org/go/rfc8037)]
  d                The private key                        OKP                        Private                       [[IESG](#IESG)]                                                                   [[RFC8037, Section 2](https://www.iana.org/go/rfc8037)]
  x                The public key                         OKP                        Public                        [[IESG](#IESG)]                                                                   [[RFC8037, Section 2](https://www.iana.org/go/rfc8037)]
  ext              Extractable                            \*                         Public                        [[W3C\_Web\_Cryptography\_Working\_Group](#W3C_Web_Cryptography_Working_Group)]   [[https://www.w3.org/TR/WebCryptoAPI](https://www.w3.org/TR/WebCryptoAPI)]

JSON Web Key Use
----------------

Registration Procedure(s)
:   Specification Required

Expert(s)
:   Jeff Hodges, Joe Hildebrand, Sean Turner

Reference
:   [[RFC7517](https://www.iana.org/go/rfc7517)]
Note
:   Registration requests should be sent to the mailing list 
        described in [RFC7517].
            

Available Formats
:   [![](/_img/icons/text-csv.png)\
    CSV](web-key-use.csv)

  Use Member Value   Use Description            Change Controller   Reference
  ------------------ -------------------------- ------------------- -----------------------------------------------------------
  sig                Digital Signature or MAC   [[IESG](#IESG)]     [[RFC7517, Section 4.2](https://www.iana.org/go/rfc7517)]
  enc                Encryption                 [[IESG](#IESG)]     [[RFC7517, Section 4.2](https://www.iana.org/go/rfc7517)]

JSON Web Key Operations
-----------------------

Registration Procedure(s)
:   Specification Required

Expert(s)
:   Jeff Hodges, Joe Hildebrand, Sean Turner

Reference
:   [[RFC7517](https://www.iana.org/go/rfc7517)]
Note
:   Registration requests should be sent to the mailing list 
        described in [RFC7517].
            

Available Formats
:   [![](/_img/icons/text-csv.png)\
    CSV](web-key-operations.csv)

  Key Operation Value   Key Operation Description                                Change Controller   Reference
  --------------------- -------------------------------------------------------- ------------------- -----------------------------------------------------------
  sign                  Compute digital signature or MAC                         [[IESG](#IESG)]     [[RFC7517, Section 4.3](https://www.iana.org/go/rfc7517)]
  verify                Verify digital signature or MAC                          [[IESG](#IESG)]     [[RFC7517, Section 4.3](https://www.iana.org/go/rfc7517)]
  encrypt               Encrypt content                                          [[IESG](#IESG)]     [[RFC7517, Section 4.3](https://www.iana.org/go/rfc7517)]
  decrypt               Decrypt content and validate decryption, if applicable   [[IESG](#IESG)]     [[RFC7517, Section 4.3](https://www.iana.org/go/rfc7517)]
  wrapKey               Encrypt key                                              [[IESG](#IESG)]     [[RFC7517, Section 4.3](https://www.iana.org/go/rfc7517)]
  unwrapKey             Decrypt key and validate decryption, if applicable       [[IESG](#IESG)]     [[RFC7517, Section 4.3](https://www.iana.org/go/rfc7517)]
  deriveKey             Derive key                                               [[IESG](#IESG)]     [[RFC7517, Section 4.3](https://www.iana.org/go/rfc7517)]
  deriveBits            Derive bits not to be used as a key                      [[IESG](#IESG)]     [[RFC7517, Section 4.3](https://www.iana.org/go/rfc7517)]

JSON Web Key Set Parameters
---------------------------

Registration Procedure(s)
:   Specification Required

Expert(s)
:   Jeff Hodges, Joe Hildebrand, Sean Turner

Reference
:   [[RFC7517](https://www.iana.org/go/rfc7517)]
Note
:   Registration requests should be sent to the mailing list 
         described in [RFC7517].
            

Available Formats
:   [![](/_img/icons/text-csv.png)\
    CSV](web-key-set-parameters.csv)

  Parameter Name   Parameter Description   Change Controller   Reference
  ---------------- ----------------------- ------------------- -----------------------------------------------------------
  keys             Array of JWK Values     [[IESG](#IESG)]     [[RFC7517, Section 5.1](https://www.iana.org/go/rfc7517)]

Contact Information {.people}
===================

  ID                                         Name                                 Contact URI                                    Last Updated
  ------------------------------------------ ------------------------------------ ---------------------------------------------- --------------
  [IESG]                                     IESG                                 [mailto:iesg&ietf.org](mailto:iesg&ietf.org)   
  [W3C\_Web\_Cryptography\_Working\_Group]   W3C Web Cryptography Working Group   [http://www.w3.org](http://www.w3.org)         


