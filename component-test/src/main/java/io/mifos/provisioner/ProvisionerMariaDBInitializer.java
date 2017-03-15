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

import ch.vorburger.exec.ManagedProcessException;
import ch.vorburger.mariadb4j.DB;
import org.junit.rules.ExternalResource;

/**
 * @author Myrle Krantz
 */
public class ProvisionerMariaDBInitializer extends ExternalResource {
  private static DB EMBEDDED_MARIA_DB;
  @Override
  protected void before() throws ManagedProcessException {
    EMBEDDED_MARIA_DB = DB.newEmbeddedDB(3306);
    EMBEDDED_MARIA_DB.start();
  }

  @Override
  protected void after() {
    try {
      EMBEDDED_MARIA_DB.stop();
    } catch (ManagedProcessException e) {
      throw new RuntimeException(e);
    }
  }
}
