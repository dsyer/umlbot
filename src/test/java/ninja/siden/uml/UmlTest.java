package ninja.siden.uml;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import static org.hamcrest.CoreMatchers.endsWith;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * @author taichi
 */
@WebMvcTest(properties = { "url=http://example.com", "token=xxxxxxxxxx" })
public class UmlTest {

	@Autowired
	MockMvc mock;

	@Test
	public void outgoing() throws Exception {
		MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
		map.set("token", "xxxxxxxxxx");
		map.set("trigger_word", "@startuml");
		String content = "hogehoge";
		map.set("text", "@startuml\n" + content + "\n@enduml");

		String enc = Uml.transcoder().encode(content);
		mock.perform(post("/").contentType(MediaType.APPLICATION_FORM_URLENCODED).params(map))
				.andExpect(jsonPath("$.text", startsWith("http://example.com")))
				.andExpect(jsonPath("$.text", endsWith(enc)));
	}

	@Test
	public void imgs() throws Exception {
		String content = "Bob->Alice : hello";
		String encoded = Uml.transcoder().encode(content);
		mock.perform(get("/{encoded}", encoded)).andExpect(status().isOk())
				.andExpect(content().string(notNullValue()));
	}
}
