package uk.ac.ebi.biosamples.ols;

import java.net.URI;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.hateoas.MediaTypes;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestOperations;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

import com.fasterxml.jackson.databind.JsonNode;

import uk.ac.ebi.biosamples.PipelinesProperties;

@Service
public class OlsProcessor {

	private Logger log = LoggerFactory.getLogger(getClass());

	private final RestOperations restOperations;
	
	//TODO make this an application.properties value
	private final UriComponents uriBuilder;
		
	public OlsProcessor(RestTemplateBuilder restTemplateBuilder, PipelinesProperties pipelinesProperties) {
		this.restOperations = restTemplateBuilder.build();
		uriBuilder = UriComponentsBuilder.fromUriString(pipelinesProperties.getOls()+"/api/terms?id={shortcode}").build();
	}
	
	
	@Cacheable("ols_short")
	public Optional<String> queryOlsForShortcode(String shortcode) {
		log.info("OLS getting : "+shortcode);
		URI uri = uriBuilder.expand(shortcode).encode().toUri();
		
		RequestEntity<Void> requestEntity = RequestEntity.get(uri).accept(MediaTypes.HAL_JSON).build();
		ResponseEntity<JsonNode> responseEntity = restOperations.exchange(requestEntity,
				new ParameterizedTypeReference<JsonNode>(){});
		
		//if zero or more than one result found, abort
		if (responseEntity.getBody().size() != 1) {
			return Optional.empty();
		}
		JsonNode n = responseEntity.getBody();
		
		if (n.has("_embedded")) {
			if (n.get("_embedded").has("terms")) {
				if (n.get("_embedded").get("terms").size() == 1) {
					if (n.get("_embedded").get("terms").get(0).has("iri")) {
						String iri = n.get("_embedded").get("terms").get(0).get("iri").asText();
						log.info("OLS mapped "+shortcode+"  to "+iri);
						return Optional.of(iri);
					}
				}
			}	
		}
		return Optional.empty();
	}
}
