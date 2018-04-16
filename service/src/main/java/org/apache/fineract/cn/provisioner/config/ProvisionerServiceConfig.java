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
package org.apache.fineract.cn.provisioner.config;

import org.apache.fineract.cn.provisioner.internal.util.TokenProvider;
import org.apache.activemq.jms.pool.PooledConnectionFactory;
import org.apache.activemq.spring.ActiveMQConnectionFactory;
import org.apache.fineract.cn.anubis.config.EnableAnubis;
import org.apache.fineract.cn.anubis.token.SystemAccessTokenSerializer;
import org.apache.fineract.cn.api.util.ApiFactory;
import org.apache.fineract.cn.async.config.EnableAsync;
import org.apache.fineract.cn.cassandra.config.EnableCassandra;
import org.apache.fineract.cn.crypto.config.EnableCrypto;
import org.apache.fineract.cn.lang.config.EnableApplicationName;
import org.apache.fineract.cn.lang.config.EnableServiceException;
import org.apache.fineract.cn.mariadb.config.EnableMariaDB;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.jms.config.DefaultJmsListenerContainerFactory;
import org.springframework.jms.config.JmsListenerContainerFactory;
import org.springframework.web.servlet.config.annotation.PathMatchConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;

@Configuration
@EnableAutoConfiguration
@ComponentScan({
    "org.apache.fineract.cn.provisioner.internal.service",
    "org.apache.fineract.cn.provisioner.internal.listener",
    "org.apache.fineract.cn.provisioner.internal.service.applications",
    "org.apache.fineract.cn.provisioner.internal.repository",
    "org.apache.fineract.cn.provisioner.rest.controller",
})
@EnableCrypto
@EnableAsync
@EnableAnubis(provideSignatureRestController = false)
@EnableMariaDB
@EnableCassandra
@EnableServiceException
@EnableApplicationName
@EnableConfigurationProperties({ProvisionerActiveMQProperties.class, ProvisionerProperties.class, SystemProperties.class})
public class ProvisionerServiceConfig extends WebMvcConfigurerAdapter {

  public ProvisionerServiceConfig() {
    super();
  }

  @Bean(name = ProvisionerConstants.LOGGER_NAME)
  public Logger logger() {
    return LoggerFactory.getLogger(ProvisionerConstants.LOGGER_NAME);
  }

  @Bean(name = "tokenProvider")
  public TokenProvider tokenProvider(final SystemProperties systemProperties,
                                     @SuppressWarnings("SpringJavaAutowiringInspection") final SystemAccessTokenSerializer tokenSerializer,
                                     @Qualifier(ProvisionerConstants.LOGGER_NAME) final Logger logger) {
    final String timestamp = systemProperties.getPublicKey().getTimestamp();
    logger.info("Provisioner key timestamp: " + timestamp);

    return new TokenProvider( timestamp,
        systemProperties.getPrivateKey().getModulus(),
        systemProperties.getPrivateKey().getExponent(), tokenSerializer);
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
