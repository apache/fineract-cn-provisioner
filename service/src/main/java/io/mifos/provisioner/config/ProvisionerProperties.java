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
package io.mifos.provisioner.config;

import io.mifos.provisioner.internal.util.DataStoreOption;
import org.springframework.boot.context.properties.ConfigurationProperties;

import javax.validation.Valid;

/**
 * @author Myrle Krantz
 */
@ConfigurationProperties(prefix = "provisioner")
public class ProvisionerProperties {
  @Valid
  private DataStoreOption dataStoreOption = DataStoreOption.ALL;

  public DataStoreOption getDataStoreOption() {
    return dataStoreOption;
  }

  public void setDataStoreOption(DataStoreOption dataStoreOption) {
    this.dataStoreOption = dataStoreOption;
  }
}
