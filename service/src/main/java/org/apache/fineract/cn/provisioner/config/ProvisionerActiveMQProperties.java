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

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @author Myrle Krantz
 */
@ConfigurationProperties(prefix = "activemq")
public class ProvisionerActiveMQProperties {
  @SuppressWarnings("unused")
  final public static String ACTIVEMQ_BROKER_URL_PROP = "activemq.brokerUrl";
  @SuppressWarnings("unused")
  final public static String ACTIVEMQ_CONCURRENCY_PROP = "activemq.concurrency";
  @SuppressWarnings("WeakerAccess")
  final public static String ACTIVEMQ_BROKER_URL_DEFAULT = "vm://activemq?broker.persistent=false";
  @SuppressWarnings("WeakerAccess")
  final public static String ACTIVEMQ_CONCURRENCY_DEFAULT = "3-10";

  private String brokerUrl = ACTIVEMQ_BROKER_URL_DEFAULT;
  private String concurrency = ACTIVEMQ_CONCURRENCY_DEFAULT;

  public ProvisionerActiveMQProperties() {
  }

  public String getBrokerUrl() {
    return brokerUrl;
  }

  public void setBrokerUrl(String brokerUrl) {
    this.brokerUrl = brokerUrl;
  }

  public String getConcurrency() {
    return concurrency;
  }

  public void setConcurrency(String concurrency) {
    this.concurrency = concurrency;
  }
}
