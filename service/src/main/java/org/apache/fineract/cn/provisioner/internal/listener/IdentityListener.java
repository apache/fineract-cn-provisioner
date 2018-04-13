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
package org.apache.fineract.cn.provisioner.internal.listener;

import static org.apache.fineract.cn.identity.api.v1.events.EventConstants.OPERATION_POST_PERMITTABLE_GROUP;
import static org.apache.fineract.cn.identity.api.v1.events.EventConstants.OPERATION_PUT_APPLICATION_SIGNATURE;

import com.google.gson.Gson;
import org.apache.fineract.cn.identity.api.v1.events.ApplicationSignatureEvent;
import org.apache.fineract.cn.identity.api.v1.events.EventConstants;
import org.apache.fineract.cn.lang.config.TenantHeaderFilter;
import org.apache.fineract.cn.lang.listening.EventExpectation;
import org.apache.fineract.cn.lang.listening.EventKey;
import org.apache.fineract.cn.lang.listening.TenantedEventListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

/**
 * @author Myrle Krantz
 */
@Component
public class IdentityListener {
  private final Gson gson;
  private final TenantedEventListener eventListener = new TenantedEventListener();

  @Autowired
  public IdentityListener(final Gson gson) {
    this.gson = gson;
  }

  @JmsListener(
          subscription = EventConstants.DESTINATION,
          destination = EventConstants.DESTINATION,
          selector = EventConstants.SELECTOR_POST_PERMITTABLE_GROUP
  )
  public void onCreatePermittableGroup(
          @Header(TenantHeaderFilter.TENANT_HEADER)final String tenantIdentifier,
          final String payload) throws Exception {
    eventListener.notify(new EventKey(tenantIdentifier, OPERATION_POST_PERMITTABLE_GROUP, payload));
  }

  @JmsListener(
          subscription = EventConstants.DESTINATION,
          destination = EventConstants.DESTINATION,
          selector = EventConstants.SELECTOR_PUT_APPLICATION_SIGNATURE
  )
  public void onSetApplicationSignature(
          @Header(TenantHeaderFilter.TENANT_HEADER)final String tenantIdentifier,
          final String payload) throws Exception {
    final ApplicationSignatureEvent event = gson.fromJson(payload, ApplicationSignatureEvent.class);
    eventListener.notify(new EventKey(tenantIdentifier, OPERATION_PUT_APPLICATION_SIGNATURE, event));
  }

  public EventExpectation expectPermittableGroupCreation(final String tenantIdentifier,
                                                         final String permittableGroupIdentifier) {
    return eventListener.expect(new EventKey(tenantIdentifier, OPERATION_POST_PERMITTABLE_GROUP, permittableGroupIdentifier));
  }

  public EventExpectation expectApplicationSignatureSet(final String tenantIdentifier,
                                                        final String applicationIdentifier,
                                                        final String keyTimestamp) {
    final ApplicationSignatureEvent expectedEvent = new ApplicationSignatureEvent(applicationIdentifier, keyTimestamp);
    return eventListener.expect(new EventKey(tenantIdentifier, OPERATION_PUT_APPLICATION_SIGNATURE, expectedEvent));
  }

  public void withdrawExpectation(final EventExpectation eventExpectation) {
    eventListener.withdrawExpectation(eventExpectation);
  }
}