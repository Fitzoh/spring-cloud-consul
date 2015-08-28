/*
 * Copyright 2013-2015 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.cloud.consul.discovery;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;

import java.util.Map;

import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.boot.test.WebIntegrationTest;
import org.springframework.cloud.consul.ConsulAutoConfiguration;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.ecwid.consul.v1.ConsulClient;
import com.ecwid.consul.v1.Response;
import com.ecwid.consul.v1.agent.model.Service;

/**
 * @author Spencer Gibb
 */
@RunWith(SpringJUnit4ClassRunner.class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
@SpringApplicationConfiguration(classes = TestConfig.class)
@WebIntegrationTest(value = "spring.application.name=myTestService1::something", randomPort = true)
public class ConsulLifecycleTests {

	@Autowired
	private ConsulLifecycle lifecycle;

	@Autowired
	private ConsulClient consul;

	@Autowired
	private ApplicationContext context;

	@Test
	public void contextLoads() {
		Response<Map<String, Service>> response = consul.getAgentServices();
		Map<String, Service> services = response.getValue();
		Service service = services.get(lifecycle.getServiceId());
		assertNotNull("service was null", service);
		assertNotEquals("service port is 0", 0, service.getPort().intValue());
		assertFalse("service id contained invalid character: " + service.getId(), service.getId().contains(":"));
		assertEquals("service id was wrong", lifecycle.getServiceId(), service.getId());
		assertEquals("service name was wrong", "myTestService1-something", service.getService());
	}

	@Test
	public void normalizeForDnsWorks() {
		assertEquals("abc1", ConsulLifecycle.normalizeForDns("abc1"));
		assertEquals("ab-c1", ConsulLifecycle.normalizeForDns("ab:c1"));
		assertEquals("ab-c1", ConsulLifecycle.normalizeForDns("ab::c1"));
	}

	@Test(expected = IllegalArgumentException.class)
	public void normalizedFailsIfFirstCharIsNumber() {
		ConsulLifecycle.normalizeForDns("9abc");
	}

	@Test(expected = IllegalArgumentException.class)
	public void normalizedFailsIfFirstCharIsNotAlpha() {
		ConsulLifecycle.normalizeForDns(":abc");
	}

	@Test(expected = IllegalArgumentException.class)
	public void normalizedFailsIfLastCharIsNotAlphaNumeric() {
		ConsulLifecycle.normalizeForDns("abc:");
	}
}

@Configuration
@EnableAutoConfiguration
@Import({ ConsulAutoConfiguration.class, ConsulDiscoveryClientConfiguration.class })
class TestConfig {

}
