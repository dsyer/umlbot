/*
 * Copyright 2019-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ninja.siden.uml;

import java.util.Map;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.json.JsonTest;
import org.springframework.boot.test.json.JacksonTester;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Dave Syer
 *
 */
@JsonTest
public class JsonBindingTest {

	@Autowired
	JacksonTester<EventWrapper> tester;

	@Test
	public void deserializeAny() throws Exception {
		assertThat(tester.parseObject("{\"foo\":\"bar\"}").getMap()).containsKey("foo");
	}

	@Test
	public void deserializeAnyMap() throws Exception {
		assertThat(tester.parseObject("{\"foo\":{\"bar\":\"spam\"}}").getMap().get("foo")).isInstanceOf(Map.class);
	}

	@Test
	public void serializeAny() throws Exception {
		EventWrapper wrapper = new EventWrapper();
		wrapper.setField("foo","bar");
		assertThat(tester.write(wrapper).getJson()).contains("foo");
	}

}
