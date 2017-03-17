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
package io.mifos.provisioner.internal.service;

import com.datastax.driver.core.BoundStatement;
import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Row;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.Statement;
import com.datastax.driver.core.utils.Bytes;
import com.datastax.driver.mapping.Mapper;
import com.datastax.driver.mapping.MappingManager;

import io.mifos.anubis.token.TokenSerializationResult;
import io.mifos.core.cassandra.core.CassandraSessionProvider;
import io.mifos.core.lang.ServiceException;
import io.mifos.provisioner.internal.repository.UserEntity;
import io.mifos.provisioner.api.v1.domain.AuthenticationResponse;
import io.mifos.provisioner.api.v1.domain.PasswordPolicy;
import io.mifos.provisioner.internal.repository.ClientEntity;
import io.mifos.provisioner.internal.repository.ConfigEntity;
import io.mifos.provisioner.internal.util.TokenProvider;
import io.mifos.provisioner.config.ProvisionerConstants;
import io.mifos.tool.crypto.HashGenerator;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.util.EncodingUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.Base64Utils;

import java.nio.ByteBuffer;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.concurrent.TimeUnit;

import javax.annotation.Nonnull;

@Service
public class AuthenticationService {

  @Value("${spring.application.name}")
  private String applicationName;
  @Value("${system.token.ttl}")
  private Integer ttl;
  private final Logger logger;
  private final CassandraSessionProvider cassandraSessionProvider;
  private final HashGenerator hashGenerator;
  private final TokenProvider tokenProvider;


  @Autowired
  public AuthenticationService(@Qualifier(ProvisionerConstants.LOGGER_NAME) final Logger logger,
                               final CassandraSessionProvider cassandraSessionProvider,
                               final HashGenerator hashGenerator,
                               final TokenProvider tokenProvider) {
    super();
    this.logger = logger;
    this.cassandraSessionProvider = cassandraSessionProvider;
    this.hashGenerator = hashGenerator;
    this.tokenProvider = tokenProvider;
  }

  public AuthenticationResponse authenticate(
      final @Nonnull String clientId,
      final @Nonnull String username,
      final @Nonnull String password) {
    final Session session = this.cassandraSessionProvider.getAdminSession();
    final MappingManager mappingManager = new MappingManager(session);

    final Mapper<ClientEntity> clientEntityMapper = mappingManager.mapper(ClientEntity.class);
    if (clientEntityMapper.get(clientId) == null) {
      this.logger.warn("Authentication attempt with unknown client: " + clientId);
      throw ServiceException.notFound("Requested resource not found!");
    }

    final Mapper<UserEntity> userEntityMapper = mappingManager.mapper(UserEntity.class);
    final Statement userQuery = userEntityMapper.getQuery(username);
    final ResultSet userResult = session.execute(userQuery);
    final Row userRow = userResult.one();
    if (userRow == null) {
      this.logger.warn("Authentication attempt with unknown user: " + username);
      throw ServiceException.notFound("Requested resource not found!");
    }
    final byte[] storedPassword = Bytes.getArray(userRow.getBytes(1));
    final byte[] salt = Bytes.getArray(userRow.getBytes(2));
    final int iterationCount = userRow.getInt(3);
    final int expiresInDays = userRow.getInt(4);
    final Date passwordResetOn = userRow.getTimestamp(5);

    final Mapper<ConfigEntity> configEntityMapper = mappingManager.mapper(ConfigEntity.class);
    final Statement configQuery = configEntityMapper.getQuery(ProvisionerConstants.CONFIG_INTERNAL);
    final ResultSet configResult = session.execute(configQuery);
    final Row configRow = configResult.one();
    final byte[] secret = Bytes.getArray(configRow.getBytes(1));

    if (this.hashGenerator.isEqual(
        storedPassword,
        Base64Utils.decodeFromString(password),
        secret,
        salt,
        iterationCount,
        256)) {

      if (expiresInDays > 0) {
        final LocalDate ld = passwordResetOn.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
        final LocalDate expiresOn = ld.plusDays(expiresInDays);
        if (LocalDate.now().isAfter(expiresOn)) {
          throw ServiceException.badRequest("Password expired");
        }
      }

      final TokenSerializationResult authToken = this.tokenProvider.createToken(username, this.applicationName, this.ttl, TimeUnit.MINUTES);
      return new AuthenticationResponse(authToken.getToken(), dateTimeToString(authToken.getExpiration()));
    } else {
      throw ServiceException.notFound("Requested resource not found!");
    }
  }

  private String dateTimeToString(final LocalDateTime dateTime) {
    return dateTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
  }

  public void updatePasswordPolicy(final String username, final PasswordPolicy passwordPolicy) {
    try {
      final Session session = this.cassandraSessionProvider.getAdminSession();
      final MappingManager mappingManager = new MappingManager(session);
      final Mapper<UserEntity> userEntityMapper = mappingManager.mapper(UserEntity.class);
      final Statement userQuery = userEntityMapper.getQuery(username);
      final ResultSet userResult = session.execute(userQuery);
      final Row userRow = userResult.one();
      if (userRow == null) {
        this.logger.warn("Authentication attempt with unknown user: " + username);
        throw ServiceException.notFound("Requested resource not found!");
      }
      final byte[] salt = Bytes.getArray(userRow.getBytes(2));
      final int iterationCount = userRow.getInt(3);

      final Mapper<ConfigEntity> configEntityMapper = mappingManager.mapper(ConfigEntity.class);
      final Statement configQuery = configEntityMapper.getQuery(ProvisionerConstants.CONFIG_INTERNAL);
      final ResultSet configResult = session.execute(configQuery);
      final Row configRow = configResult.one();
      final byte[] secret = Bytes.getArray(configRow.getBytes(1));

      if (passwordPolicy.getNewPassword() != null) {
        final byte[] newPasswordHash = this.hashGenerator.hash(passwordPolicy.getNewPassword(), EncodingUtils.concatenate(salt, secret), iterationCount, ProvisionerConstants.HASH_LENGTH);
        final BoundStatement updateStatement = session.prepare(
            "UPDATE users SET passwordWord = ?, password_reset_on = ? WHERE name = ?").bind();
        updateStatement.setBytes(0, ByteBuffer.wrap(newPasswordHash));
        updateStatement.setTimestamp(1, new Date());
        updateStatement.setString(2, username);
        session.execute(updateStatement);
      }

      if (passwordPolicy.getExpiresInDays() != null) {
        final BoundStatement updateStatement = session.prepare(
            "UPDATE users SET expires_in_days = ? WHERE name = ?").bind();
        updateStatement.setInt(0, passwordPolicy.getExpiresInDays());
        updateStatement.setString(1, username);
        session.execute(updateStatement);
      }
    } catch (final Exception ex) {
      this.logger.error("Error updating password policy!", ex);
      throw ServiceException.internalError(ex.getMessage());
    }
  }
}
