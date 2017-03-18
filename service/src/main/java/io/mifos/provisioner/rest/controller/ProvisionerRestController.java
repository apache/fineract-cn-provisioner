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
package io.mifos.provisioner.rest.controller;

import io.mifos.anubis.annotation.AcceptedTokenType;
import io.mifos.anubis.annotation.Permittable;
import io.mifos.core.lang.ServiceException;
import io.mifos.provisioner.api.v1.domain.*;
import io.mifos.provisioner.internal.repository.ClientEntity;
import io.mifos.provisioner.internal.repository.TenantApplicationEntity;
import io.mifos.provisioner.rest.mapper.ApplicationMapper;
import io.mifos.provisioner.rest.mapper.AssignedApplicationMapper;
import io.mifos.provisioner.rest.mapper.ClientMapper;
import io.mifos.provisioner.internal.service.ApplicationService;
import io.mifos.provisioner.internal.service.AuthenticationService;
import io.mifos.provisioner.internal.service.ClientService;
import io.mifos.provisioner.internal.service.TenantApplicationService;
import io.mifos.provisioner.internal.service.TenantService;
import io.mifos.provisioner.config.ProvisionerConstants;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.*;
import java.util.stream.Collectors;

@SuppressWarnings("unused")
@RestController
@RequestMapping("/")
public class ProvisionerRestController {

  private final Logger logger;
  private final AuthenticationService authenticationService;
  private final ClientService clientService;
  private final TenantService tenantService;
  private final ApplicationService applicationService;
  private final TenantApplicationService tenantApplicationService;

  @Autowired
  public ProvisionerRestController(@Qualifier(ProvisionerConstants.LOGGER_NAME) final Logger logger,
                                   final AuthenticationService authenticationService,
                                   final ClientService clientService,
                                   final TenantService tenantService,
                                   final ApplicationService applicationService,
                                   final TenantApplicationService tenantApplicationService) {
    super();
    this.logger = logger;
    this.authenticationService = authenticationService;
    this.clientService = clientService;
    this.tenantService = tenantService;
    this.applicationService = applicationService;
    this.tenantApplicationService = tenantApplicationService;
  }

  @Permittable(AcceptedTokenType.GUEST)
  @RequestMapping(
      value = "/auth/token",
      method = RequestMethod.POST,
      consumes = {MediaType.ALL_VALUE},
      produces = {MediaType.APPLICATION_JSON_VALUE}
  )
  public
  @ResponseBody
  ResponseEntity<AuthenticationResponse> authenticate(@RequestParam("grant_type") final String grantType,
                                                      @RequestParam("client_id") final String clientId,
                                                      @RequestParam("username") final String username,
                                                      @RequestParam("password") final String password) {
    if (!grantType.equals("password")) {
      this.logger.info("Authentication attempt with unknown grant type: " + grantType);
      throw ServiceException.badRequest("Authentication attempt with unknown grant type: {0}", grantType);
    }
    return ResponseEntity.ok(this.authenticationService.authenticate(clientId, username, password));
  }

  @Permittable(AcceptedTokenType.SYSTEM)
  @RequestMapping(
      value = "/auth/user/{useridentifier}/password",
      method = RequestMethod.PUT,
      consumes = {MediaType.APPLICATION_JSON_VALUE},
      produces = {MediaType.APPLICATION_JSON_VALUE}
  )
  public
  @ResponseBody
  ResponseEntity<Void> updatePasswordPolicy(@PathVariable("useridentifier") final String username,
                                            @RequestBody final PasswordPolicy passwordPolicy) {
    this.authenticationService.updatePasswordPolicy(username, passwordPolicy);
    return ResponseEntity.accepted().build();
  }

  @Permittable(AcceptedTokenType.SYSTEM)
  @RequestMapping(
      value = "/clients",
      method = RequestMethod.GET,
      consumes = {MediaType.ALL_VALUE},
      produces = {MediaType.APPLICATION_JSON_VALUE}
  )
  public
  @ResponseBody
  ResponseEntity<List<Client>> getClients() {
    final ArrayList<Client> result = new ArrayList<>();
    final List<ClientEntity> clientEntities = this.clientService.fetchAll();
    result.addAll(clientEntities
        .stream().map(ClientMapper::map)
        .collect(Collectors.toList()));
    return ResponseEntity.ok(result);
  }

