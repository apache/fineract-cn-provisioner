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
package org.apache.fineract.cn.provisioner.tenant;

import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.anyObject;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;

import com.google.gson.Gson;
import org.apache.fineract.cn.provisioner.ProvisionerCassandraInitializer;
import org.apache.fineract.cn.provisioner.ProvisionerMariaDBInitializer;
import org.apache.fineract.cn.provisioner.api.v1.client.Provisioner;
import org.apache.fineract.cn.provisioner.api.v1.domain.Application;
import org.apache.fineract.cn.provisioner.api.v1.domain.AssignedApplication;
import org.apache.fineract.cn.provisioner.api.v1.domain.AuthenticationResponse;
import org.apache.fineract.cn.provisioner.api.v1.domain.IdentityManagerInitialization;
import org.apache.fineract.cn.provisioner.api.v1.domain.Tenant;
import org.apache.fineract.cn.provisioner.config.ProvisionerConstants;
import org.apache.fineract.cn.provisioner.config.ProvisionerServiceConfig;
import org.apache.fineract.cn.provisioner.internal.listener.IdentityListener;
import org.apache.fineract.cn.provisioner.internal.service.applications.ApplicationCallContextProvider;
import org.apache.fineract.cn.provisioner.internal.util.TokenProvider;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.apache.finearct.cn.permittedfeignclient.api.v1.client.ApplicationPermissionRequirements;
import org.apache.finearct.cn.permittedfeignclient.api.v1.domain.ApplicationPermission;
import org.apache.fineract.cn.anubis.api.v1.client.Anubis;
import org.apache.fineract.cn.anubis.api.v1.domain.AllowedOperation;
import org.apache.fineract.cn.anubis.api.v1.domain.ApplicationSignatureSet;
import org.apache.fineract.cn.anubis.api.v1.domain.PermittableEndpoint;
import org.apache.fineract.cn.anubis.api.v1.domain.Signature;
import org.apache.fineract.cn.anubis.test.v1.SystemSecurityEnvironment;
import org.apache.fineract.cn.api.context.AutoSeshat;
import org.apache.fineract.cn.api.util.ApiConstants;
import org.apache.fineract.cn.api.util.ApiFactory;
import org.apache.fineract.cn.identity.api.v1.client.IdentityManager;
import org.apache.fineract.cn.identity.api.v1.domain.CallEndpointSet;
import org.apache.fineract.cn.identity.api.v1.domain.Permission;
import org.apache.fineract.cn.identity.api.v1.domain.PermittableGroup;
import org.apache.fineract.cn.identity.api.v1.events.ApplicationSignatureEvent;
import org.apache.fineract.cn.lang.AutoTenantContext;
import org.apache.fineract.cn.lang.security.RsaKeyPairFactory;
import org.apache.fineract.cn.test.env.TestEnvironment;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
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

