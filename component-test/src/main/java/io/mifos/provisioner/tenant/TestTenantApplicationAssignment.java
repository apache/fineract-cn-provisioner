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
package io.mifos.provisioner.tenant;

import io.mifos.anubis.api.v1.client.Anubis;
import io.mifos.anubis.api.v1.domain.PermittableEndpoint;
import io.mifos.anubis.api.v1.domain.Signature;
import io.mifos.anubis.provider.SystemRsaKeyProvider;
import io.mifos.anubis.test.v1.SystemSecurityEnvironment;
import io.mifos.anubis.token.TokenSerializationResult;
import io.mifos.core.api.context.AutoSeshat;
import io.mifos.core.api.util.ApiConstants;
import io.mifos.core.api.util.ApiFactory;
import io.mifos.core.lang.AutoTenantContext;
import io.mifos.core.test.env.TestEnvironment;
import io.mifos.identity.api.v1.client.IdentityService;
import io.mifos.identity.api.v1.domain.PermittableGroup;
import io.mifos.provisioner.ProvisionerCassandraInitializer;
import io.mifos.provisioner.ProvisionerMariaDBInitializer;
import io.mifos.provisioner.api.v1.client.ProvisionerService;
import io.mifos.provisioner.api.v1.domain.*;
import io.mifos.provisioner.config.ProvisionerConstants;
import io.mifos.provisioner.config.ProvisionerServiceConfig;
import io.mifos.provisioner.internal.service.applications.ApplicationCallContextProvider;
import io.mifos.provisioner.internal.util.TokenProvider;
import org.junit.*;
import org.junit.rules.RuleChain;
import org.junit.rules.TestRule;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.netflix.feign.EnableFeignClients;
import org.springframework.cloud.netflix.ribbon.RibbonClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.*;