  @Permittable(AcceptedTokenType.SYSTEM)
  @RequestMapping(
      value = "/clients",
      method = RequestMethod.POST,
      consumes = {MediaType.APPLICATION_JSON_VALUE},
      produces = {MediaType.APPLICATION_JSON_VALUE}
  )
  public
  @ResponseBody
  ResponseEntity<Void> createClient(@RequestBody @Valid final Client client) {
    this.clientService.create(ClientMapper.map(client));
    return ResponseEntity.accepted().build();
  }

  @Permittable(AcceptedTokenType.SYSTEM)
  @RequestMapping(
      value = "/clients/{clientidentifier}",
      method = RequestMethod.GET,
      consumes = {MediaType.ALL_VALUE},
      produces = {MediaType.APPLICATION_JSON_VALUE}
  )
  public
  @ResponseBody
  ResponseEntity<Client> getClient(@PathVariable("clientidentifier") final String clientIdentifier) {
    return ResponseEntity.ok(ClientMapper.map(this.clientService.findByName(clientIdentifier)));
  }

  @Permittable(AcceptedTokenType.SYSTEM)
  @RequestMapping(
      value = "/clients/{clientidentifier}",
      method = RequestMethod.DELETE,
      consumes = {MediaType.ALL_VALUE},
      produces = {MediaType.APPLICATION_JSON_VALUE}
  )
  public
  @ResponseBody
  ResponseEntity<Void> deleteClient(@PathVariable("clientidentifier") final String clientIdentifier) {
    this.clientService.delete(clientIdentifier);
    return ResponseEntity.accepted().build();
  }

  @Permittable(AcceptedTokenType.SYSTEM)
  @RequestMapping(
      value = "/tenants",
      method = RequestMethod.POST,
      consumes = {MediaType.APPLICATION_JSON_VALUE},
      produces = {MediaType.APPLICATION_JSON_VALUE}
  )
  public
  @ResponseBody
  ResponseEntity<Void> createTenant(@RequestBody final Tenant tenant) {
    this.tenantService.create(tenant);
    return ResponseEntity.accepted().build();
  }


  @Permittable(AcceptedTokenType.SYSTEM)
  @RequestMapping(
      value = "/tenants",
      method = RequestMethod.GET,
      consumes = {MediaType.ALL_VALUE},
      produces = {MediaType.APPLICATION_JSON_VALUE}
  )
  public
  @ResponseBody
  ResponseEntity<List<Tenant>> getTenants() {
    return ResponseEntity.ok(this.tenantService.fetchAll());
  }

  @Permittable(AcceptedTokenType.SYSTEM)
  @RequestMapping(
      value = "/tenants/{tenantidentifier}",
      method = RequestMethod.GET,
      consumes = {MediaType.ALL_VALUE},
      produces = {MediaType.APPLICATION_JSON_VALUE}
  )
  public
  @ResponseBody
  ResponseEntity<Tenant> getTenant(@PathVariable("tenantidentifier") final String tenantIdentifier) {
    final Optional<Tenant> result = this.tenantService.find(tenantIdentifier);
    if (result.isPresent()) {
      return ResponseEntity.ok(result.get());
    } else {
      throw ServiceException.notFound("Tenant {0} not found!", tenantIdentifier);
    }
  }

  @Permittable(AcceptedTokenType.SYSTEM)
  @RequestMapping(
      value = "/tenants/{tenantidentifier}",
      method = RequestMethod.DELETE,
      consumes = {MediaType.ALL_VALUE},
      produces = {MediaType.APPLICATION_JSON_VALUE}
  )
  public
  @ResponseBody
  ResponseEntity<Void> deleteTenant(@PathVariable("tenantidentifier") final String tenantIdentifier) {
    this.tenantService.delete(tenantIdentifier);
    return ResponseEntity.accepted().build();
  }

