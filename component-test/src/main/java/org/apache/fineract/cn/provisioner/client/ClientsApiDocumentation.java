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
package org.apache.fineract.cn.provisioner.client;

import com.google.gson.Gson;
import org.apache.fineract.cn.api.context.AutoSeshat;
import org.apache.fineract.cn.api.util.ApiConstants;
import org.apache.fineract.cn.provisioner.AbstractServiceTest;
import org.apache.fineract.cn.provisioner.api.v1.domain.AuthenticationResponse;
import org.apache.fineract.cn.provisioner.api.v1.domain.Client;
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

public class ClientsApiDocumentation extends AbstractServiceTest {
  @Rule
  public final JUnitRestDocumentation restDocumentation = new JUnitRestDocumentation("build/doc/generated-snippets/test-client");

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
  public void after ( ) {
    provisioner.deleteClient(Fixture.getCompTestClient().getName());
    autoSeshat.close();
  }

  @Test
  public void documentCreateClient ( ) throws Exception {
    final Client client = Fixture.getCompTestClient();

    Gson gson = new Gson();
    this.mockMvc.perform(post("/clients")
            .accept(MediaType.APPLICATION_JSON_VALUE)
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .content(gson.toJson(client)))
            .andExpect(status().isAccepted())
            .andDo(document("document-create-client", preprocessRequest(prettyPrint()),
                    requestFields(
                            fieldWithPath("name").description("Client's name"),
                            fieldWithPath("description").description("Client's description"),
                            fieldWithPath("redirectUri").description("Client's Redirect URI"),
                            fieldWithPath("vendor").description("Client's vendor"),
                            fieldWithPath("homepage").description("Client's Homepage"))));
  }

  @Test
  public void documentFindClient ( ) throws Exception {
    final Client client = Fixture.getCompTestClient();
    provisioner.createClient(client);

    this.mockMvc.perform(get("/clients/" + client.getName())
            .accept(MediaType.APPLICATION_JSON_VALUE)
            .contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(status().isOk())
            .andDo(document("document-find-client", preprocessResponse(prettyPrint()),
                    responseFields(
                            fieldWithPath("name").description("Client's name"),
                            fieldWithPath("description").description("Client's description"),
                            fieldWithPath("redirectUri").description("Client's Redirect URI"),
                            fieldWithPath("vendor").description("Client's vendor"),
                            fieldWithPath("homepage").description("Client's Homepage"))));
  }

  @Test
  public void documentFetchClients ( ) throws Exception {
    final Client firstClient = new Client();
    firstClient.setName("client-comp-test-hd8");
    firstClient.setDescription("Component Test Client Descr hd8");
    firstClient.setHomepage("http://hd8.example.org");
    firstClient.setVendor("Component Test Vendor hd8");
    firstClient.setRedirectUri("http://hd8.redirect.me");
    provisioner.createClient(firstClient);

    final Client secondClient = Fixture.getCompTestClient();
    secondClient.setName("client-comp-test-832");
    secondClient.setDescription("Component Test Client Descr 832");
    secondClient.setHomepage("http://832.example.org");
    secondClient.setVendor("Component Test Vendor 832");
    secondClient.setRedirectUri("http://832.redirect.me");
    provisioner.createClient(secondClient);

    this.mockMvc.perform(get("/clients")
            .accept(MediaType.APPLICATION_JSON_VALUE)
            .contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(status().isOk())
            .andDo(document("document-fetch-clients", preprocessResponse(prettyPrint()),
                    responseFields(
                            fieldWithPath("[].name").description("First Client's name"),
                            fieldWithPath("[].description").description("First Client's description"),
                            fieldWithPath("[].redirectUri").description("First Client's Redirect URI"),
                            fieldWithPath("[].vendor").description("First Client's vendor"),
                            fieldWithPath("[].homepage").description("First Client's Homepage"),
                            fieldWithPath("[1].name").description("Second Client's name"),
                            fieldWithPath("[1].description").description("Second Client's description"),
                            fieldWithPath("[1].redirectUri").description("Second Client's Redirect URI"),
                            fieldWithPath("[1].vendor").description("Second Client's vendor"),
                            fieldWithPath("[1].homepage").description("Second Client's Homepage")
                    )));
  }

  @Test
  public void documentDeleteClient ( ) throws Exception {
    final Client firstClient = Fixture.getCompTestClient();
    provisioner.createClient(firstClient);

    this.mockMvc.perform(delete("/clients/" + firstClient.getName())
            .accept(MediaType.APPLICATION_JSON_VALUE)
            .contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(status().isAccepted())
            .andDo(document("document-delete-client"));
  }
}
