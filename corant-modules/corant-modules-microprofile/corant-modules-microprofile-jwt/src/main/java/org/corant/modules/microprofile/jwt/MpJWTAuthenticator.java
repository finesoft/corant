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
package org.corant.modules.microprofile.jwt;

import static java.util.Collections.singletonList;
import static org.corant.context.Beans.resolve;
import static org.corant.context.Beans.select;
import java.util.stream.Stream;
import javax.enterprise.inject.Instance;
import org.corant.modules.security.AuthenticationData;
import org.corant.modules.security.AuthenticationException;
import org.corant.modules.security.AuthenticatorCallback;
import org.corant.modules.security.Token;
import org.corant.modules.security.shared.AbstractAuthenticator;
import org.corant.modules.security.shared.SimpleAuthcData;
import org.corant.shared.ubiquity.Sortable;
import org.eclipse.microprofile.jwt.JsonWebToken;
import io.smallrye.jwt.auth.principal.JWTParser;

/**
 * corant-modules-microprofile-jwt
 *
 * @author bingo 上午11:00:57
 *
 */
public class MpJWTAuthenticator extends AbstractAuthenticator {

  public static final MpJWTAuthenticator DFLT_INST = new MpJWTAuthenticator();

  @Override
  protected AuthenticationData doAuthenticate(Token token) throws AuthenticationException {
    if (!(token instanceof MpJWTJsonWebToken)) {
      throw new AuthenticationException();
    }
    String bearerToken = ((MpJWTJsonWebToken) token).getData();
    if (bearerToken != null) {
      try {
        JsonWebToken jwtPrincipal = resolve(JWTParser.class).parse(bearerToken);
        return new SimpleAuthcData(bearerToken, singletonList(new MpJWTPrincipal(jwtPrincipal)));
      } catch (Exception e) {
        throw new AuthenticationException(e);
      }
    }
    return null;
  }

  @Override
  protected Stream<AuthenticatorCallback> resolveCallbacks() {
    Instance<AuthenticatorCallback> cbs = select(AuthenticatorCallback.class);
    if (!cbs.isUnsatisfied()) {
      cbs.stream().sorted(Sortable::compare);
    }
    return Stream.empty();
  }

}
