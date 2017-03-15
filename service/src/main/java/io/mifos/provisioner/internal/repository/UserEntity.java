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

import java.util.Date;

@SuppressWarnings("unused")
@Table(name = UserEntity.TABLE_NAME)
public class UserEntity {

  static final java.lang.String TABLE_NAME = "users";
  static final String NAME_COLUMN = "name";
  static final String PASSWORD_COLUMN = "passwordWord";
  static final String SALT_COLUMN = "salt";
  static final String ITERATION_COUNT_COLUMN = "iteration_count";
  static final String EXPIRES_IN_DAYS_COLUMN = "expires_in_days";
  static final String PASSWORD_RESET_ON_COLUMN = "password_reset_on";

  @PartitionKey
  @Column(name = NAME_COLUMN)
  private String name;
  @Column(name = PASSWORD_COLUMN)
  private byte[] password;
  @Column(name = SALT_COLUMN)
  private byte[] salt;
  @Column(name = ITERATION_COUNT_COLUMN)
  private int iterationCount;
  @Column(name = EXPIRES_IN_DAYS_COLUMN)
  private int expiresInDays;
  @Column(name = PASSWORD_RESET_ON_COLUMN)
  private Date passwordResetOn;

  public UserEntity() {
    super();
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public byte[] getPassword() {
    return password;
  }

  public void setPassword(byte[] password) {
    this.password = password;
  }

  public byte[] getSalt() {
    return salt;
  }

  public void setSalt(byte[] salt) {
    this.salt = salt;
  }

  public int getIterationCount() {
    return iterationCount;
  }

  public void setIterationCount(int iterationCount) {
    this.iterationCount = iterationCount;
  }

  public int getExpiresInDays() {
    return expiresInDays;
  }

  public void setExpiresInDays(int expiresInDays) {
    this.expiresInDays = expiresInDays;
  }

  public Date getPasswordResetOn() {
    return passwordResetOn;
  }

  public void setPasswordResetOn(Date passwordResetOn) {
    this.passwordResetOn = passwordResetOn;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    UserEntity that = (UserEntity) o;

    return name.equals(that.name);

  }

  @Override
  public int hashCode() {
    return name.hashCode();
  }
}
