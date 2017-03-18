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
package io.mifos.provisioner.api.v1.client;

import io.mifos.core.api.util.CustomFeignClientsConfiguration;
import io.mifos.provisioner.api.v1.domain.*;
import org.springframework.cloud.netflix.feign.FeignClient;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

import javax.validation.Valid;

import io.mifos.core.api.annotation.ThrowsException;

/**
 * @author Myrle Krantz
 */
@SuppressWarnings("unused")
@FeignClient(name="provisioner-v1", path="/provisioner/v1", configuration=CustomFeignClientsConfiguration.class)
public interface Provisioner {


  @RequestMapping(
      value = "/auth/token?grant_type=password",
      method = RequestMethod.POST,
      produces = {MediaType.ALL_VALUE},
      consumes = {MediaType.APPLICATION_JSON_VALUE}
  )
  @ThrowsException(status = HttpStatus.NOT_FOUND, exception = InvalidProvisionerCredentialsException.class)
  AuthenticationResponse authenticate(@RequestParam("client_id") final String clientId,
                                      @RequestParam("username") final String username,
                                      @RequestParam("password") final String password);


  @RequestMapping(
      value = "/auth/user/{useridentifier}/password",
      method = RequestMethod.PUT,
      produces = {MediaType.APPLICATION_JSON_VALUE},
      consumes = {MediaType.APPLICATION_JSON_VALUE}
  )
  void updatePasswordPolicy(@PathVariable("useridentifier") final String userIdentifier,
                            @RequestBody final PasswordPolicy passwordPolicy);

  @RequestMapping(
      value = "/clients",
      method = RequestMethod.GET,
      produces = {MediaType.ALL_VALUE},
      consumes = {MediaType.APPLICATION_JSON_VALUE}
  )
  List<Client> getClients();


  @RequestMapping(
      value = "/clients",
      method = RequestMethod.POST,
      produces = {MediaType.APPLICATION_JSON_VALUE},
      consumes = {MediaType.APPLICATION_JSON_VALUE}
  )
  @ThrowsException(status = HttpStatus.CONFLICT, exception = DuplicateIdentifierException.class)
  void createClient(@RequestBody @Valid final Client client);



  @RequestMapping(
      value = "/clients/{clientidentifier}",
      method = RequestMethod.GET,
      produces = {MediaType.ALL_VALUE},
      consumes = {MediaType.APPLICATION_JSON_VALUE}
  )
  Client getClient(@PathVariable("clientidentifier") final String clientIdentifier);

  @RequestMapping(
      value = "/clients/{clientidentifier}",
      method = RequestMethod.DELETE,
      produces = {MediaType.ALL_VALUE},
      consumes = {MediaType.APPLICATION_JSON_VALUE}
  )
  void deleteClient(@PathVariable("clientidentifier") final String clientIdentifier);


  @RequestMapping(
      value = "/tenants",
      method = RequestMethod.POST,
      produces = {MediaType.APPLICATION_JSON_VALUE},
      consumes = {MediaType.APPLICATION_JSON_VALUE}
  )
  @ThrowsException(status = HttpStatus.CONFLICT, exception = DuplicateIdentifierException.class)
  void createTenant(@RequestBody final Tenant tenant);



  @RequestMapping(
      value = "/tenants",
      method = RequestMethod.GET,
      produces = {MediaType.ALL_VALUE},
      consumes = {MediaType.APPLICATION_JSON_VALUE}
  )
  List<Tenant> getTenants();


  @RequestMapping(
      value = "/tenants/{tenantidentifier}",
      method = RequestMethod.GET,
      produces = {MediaType.ALL_VALUE},
      consumes = {MediaType.APPLICATION_JSON_VALUE}
  )
  Tenant getTenant(@PathVariable("tenantidentifier") final String tenantIdentifier);


  @RequestMapping(
      value = "/tenants/{tenantidentifier}",
      method = RequestMethod.DELETE,
      produces = {MediaType.ALL_VALUE},
      consumes = {MediaType.APPLICATION_JSON_VALUE}
  )
  void deleteTenant(@PathVariable("tenantidentifier") final String tenantIdentifier);


  @RequestMapping(
      value = "/applications",
      method = RequestMethod.POST,
      produces = {MediaType.APPLICATION_JSON_VALUE},
      consumes = {MediaType.APPLICATION_JSON_VALUE}
  )
  @ThrowsException(status = HttpStatus.CONFLICT, exception = DuplicateIdentifierException.class)
  void createApplication(@RequestBody final Application application);


  @RequestMapping(
      value = "/applications",
      method = RequestMethod.GET,
      produces = {MediaType.ALL_VALUE},
      consumes = {MediaType.APPLICATION_JSON_VALUE}
  )
  List<Application> getApplications();


  @RequestMapping(
      value = "/applications/{name}",
      method = RequestMethod.GET,
      produces = {MediaType.ALL_VALUE},
      consumes = {MediaType.APPLICATION_JSON_VALUE}
  )
  Application getApplication(@PathVariable("name") final String name);


  @RequestMapping(
      value = "/applications/{name}",
      method = RequestMethod.DELETE,
      produces = {MediaType.ALL_VALUE},
      consumes = {MediaType.APPLICATION_JSON_VALUE}
  )
  void deleteApplication(@PathVariable("name") final String name);

  @RequestMapping(
          value = "tenants/{tenantidentifier}/identityservice",
          method = RequestMethod.POST,
          produces = {MediaType.APPLICATION_JSON_VALUE},
          consumes = {MediaType.APPLICATION_JSON_VALUE}
  )
  IdentityManagerInitialization assignIdentityManager(@PathVariable("tenantidentifier") final String tenantIdentifier,
                                                      @RequestBody final AssignedApplication assignedApplications);

  @RequestMapping(
          value = "tenants/{tenantidentifier}/applications",
          method = RequestMethod.PUT,
          produces = {MediaType.APPLICATION_JSON_VALUE},
          consumes = {MediaType.APPLICATION_JSON_VALUE}
  )
  void assignApplications(@PathVariable("tenantidentifier") final String tenantIdentifier,
                          @RequestBody final List<AssignedApplication> assignedApplications);


  @RequestMapping(
      value = "tenants/{tenantidentifier}/applications",
      method = RequestMethod.GET,
      produces = {MediaType.ALL_VALUE},
      consumes = {MediaType.APPLICATION_JSON_VALUE}
  )
  List<AssignedApplication> getAssignedApplications(
      @PathVariable("tenantidentifier") final String tenantIdentifier);
}