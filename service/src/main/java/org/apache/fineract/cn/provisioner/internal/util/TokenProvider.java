/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.fineract.cn.provisioner.internal.util;

import java.math.BigInteger;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.spec.RSAPrivateKeySpec;
import java.util.concurrent.TimeUnit;
import org.apache.fineract.cn.anubis.api.v1.RoleConstants;
import org.apache.fineract.cn.anubis.token.SystemAccessTokenSerializer;
import org.apache.fineract.cn.anubis.token.TokenSerializationResult;

public class TokenProvider {
  private final String keyTimestamp;
  private final PrivateKey privateKey;
  private final SystemAccessTokenSerializer tokenSerializer;

  public TokenProvider(
      final String keyTimestamp,
      final BigInteger privateKeyModulus,
      final BigInteger privateKeyExponent,
      final SystemAccessTokenSerializer tokenSerializer) {
    super();
    this.tokenSerializer = tokenSerializer;

    try {
      this.keyTimestamp = keyTimestamp;
      final KeyFactory keyFactory = KeyFactory.getInstance("RSA");

      final RSAPrivateKeySpec rsaPrivateKeySpec
          = new RSAPrivateKeySpec(privateKeyModulus, privateKeyExponent);
      this.privateKey = keyFactory.generatePrivate(rsaPrivateKeySpec);

    } catch (final Exception ex) {
      throw new IllegalStateException("Could not read RSA key pair!", ex);
    }
  }

  public TokenSerializationResult createToken(
          final String subject,
          final String audience,
          final long ttl,
          final TimeUnit timeUnit) {
    SystemAccessTokenSerializer.Specification specification = new SystemAccessTokenSerializer.Specification();
    specification.setKeyTimestamp(keyTimestamp);
    specification.setTenant(subject);
    specification.setTargetApplicationName(audience);
    specification.setSecondsToLive(timeUnit.toSeconds(ttl));
    specification.setRole(RoleConstants.SYSTEM_ADMIN_ROLE_IDENTIFIER);
    specification.setPrivateKey(privateKey);

    return this.tokenSerializer.build(specification);
  }
}
