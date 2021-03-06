/*
 * Copyright © 2013-2018 camunda services GmbH and various authors (info@camunda.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.camunda.bpm.engine.test.api.multitenancy.query;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.camunda.bpm.engine.exception.NullValueException;
import org.camunda.bpm.engine.impl.test.PluggableProcessEngineTestCase;
import org.camunda.bpm.engine.repository.DecisionDefinition;
import org.camunda.bpm.engine.repository.DecisionDefinitionQuery;

public class MultiTenancyDecisionDefinitionQueryTest extends PluggableProcessEngineTestCase {

  protected static final String DECISION_DEFINITION_KEY = "decision";
  protected static final String DMN = "org/camunda/bpm/engine/test/api/multitenancy/simpleDecisionTable.dmn";

  protected static final String TENANT_ONE = "tenant1";
  protected static final String TENANT_TWO = "tenant2";

  @Override
  protected void setUp() {
    deployment(DMN);
    deploymentForTenant(TENANT_ONE, DMN);
    deploymentForTenant(TENANT_TWO, DMN);
  }

  public void testQueryNoTenantIdSet() {
    DecisionDefinitionQuery query = repositoryService
        .createDecisionDefinitionQuery();

    assertThat(query.count(), is(3L));
  }

  public void testQueryByTenantId() {
    DecisionDefinitionQuery query = repositoryService
        .createDecisionDefinitionQuery()
        .tenantIdIn(TENANT_ONE);

    assertThat(query.count(), is(1L));

    query = repositoryService.
        createDecisionDefinitionQuery()
        .tenantIdIn(TENANT_TWO);

    assertThat(query.count(), is(1L));
  }

  public void testQueryByTenantIds() {
    DecisionDefinitionQuery query = repositoryService
        .createDecisionDefinitionQuery()
        .tenantIdIn(TENANT_ONE, TENANT_TWO);

    assertThat(query.count(), is(2L));
  }

  public void testQueryByDefinitionsWithoutTenantId() {
    DecisionDefinitionQuery query = repositoryService
        .createDecisionDefinitionQuery()
        .withoutTenantId();

    assertThat(query.count(), is(1L));
  }

  public void testQueryByTenantIdsIncludeDefinitionsWithoutTenantId() {
    DecisionDefinitionQuery query = repositoryService
        .createDecisionDefinitionQuery()
        .tenantIdIn(TENANT_ONE)
        .includeDecisionDefinitionsWithoutTenantId();

    assertThat(query.count(), is(2L));

    query = repositoryService
        .createDecisionDefinitionQuery()
        .tenantIdIn(TENANT_TWO)
        .includeDecisionDefinitionsWithoutTenantId();

    assertThat(query.count(), is(2L));

    query = repositoryService
        .createDecisionDefinitionQuery()
        .tenantIdIn(TENANT_ONE, TENANT_TWO)
        .includeDecisionDefinitionsWithoutTenantId();

    assertThat(query.count(), is(3L));
  }

  public void testQueryByKey() {
    DecisionDefinitionQuery query = repositoryService
        .createDecisionDefinitionQuery()
        .decisionDefinitionKey(DECISION_DEFINITION_KEY);
    // one definition for each tenant
    assertThat(query.count(), is(3L));

    query = repositoryService
        .createDecisionDefinitionQuery()
        .decisionDefinitionKey(DECISION_DEFINITION_KEY)
        .withoutTenantId();
    // one definition without tenant id
    assertThat(query.count(), is(1L));

    query = repositoryService
        .createDecisionDefinitionQuery()
        .decisionDefinitionKey(DECISION_DEFINITION_KEY)
        .tenantIdIn(TENANT_ONE);
    // one definition for tenant one
    assertThat(query.count(), is(1L));
  }

  public void testQueryByLatestNoTenantIdSet() {
    // deploy a second version for tenant one
    deploymentForTenant(TENANT_ONE, DMN);

    DecisionDefinitionQuery query = repositoryService
        .createDecisionDefinitionQuery()
        .decisionDefinitionKey(DECISION_DEFINITION_KEY)
        .latestVersion();
    // one definition for each tenant
    assertThat(query.count(), is(3L));

    Map<String, DecisionDefinition> decisionDefinitionsForTenant = getDecisionDefinitionsForTenant(query.list());
    assertThat(decisionDefinitionsForTenant.get(TENANT_ONE).getVersion(), is(2));
    assertThat(decisionDefinitionsForTenant.get(TENANT_TWO).getVersion(), is(1));
    assertThat(decisionDefinitionsForTenant.get(null).getVersion(), is(1));
  }

  public void testQueryByLatestWithTenantId() {
    // deploy a second version for tenant one
    deploymentForTenant(TENANT_ONE, DMN);

    DecisionDefinitionQuery query = repositoryService
        .createDecisionDefinitionQuery()
        .decisionDefinitionKey(DECISION_DEFINITION_KEY)
        .latestVersion()
        .tenantIdIn(TENANT_ONE);

    assertThat(query.count(), is(1L));

    DecisionDefinition decisionDefinition = query.singleResult();
    assertThat(decisionDefinition.getTenantId(), is(TENANT_ONE));
    assertThat(decisionDefinition.getVersion(), is(2));

    query = repositoryService
        .createDecisionDefinitionQuery()
        .decisionDefinitionKey(DECISION_DEFINITION_KEY)
        .latestVersion()
        .tenantIdIn(TENANT_TWO);

    assertThat(query.count(), is(1L));

    decisionDefinition = query.singleResult();
    assertThat(decisionDefinition.getTenantId(), is(TENANT_TWO));
    assertThat(decisionDefinition.getVersion(), is(1));
  }

  public void testQueryByLatestWithTenantIds() {
    // deploy a second version for tenant one
    deploymentForTenant(TENANT_ONE, DMN);

    DecisionDefinitionQuery query = repositoryService
        .createDecisionDefinitionQuery()
        .decisionDefinitionKey(DECISION_DEFINITION_KEY)
        .latestVersion()
        .tenantIdIn(TENANT_ONE, TENANT_TWO)
        .orderByTenantId()
        .asc();
    // one definition for each tenant
    assertThat(query.count(), is(2L));

    Map<String, DecisionDefinition> decisionDefinitionsForTenant = getDecisionDefinitionsForTenant(query.list());
    assertThat(decisionDefinitionsForTenant.get(TENANT_ONE).getVersion(), is(2));
    assertThat(decisionDefinitionsForTenant.get(TENANT_TWO).getVersion(), is(1));
  }

  public void testQueryByLatestWithoutTenantId() {
    // deploy a second version without tenant id
    deployment(DMN);

    DecisionDefinitionQuery query = repositoryService
        .createDecisionDefinitionQuery()
        .decisionDefinitionKey(DECISION_DEFINITION_KEY)
        .latestVersion()
        .withoutTenantId();

    assertThat(query.count(), is(1L));

    DecisionDefinition decisionDefinition = query.singleResult();
    assertThat(decisionDefinition.getTenantId(), is(nullValue()));
    assertThat(decisionDefinition.getVersion(), is(2));
  }

  public void testQueryByLatestWithTenantIdsIncludeDefinitionsWithoutTenantId() {
    // deploy a second version without tenant id
    deployment(DMN);
    // deploy a third version for tenant one
    deploymentForTenant(TENANT_ONE, DMN);
    deploymentForTenant(TENANT_ONE, DMN);

    DecisionDefinitionQuery query = repositoryService
        .createDecisionDefinitionQuery()
        .decisionDefinitionKey(DECISION_DEFINITION_KEY)
        .latestVersion()
        .tenantIdIn(TENANT_ONE, TENANT_TWO)
        .includeDecisionDefinitionsWithoutTenantId();

    assertThat(query.count(), is(3L));

    Map<String, DecisionDefinition> decisionDefinitionsForTenant = getDecisionDefinitionsForTenant(query.list());
    assertThat(decisionDefinitionsForTenant.get(TENANT_ONE).getVersion(), is(3));
    assertThat(decisionDefinitionsForTenant.get(TENANT_TWO).getVersion(), is(1));
    assertThat(decisionDefinitionsForTenant.get(null).getVersion(), is(2));
  }

  public void testQueryByNonExistingTenantId() {
    DecisionDefinitionQuery query = repositoryService
        .createDecisionDefinitionQuery()
        .tenantIdIn("nonExisting");

    assertThat(query.count(), is(0L));
  }

  public void testFailQueryByTenantIdNull() {
    try {
      repositoryService.createDecisionDefinitionQuery()
        .tenantIdIn((String) null);

      fail("expected exception");
    } catch (NullValueException e) {
    }
  }

  public void testQuerySortingAsc() {
    // exclude definitions without tenant id because of database-specific ordering
    List<DecisionDefinition> decisionDefinitions = repositoryService
        .createDecisionDefinitionQuery()
        .tenantIdIn(TENANT_ONE, TENANT_TWO)
        .orderByTenantId()
        .asc()
        .list();

    assertThat(decisionDefinitions.size(), is(2));
    assertThat(decisionDefinitions.get(0).getTenantId(), is(TENANT_ONE));
    assertThat(decisionDefinitions.get(1).getTenantId(), is(TENANT_TWO));
  }

  public void testQuerySortingDesc() {
    // exclude definitions without tenant id because of database-specific ordering
    List<DecisionDefinition> decisionDefinitions = repositoryService
        .createDecisionDefinitionQuery()
        .tenantIdIn(TENANT_ONE, TENANT_TWO)
        .orderByTenantId()
        .desc()
        .list();

    assertThat(decisionDefinitions.size(), is(2));
    assertThat(decisionDefinitions.get(0).getTenantId(), is(TENANT_TWO));
    assertThat(decisionDefinitions.get(1).getTenantId(), is(TENANT_ONE));
  }

  public void testQueryNoAuthenticatedTenants() {
    identityService.setAuthentication("user", null, null);

    DecisionDefinitionQuery query = repositoryService.createDecisionDefinitionQuery();
    assertThat(query.count(), is(1L));
  }

  public void testQueryAuthenticatedTenant() {
    identityService.setAuthentication("user", null, Arrays.asList(TENANT_ONE));

    DecisionDefinitionQuery query = repositoryService.createDecisionDefinitionQuery();

    assertThat(query.count(), is(2L));
    assertThat(query.tenantIdIn(TENANT_ONE).count(), is(1L));
    assertThat(query.tenantIdIn(TENANT_TWO).count(), is(0L));
    assertThat(query.tenantIdIn(TENANT_ONE, TENANT_TWO).includeDecisionDefinitionsWithoutTenantId().count(), is(2L));
  }

  public void testQueryAuthenticatedTenants() {
    identityService.setAuthentication("user", null, Arrays.asList(TENANT_ONE, TENANT_TWO));

    DecisionDefinitionQuery query = repositoryService.createDecisionDefinitionQuery();

    assertThat(query.count(), is(3L));
    assertThat(query.tenantIdIn(TENANT_ONE).count(), is(1L));
    assertThat(query.tenantIdIn(TENANT_TWO).count(), is(1L));
    assertThat(query.withoutTenantId().count(), is(1L));
  }

  public void testQueryDisabledTenantCheck() {
    processEngineConfiguration.setTenantCheckEnabled(false);
    identityService.setAuthentication("user", null, null);

    DecisionDefinitionQuery query = repositoryService.createDecisionDefinitionQuery();
    assertThat(query.count(), is(3L));
  }

  protected Map<String, DecisionDefinition> getDecisionDefinitionsForTenant(List<DecisionDefinition> decisionDefinitions) {
    Map<String, DecisionDefinition> definitionsForTenant = new HashMap<String, DecisionDefinition>();

    for (DecisionDefinition definition : decisionDefinitions) {
      definitionsForTenant.put(definition.getTenantId(), definition);
    }
    return definitionsForTenant;
  }

}
