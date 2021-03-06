/*
 * Copyright (C) 2017-2019 HERE Europe B.V.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 * License-Filename: LICENSE
 */

package com.here.xyz.hub.auth;

import static io.netty.handler.codec.http.HttpResponseStatus.UNAUTHORIZED;

import com.here.xyz.hub.rest.ApiParam.Query;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.jwt.JWTAuth;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.JWTAuthHandler;
import io.vertx.ext.web.handler.impl.AuthHandlerImpl;
import io.vertx.ext.web.handler.impl.HttpStatusException;
import java.util.List;

public class JWTURIHandler extends AuthHandlerImpl implements JWTAuthHandler {

  private final JsonObject options = new JsonObject();

  private JWTURIHandler(JWTAuth authProvider) {
    super(authProvider, "Bearer");
  }

  public static JWTAuthHandler create(JWTAuth authProvider) {
    return new JWTURIHandler(authProvider);
  }

  @Override
  public JWTAuthHandler setAudience(List<String> audience) {
    options.put("audience", new JsonArray(audience));
    return this;
  }

  @Override
  public JWTAuthHandler setIssuer(String issuer) {
    options.put("issuer", issuer);
    return this;
  }

  @Override
  public JWTAuthHandler setIgnoreExpiration(boolean ignoreExpiration) {
    options.put("ignoreExpiration", ignoreExpiration);
    return this;
  }

  @Override
  public void parseCredentials(RoutingContext context, Handler<AsyncResult<JsonObject>> handler) {
    final List<String> access_token = Query.queryParam(Query.ACCESS_TOKEN, context);
    if (access_token != null && access_token.size() > 0) {
      handler.handle(Future.succeededFuture(new JsonObject().put("jwt", access_token.get(0)).put("options", options)));
      return;
    }
    handler.handle(Future.failedFuture(new HttpStatusException(UNAUTHORIZED.code(), "Missing auth credentials.")));
  }

  @Override
  protected String authenticateHeader(RoutingContext context) {
    return "Bearer";
  }
}
