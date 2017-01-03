package uk.ac.ebi.biosamples.service;

import org.springframework.hateoas.mvc.ControllerLinkBuilder;
import org.springframework.hateoas.mvc.ResourceAssemblerSupport;
import org.springframework.stereotype.Service;

import uk.ac.ebi.biosamples.controller.SampleRestController;
import uk.ac.ebi.biosamples.models.Sample;

@Service
public class SampleResourceAssembler extends ResourceAssemblerSupport<Sample, SampleResource> {

	public SampleResourceAssembler() {
		super(SampleRestController.class, SampleResource.class);
	}

	@Override
	public SampleResource toResource(Sample sample) {
		SampleResource resource = new SampleResource(sample);
		resource.add(ControllerLinkBuilder.linkTo(ControllerLinkBuilder.methodOn(SampleRestController.class).read(sample.getAccession())).withSelfRel());
		return resource;
	}

}