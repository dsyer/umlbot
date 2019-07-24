package ninja.siden.uml;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import reactor.blockhound.BlockHound;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
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

	@MockBean
	SlackClient slackClient;

	@Test
	public void challenge() throws Exception {
		EventWrapper map = new EventWrapper();
		map.setToken("xxxxxxxxxx");
		map.setChallenge("challenging");
		mock.post().uri("/").contentType(MediaType.APPLICATION_JSON).body(map).exchange()
				.expectStatus().isOk().expectBody(String.class).isEqualTo("challenging");
	}

	@Test
	public void outgoing() throws Exception {
		EventWrapper map = new EventWrapper();
		map.setToken("xxxxxxxxxx");
		map.getEvent().setText("<@UID> hogehoge");
		String content = "hogehoge";

		String enc = Uml.transcoder().encode(content);
		final ArgumentCaptor<Message> captor = ArgumentCaptor.forClass(Message.class);
		mock.post().uri("/").contentType(MediaType.APPLICATION_JSON).body(map).exchange()
				.expectStatus().isOk().expectBody(String.class)
				.value(text -> assertThat(text).isNotEmpty());
		// Thread.sleep(200L);
		Mockito.verify(slackClient).post(captor.capture());
		assertThat(captor.getValue().getText().endsWith(enc));
		assertThat(captor.getValue().getText().startsWith("http://example.com"));
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
