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

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.RSAPrivateKeySpec;
import java.security.spec.RSAPublicKeySpec;

/**
 * @author Myrle Krantz
 */
public class CheckKeysValid implements ConstraintValidator<KeysValid, SystemProperties> {

  @Override
  public void initialize(KeysValid constraintAnnotation) {
  }

  @Override
  public boolean isValid(final SystemProperties value, final ConstraintValidatorContext context) {
    if (value.getPrivateKey().getModulus() == null || value.getPrivateKey().getExponent() == null ||
        value.getPublicKey().getModulus() == null ||value.getPublicKey().getExponent() == null)
      return false;

    try {
      final KeyFactory keyFactory = KeyFactory.getInstance("RSA");
      final RSAPrivateKeySpec rsaPrivateKeySpec
          = new RSAPrivateKeySpec(value.getPrivateKey().getModulus(), value.getPrivateKey().getExponent());
      final PrivateKey privateKey = keyFactory.generatePrivate(rsaPrivateKeySpec);

      final RSAPublicKeySpec rsaPublicKeySpec
          = new RSAPublicKeySpec(value.getPublicKey().getModulus(), value.getPublicKey().getExponent());
      final PublicKey publicKey = keyFactory.generatePublic(rsaPublicKeySpec);

      final Signature signature = Signature.getInstance("NONEwithRSA");
      signature.initSign(privateKey);
      final byte[] signed = signature.sign();

      signature.initVerify(publicKey);
      return signature.verify(signed);
    } catch (final NoSuchAlgorithmException | InvalidKeySpecException | InvalidKeyException | SignatureException e) {
      return false;
    }
  }
}
