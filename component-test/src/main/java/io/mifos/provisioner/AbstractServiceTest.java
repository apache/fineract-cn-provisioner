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

import io.mifos.core.test.env.TestEnvironment;
import io.mifos.provisioner.api.v1.client.Provisioner;
import io.mifos.provisioner.config.ProvisionerActiveMQProperties;
import io.mifos.provisioner.config.ProvisionerServiceConfig;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.rules.RuleChain;
import org.junit.rules.TestRule;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.netflix.feign.EnableFeignClients;
import org.springframework.cloud.netflix.ribbon.RibbonClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT,
        classes = {AbstractServiceTest.TestConfiguration.class},
        properties = {
                ProvisionerActiveMQProperties.ACTIVEMQ_BROKER_URL_PROP + "=" + ProvisionerActiveMQProperties.ACTIVEMQ_BROKER_URL_DEFAULT,
                ProvisionerActiveMQProperties.ACTIVEMQ_CONCURRENCY_PROP + "=" + ProvisionerActiveMQProperties.ACTIVEMQ_CONCURRENCY_DEFAULT}
)
public class AbstractServiceTest {
  private static final String APP_NAME = "provisioner-v1";
  private static final String CLIENT_ID = "sillyRabbit";

  @Configuration
  @EnableFeignClients(basePackages = {"io.mifos.provisioner.api.v1.client"})
  @RibbonClient(name = APP_NAME)
  @Import({ProvisionerServiceConfig.class})
  public static class TestConfiguration {
    public TestConfiguration() {
      super();
    }

    @Bean()
    public Logger logger() {
      return LoggerFactory.getLogger("test-logger");
    }
  }


  private static TestEnvironment testEnvironment = new TestEnvironment(APP_NAME);
  private static ProvisionerMariaDBInitializer mariaDBInitializer = new ProvisionerMariaDBInitializer();
  private static ProvisionerCassandraInitializer cassandraInitializer = new ProvisionerCassandraInitializer();

  @ClassRule
  public static TestRule orderClassRules = RuleChain
          .outerRule(testEnvironment)
          .around(mariaDBInitializer)
          .around(cassandraInitializer);

  @Autowired
  protected Provisioner provisioner;

  public AbstractServiceTest() {
    super();
  }

  @BeforeClass
  public static void setup() throws Exception {
    System.setProperty("system.privateKey.modulus", testEnvironment.getSystemPrivateKey().getModulus().toString());
    System.setProperty("system.privateKey.exponent", testEnvironment.getSystemPrivateKey().getPrivateExponent().toString());
    System.setProperty("system.initialclientid", CLIENT_ID);
  }

  protected String getClientId() {
    return CLIENT_ID;
  }
}
