/*
 * Copyright 2017 The Mifos Initiative.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.mifos.provisioner.api.v1.domain;

@SuppressWarnings("unused")
public class AuthenticationResponse {

  private final String token;
  private final String accessTokenExpiration;

  public AuthenticationResponse(final String token, final String accessTokenExpiration) {
    super();
    this.token = token;
    this.accessTokenExpiration = accessTokenExpiration;
  }

  public String getToken() {
    return token;
  }

  public String getAccessTokenExpiration() {
    return accessTokenExpiration;
  }
}
