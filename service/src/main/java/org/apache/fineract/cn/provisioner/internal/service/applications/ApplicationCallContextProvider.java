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
package org.apache.fineract.cn.provisioner.internal.service.applications;

import org.apache.fineract.cn.provisioner.internal.util.TokenProvider;
import org.apache.fineract.cn.anubis.api.v1.RoleConstants;
import org.apache.fineract.cn.anubis.api.v1.TokenConstants;
import org.apache.fineract.cn.api.context.AutoSeshat;
import org.apache.fineract.cn.api.context.AutoUserContext;
import org.apache.fineract.cn.api.util.ApiFactory;
import org.apache.fineract.cn.lang.AutoTenantContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@SuppressWarnings("WeakerAccess")
@Component
public class ApplicationCallContextProvider {

  private final ApiFactory apiFactory;
  private final TokenProvider tokenProvider;

  static private class ApplicationCallContext implements AutoCloseable
  {
    private final AutoTenantContext tenantContext;
    private final AutoUserContext userContext;


    private ApplicationCallContext(final AutoTenantContext tenantContext, final AutoUserContext userContext) {
      this.tenantContext = tenantContext;
      this.userContext = userContext;
    }

    @Override
    public void close() {
      tenantContext.close();
      userContext.close();
    }
  }

  @Autowired
  public ApplicationCallContextProvider(final ApiFactory apiFactory,
                                        final TokenProvider tokenProvider) {
    super();
    this.apiFactory = apiFactory;
    this.tokenProvider = tokenProvider;
  }

  public AutoCloseable getApplicationCallContext(final String tenantIdentifier, final String applicationName)
  {
    final String token = this.tokenProvider.createToken(tenantIdentifier, applicationName, 2L, TimeUnit.MINUTES).getToken();
    final AutoTenantContext tenantContext = new AutoTenantContext(tenantIdentifier);
    final AutoUserContext userContext = new AutoSeshat(token);

    return new ApplicationCallContext(tenantContext, userContext);
  }

  public AutoCloseable getApplicationCallGuestContext(final String tenantIdentifier)
  {
    final AutoTenantContext tenantContext = new AutoTenantContext(tenantIdentifier);
    final AutoUserContext userContext = new AutoUserContext(RoleConstants.GUEST_USER_IDENTIFIER, TokenConstants.NO_AUTHENTICATION);

    return new ApplicationCallContext(tenantContext, userContext);

  }

  public <T> T getApplication(final Class<T> clazz, final String applicationUri)
  {
    return this.apiFactory.create(clazz, applicationUri);
  }
}
