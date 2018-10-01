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

import com.google.gson.Gson;
import org.apache.fineract.cn.api.context.AutoSeshat;
import org.apache.fineract.cn.api.util.ApiConstants;
import org.apache.fineract.cn.provisioner.AbstractServiceTest;
import org.apache.fineract.cn.provisioner.api.v1.domain.*;
import org.apache.fineract.cn.provisioner.config.ProvisionerConstants;
import org.junit.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.restdocs.JUnitRestDocumentation;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.documentationConfiguration;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.delete;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.post;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessRequest;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessResponse;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.prettyPrint;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.requestFields;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class TenantApiDocumentation extends AbstractServiceTest {
  @Rule
  public final JUnitRestDocumentation restDocumentation = new JUnitRestDocumentation("build/doc/generated-snippets/test-tenant");

  @Autowired
  private WebApplicationContext context;

  private MockMvc mockMvc;

  private AutoSeshat autoSeshat;

  @Before
  public void setUp ( ) {

    this.mockMvc = MockMvcBuilders.webAppContextSetup(this.context)
            .apply(documentationConfiguration(this.restDocumentation))
            .build();
  }

  @Before
  public void before ( ) {
    final AuthenticationResponse authentication = provisioner.authenticate(
            this.getClientId(), ApiConstants.SYSTEM_SU, ProvisionerConstants.INITIAL_PWD);
    autoSeshat = new AutoSeshat(authentication.getToken());
  }

  @After
  public void after ( ) throws InterruptedException {
    //provisioner.deleteTenant(Fixture.getCompTestTenant().getIdentifier());
    autoSeshat.close();
  }

  @Test
  public void documentCreateTenant ( ) throws Exception {
    final Tenant myTenant = Fixture.getCompTestTenant();

    Gson gson = new Gson();
    this.mockMvc.perform(post("/tenants")
            .accept(MediaType.APPLICATION_JSON_VALUE)
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .content(gson.toJson(myTenant)))
            .andExpect(status().isAccepted())
            .andDo(document("document-create-tenant", preprocessRequest(prettyPrint()),
                    requestFields(
                            fieldWithPath("identifier").description("Tenant's Identifier"),
                            fieldWithPath("name").description("Tenant's name"),
                            fieldWithPath("description").description("Tenant's description"),
                            fieldWithPath("cassandraConnectionInfo").type("CassandraConnectionInfo").description("Tenant's Cassandra Connection Information +\n" +
                                    " +\n" +
                                    "*class* _CassandraConnectionInfo_ { +\n" +
                                    "    String clusterName; +\n" +
                                    "    String contactPoints; +\n" +
                                    "    String keyspace; +\n" +
                                    "    String replicationType; +\n" +
                                    "    String replicas; +\n" +
                                    "  } +"),
                            fieldWithPath("databaseConnectionInfo").type("_DatabaseConnectionInfo_").description("Tenant's Database Connection Information +\n" +
                                    " +\n" +
                                    "*class* _DatabaseConnectionInfo_ { +\n" +
                                    "    String driverClass; +\n" +
                                    "    String databaseName; +\n" +
                                    "    String host; +\n" +
                                    "    String port; +\n" +
                                    "    String user; +\n" +
                                    "    String password; +\n" +
                                    "  } +")
                    )
            ));
  }

  @Test
  public void documentFindTenant ( ) throws Exception {
    final Tenant mytenant = Fixture.getCompTestTenant();
    provisioner.createTenant(mytenant);

    this.mockMvc.perform(get("/tenants/" + mytenant.getIdentifier())
            .accept(MediaType.APPLICATION_JSON_VALUE)
            .contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(status().isOk())
            .andDo(document("document-find-tenant", preprocessResponse(prettyPrint()),
                    responseFields(
                            fieldWithPath("identifier").description("Tenant's Identifier"),
                            fieldWithPath("name").description("Tenant's name"),
                            fieldWithPath("description").description("Tenant's description"),
                            fieldWithPath("cassandraConnectionInfo").type("CassandraConnectionInfo").description("Tenant's Cassandra Connection Information +\n" +
                                    " +\n" +
                                    "*class* _CassandraConnectionInfo_ { +\n" +
                                    "    String clusterName; +\n" +
                                    "    String contactPoints; +\n" +
                                    "    String keyspace; +\n" +
                                    "    String replicationType; +\n" +
                                    "    String replicas; +\n" +
                                    "  } +"),
                            fieldWithPath("databaseConnectionInfo").type("_DatabaseConnectionInfo_").description("Tenant's Database Connection Information +\n" +
                                    " +\n" +
                                    "*class* _DatabaseConnectionInfo_ { +\n" +
                                    "    String driverClass; +\n" +
                                    "    String databaseName; +\n" +
                                    "    String host; +\n" +
                                    "    String port; +\n" +
                                    "    String user; +\n" +
                                    "    String password; +\n" +
                                    "  } +")
                    )
            ));
  }

  @Test
  public void documentFetchTenants ( ) throws Exception {
    final Tenant firstTenant = Fixture.getCompTestTenant();
    provisioner.createTenant(firstTenant);

    final Tenant secondTenant = Fixture.getCompTestTenant();
    provisioner.createTenant(secondTenant);

    this.mockMvc.perform(get("/tenants")
            .accept(MediaType.APPLICATION_JSON_VALUE)
            .contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(status().isOk())
            .andDo(document("document-fetch-tenants", preprocessResponse(prettyPrint()),
                    responseFields(
                            fieldWithPath("[].identifier").description("Tenant's Identifier"),
                            fieldWithPath("[].name").description("Tenant's name"),
                            fieldWithPath("[].description").description("Tenant's description"),
                            fieldWithPath("[].cassandraConnectionInfo").type("CassandraConnectionInfo").description("Tenant's Cassandra Connection Information +\n" +
                                    " +\n" +
                                    "*class* _CassandraConnectionInfo_ { +\n" +
                                    "    String clusterName; +\n" +
                                    "    String contactPoints; +\n" +
                                    "    String keyspace; +\n" +
                                    "    String replicationType; +\n" +
                                    "    String replicas; +\n" +
                                    "  } +"),
                            fieldWithPath("[].databaseConnectionInfo").type("_DatabaseConnectionInfo_").description("Tenant's Database Connection Information +\n" +
                                    " +\n" +
                                    "*class* _DatabaseConnectionInfo_ { +\n" +
                                    "    String driverClass; +\n" +
                                    "    String databaseName; +\n" +
                                    "    String host; +\n" +
                                    "    String port; +\n" +
                                    "    String user; +\n" +
                                    "    String password; +\n" +
                                    "  } +"),
                            fieldWithPath("[1].identifier").description("Tenant's Identifier"),
                            fieldWithPath("[1].name").description("Tenant's name"),
                            fieldWithPath("[1].description").description("Tenant's description"),
                            fieldWithPath("[1].cassandraConnectionInfo").type("CassandraConnectionInfo").description("Tenant's Cassandra Connection Information +\n" +
                                    " +\n" +
                                    "*class* _CassandraConnectionInfo_ { +\n" +
                                    "    String clusterName; +\n" +
                                    "    String contactPoints; +\n" +
                                    "    String keyspace; +\n" +
                                    "    String replicationType; +\n" +
                                    "    String replicas; +\n" +
                                    "  } +"),
                            fieldWithPath("[1].databaseConnectionInfo").type("_DatabaseConnectionInfo_").description("Tenant's Database Connection Information +\n" +
                                    " +\n" +
                                    "*class* _DatabaseConnectionInfo_ { +\n" +
                                    "    String driverClass; +\n" +
                                    "    String databaseName; +\n" +
                                    "    String host; +\n" +
                                    "    String port; +\n" +
                                    "    String user; +\n" +
                                    "    String password; +\n" +
                                    "  } +")
                    )
            ));
  }

  @Test
  public void documentDeleteTenant ( ) throws Exception {
    final Tenant firstTenant = Fixture.getCompTestTenant();
    provisioner.createTenant(firstTenant);

    this.mockMvc.perform(delete("/tenants/" + firstTenant.getIdentifier())
            .accept(MediaType.APPLICATION_JSON_VALUE)
            .contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(status().isAccepted())
            .andDo(document("document-delete-tenant"));
  }
}
