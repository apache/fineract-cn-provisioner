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
package org.apache.fineract.cn.provisioner.config;

import org.hibernate.validator.constraints.NotEmpty;
import org.hibernate.validator.constraints.Range;
import org.springframework.boot.context.properties.ConfigurationProperties;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.math.BigInteger;

/**
 * @author Myrle Krantz
 */
@KeysValid
@ConfigurationProperties(prefix = "system")
public class SystemProperties {
  @NotEmpty
  private String domain = "fineract.apache.org";

  @Valid
  private final Token token = new Token();

  @Valid
  private final PublicKey publicKey = new PublicKey();

  @Valid
  private final PrivateKey privateKey = new PrivateKey();

  public static class Token {
    @Range(min = 1)
    private int ttl = 60;

    public int getTtl() {
      return ttl;
    }

    public void setTtl(int ttl) {
      this.ttl = ttl;
    }
  }

  public static class PublicKey {
    @NotEmpty
    private String timestamp;

    @NotNull
    private BigInteger modulus;

    @NotNull
    private BigInteger exponent;

    public String getTimestamp() {
      return timestamp;
    }

    public void setTimestamp(String timestamp) {
      this.timestamp = timestamp;
    }

    public BigInteger getModulus() {
      return modulus;
    }

    public void setModulus(BigInteger modulus) {
      this.modulus = modulus;
    }

    public BigInteger getExponent() {
      return exponent;
    }

    public void setExponent(BigInteger exponent) {
      this.exponent = exponent;
    }
  }

  public static class PrivateKey {
    @NotNull
    private BigInteger modulus;

    @NotNull
    private BigInteger exponent;

    public BigInteger getModulus() {
      return modulus;
    }

    public void setModulus(BigInteger modulus) {
      this.modulus = modulus;
    }

    public BigInteger getExponent() {
      return exponent;
    }

    public void setExponent(BigInteger exponent) {
      this.exponent = exponent;
    }
  }

  public String getDomain() {
    return domain;
  }

  public void setDomain(String domain) {
    this.domain = domain;
  }

  public Token getToken() {
    return token;
  }

  public PublicKey getPublicKey() {
    return publicKey;
  }

  public PrivateKey getPrivateKey() {
    return privateKey;
  }
}
