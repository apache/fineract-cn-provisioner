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
package io.mifos.provisioner.internal.repository;

import com.datastax.driver.mapping.annotations.Column;
import com.datastax.driver.mapping.annotations.PartitionKey;
import com.datastax.driver.mapping.annotations.Table;

@Table(name = ApplicationEntity.TABLE_NAME)
public class ApplicationEntity {

  static final String TABLE_NAME = "applications";
  static final String NAME_COLUMN = "name";
  static final String DESCRIPTION_COLUMN = "description";
  static final String VENDOR_COLUMN = "vendor";
  static final String HOMEPAGE_COLUMN = "homepage";

  @PartitionKey
  @Column(name = NAME_COLUMN)
  private String name;
  @Column(name = DESCRIPTION_COLUMN)
  private String description;
  @Column(name = VENDOR_COLUMN)
  private String vendor;
  @Column(name = HOMEPAGE_COLUMN)
  private String homepage;

  public ApplicationEntity() {
    super();
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public String getVendor() {
    return vendor;
  }

  public void setVendor(String vendor) {
    this.vendor = vendor;
  }

  public String getHomepage() {
    return homepage;
  }

  public void setHomepage(String homepage) {
    this.homepage = homepage;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    ApplicationEntity that = (ApplicationEntity) o;

    return name.equals(that.name);

  }

  @Override
  public int hashCode() {
    return name.hashCode();
  }
}
