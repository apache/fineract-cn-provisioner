package io.mifos.provisioner.api.v1.domain;

import io.mifos.core.test.domain.ValidationTest;
import io.mifos.core.test.domain.ValidationTestCase;
import org.junit.runners.Parameterized;

import java.util.ArrayList;
import java.util.Collection;

/**
 * @author Myrle Krantz
 */
public class AssignedApplicationTest extends ValidationTest<AssignedApplication> {

  public AssignedApplicationTest(ValidationTestCase<AssignedApplication> testCase) {
    super(testCase);
  }

  @Override
  protected AssignedApplication createValidTestSubject() {
    final AssignedApplication ret = new AssignedApplication();
    ret.setName("bebop-v3");
    return ret;
  }

  @Parameterized.Parameters
  public static Collection testCases() {
    final Collection<ValidationTestCase> ret = new ArrayList<>();
    ret.add(new ValidationTestCase<AssignedApplication>("basicCase")
            .adjustment(x -> {})
            .valid(true));
    ret.add(new ValidationTestCase<AssignedApplication>("invalidApplicationName")
            .adjustment(x -> x.setName("bebop-dowop"))
            .valid(false));
    ret.add(new ValidationTestCase<AssignedApplication>("nullApplicationName")
            .adjustment(x -> x.setName(null))
            .valid(false));
    return ret;
  }

}