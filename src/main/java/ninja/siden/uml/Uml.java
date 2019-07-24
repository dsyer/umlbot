package ninja.siden.uml;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import net.sourceforge.plantuml.FileFormat;
import net.sourceforge.plantuml.FileFormatOption;
import net.sourceforge.plantuml.SourceStringReader;
import net.sourceforge.plantuml.code.Transcoder;
import net.sourceforge.plantuml.code.TranscoderUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * @author taichi
 * @author Dave Syer
 */
@SpringBootApplication(proxyBeanMethods = false)
@RestController
public class Uml {

	static final Logger LOG = LoggerFactory.getLogger(Uml.class);

	static final String ignored = "{\"text\":\"\"}";

	@Value("${url}")
	String url;
	@Value("${token}")
	Set<String> tokens;
	@Value("${bot.token}")
	String botToken;

	@Autowired
	SlackClient slackClient;

	@Bean
	SlackClient slackClient(WebClient.Builder builder) {
		System.err.println("TOKEN: " + botToken);
		return new SlackClient(
				builder.filter(ExchangeFilterFunction.ofRequestProcessor(request -> {
					LOG.debug("Request: " + request.method() + " " + request.url());
					request.headers()
							.forEach((name, value) -> LOG.debug(name + ":" + value));
					return Mono.just(request);
				})).defaultHeader("Authorization", "Bearer " + botToken)
						.baseUrl("https://slack.com/api").build());
	}

	@GetMapping("/")
	String yay() {
		return "I'm running!! yey!";
	}

	@PostMapping("/")
	Mono<String> outgoing(@RequestBody Mono<EventWrapper> body) throws Exception {
		return body.map(form -> {
			LOG.info("" + form);

			if (form.getChallenge() != null) {
				return form.getChallenge();
			}

			if (!tokens.contains(form.getToken())) {
				return ignored;
			}

			String content = content(form.getEvent().getText());
			if (!content.isEmpty()) {
				content = unescape(content);
			}
			if (content.isEmpty()) {
				return ignored;
			}

			Message message = new Message();

			if (5000 < content.length()) {
				message.setText(
						"very large content has come. i cant process huge content.");
			}
			else {
				try {
					message.setText(url + transcoder().encode(content));
				}
				catch (IOException e) {
					throw new IllegalStateException(e);
				}
			}
			message.setChannel(form.getEvent().getChannel());
			Schedulers.elastic().schedule(() -> slackClient.post(message).block());

			return "OK";
		});
	}

	private static Pattern USER_MESSAGE_PATTERN = Pattern.compile("<@[a-zA-Z0-9]*>");

	private String content(String text) {
		return USER_MESSAGE_PATTERN.splitAsStream(text).collect(Collectors.joining())
				.trim();
	}

	String unescape(String txt) {
		// https://api.slack.com/docs/formatting
		return txt.replace("&amp", "&").replace("&lt;", "<").replace("&gt;", ">");
	}

	static Transcoder transcoder() {
		return TranscoderUtil.getDefaultTranscoder();
	}

	@GetMapping(path = "/{encoded}", produces = "image/png")
	Mono<byte[]> imgs(@PathVariable String encoded) throws Exception {
		return Mono.fromSupplier(() -> {
			try {
				String decoded = transcoder().decode(encoded);
				ByteArrayOutputStream os = new ByteArrayOutputStream();
				new SourceStringReader(decoded).generateImage(os,
						new FileFormatOption(FileFormat.PNG, false));
				return os.toByteArray();
			}
			catch (Exception ex) {
				LOG.error(ex.getMessage(), ex);
				throw new ResourceNotFoundException();
			}
		}).subscribeOn(Schedulers.elastic());
	}

	@ResponseStatus(value = HttpStatus.NOT_FOUND)
	public static class ResourceNotFoundException extends RuntimeException {
	}

	public static void main(String[] args) throws Exception {
		SpringApplication.run(Uml.class, args);
	}

	@Bean
	public CommandLineRunner runner() {
		return args -> {

			// https://devcenter.heroku.com/articles/dynos#local-environment-variables
			LOG.info("" + System.getenv());

			if (url == null || url.isEmpty()) {
				LOG.error("URL is not defined.");
				return;
			}
			try {
				URL u = new URL(url);
				if (u.getProtocol().startsWith("http") == false) {
					LOG.error("URL protocol must be http");
					return;
				}
			}
			catch (IOException e) {
				LOG.error("URL is not valid.");
				return;
			}
			if (!url.endsWith("/")) {
				url = url + "/";
			}

			if (tokens == null || tokens.isEmpty()) {
				LOG.error("TOKEN is not defined.");
				return;
			}

		};
	}
}

class SlackClient {

	static final Logger LOG = LoggerFactory.getLogger(SlackClient.class);
	private WebClient client;

	public SlackClient(WebClient client) {
		this.client = client;
	}

	Mono<Void> post(Message message) {
		LOG.info("Reponse: " + message);
		return client.post().uri("/chat.postMessage")
				.contentType(MediaType.APPLICATION_JSON).body(message).exchange().log()
				.and(Mono.empty());
	}

}

class Message {

	private String text = "";

	private String channel;

	public String getChannel() {
		return channel;
	}

	public void setChannel(String channel) {
		this.channel = channel;
	}

	public String getText() {
		return text;
	}

	public void setText(String text) {
		this.text = text;
	}

	@Override
	public String toString() {
		return "Message [channel=" + channel + ", text=" + text + "]";
	}

}

@JsonInclude(Include.NON_EMPTY)
class EventWrapper {
	private String token;
	private String challenge;
	private String type;
	private Event event = new Event();
	private Map<String, Object> map = new HashMap<>();

	public String getToken() {
		return token;
	}

	public void setToken(String token) {
		this.token = token;
	}

	public String getChallenge() {
		return challenge;
	}

	public void setChallenge(String challenge) {
		this.challenge = challenge;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public Event getEvent() {
		return event;
	}

	public void setEvent(Event event) {
		this.event = event;
	}

	@JsonAnyGetter
	public Map<String, Object> getMap() {
		return this.map;
	}

	@JsonAnySetter
	public void setField(String key, Object value) {
		this.map.put(key, value);
	}

	@JsonInclude(Include.NON_EMPTY)
	static class Event {
		private Map<String, Object> map = new HashMap<>();

		private String text = "";

		private String channel;

		public String getChannel() {
			return channel;
		}

		public void setChannel(String channel) {
			this.channel = channel;
		}

		public String getText() {
			return text;
		}

		public void setText(String text) {
			this.text = text;
		}

		@Override
		public String toString() {
			return "Event [text=" + text + ", channel=" + channel + ", map=" + map + "]";
		}

		@JsonAnyGetter
		public Map<String, Object> getMap() {
			return this.map;
		}

		@JsonAnySetter
		public void setField(String key, Object value) {
			this.map.put(key, value);
		}

	}

	@Override
	public String toString() {
		return "EventWrapper [token=" + token + ", challenge=" + challenge + ", type="
				+ type + ", event=" + event + ", map=" + map + "]";
	}

}
