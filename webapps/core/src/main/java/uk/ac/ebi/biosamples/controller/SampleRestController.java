package uk.ac.ebi.biosamples.controller;

import java.net.URI;
import java.time.Instant;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.hateoas.mvc.ControllerLinkBuilder;
import org.springframework.util.MultiValueMap;
import uk.ac.ebi.biosamples.model.JsonLDSample;
import org.springframework.hateoas.EntityLinks;
import org.springframework.hateoas.ExposesResourceFor;
import org.springframework.hateoas.MediaTypes;
import org.springframework.hateoas.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import uk.ac.ebi.biosamples.controller.exception.SampleNotAccessibleException;
import uk.ac.ebi.biosamples.controller.exception.SampleNotFoundException;
import uk.ac.ebi.biosamples.model.Sample;
import uk.ac.ebi.biosamples.service.BioSamplesAapService;
import uk.ac.ebi.biosamples.service.FilterService;
import uk.ac.ebi.biosamples.service.JsonLDService;
import uk.ac.ebi.biosamples.service.SamplePageService;
import uk.ac.ebi.biosamples.service.SampleReadService;
import uk.ac.ebi.biosamples.service.SampleResourceAssembler;
import uk.ac.ebi.biosamples.service.SampleService;
import java.time.ZoneOffset;
import java.util.Optional;

/**
 * Primary controller for REST operations both in JSON and XML and both read and
 * write.
 * 
 * See {@link SampleHtmlController} for the HTML equivalent controller.
 * 
 * @author faulcon
 *
 */
@RestController
@ExposesResourceFor(Sample.class)
@RequestMapping("/samples")
public class SampleRestController {

	private final SampleService sampleService;
	private final SamplePageService samplePageService;
	private final FilterService filterService;
	private final BioSamplesAapService bioSamplesAapService;

	private final SampleResourceAssembler sampleResourceAssembler;

	private final EntityLinks entityLinks;

    private final JsonLDService jsonLDService;

    private Logger log = LoggerFactory.getLogger(getClass());

	public SampleRestController(SampleService sampleService, 
			SamplePageService samplePageService,FilterService filterService, 
			BioSamplesAapService bioSamplesAapService,
			SampleResourceAssembler sampleResourceAssembler, 
			EntityLinks entityLinks,
            JsonLDService jsonLDService) {
		this.sampleService = sampleService;
		this.samplePageService = samplePageService;
		this.bioSamplesAapService = bioSamplesAapService;
		this.filterService = filterService;
		this.sampleResourceAssembler = sampleResourceAssembler;
		this.jsonLDService = jsonLDService;
		this.entityLinks = entityLinks;
	}

    @PreAuthorize("isAuthenticated()")
	@GetMapping(value = "/{accession}", produces = { MediaTypes.HAL_JSON_VALUE, MediaType.APPLICATION_JSON_VALUE })
	public Resource<Sample> getSampleHal(@PathVariable String accession) {
		log.trace("starting call");
		// convert it into the format to return
		Optional<Sample> sample = sampleService.fetch(accession);
		if (!sample.isPresent()) {
			throw new SampleNotFoundException();
		}
		bioSamplesAapService.checkAccessible(sample.get());
		Resource<Sample> sampleResource = sampleResourceAssembler.toResource(sample.get());
		return sampleResource;
	}

    @PreAuthorize("isAuthenticated()")
	@CrossOrigin(methods = RequestMethod.GET)
	@GetMapping(value = "/{accession}", produces = { MediaType.APPLICATION_XML_VALUE, MediaType.TEXT_XML_VALUE })
	public Sample getSampleXml(@PathVariable String accession) {
		return this.getSampleHal(accession).getContent();
	}

    @PreAuthorize("isAuthenticated()")
	@CrossOrigin(methods = RequestMethod.GET)
    @GetMapping(value = "/{accession}", produces = "application/ld+json")
    public JsonLDSample getJsonLDSample(@PathVariable String accession) {
		Optional<Sample> sample = sampleService.fetch(accession);
		if (!sample.isPresent()) {
			throw new SampleNotFoundException();
		}
		bioSamplesAapService.checkAccessible(sample.get());

        // check if the release date is in the future and if so return it as
        // private
        if (sample.get().getRelease().isAfter(Instant.now())) {
			throw new SampleNotAccessibleException();
        }

        JsonLDSample jsonLDSample = jsonLDService.sampleToJsonLD(sample.get());
        
        return jsonLDSample;
    }

	@ResponseStatus(value = HttpStatus.BAD_REQUEST, reason = "Sample accession must match URL accession") // 400
	public static class SampleAccessionMismatchException extends RuntimeException {
	}

    @PreAuthorize("isAuthenticated()")
	@PutMapping(value = "/{accession}", consumes = { MediaType.APPLICATION_JSON_VALUE })
	public Resource<Sample> put(@PathVariable String accession, 
			@RequestBody Sample sample,
			@RequestParam(name = "setupdatedate", required = false, defaultValue="true") boolean setUpdateDate) {
    	
    	if (sample.getAccession() == null || !sample.getAccession().equals(accession)) {
			// if the accession in the body is different to the accession in the
			// url, throw an error
			// TODO create proper exception with right http error code
			throw new SampleAccessionMismatchException();
		}		
		
		log.info("Recieved PUT for " + accession);
		sample = bioSamplesAapService.handleSampleDomain(sample);		
		
		//TODO limit use of this method to write super-users only
		if (setUpdateDate) {
			sample = Sample.build(sample.getName(), sample.getAccession(), sample.getDomain(), sample.getRelease(), Instant.now(),
					sample.getCharacteristics(), sample.getRelationships(), sample.getExternalReferences());
		}
		
		sample = sampleService.store(sample);

		// assemble a resource to return
		Resource<Sample> sampleResource = sampleResourceAssembler.toResource(sample);

		// create the response object with the appropriate status
		return sampleResource;
	}

	@PreAuthorize("isAuthenticated()")
	@PostMapping(consumes = { MediaType.APPLICATION_JSON_VALUE })
	public ResponseEntity<Resource<Sample>> post(@RequestBody Sample sample,
			@RequestParam(name = "setupdatedate", required = false, defaultValue="true") boolean setUpdateDate) {
		
		log.debug("Recieved POST for "+sample);
		sample = bioSamplesAapService.handleSampleDomain(sample);

		//TODO limit use of this method to write super-users only
		if (setUpdateDate) {
			sample = Sample.build(sample.getName(), sample.getAccession(), sample.getDomain(), sample.getRelease(), Instant.now(),
					sample.getCharacteristics(), sample.getRelationships(), sample.getExternalReferences());
		}
		
		sample = sampleService.store(sample);
		
		// assemble a resource to return
		Resource<Sample> sampleResource = sampleResourceAssembler.toResource(sample);

		// create the response object with the appropriate status
		//TODO work out how to avoid using ResponseEntity but also set location header
		return ResponseEntity.created(URI.create(sampleResource.getLink("self").getHref())).body(sampleResource);
	}

}
