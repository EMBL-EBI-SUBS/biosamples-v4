package uk.ac.ebi.biosamples.client.service;

import java.net.URI;
import java.util.concurrent.ExecutorService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.hateoas.MediaTypes;
import org.springframework.hateoas.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestOperations;
import org.springframework.web.util.UriComponentsBuilder;

import uk.ac.ebi.biosamples.client.ClientProperties;
import uk.ac.ebi.biosamples.model.Sample;

public class SampleSubmissionService {
	
	private Logger log = LoggerFactory.getLogger(getClass());
	
	private final ClientProperties clientProperties;
	
	//use RestOperations as the interface implemented by RestTemplate
	//easier to mock for testing
	private final RestOperations restOperations;
	
	private final ExecutorService executor;
	
	public SampleSubmissionService(ClientProperties clientProperties, RestOperations restOperations, ExecutorService executor) {
		this.clientProperties = clientProperties;
		this.restOperations = restOperations;
		this.executor = executor;
	}

	/**
	 * This will send the sample to biosamples, either by POST if it has no accession or by PUT
	 * if the sample already has an accession associated
	 * 
	 * @param sample
	 * @return
	 */
	//TODO make async
	public Resource<Sample> submit(Sample sample) throws RestClientException{
		//if the sample has an accession, put to that
		if (sample.getAccession() != null) {
			//samples with an existing accession should be PUT			
			URI uri = UriComponentsBuilder.fromUri(clientProperties.getBiosamplesClientUri())
					.pathSegment("samples",sample.getAccession())
					.build().toUri();
			
			log.trace("PUTing to "+uri+" "+sample);
			
			RequestEntity<Sample> requestEntity = RequestEntity.put(uri)
					.contentType(MediaType.APPLICATION_JSON).accept(MediaTypes.HAL_JSON).body(sample);
			ResponseEntity<Resource<Sample>> responseEntity = restOperations.exchange(requestEntity, new ParameterizedTypeReference<Resource<Sample>>(){});
						
			if (!responseEntity.getStatusCode().is2xxSuccessful()) {
				log.error("Unable to PUT "+sample.getAccession()+" : "+responseEntity.toString());
				throw new RuntimeException("Problem PUTing "+sample.getAccession());
			}			
			return responseEntity.getBody();
			
		} else {
			//samples without an existing accession should be POST			
			URI uri = UriComponentsBuilder.fromUri(clientProperties.getBiosamplesClientUri())
					.pathSegment("samples")
					.build().toUri();
			
			log.trace("POSTing to "+uri+" "+sample);
			
			RequestEntity<Sample> requestEntity = RequestEntity.post(uri)
					.contentType(MediaType.APPLICATION_JSON).accept(MediaTypes.HAL_JSON).body(sample);
			ResponseEntity<Resource<Sample>> responseEntity = restOperations.exchange(requestEntity, new ParameterizedTypeReference<Resource<Sample>>(){});
						
			return responseEntity.getBody();
		}
	}
}