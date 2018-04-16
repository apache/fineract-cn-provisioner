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

import java.util.ArrayList;
import java.util.Collection;
import org.apache.fineract.cn.lang.security.RsaKeyPairFactory;
import org.apache.fineract.cn.test.domain.ValidationTest;
import org.apache.fineract.cn.test.domain.ValidationTestCase;
import org.junit.runners.Parameterized;

/**
 * @author Myrle Krantz
 */
public class SystemPropertiesTest extends ValidationTest<SystemProperties> {
  private static final RsaKeyPairFactory.KeyPairHolder keyPairHolder = RsaKeyPairFactory.createKeyPair();

  public SystemPropertiesTest(ValidationTestCase<SystemProperties> testCase) {
    super(testCase);
  }

  @Override
  protected SystemProperties createValidTestSubject() {
    final SystemProperties ret = new SystemProperties();
    ret.getPrivateKey().setModulus(keyPairHolder.getPrivateKeyMod());
    ret.getPrivateKey().setExponent(keyPairHolder.getPrivateKeyExp());
    ret.getPublicKey().setTimestamp(keyPairHolder.getTimestamp());
    ret.getPublicKey().setModulus(keyPairHolder.getPublicKeyMod());
    ret.getPublicKey().setExponent(keyPairHolder.getPublicKeyExp());
    return ret;
  }

  @Parameterized.Parameters
  public static Collection testCases() {
    final Collection<ValidationTestCase> ret = new ArrayList<>();
    ret.add(new ValidationTestCase<SystemProperties>("basicCase")
        .adjustment(x -> {})
        .valid(true));
    ret.add(new ValidationTestCase<SystemProperties>("missing private modulus")
        .adjustment(x -> x.getPrivateKey().setModulus(null))
        .valid(false));
    ret.add(new ValidationTestCase<SystemProperties>("mismatched keys")
        .adjustment(x -> {
          final RsaKeyPairFactory.KeyPairHolder keyPairHolder = RsaKeyPairFactory.createKeyPair();
          x.getPrivateKey().setModulus(keyPairHolder.getPrivateKeyMod());
          x.getPrivateKey().setExponent(keyPairHolder.getPrivateKeyExp());
        })
        .valid(false));
    ret.add(new ValidationTestCase<SystemProperties>("missing timestamp")
        .adjustment(x -> x.getPublicKey().setTimestamp(null))
        .valid(false));
    return ret;
  }

}