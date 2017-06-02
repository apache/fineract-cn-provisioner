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

import io.mifos.anubis.config.AnubisConstants;
import io.mifos.anubis.config.EnableAnubis;
import io.mifos.anubis.token.SystemAccessTokenSerializer;
import io.mifos.core.api.util.ApiFactory;
import io.mifos.core.async.config.EnableAsync;
import io.mifos.core.cassandra.config.EnableCassandra;
import io.mifos.core.lang.config.EnableApplicationName;
import io.mifos.core.lang.config.EnableServiceException;
import io.mifos.core.mariadb.config.EnableMariaDB;
import io.mifos.provisioner.internal.util.TokenProvider;
import io.mifos.tool.crypto.config.EnableCrypto;
import org.apache.activemq.jms.pool.PooledConnectionFactory;
import org.apache.activemq.spring.ActiveMQConnectionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.jms.config.DefaultJmsListenerContainerFactory;
import org.springframework.jms.config.JmsListenerContainerFactory;
import org.springframework.web.servlet.config.annotation.PathMatchConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;

import java.math.BigInteger;

@Configuration
@EnableAutoConfiguration
@ComponentScan({
    "io.mifos.provisioner.internal.service",
    "io.mifos.provisioner.internal.listener",
    "io.mifos.provisioner.internal.service.applications",
    "io.mifos.provisioner.internal.repository",
    "io.mifos.provisioner.rest.controller",
})
@EnableCrypto
@EnableAsync
@EnableAnubis(provideSignatureRestController = false)
@EnableMariaDB
@EnableCassandra
@EnableServiceException
@EnableApplicationName
@EnableConfigurationProperties({ProvisionerActiveMQProperties.class})
public class ProvisionerServiceConfig extends WebMvcConfigurerAdapter {

  public ProvisionerServiceConfig() {
    super();
  }

  @Bean(name = ProvisionerConstants.LOGGER_NAME)
  public Logger logger() {
    return LoggerFactory.getLogger(ProvisionerConstants.LOGGER_NAME);
  }

  @Bean(name = "tokenProvider")
  public TokenProvider tokenProvider(final Environment environment,
                                     @SuppressWarnings("SpringJavaAutowiringInspection") final SystemAccessTokenSerializer tokenSerializer,
                                     @Qualifier(ProvisionerConstants.LOGGER_NAME) final Logger logger) {
    final String timestamp = environment.getProperty(AnubisConstants.PUBLIC_KEY_TIMESTAMP_PROPERTY);
    logger.info("Provisioner key timestamp: " + timestamp);

    return new TokenProvider( timestamp,
        new BigInteger(environment.getProperty("system.privateKey.modulus")),
        new BigInteger(environment.getProperty("system.privateKey.exponent")), tokenSerializer);
  }

  @Bean
  public ApiFactory apiFactory(@Qualifier(ProvisionerConstants.LOGGER_NAME) final Logger logger) {
    return new ApiFactory(logger);
  }

  @Override
  public void configurePathMatch(final PathMatchConfigurer configurer) {
    configurer.setUseSuffixPatternMatch(Boolean.FALSE);
  }

  @Bean
  public PooledConnectionFactory jmsFactory(final ProvisionerActiveMQProperties activeMQProperties) {
    final PooledConnectionFactory pooledConnectionFactory = new PooledConnectionFactory();
    final ActiveMQConnectionFactory activeMQConnectionFactory = new ActiveMQConnectionFactory();
    activeMQConnectionFactory.setBrokerURL(activeMQProperties.getBrokerUrl());
    pooledConnectionFactory.setConnectionFactory(activeMQConnectionFactory);

    return pooledConnectionFactory;
  }


  @Bean
  public JmsListenerContainerFactory jmsListenerContainerFactory(final PooledConnectionFactory jmsFactory, final ProvisionerActiveMQProperties activeMQProperties) {
    final DefaultJmsListenerContainerFactory factory = new DefaultJmsListenerContainerFactory();
    factory.setPubSubDomain(true);
    factory.setConnectionFactory(jmsFactory);
    factory.setConcurrency(activeMQProperties.getConcurrency());
    return factory;
  }
}
