package ninja.siden.uml;

import org.junit.jupiter.api.Test;
import reactor.blockhound.BlockHound;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.EntityExchangeResult;
import org.springframework.test.web.reactive.server.WebTestClient;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author taichi
 */
@WebFluxTest(properties = { "url=http://example.com", "token=xxxxxxxxxx" })
public class UmlTest {

	@Autowired
	WebTestClient mock;

	@Test
	public void challenge() throws Exception {
		EventWrapper map = new EventWrapper();
		map.setToken("xxxxxxxxxx");
		map.setChallenge("challenging");
		mock.post().uri("/").contentType(MediaType.APPLICATION_JSON).body(map)
				.exchange().expectStatus().isOk().expectBody(String.class)
				.isEqualTo("challenging");
	}

	@Test
	public void outgoing() throws Exception {
		EventWrapper map = new EventWrapper();
		map.setToken("xxxxxxxxxx");
		map.setText("<@UID> hogehoge");
		String content = "hogehoge";

		String enc = Uml.transcoder().encode(content);
		mock.post().uri("/").contentType(MediaType.APPLICATION_JSON).body(map)
				.exchange().expectStatus().isOk().expectBody().jsonPath("$.text")
				.value((String text) -> valid(text, enc));
	}

	void valid(String text, String enc) {
		assertThat(text).startsWith("http://example.com");
		assertThat(text).endsWith(enc);
	}

	@Test
	public void imgs() throws Exception {
		String content = "Bob->Alice : hello";
		String encoded = Uml.transcoder().encode(content);
		EntityExchangeResult<byte[]> result = mock.get().uri("/{encoded}", encoded)
				.exchange().expectStatus().isOk().expectHeader().contentType("image/png")
				.expectBody().returnResult();
		assertThat(result.getResponseBody()).isNotNull();
	}

	@Test
	public void missing() throws Exception {
		mock.get().uri("/{encoded}", "foo").exchange().expectStatus().isNotFound();
	}

	public static void main(String[] args) {
		BlockHound.install();
		System.setProperty("url", "http://localhost:8080");
		System.setProperty("token", "xxxxx");
		SpringApplication.run(Uml.class, args);
	}
}
