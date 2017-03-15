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
package io.mifos.provisioner.api.v1.domain;

import org.springframework.util.Assert;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.validation.constraints.NotNull;

@SuppressWarnings("unused")
public final class Client {

  @NotNull
  private String name;
  private String description;
  private String redirectUri;
  private String vendor;
  private String homepage;

  public Client() {
    super();
  }

  @Nonnull
  public String getName() {
    return name;
  }

  public void setName(@Nonnull final String name) {
    Assert.notNull(name, "Client name must be given!");
    this.name = name;
  }

  @Nullable
  public String getDescription() {
    return description;
  }

  public void setDescription(@Nullable final String description) {
    this.description = description;
  }

  @Nullable
  public String getRedirectUri() {
    return redirectUri;
  }

  public void setRedirectUri(@Nullable final String redirectUri) {
    this.redirectUri = redirectUri;
  }

  @Nullable
  public String getVendor() {
    return vendor;
  }

  public void setVendor(@Nullable final String vendor) {
    this.vendor = vendor;
  }

  @Nullable
  public String getHomepage() {
    return homepage;
  }

  public void setHomepage(@Nullable final String homepage) {
    this.homepage = homepage;
  }
}
