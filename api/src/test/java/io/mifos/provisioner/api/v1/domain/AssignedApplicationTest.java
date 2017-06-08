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