/**
 * @author Myrle Krantz
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
public class TestTenantApplicationAssignment {
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

    @Bean()
    public ApplicationCallContextProvider applicationCallContextProvider(
            final ApiFactory apiFactory,
            final @Qualifier("tokenProviderSpy") TokenProvider tokenProviderSpy)
    {
      return Mockito.spy(new ApplicationCallContextProvider(apiFactory, tokenProviderSpy));
    }

    @Bean(name = "tokenProviderSpy")
    public TokenProvider tokenProviderSpy(final @Qualifier("tokenProvider") TokenProvider tokenProvider)
    {
      return Mockito.spy(tokenProvider);
    }
  }


  private static TestEnvironment testEnvironment = new TestEnvironment(APP_NAME);
  private static ProvisionerMariaDBInitializer mariaDBInitializer = new ProvisionerMariaDBInitializer();
  private static ProvisionerCassandraInitializer cassandraInitializer = new ProvisionerCassandraInitializer();
  private static SystemSecurityEnvironment systemSecurityEnvironment
          = new SystemSecurityEnvironment(testEnvironment.getSeshatPublicKey(), testEnvironment.getSeshatPrivateKey());

  @ClassRule
  public static TestRule orderClassRules = RuleChain
          .outerRule(testEnvironment)
          .around(mariaDBInitializer)
          .around(cassandraInitializer);

  @Autowired
  private ProvisionerService provisionerService;

  @Autowired
  @Qualifier("tokenProviderSpy")
  protected TokenProvider tokenProviderSpy;

  @Autowired
  protected ApplicationCallContextProvider applicationCallContextProviderSpy;

  @Autowired
  protected SystemRsaKeyProvider systemRsaKeyProvider;

  private AutoSeshat autoSeshat;

  public TestTenantApplicationAssignment() {
    super();
  }

  @BeforeClass
  public static void setup() throws Exception {

    System.setProperty("provisioner.privateKey.modulus", testEnvironment.getSeshatPrivateKey().getModulus().toString());
    System.setProperty("provisioner.privateKey.exponent", testEnvironment.getSeshatPrivateKey().getPrivateExponent().toString());
    System.setProperty("provisioner.initialclientid", CLIENT_ID);
  }

  @Before
  public void before()
  {
    final AuthenticationResponse authentication = this.provisionerService.authenticate(
            CLIENT_ID, ApiConstants.SYSTEM_SU, ProvisionerConstants.INITIAL_PWD);
    autoSeshat = new AutoSeshat(authentication.getToken());
  }

  @After
  public void after() throws InterruptedException {
    this.provisionerService.deleteTenant(Fixture.getCompTestTenant().getIdentifier());
    Thread.sleep(500L);
    autoSeshat.close();
  }

  private static class TokenChecker implements Answer<TokenSerializationResult> {
    TokenSerializationResult result = null;

    @Override
    public TokenSerializationResult answer(final InvocationOnMock invocation) throws Throwable {
      result = (TokenSerializationResult) invocation.callRealMethod();
      Assert.assertNotNull(result);
      return result;
    }
  }

  private class VerifyIsisInitializeContext implements Answer<Signature> {

    private final BigInteger modulus;
    private final BigInteger exponent;

    private boolean validSecurityContext = false;

    VerifyIsisInitializeContext(final BigInteger modulus, final BigInteger exponent) {
      this.modulus = modulus;
      this.exponent = exponent;
    }

    @Override
    public Signature answer(final InvocationOnMock invocation) throws Throwable {
      validSecurityContext = systemSecurityEnvironment.isValidSystemSecurityContext("identity", "1", Fixture.TENANT_IDENTIFIER);

      final Signature fakeSignature = new Signature();
      fakeSignature.setPublicKeyMod(modulus);
      fakeSignature.setPublicKeyExp(exponent);

      return fakeSignature;
    }

    boolean isValidSecurityContext() {
      return validSecurityContext;
    }
  }

  private class VerifyAnubisInitializeContext implements Answer<Void> {

    private boolean validSecurityContext = false;
    final private String target;

    private VerifyAnubisInitializeContext(final String target) {
      this.target = target;
    }

    @Override
    public Void answer(final InvocationOnMock invocation) throws Throwable {
      validSecurityContext = systemSecurityEnvironment.isValidSystemSecurityContext(target, "1", Fixture.TENANT_IDENTIFIER);
      return null;
    }

    boolean isValidSecurityContext() {
      return validSecurityContext;
    }
  }


  private class VerifyAnubisPermittablesContext implements Answer<List<PermittableEndpoint>> {

    private boolean validSecurityContext = false;
    private final List<PermittableEndpoint> answer;

    private VerifyAnubisPermittablesContext(final List<PermittableEndpoint> answer) {
      this.answer = answer;
    }

    @Override
    public List<PermittableEndpoint> answer(final InvocationOnMock invocation) throws Throwable {
      validSecurityContext = systemSecurityEnvironment.isValidGuestSecurityContext(Fixture.TENANT_IDENTIFIER);
      return answer;
    }

    boolean isValidSecurityContext() {
      return validSecurityContext;
    }
  }

  @Test
  public void testTenantApplicationAssignment() throws InterruptedException {
    //Create io.mifos.provisioner.tenant
    final Tenant tenant = Fixture.getCompTestTenant();
    provisionerService.createTenant(tenant);


    //Create identity service application
    final Application identityServiceApp = new Application();
    identityServiceApp.setName("identity-v1");
    identityServiceApp.setHomepage("http://xyz.identity:2020/v1");
    identityServiceApp.setDescription("identity manager");
    identityServiceApp.setVendor("fineract");

    provisionerService.createApplication(identityServiceApp);


    //Assign identity service application.  This requires some mocking since we can't actually call initialize in a component test.
    final AssignedApplication identityServiceAssigned = new AssignedApplication();
    identityServiceAssigned.setName("identity-v1");


    final IdentityService identityServiceMock = Mockito.mock(IdentityService.class);
    when(applicationCallContextProviderSpy.getApplication(IdentityService.class, "http://xyz.identity:2020/v1")).thenReturn(identityServiceMock);

    final VerifyIsisInitializeContext verifyInitializeContextAndReturnSignature;
    try (final AutoTenantContext ignored = new AutoTenantContext(Fixture.TENANT_IDENTIFIER)) {
      verifyInitializeContextAndReturnSignature = new VerifyIsisInitializeContext(
              systemSecurityEnvironment.tenantPublicKey().getModulus(),
              systemSecurityEnvironment.tenantPublicKey().getPublicExponent());
    }
    doAnswer(verifyInitializeContextAndReturnSignature).when(identityServiceMock).initialize(anyString());

    final TokenChecker tokenChecker = new TokenChecker();
    doAnswer(tokenChecker).when(tokenProviderSpy).createToken(Fixture.TENANT_IDENTIFIER, "identity-v1", 2L, TimeUnit.MINUTES);

    {
      final IdentityManagerInitialization identityServiceAdminInitialization
              = provisionerService.assignIdentityManager(tenant.getIdentifier(), identityServiceAssigned);

      Assert.assertTrue(verifyInitializeContextAndReturnSignature.isValidSecurityContext());
      Assert.assertNotNull(identityServiceAdminInitialization);
      Assert.assertNotNull(identityServiceAdminInitialization.getAdminPassword());
    }

    verify(applicationCallContextProviderSpy).getApplicationCallContext(Fixture.TENANT_IDENTIFIER, "identity-v1");


    //Create horus application.
    final Application officeApp = new Application();
    officeApp.setName("office-v1");
    officeApp.setHomepage("http://xyz.office:2021/v1");
    officeApp.setDescription("organization manager");
    officeApp.setVendor("fineract");

    provisionerService.createApplication(officeApp);


    //Assign horus application.
    final AssignedApplication officeAssigned = new AssignedApplication();
    officeAssigned.setName("office-v1");

    final Anubis anubisMock = Mockito.mock(Anubis.class);
    when(applicationCallContextProviderSpy.getApplication(Anubis.class, "http://xyz.office:2021/v1")).thenReturn(anubisMock);


    final PermittableEndpoint xxPermittableEndpoint = new PermittableEndpoint("/x/y", "POST", "x");
    final PermittableEndpoint xyPermittableEndpoint = new PermittableEndpoint("/y/z", "POST", "x");
    final PermittableEndpoint xyGetPermittableEndpoint = new PermittableEndpoint("/y/z", "GET", "x");
    final PermittableEndpoint mPermittableEndpoint = new PermittableEndpoint("/m/n", "GET", "m");

    final VerifyAnubisInitializeContext verifyAnubisInitializeContext;
    final VerifyAnubisPermittablesContext verifyAnubisPermittablesContext;
    try (final AutoTenantContext ignored = new AutoTenantContext(Fixture.TENANT_IDENTIFIER)) {
      verifyAnubisInitializeContext = new VerifyAnubisInitializeContext("office");
      verifyAnubisPermittablesContext = new VerifyAnubisPermittablesContext(Arrays.asList(xxPermittableEndpoint, xxPermittableEndpoint, xyPermittableEndpoint, xyGetPermittableEndpoint, mPermittableEndpoint));
    }
    doAnswer(verifyAnubisInitializeContext).when(anubisMock).initialize(anyObject(), anyObject());
    doAnswer(verifyAnubisPermittablesContext).when(anubisMock).getPermittableEndpoints();

    {
      provisionerService.assignApplications(tenant.getIdentifier(), Collections.singletonList(officeAssigned));
      Thread.sleep(500L); //Application assigning is asynchronous.
    }

    verify(applicationCallContextProviderSpy).getApplicationCallContext(Fixture.TENANT_IDENTIFIER, "office-v1");
    verify(applicationCallContextProviderSpy, never()).getApplicationCallContext(eq(Fixture.TENANT_NAME), Mockito.anyString());
    verify(tokenProviderSpy).createToken(Fixture.TENANT_IDENTIFIER, "office-v1", 2L, TimeUnit.MINUTES);

    verify(identityServiceMock).createPermittableGroup(new PermittableGroup("x", Arrays.asList(xxPermittableEndpoint, xyPermittableEndpoint, xyGetPermittableEndpoint)));
    verify(identityServiceMock).createPermittableGroup(new PermittableGroup("m", Collections.singletonList(mPermittableEndpoint)));

    Assert.assertTrue(verifyAnubisInitializeContext.isValidSecurityContext());
    Assert.assertTrue(verifyAnubisPermittablesContext.isValidSecurityContext());
  }
}
