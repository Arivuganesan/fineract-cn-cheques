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
package io.mifos.cheque;

import io.mifos.anubis.test.v1.TenantApplicationSecurityEnvironmentTestRule;
import io.mifos.cheque.api.v1.EventConstants;
import io.mifos.cheque.api.v1.client.ChequeManager;
import io.mifos.cheque.service.ChequeConfiguration;
import io.mifos.core.api.context.AutoUserContext;
import io.mifos.core.lang.ApplicationName;
import io.mifos.core.test.fixture.TenantDataStoreContextTestRule;
import io.mifos.core.test.listener.EnableEventRecording;
import io.mifos.core.test.listener.EventRecorder;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.netflix.feign.EnableFeignClients;
import org.springframework.cloud.netflix.ribbon.RibbonClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT,
    classes = {AbstractChequeTest.TestConfiguration.class}
)
public abstract class AbstractChequeTest extends SuiteTestEnvironment {
  private static final String TEST_USER = "shed";
  public static final String TEST_LOGGER = "test-logger";

  @ClassRule
  public final static TenantDataStoreContextTestRule tenantDataStoreContext =
      TenantDataStoreContextTestRule.forRandomTenantName(cassandraInitializer, mariaDBInitializer);

  @Rule
  public final TenantApplicationSecurityEnvironmentTestRule tenantApplicationSecurityEnvironment
      = new TenantApplicationSecurityEnvironmentTestRule(testEnvironment, this::waitForInitialize);

  @Autowired
  @Qualifier(TEST_LOGGER)
  protected Logger logger;

  @Autowired
  ChequeManager chequeManager;

  @Autowired
  private ApplicationName applicationName;

  @Autowired
  EventRecorder eventRecorder;

  private AutoUserContext autoUserContext;

  AbstractChequeTest() {
    super();
  }

  @Before
  public void prepTest() throws Exception {
    this.autoUserContext = this.tenantApplicationSecurityEnvironment.createAutoUserContext(AbstractChequeTest.TEST_USER);
  }

  @After
  public void cleanTest() throws Exception {
    this.autoUserContext.close();
  }

  public boolean waitForInitialize() {
    try {
      final String version = this.applicationName.getVersionString();
      this.logger.info("Waiting on initialize event for version: {}.", version);
      return this.eventRecorder.wait(EventConstants.INITIALIZE, version);
    } catch (final InterruptedException e) {
      throw new IllegalStateException(e);
    }
  }

  @Configuration
  @EnableEventRecording
  @EnableFeignClients(basePackages = {"io.mifos.cheque.api.v1"})
  @RibbonClient(name = APP_NAME)
  @Import({ChequeConfiguration.class})
  @ComponentScan("io.mifos.cheque.listener")
  public static class TestConfiguration {
    public TestConfiguration() {
      super();
    }

    @Bean(name= TEST_LOGGER)
    public Logger logger() {
      return LoggerFactory.getLogger(TEST_LOGGER);
    }
  }
}

