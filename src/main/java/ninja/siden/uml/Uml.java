package ninja.siden.uml;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URL;
import java.util.Set;

import net.sourceforge.plantuml.FileFormat;
import net.sourceforge.plantuml.FileFormatOption;
import net.sourceforge.plantuml.SourceStringReader;
import net.sourceforge.plantuml.code.Transcoder;
import net.sourceforge.plantuml.code.TranscoderUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;

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

	@GetMapping("/")
	String yay() {
		return "I'm running!! yey!";
	}

	@PostMapping("/")
	Mono<String> outgoing(ServerWebExchange exchange) throws Exception {
		return exchange.getFormData().map(form -> form.toSingleValueMap()).map(form -> {
			LOG.debug("" + form);

			if (!tokens.contains(form.get("token"))) {
				return ignored;
			}

			String tw = form.getOrDefault("trigger_word", "");
			String txt = form.getOrDefault("text", "");
			if (!txt.isEmpty()) {
				txt = unescape(txt);
			}
			if (txt.length() < tw.length()) {
				return ignored;
			}

			String content = txt.substring(tw.length()).trim();
			String end = "@enduml";
			if (content.endsWith(end)) {
				content = content.substring(0, content.length() - end.length());
			}
			if (5000 < content.length()) {
				StringBuilder stb = new StringBuilder();
				stb.append("{\"text\":\"");
				stb.append("very large content has come. i cant process huge content.");
				stb.append("\"}");
				return stb.toString();
			}

			StringBuilder stb = new StringBuilder(100);
			stb.append("{\"text\":\"");
			stb.append(url);
			stb.append('/');
			try {
				stb.append(transcoder().encode(content));
			}
			catch (IOException e) {
				throw new IllegalStateException(e);
			}
			stb.append("\"}");
			return stb.toString();
		});
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

			if (tokens == null || tokens.isEmpty()) {
				LOG.error("TOKEN is not defined.");
				return;
			}

		};
	}
}
