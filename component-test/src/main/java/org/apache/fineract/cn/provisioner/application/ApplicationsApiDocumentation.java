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
package org.apache.fineract.cn.provisioner.application;

import com.google.gson.Gson;
import org.apache.fineract.cn.api.context.AutoSeshat;
import org.apache.fineract.cn.api.util.ApiConstants;
import org.apache.fineract.cn.provisioner.api.v1.domain.Application;
import org.apache.fineract.cn.provisioner.api.v1.domain.AuthenticationResponse;
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
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.post;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessRequest;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessResponse;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.prettyPrint;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.delete;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.requestFields;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;

import org.apache.fineract.cn.provisioner.AbstractServiceTest;

public class ApplicationsApiDocumentation extends AbstractServiceTest {
  @Rule
  public final JUnitRestDocumentation restDocumentation = new JUnitRestDocumentation("build/doc/generated-snippets/test-application");

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
    provisioner.deleteApplication(Fixture.getApplication().getName());
    autoSeshat.close();
  }

  @Test
  public void documentCreateApplication ( ) throws Exception {
    final Application application = Fixture.getApplication();

    Gson gson = new Gson();
    this.mockMvc.perform(post("/applications")
            .accept(MediaType.APPLICATION_JSON_VALUE)
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .content(gson.toJson(application)))
            .andExpect(status().isAccepted())
            .andDo(document("document-create-application", preprocessRequest(prettyPrint()),
                    requestFields(
                            fieldWithPath("name").description("Application's name"),
                            fieldWithPath("description").description("Application's description"),
                            fieldWithPath("vendor").description("Application's vendor"),
                            fieldWithPath("homepage").description("Application's homepage")
                    )
            ));
  }

  @Test
  public void shouldFindApplication ( ) throws Exception {
    Application application = new Application();
    application.setName("comp-test-app");
    application.setDescription("Component Test Application");
    application.setHomepage("http://www.component.test");
    application.setVendor("Component Test");

    provisioner.createApplication(application);

    this.mockMvc.perform(get("/applications/" + application.getName())
            .accept(MediaType.APPLICATION_JSON_VALUE)
            .contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(status().isOk())
            .andDo(document("document-find-application", preprocessResponse(prettyPrint()),
                    responseFields(
                            fieldWithPath("name").description("Application's name"),
                            fieldWithPath("description").description("Application's description"),
                            fieldWithPath("vendor").description("Application's vendor"),
                            fieldWithPath("homepage").description("Application's homepage")
                    )
            ));
  }

  @Test
  public void documentFetchApplications ( ) throws Exception {
    Application firstApplication = new Application();
    firstApplication.setName("first-comp-test-app");
    firstApplication.setDescription("First Component Test Application");
    firstApplication.setHomepage("http://www.first-component.test");
    firstApplication.setVendor("First Component Test");

    Application secondApplication = new Application();
    secondApplication.setName("second-comp-test-app");
    secondApplication.setDescription("Second Component Test Application");
    secondApplication.setHomepage("http://www.second-component.test");
    secondApplication.setVendor("Second Component Test");

    provisioner.createApplication(firstApplication);
    provisioner.createApplication(secondApplication);

    this.mockMvc.perform(get("/applications")
            .accept(MediaType.APPLICATION_JSON_VALUE)
            .contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(status().isOk())
            .andDo(document("document-fetch-applications", preprocessResponse(prettyPrint()),
                    responseFields(
                            fieldWithPath("[].name").description("First Application's name"),
                            fieldWithPath("[].description").description("First Application's description"),
                            fieldWithPath("[].vendor").description("First Application's vendor"),
                            fieldWithPath("[].homepage").description("First Application's homepage"),
                            fieldWithPath("[1].name").description("Second Application's name"),
                            fieldWithPath("[1].description").description("Second Application's description"),
                            fieldWithPath("[1].vendor").description("Second Application's vendor"),
                            fieldWithPath("[1].homepage").description("Second Application's homepage")
                    )
            ));
  }

  @Test
  public void documentDeleteApplication ( ) throws Exception {
    Application randApplication = new Application();
    randApplication.setName("random-comp-test-app");
    randApplication.setDescription("Random Component Test Application");
    randApplication.setHomepage("http://www.random-component.test");
    randApplication.setVendor("Random Component Test");

    provisioner.createApplication(randApplication);

    this.mockMvc.perform(delete("/applications/" + randApplication.getName())
            .accept(MediaType.APPLICATION_JSON_VALUE)
            .contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(status().isAccepted())
            .andDo(document("document-delete-application"));
  }
}
