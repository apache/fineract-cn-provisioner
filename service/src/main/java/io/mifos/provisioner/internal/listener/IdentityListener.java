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
package io.mifos.provisioner.internal.listener;

import com.google.gson.Gson;
import io.mifos.core.lang.config.TenantHeaderFilter;
import io.mifos.identity.api.v1.events.ApplicationPermissionEvent;
import io.mifos.identity.api.v1.events.ApplicationSignatureEvent;
import io.mifos.identity.api.v1.events.EventConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static io.mifos.identity.api.v1.events.EventConstants.OPERATION_POST_APPLICATION_PERMISSION;
import static io.mifos.identity.api.v1.events.EventConstants.OPERATION_POST_PERMITTABLE_GROUP;
import static io.mifos.identity.api.v1.events.EventConstants.OPERATION_PUT_APPLICATION_SIGNATURE;

/**
 * @author Myrle Krantz
 */
@Component
public class IdentityListener {
  private final Gson gson;
  private final Map<EventKey, EventExpectation> eventExpectations = new ConcurrentHashMap<>();

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
    final EventExpectation eventExpectation = eventExpectations.remove(new EventKey(tenantIdentifier, OPERATION_POST_PERMITTABLE_GROUP, payload));
    if (eventExpectation != null) {
      eventExpectation.setEventFound(true);
    }
  }

  @JmsListener(
          subscription = EventConstants.DESTINATION,
          destination = EventConstants.DESTINATION,
          selector = EventConstants.SELECTOR_POST_APPLICATION_PERMISSION
  )
  public void onCreateApplicationPermission(
          @Header(TenantHeaderFilter.TENANT_HEADER)final String tenantIdentifier,
          final String payload) throws Exception {
    final ApplicationPermissionEvent event = gson.fromJson(payload, ApplicationPermissionEvent.class);
    final EventExpectation eventExpectation = eventExpectations.remove(new EventKey(tenantIdentifier, OPERATION_POST_APPLICATION_PERMISSION, event));
    if (eventExpectation != null) {
      eventExpectation.setEventFound(true);
    }
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
    final EventExpectation eventExpectation = eventExpectations.remove(new EventKey(tenantIdentifier, OPERATION_PUT_APPLICATION_SIGNATURE, event));
    if (eventExpectation != null) {
      eventExpectation.setEventFound(true);
    }
  }

  public EventExpectation expectPermittableGroupCreation(final String tenantIdentifier,
                                                         final String permittableGroupIdentifier) {
    final EventKey key = new EventKey(tenantIdentifier, OPERATION_POST_PERMITTABLE_GROUP, permittableGroupIdentifier);
    final EventExpectation value = new EventExpectation(key);
    eventExpectations.put(key, value);
    return value;
  }

  public EventExpectation expectApplicationPermissionCreation(final String tenantIdentifier,
                                                              final String applicationIdentifier,
                                                              final String permittableGroupIdentifier) {
    final ApplicationPermissionEvent expectedEvent = new ApplicationPermissionEvent(applicationIdentifier, permittableGroupIdentifier);
    final EventKey key = new EventKey(tenantIdentifier, OPERATION_POST_APPLICATION_PERMISSION, expectedEvent);
    final EventExpectation value = new EventExpectation(key);
    eventExpectations.put(key, value);
    return value;
  }

  public EventExpectation expectApplicationSignatureSet(final String tenantIdentifier,
                                                        final String applicationIdentifier,
                                                        final String keyTimestamp) {
    final ApplicationSignatureEvent expectedEvent = new ApplicationSignatureEvent(applicationIdentifier, keyTimestamp);
    final EventKey key = new EventKey(tenantIdentifier, OPERATION_PUT_APPLICATION_SIGNATURE, expectedEvent);
    final EventExpectation value = new EventExpectation(key);
    eventExpectations.put(key, value);
    return value;
  }

  public void withdrawExpectation(final EventExpectation eventExpectation) {
    final EventExpectation expectation = eventExpectations.remove(eventExpectation.getKey());
    if (expectation != null) {
      eventExpectation.setEventWithdrawn(true);
    }
  }
}