/**
 * @author Myrle Krantz
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
public class TestTenantApplicationAssignment {
  private static final String APP_NAME = "provisioner-v1";
  private static final String CLIENT_ID = "sillyRabbit";

  @Configuration
  @EnableFeignClients(basePackages = {"org.apache.fineract.cn.provisioner.api.v1.client"})
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
          = new SystemSecurityEnvironment(testEnvironment.getSystemKeyTimestamp(), testEnvironment.getSystemPublicKey(), testEnvironment.getSystemPrivateKey());

  @ClassRule
  public static TestRule orderClassRules = RuleChain
          .outerRule(testEnvironment)
          .around(mariaDBInitializer)
          .around(cassandraInitializer);

  @SuppressWarnings("SpringAutowiredFieldsWarningInspection")
  @Autowired
  private Provisioner provisioner;

  @SuppressWarnings("SpringAutowiredFieldsWarningInspection")
  @Autowired
  private ApplicationCallContextProvider applicationCallContextProviderSpy;

  @SuppressWarnings("SpringAutowiredFieldsWarningInspection")
  @Autowired
  private IdentityListener identityListener;

  @SuppressWarnings("SpringAutowiredFieldsWarningInspection")
  @Autowired
  private Gson gson;

  private AutoSeshat autoSeshat;

  public TestTenantApplicationAssignment() {
    super();
  }

  @BeforeClass
  public static void setup() throws Exception {

    System.setProperty("system.privateKey.modulus", testEnvironment.getSystemPrivateKey().getModulus().toString());
    System.setProperty("system.privateKey.exponent", testEnvironment.getSystemPrivateKey().getPrivateExponent().toString());
    System.setProperty("system.initialclientid", CLIENT_ID);
  }

  @Before
  public void before()
  {
    final AuthenticationResponse authentication = this.provisioner.authenticate(
            CLIENT_ID, ApiConstants.SYSTEM_SU, ProvisionerConstants.INITIAL_PWD);
    autoSeshat = new AutoSeshat(authentication.getToken());
  }

  @After
  public void after() throws InterruptedException {
    this.provisioner.deleteTenant(Fixture.getCompTestTenant().getIdentifier());
    Thread.sleep(500L);
    autoSeshat.close();
  }

  private class VerifyIsisInitializeContext implements Answer<ApplicationSignatureSet> {

    private final String keyTimestamp;
    private final BigInteger modulus;
    private final BigInteger exponent;
    private final String tenantIdentifier;

    private boolean validSecurityContext = false;

    VerifyIsisInitializeContext(
            final String keyTimestamp,
            final BigInteger modulus,
            final BigInteger exponent,
            final String tenantIdentifier) {
      this.keyTimestamp = keyTimestamp;
      this.modulus = modulus;
      this.exponent = exponent;
      this.tenantIdentifier = tenantIdentifier;
    }

    @Override
    public ApplicationSignatureSet answer(final InvocationOnMock invocation) throws Throwable {
      validSecurityContext = systemSecurityEnvironment.isValidSystemSecurityContext("identity", "1", tenantIdentifier);

      final Signature fakeSignature = new Signature();
      fakeSignature.setPublicKeyMod(modulus);
      fakeSignature.setPublicKeyExp(exponent);

      final ApplicationSignatureSet ret = new ApplicationSignatureSet();
      ret.setTimestamp(keyTimestamp);
      ret.setApplicationSignature(fakeSignature);
      ret.setIdentityManagerSignature(fakeSignature);

      return ret;
    }

    String getKeyTimestamp() {
      return keyTimestamp;
    }

    boolean isValidSecurityContext() {
      return validSecurityContext;
    }
  }

  private class VerifyAnubisInitializeContext implements Answer<Void> {

    private boolean validSecurityContext = false;
    final private String target;
    private final String tenantIdentifier;

    private VerifyAnubisInitializeContext(final String target, String tenantIdentifier) {
      this.target = target;
      this.tenantIdentifier = tenantIdentifier;
    }

    @Override
    public Void answer(final InvocationOnMock invocation) throws Throwable {
      validSecurityContext = systemSecurityEnvironment.isValidSystemSecurityContext(target, "1", tenantIdentifier);
      return null;
    }

    boolean isValidSecurityContext() {
      return validSecurityContext;
    }
  }

  private class VerifyCreateSignatureSetContext implements Answer<ApplicationSignatureSet> {

    private final RsaKeyPairFactory.KeyPairHolder answer;
    private boolean validSecurityContext = false;
    final private String target;
    private final String tenantIdentifier;

    private VerifyCreateSignatureSetContext(final RsaKeyPairFactory.KeyPairHolder answer, final String target, final String tenantIdentifier) {
      this.answer = answer;
      this.target = target;
      this.tenantIdentifier = tenantIdentifier;
    }

    @Override
    public ApplicationSignatureSet answer(final InvocationOnMock invocation) throws Throwable {
      final String timestamp = invocation.getArgumentAt(0, String.class);
      final Signature identityManagerSignature = invocation.getArgumentAt(1, Signature.class);
      validSecurityContext = systemSecurityEnvironment.isValidSystemSecurityContext(target, "1", tenantIdentifier);

      return new ApplicationSignatureSet(
              timestamp,
              new Signature(answer.getPublicKeyMod(), answer.getPublicKeyExp()),
              identityManagerSignature);
    }

    boolean isValidSecurityContext() {
      return validSecurityContext;
    }
  }


  private class VerifyAnubisPermittablesContext implements Answer<List<PermittableEndpoint>> {

    private boolean validSecurityContext = false;
    private final List<PermittableEndpoint> answer;
    private final String tenantIdentifier;

    private VerifyAnubisPermittablesContext(final List<PermittableEndpoint> answer, final String tenantIdentifier) {
      this.answer = answer;
      this.tenantIdentifier = tenantIdentifier;
    }

    @Override
    public List<PermittableEndpoint> answer(final InvocationOnMock invocation) throws Throwable {
      validSecurityContext = systemSecurityEnvironment.isValidGuestSecurityContext(tenantIdentifier);
      return answer;
    }

    boolean isValidSecurityContext() {
      return validSecurityContext;
    }
  }


  private class VerifyAnputRequiredPermissionsContext implements Answer<List<ApplicationPermission>> {

    private boolean validSecurityContext = false;
    private final List<ApplicationPermission> answer;
    private final String tenantIdentifier;

    private VerifyAnputRequiredPermissionsContext(final List<ApplicationPermission> answer, final String tenantIdentifier) {
      this.answer = answer;
      this.tenantIdentifier = tenantIdentifier;
    }

    @Override
    public List<ApplicationPermission> answer(final InvocationOnMock invocation) throws Throwable {
      validSecurityContext = systemSecurityEnvironment.isValidGuestSecurityContext(tenantIdentifier);
      return answer;
    }

    boolean isValidSecurityContext() {
      return validSecurityContext;
    }
  }

  private class VerifyIsisCreatePermittableGroup implements Answer<Void> {

    private boolean validSecurityContext = true;
    private final String tenantIdentifier;
    private int callCount = 0;

    private VerifyIsisCreatePermittableGroup(final String tenantIdentifier) {
      this.tenantIdentifier = tenantIdentifier;
    }

    @Override
    public Void answer(final InvocationOnMock invocation) throws Throwable {
      final boolean validSecurityContextForThisCall = systemSecurityEnvironment.isValidSystemSecurityContext("identity", "1", tenantIdentifier);
      validSecurityContext = validSecurityContext && validSecurityContextForThisCall;
      callCount++;

      final PermittableGroup arg = invocation.getArgumentAt(0, PermittableGroup.class);
      identityListener.onCreatePermittableGroup(tenantIdentifier, arg.getIdentifier());
      return null;
    }

    boolean isValidSecurityContext() {
      return validSecurityContext;
    }

    int getCallCount() {
      return callCount;
    }
  }

  private class VerifyIsisSetApplicationSignature implements Answer<Void> {

    private boolean validSecurityContext = false;
    private final String tenantIdentifier;

    private VerifyIsisSetApplicationSignature(final String tenantIdentifier) {
      this.tenantIdentifier = tenantIdentifier;
    }

    @Override
    public Void answer(final InvocationOnMock invocation) throws Throwable {
      validSecurityContext = systemSecurityEnvironment.isValidSystemSecurityContext("identity", "1", tenantIdentifier);

      final String applicationIdentifier = invocation.getArgumentAt(0, String.class);
      final String keyTimestamp = invocation.getArgumentAt(1, String.class);
      identityListener.onSetApplicationSignature(tenantIdentifier,
              gson.toJson(new ApplicationSignatureEvent(applicationIdentifier, keyTimestamp)));
      return null;
    }

    boolean isValidSecurityContext() {
      return validSecurityContext;
    }
  }

  private class VerifyIsisCreateApplicationPermission implements Answer<Void> {

    private boolean validSecurityContext = true;
    private final String tenantIdentifier;
    private final String applicationIdentifier;
    private int callCount = 0;

    private VerifyIsisCreateApplicationPermission(final String tenantIdentifier, final String applicationIdentifier) {
      this.tenantIdentifier = tenantIdentifier;
      this.applicationIdentifier = applicationIdentifier;
    }

    @Override
    public Void answer(final InvocationOnMock invocation) throws Throwable {
      final boolean validSecurityContextForThisCall = systemSecurityEnvironment.isValidSystemSecurityContext("identity", "1", tenantIdentifier);
      validSecurityContext = validSecurityContext && validSecurityContextForThisCall;
      callCount++;

      final String callApplicationIdentifier = invocation.getArgumentAt(0, String.class);
      Assert.assertEquals(this.applicationIdentifier, callApplicationIdentifier);
      return null;
    }



    boolean isValidSecurityContext() {
      return validSecurityContext;
    }

    int getCallCount() {
      return callCount;
    }
  }

  @Test
  public void testTenantApplicationAssignment() throws Exception {
    //Create org.apache.fineract.cn.provisioner.tenant
    final Tenant tenant = Fixture.getCompTestTenant();
    provisioner.createTenant(tenant);



    //Create identity service application
    final Application identityServiceApp = new Application();
    identityServiceApp.setName("identity-v1");
    identityServiceApp.setHomepage("http://xyz.identity:2020/v1");
    identityServiceApp.setDescription("identity manager");
    identityServiceApp.setVendor("fineract");

    provisioner.createApplication(identityServiceApp);


    //Assign identity service application.  This requires some mocking since we can't actually call initialize in a component test.
    final AssignedApplication identityServiceAssigned = new AssignedApplication();
    identityServiceAssigned.setName("identity-v1");


    final IdentityManager identityServiceMock = Mockito.mock(IdentityManager.class);
    when(applicationCallContextProviderSpy.getApplication(IdentityManager.class, "http://xyz.identity:2020/v1")).thenReturn(identityServiceMock);

    final VerifyIsisInitializeContext verifyInitializeContextAndReturnSignature;
    try (final AutoTenantContext ignored = new AutoTenantContext(tenant.getIdentifier())) {
      verifyInitializeContextAndReturnSignature = new VerifyIsisInitializeContext(
              systemSecurityEnvironment.tenantKeyTimestamp(),
              systemSecurityEnvironment.tenantPublicKey().getModulus(),
              systemSecurityEnvironment.tenantPublicKey().getPublicExponent(), tenant.getIdentifier());
    }
    doAnswer(verifyInitializeContextAndReturnSignature).when(identityServiceMock).initialize(anyString());

    {
      final IdentityManagerInitialization identityServiceAdminInitialization
              = provisioner.assignIdentityManager(tenant.getIdentifier(), identityServiceAssigned);

      Assert.assertTrue(verifyInitializeContextAndReturnSignature.isValidSecurityContext());
      Assert.assertNotNull(identityServiceAdminInitialization);
      Assert.assertNotNull(identityServiceAdminInitialization.getAdminPassword());
    }

    //Create horus application.
    final Application officeApp = new Application();
    officeApp.setName("office-v1");
    officeApp.setHomepage("http://xyz.office:2021/v1");
    officeApp.setDescription("organization manager");
    officeApp.setVendor("fineract");

    provisioner.createApplication(officeApp);


    //Assign horus application.
    final AssignedApplication officeAssigned = new AssignedApplication();
    officeAssigned.setName("office-v1");

    final Anubis anubisMock = Mockito.mock(Anubis.class);
    when(applicationCallContextProviderSpy.getApplication(Anubis.class, "http://xyz.office:2021/v1")).thenReturn(anubisMock);

    final ApplicationPermissionRequirements anputMock = Mockito.mock(ApplicationPermissionRequirements.class);
    when(applicationCallContextProviderSpy.getApplication(ApplicationPermissionRequirements.class, "http://xyz.office:2021/v1")).thenReturn(anputMock);

    final RsaKeyPairFactory.KeyPairHolder keysInApplicationSignature = RsaKeyPairFactory.createKeyPair();

    final PermittableEndpoint xxPermittableEndpoint = new PermittableEndpoint("/x/y", "POST", "x");
    final PermittableEndpoint xyPermittableEndpoint = new PermittableEndpoint("/y/z", "POST", "x");
    final PermittableEndpoint xyGetPermittableEndpoint = new PermittableEndpoint("/y/z", "GET", "x");
    final PermittableEndpoint mPermittableEndpoint = new PermittableEndpoint("/m/n", "GET", "m");

    final ApplicationPermission forFooPermission = new ApplicationPermission("forPurposeFoo", new Permission("x", AllowedOperation.ALL));
    final ApplicationPermission forBarPermission = new ApplicationPermission("forPurposeBar", new Permission("m", Collections.singleton(AllowedOperation.READ)));

    final VerifyAnubisInitializeContext verifyAnubisInitializeContext;
    final VerifyCreateSignatureSetContext verifyCreateSignatureSetContext;
    final VerifyAnubisPermittablesContext verifyAnubisPermittablesContext;
    final VerifyAnputRequiredPermissionsContext verifyAnputRequiredPermissionsContext;
    final VerifyIsisCreatePermittableGroup verifyIsisCreatePermittableGroup;
    final VerifyIsisSetApplicationSignature verifyIsisSetApplicationSignature;
    final VerifyIsisCreateApplicationPermission verifyIsisCreateApplicationPermission;
    try (final AutoTenantContext ignored = new AutoTenantContext(tenant.getIdentifier())) {
      verifyAnubisInitializeContext = new VerifyAnubisInitializeContext("office", tenant.getIdentifier());
      verifyCreateSignatureSetContext = new VerifyCreateSignatureSetContext(keysInApplicationSignature, "office", tenant.getIdentifier());
      verifyAnubisPermittablesContext = new VerifyAnubisPermittablesContext(Arrays.asList(xxPermittableEndpoint, xxPermittableEndpoint, xyPermittableEndpoint, xyGetPermittableEndpoint, mPermittableEndpoint), tenant.getIdentifier());
      verifyAnputRequiredPermissionsContext = new VerifyAnputRequiredPermissionsContext(Arrays.asList(forFooPermission, forBarPermission), tenant.getIdentifier());
      verifyIsisCreatePermittableGroup = new VerifyIsisCreatePermittableGroup(tenant.getIdentifier());
      verifyIsisSetApplicationSignature = new VerifyIsisSetApplicationSignature(tenant.getIdentifier());
      verifyIsisCreateApplicationPermission = new VerifyIsisCreateApplicationPermission(tenant.getIdentifier(), "office-v1");
    }
    doAnswer(verifyAnubisInitializeContext).when(anubisMock).initializeResources();
    doAnswer(verifyCreateSignatureSetContext).when(anubisMock).createSignatureSet(anyString(), anyObject());
    doAnswer(verifyAnubisPermittablesContext).when(anubisMock).getPermittableEndpoints();
    doAnswer(verifyAnputRequiredPermissionsContext).when(anputMock).getRequiredPermissions();
    doAnswer(verifyIsisCreatePermittableGroup).when(identityServiceMock).createPermittableGroup(new PermittableGroup("x", Arrays.asList(xxPermittableEndpoint, xyPermittableEndpoint, xyGetPermittableEndpoint)));
    doAnswer(verifyIsisCreatePermittableGroup).when(identityServiceMock).createPermittableGroup(new PermittableGroup("m", Collections.singletonList(mPermittableEndpoint)));
    doAnswer(verifyIsisSetApplicationSignature).when(identityServiceMock).setApplicationSignature(
            "office-v1",
            verifyInitializeContextAndReturnSignature.getKeyTimestamp(),
            new Signature(keysInApplicationSignature.getPublicKeyMod(), keysInApplicationSignature.getPublicKeyExp()));
    doAnswer(verifyIsisCreateApplicationPermission).when(identityServiceMock).createApplicationPermission("office-v1", new Permission("x", AllowedOperation.ALL));
    doAnswer(verifyIsisCreateApplicationPermission).when(identityServiceMock).createApplicationPermission("office-v1", new Permission("m", Collections.singleton(AllowedOperation.READ)));
    doAnswer(verifyIsisCreateApplicationPermission).when(identityServiceMock).createApplicationCallEndpointSet("office-v1", new CallEndpointSet("forPurposeFoo", Collections.singletonList("x")));
    doAnswer(verifyIsisCreateApplicationPermission).when(identityServiceMock).createApplicationCallEndpointSet("office-v1", new CallEndpointSet("forPurposeBar", Collections.singletonList("m")));

    {
      provisioner.assignApplications(tenant.getIdentifier(), Collections.singletonList(officeAssigned));

      Thread.sleep(1500L); //Application assigning is asynchronous and I have no message queue.
    }

    Assert.assertTrue(verifyAnubisInitializeContext.isValidSecurityContext());
    Assert.assertTrue(verifyCreateSignatureSetContext.isValidSecurityContext());
    Assert.assertTrue(verifyAnubisPermittablesContext.isValidSecurityContext());
    Assert.assertTrue(verifyAnputRequiredPermissionsContext.isValidSecurityContext());
    Assert.assertEquals(2, verifyIsisCreatePermittableGroup.getCallCount());
    Assert.assertTrue(verifyIsisCreatePermittableGroup.isValidSecurityContext());
    Assert.assertTrue(verifyIsisSetApplicationSignature.isValidSecurityContext());
    Assert.assertEquals(4, verifyIsisCreateApplicationPermission.getCallCount());
    Assert.assertTrue(verifyIsisCreateApplicationPermission.isValidSecurityContext());
  }
}
