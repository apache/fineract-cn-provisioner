package io.mifos.provisioner.api.v1.domain;

import io.mifos.core.test.domain.ValidationTest;
import io.mifos.core.test.domain.ValidationTestCase;
import org.junit.runners.Parameterized;

import java.util.ArrayList;
import java.util.Collection;

/**
 * @author Myrle Krantz
 */
public class ApplicationTest extends ValidationTest<Application> {

  public ApplicationTest(ValidationTestCase<Application> testCase) {
    super(testCase);
  }

  @Override
  protected Application createValidTestSubject() {
    final Application ret = new Application();
    ret.setName("bebop-v3");
    ret.setHomepage("http://xyz.bebop:2021/v1");
    ret.setDescription("bebop manager");
    ret.setVendor("fineract");
    return ret;
  }

  @Parameterized.Parameters
  public static Collection testCases() {
    final Collection<ValidationTestCase> ret = new ArrayList<>();
    ret.add(new ValidationTestCase<Application>("basicCase")
            .adjustment(x -> {})
            .valid(true));
    ret.add(new ValidationTestCase<Application>("invalidApplicationName")
            .adjustment(x -> x.setName("bebop-dowop"))
            .valid(false));
    ret.add(new ValidationTestCase<Application>("nullApplicationName")
            .adjustment(x -> x.setName(null))
            .valid(false));
    return ret;
  }

}