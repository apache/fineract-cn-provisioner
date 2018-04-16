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
package org.apache.fineract.cn.provisioner.rest.mapper;

import org.apache.fineract.cn.provisioner.api.v1.domain.AssignedApplication;
import org.apache.fineract.cn.provisioner.internal.repository.TenantApplicationEntity;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

public class AssignedApplicationMapper {

  private AssignedApplicationMapper() {
    super();
  }

  public static TenantApplicationEntity map(final String identifier, final List<AssignedApplication> assignedApplications) {
    final TenantApplicationEntity tenantApplicationEntity = new TenantApplicationEntity();
    tenantApplicationEntity.setTenantIdentifier(identifier);

    final HashSet<String> applicationNames = new HashSet<>();
    tenantApplicationEntity.setApplications(applicationNames);
    applicationNames.addAll(assignedApplications
        .stream().map(AssignedApplication::getName)
        .collect(Collectors.toList()));
    return tenantApplicationEntity;
  }

  public static List<AssignedApplication> map(final TenantApplicationEntity tenantApplicationEntity) {
    final ArrayList<AssignedApplication> assignedApplications = new ArrayList<>();
    if (tenantApplicationEntity != null) {
      for (final String name : tenantApplicationEntity.getApplications()) {
        final AssignedApplication assignedApplication = new AssignedApplication();
        assignedApplications.add(assignedApplication);
        assignedApplication.setName(name);
      }

    }
    return assignedApplications;
  }
}