  @RequestMapping(
          value = "tenants/{tenantidentifier}/identityservice",
          method = RequestMethod.POST,
          produces = {MediaType.APPLICATION_JSON_VALUE},
          consumes = {MediaType.APPLICATION_JSON_VALUE}
  )
  ResponseEntity<IdentityManagerInitialization> assignIdentityManager(@PathVariable("tenantidentifier") final String tenantIdentifier,
                                             @RequestBody final AssignedApplication assignedApplication)
  {
    final String identityManagerUri = applicationService.find(assignedApplication.getName()).getHomepage();

    final Optional<String> adminPassword = tenantService.assignIdentityManager(
            tenantIdentifier,
            assignedApplication.getName(),
            identityManagerUri);
    final IdentityManagerInitialization ret = new IdentityManagerInitialization();
    ret.setAdminPassword(adminPassword.orElse(""));
    return ResponseEntity.ok(ret);
  }

  @Permittable(AcceptedTokenType.SYSTEM)
  @RequestMapping(
      value = "/applications",
      method = RequestMethod.POST,
      consumes = {MediaType.APPLICATION_JSON_VALUE},
      produces = {MediaType.APPLICATION_JSON_VALUE}
  )
  public
  @ResponseBody
  ResponseEntity<Void> createApplication(@RequestBody final Application application) {
    this.applicationService.create(ApplicationMapper.map(application));
    return ResponseEntity.accepted().build();
  }

  @Permittable(AcceptedTokenType.SYSTEM)
  @RequestMapping(
      value = "/applications",
      method = RequestMethod.GET,
      consumes = {MediaType.ALL_VALUE},
      produces = {MediaType.APPLICATION_JSON_VALUE}
  )
  public
  @ResponseBody
  ResponseEntity<List<Application>> getApplications() {
    return ResponseEntity.ok(
        this.applicationService.fetchAll()
            .stream().map(ApplicationMapper::map)
            .collect(Collectors.toList()));
  }

  @Permittable(AcceptedTokenType.SYSTEM)
  @RequestMapping(
      value = "/applications/{name}",
      method = RequestMethod.GET,
      consumes = {MediaType.ALL_VALUE},
      produces = {MediaType.APPLICATION_JSON_VALUE}
  )
  public
  @ResponseBody
  ResponseEntity<Application> getApplication(@PathVariable("name") final String name) {
    return ResponseEntity.ok(ApplicationMapper.map(this.applicationService.find(name)));
  }

  @Permittable(AcceptedTokenType.SYSTEM)
  @RequestMapping(
      value = "/applications/{name}",
      method = RequestMethod.DELETE,
      consumes = {MediaType.ALL_VALUE},
      produces = {MediaType.APPLICATION_JSON_VALUE}
  )
  public
  @ResponseBody
  ResponseEntity<Void> deleteApplication(@PathVariable("name") final String name) {
    this.applicationService.delete(name);
    return ResponseEntity.accepted().build();
  }

  @Permittable(AcceptedTokenType.SYSTEM)
  @RequestMapping(
      value = "tenants/{tenantidentifier}/applications",
      method = RequestMethod.PUT,
      consumes = {MediaType.APPLICATION_JSON_VALUE},
      produces = {MediaType.APPLICATION_JSON_VALUE}
  )
  public
  @ResponseBody
  ResponseEntity<Void> assignApplications(@PathVariable("tenantidentifier") final String tenantIdentifier,
                                          @RequestBody final List<AssignedApplication> assignedApplications) {
    final TenantApplicationEntity tenantApplicationEntity = AssignedApplicationMapper.map(tenantIdentifier, assignedApplications);

    final Map<String, String> appNameToUriMap = new HashMap<>();
    tenantApplicationEntity.getApplications().forEach(
            appName -> appNameToUriMap.put(appName, applicationService.find(appName).getHomepage()));

    tenantApplicationService.assign(tenantApplicationEntity, appNameToUriMap);
    return ResponseEntity.accepted().build();
  }

  @Permittable(AcceptedTokenType.SYSTEM)
  @RequestMapping(
      value = "tenants/{tenantidentifier}/applications",
      method = RequestMethod.GET,
      consumes = {MediaType.ALL_VALUE},
      produces = {MediaType.APPLICATION_JSON_VALUE}
  )
  public
  @ResponseBody
  ResponseEntity<List<AssignedApplication>> getAssignedApplications(@PathVariable("tenantidentifier") final String tenantIdentifier) {
    final TenantApplicationEntity tenantApplicationEntity = this.tenantApplicationService.find(tenantIdentifier);
    return ResponseEntity.ok(AssignedApplicationMapper.map(tenantApplicationEntity));
  }
}
