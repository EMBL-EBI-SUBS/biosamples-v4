package uk.ac.ebi.biosamples.service;

import org.springframework.hateoas.EntityLinks;
import org.springframework.hateoas.Resource;
import org.springframework.hateoas.ResourceAssembler;
import org.springframework.hateoas.mvc.ControllerLinkBuilder;
import org.springframework.stereotype.Service;

import uk.ac.ebi.biosamples.controller.SampleRestController;
import uk.ac.ebi.biosamples.model.ExternalReference;
import uk.ac.ebi.biosamples.model.Sample;

/**
 * This class is used by Spring to add HAL _links for {@Link Sample} objects.
 * 
 * @author faulcon
 *
 */
@Service
public class ExternalReferenceResourceAssembler implements ResourceAssembler<ExternalReference, Resource<ExternalReference>> {

	private final EntityLinks entityLinks;
	
	public ExternalReferenceResourceAssembler(EntityLinks entityLinks) {
		this.entityLinks = entityLinks;
	}

	@Override
	public Resource<ExternalReference> toResource(ExternalReference externalReference) {
		Resource<ExternalReference> resource = new Resource<>(externalReference);
		
		resource.add(entityLinks.linkToSingleResource(ExternalReference.class, externalReference.getId()).withSelfRel());
		
		for (String sample : externalReference.getSamples()) {
			resource.add(entityLinks.linkToSingleResource(Sample.class, sample).withRel("sample"));
		}
		
		return resource;
	}

}
