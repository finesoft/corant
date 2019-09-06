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
package org.corant.microprofile.jwt;

import org.jose4j.jwt.consumer.JwtContext;
import io.smallrye.jwt.auth.principal.DefaultJWTCallerPrincipal;
import io.smallrye.jwt.auth.principal.JWTAuthContextInfo;
import io.smallrye.jwt.auth.principal.JWTCallerPrincipal;
import io.smallrye.jwt.auth.principal.JWTCallerPrincipalFactory;
import io.smallrye.jwt.auth.principal.ParseException;

/**
 * corant-suites-mp-jwt
 *
 * @author bingo 下午4:31:57
 *
 */
public class MpJWTCallerPrincipalFactory extends JWTCallerPrincipalFactory {

  private MpDefaultJWTTokenParser parser = new MpDefaultJWTTokenParser();

  /**
   * Tries to load the JWTAuthContextInfo from CDI if the class level authContextInfo has not been
   * set.
   */
  public MpJWTCallerPrincipalFactory() {}

  @Override
  public JWTCallerPrincipal parse(final String token, final JWTAuthContextInfo authContextInfo)
      throws ParseException {

    JwtContext jwtContext = parser.parse(token, authContextInfo);
    String type = jwtContext.getJoseObjects().get(0).getHeader("typ");
    return new DefaultJWTCallerPrincipal(type, jwtContext.getJwtClaims());
  }
}
