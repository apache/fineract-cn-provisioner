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

import io.mifos.core.test.domain.ValidationTest;
import io.mifos.core.test.domain.ValidationTestCase;
import org.apache.commons.lang.RandomStringUtils;
import org.junit.runners.Parameterized;

import java.util.ArrayList;
import java.util.Collection;

/**
 * @author Myrle Krantz
 */
public class TenantTest extends ValidationTest<Tenant> {

  public TenantTest(ValidationTestCase<Tenant> testCase) {
    super(testCase);
  }

  @Override
  protected Tenant createValidTestSubject() {
    final Tenant ret = new Tenant();
    ret.setIdentifier("identifier");
    ret.setName("bebop-v3");
    final CassandraConnectionInfo cassandraConnectionInfo = new CassandraConnectionInfo();
    cassandraConnectionInfo.setClusterName("");
    cassandraConnectionInfo.setContactPoints("");
    cassandraConnectionInfo.setKeyspace("");
    cassandraConnectionInfo.setReplicas("");
    cassandraConnectionInfo.setReplicationType("");
    ret.setCassandraConnectionInfo(cassandraConnectionInfo);
    ret.setDatabaseConnectionInfo(new DatabaseConnectionInfo());
    return ret;
  }

  @Parameterized.Parameters
  public static Collection testCases() {
    final Collection<ValidationTestCase> ret = new ArrayList<>();
    ret.add(new ValidationTestCase<Tenant>("basicCase")
            .adjustment(x -> {})
            .valid(true));
    ret.add(new ValidationTestCase<Tenant>("invalidIdentifier")
            .adjustment(x -> x.setIdentifier(RandomStringUtils.randomAlphanumeric(33)))
            .valid(false));
    return ret;
  }

}