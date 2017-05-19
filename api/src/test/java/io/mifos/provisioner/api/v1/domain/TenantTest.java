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