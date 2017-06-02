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
package io.mifos.provisioner.internal.service.applications;

import io.mifos.anubis.api.v1.client.Anubis;
import io.mifos.anubis.api.v1.domain.PermittableEndpoint;
import io.mifos.core.lang.AutoTenantContext;
import io.mifos.identity.api.v1.client.IdentityManager;
import io.mifos.identity.api.v1.client.PermittableGroupAlreadyExistsException;
import io.mifos.identity.api.v1.domain.PermittableGroup;
import io.mifos.provisioner.internal.listener.IdentityListener;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;
import org.slf4j.Logger;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.*;

/**
 * @author Myrle Krantz
 */
public class IdentityServiceInitializerTest {

  private final PermittableEndpoint abcPost1 = new PermittableEndpoint("/a/b/c", "POST", "1");
  private final PermittableEndpoint abcGet1 = new PermittableEndpoint("/a/b/c", "GET", "1");
  private final PermittableEndpoint defGet1 = new PermittableEndpoint("/d/e/f", "POST", "1");
  private final PermittableGroup group1 = new PermittableGroup("1", Arrays.asList(abcPost1, abcGet1, defGet1));
  private final PermittableGroup reorderedGroup1 = new PermittableGroup("1", Arrays.asList(abcGet1, abcPost1, defGet1));
  private final PermittableGroup changedGroup1 = new PermittableGroup("1", Arrays.asList(abcPost1, defGet1));

  private final PermittableEndpoint abcPost2 = new PermittableEndpoint("/a/b/c", "POST", "2");
  private final PermittableEndpoint abcGet2 = new PermittableEndpoint("/a/b/c", "GET", "2");
  private final PermittableEndpoint defGet2 = new PermittableEndpoint("/d/e/f", "POST", "2");
  private final PermittableGroup group2 = new PermittableGroup("2", Arrays.asList(abcPost2, abcGet2, defGet2));

  private final PermittableEndpoint defGet3 = new PermittableEndpoint("/d/e/f", "POST", "3");
  private final PermittableGroup group3 = new PermittableGroup("3", Collections.singletonList(defGet3));

  @Test
  public void getPermittablesAnubisCallFails() throws Exception {
    final IdentityListener identityListenerMock = Mockito.mock(IdentityListener.class);
    final ApplicationCallContextProvider applicationCallContextProviderMock = Mockito.mock(ApplicationCallContextProvider.class);
    final Logger loggerMock = Mockito.mock(Logger.class);
    final Anubis anubisMock = Mockito.mock(Anubis.class);

    when(applicationCallContextProviderMock.getApplication(Anubis.class, "blah")).thenReturn(anubisMock);
    //noinspection unchecked
    when(anubisMock.getPermittableEndpoints()).thenThrow(IllegalStateException.class);

    final List<PermittableEndpoint> ret = new IdentityServiceInitializer(identityListenerMock, applicationCallContextProviderMock, null, loggerMock)
            .getPermittables("blah");

    Assert.assertEquals(ret, Collections.emptyList());
    verify(loggerMock).error(anyString(), anyString(), isA(IllegalStateException.class));
  }



  @Test
  public void getPermittableGroups() throws Exception {

    final List<PermittableEndpoint> permittableEndpoints = Arrays.asList(abcPost1, abcGet1, defGet1, abcPost2, abcGet2, defGet2, defGet3);
    final List<PermittableGroup> ret = IdentityServiceInitializer.getPermittableGroups(permittableEndpoints).collect(Collectors.toList());
    Assert.assertEquals(ret, Arrays.asList(group1, group2, group3));
  }

  @Test
  public void getPermittableGroupsOnEmptyList() throws Exception {
    final List<PermittableGroup> ret = IdentityServiceInitializer.getPermittableGroups(Collections.emptyList()).collect(Collectors.toList());
    Assert.assertEquals(ret, Collections.emptyList());
  }

  @Test
  public void createOrFindPermittableGroupThatAlreadyExists() throws Exception {
    final IdentityListener identityListenerMock = Mockito.mock(IdentityListener.class);
    final Logger loggerMock = Mockito.mock(Logger.class);

    final IdentityManager identityServiceMock = Mockito.mock(IdentityManager.class);
    doThrow(PermittableGroupAlreadyExistsException.class).when(identityServiceMock).createPermittableGroup(group1);
    doReturn(reorderedGroup1).when(identityServiceMock).getPermittableGroup(group1.getIdentifier());

    try (final AutoTenantContext ignored = new AutoTenantContext("blah")) {
      new IdentityServiceInitializer(identityListenerMock, null, null, loggerMock).createOrFindPermittableGroup(identityServiceMock, group1);
    }
  }

  @Test
  public void createOrFindPermittableGroupThatAlreadyExistsDifferently() throws Exception {
    final IdentityListener identityListenerMock = Mockito.mock(IdentityListener.class);
    final Logger loggerMock = Mockito.mock(Logger.class);

    final IdentityManager identityServiceMock = Mockito.mock(IdentityManager.class);
    doThrow(PermittableGroupAlreadyExistsException.class).when(identityServiceMock).createPermittableGroup(group1);
    doReturn(changedGroup1).when(identityServiceMock).getPermittableGroup(group1.getIdentifier());

    try (final AutoTenantContext ignored = new AutoTenantContext("blah")) {
      new IdentityServiceInitializer(identityListenerMock, null, null, loggerMock).createOrFindPermittableGroup(identityServiceMock, group1);
    }

    verify(loggerMock).error(anyString(), anyString(), anyString());
  }

  @Test
  public void createOrFindPermittableGroupWhenIsisCallFails() throws Exception {
    final IdentityListener identityListenerMock = Mockito.mock(IdentityListener.class);
    final Logger loggerMock = Mockito.mock(Logger.class);

    final IdentityManager identityServiceMock = Mockito.mock(IdentityManager.class);
    doThrow(IllegalStateException.class).when(identityServiceMock).createPermittableGroup(group1);
    doReturn(changedGroup1).when(identityServiceMock).getPermittableGroup(group1.getIdentifier());

    try (final AutoTenantContext ignored = new AutoTenantContext("blah")) {
      new IdentityServiceInitializer(identityListenerMock, null, null, loggerMock).createOrFindPermittableGroup(identityServiceMock, group1);
    }

    verify(loggerMock).error(anyString(), anyString(), anyString(), isA(IllegalStateException.class));
  }


}