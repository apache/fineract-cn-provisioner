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
package io.mifos.provisioner;

import java.io.BufferedWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.spec.RSAPrivateKeySpec;
import java.security.spec.RSAPublicKeySpec;

public class GenerateRsaKeyPair {

  private GenerateRsaKeyPair() {
    super();
  }

  public static void main(String[] args) throws Exception {
    final GenerateRsaKeyPair generateRsaKeyPair = new GenerateRsaKeyPair();
    generateRsaKeyPair.createKeyPair();
  }

  private void createKeyPair() throws Exception {
    final KeyFactory keyFactory = KeyFactory.getInstance("RSA");

    final KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
    keyPairGenerator.initialize(2048);
    final KeyPair keyPair = keyPairGenerator.genKeyPair();

    final RSAPublicKeySpec rsaPublicKeySpec = keyFactory.getKeySpec(keyPair.getPublic(), RSAPublicKeySpec.class);
    final BufferedWriter bufferedWriterPubKey = Files.newBufferedWriter(Paths.get("/home/mage/system.pub"));
    bufferedWriterPubKey.write(rsaPublicKeySpec.getModulus().toString());
    bufferedWriterPubKey.newLine();
    bufferedWriterPubKey.write(rsaPublicKeySpec.getPublicExponent().toString());
    bufferedWriterPubKey.flush();
    bufferedWriterPubKey.close();

    final RSAPrivateKeySpec rsaPrivateKeySpec = keyFactory.getKeySpec(keyPair.getPrivate(), RSAPrivateKeySpec.class);
    final BufferedWriter bufferedWriterPrivateKey = Files.newBufferedWriter(Paths.get("/home/mage/system"));
    bufferedWriterPrivateKey.write(rsaPrivateKeySpec.getModulus().toString());
    bufferedWriterPrivateKey.newLine();
    bufferedWriterPrivateKey.write(rsaPrivateKeySpec.getPrivateExponent().toString());
    bufferedWriterPrivateKey.flush();
    bufferedWriterPrivateKey.close();

  }
}
