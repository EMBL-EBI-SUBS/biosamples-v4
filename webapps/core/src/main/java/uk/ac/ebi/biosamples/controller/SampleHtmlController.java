package uk.ac.ebi.biosamples.controller;

import java.net.URI;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import org.springframework.web.util.UriComponentsBuilder;
import uk.ac.ebi.biosamples.model.Sample;
import uk.ac.ebi.biosamples.model.SampleFacet;
import uk.ac.ebi.biosamples.model.SampleFacetValue;
import uk.ac.ebi.biosamples.service.BioSamplesAapService;
import uk.ac.ebi.biosamples.service.FacetService;
import uk.ac.ebi.biosamples.service.FilterService;
import uk.ac.ebi.biosamples.service.JsonLDService;
import uk.ac.ebi.biosamples.service.SamplePageService;
import uk.ac.ebi.biosamples.service.SampleReadService;
import uk.ac.ebi.biosamples.service.SampleService;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.net.URI;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/**
 * Primary controller for HTML operations.
 * 
 * See {@link SampleRestController} for the equivalent REST controller.
 * 
 * @author faulcon
 *
 */
@Controller
@RequestMapping(value = "/", produces = MediaType.TEXT_HTML_VALUE)
public class SampleHtmlController {

	private Logger log = LoggerFactory.getLogger(getClass());

	private final SampleService sampleService;
	private final SamplePageService samplePageService;
	private final JsonLDService jsonLDService;
	private final FacetService facetService;
	private final FilterService filterService;
	private final BioSamplesAapService bioSamplesAapService;

	public SampleHtmlController(SampleService sampleService, 
			SamplePageService samplePageService,
			JsonLDService jsonLDService,
			FacetService facetService, 
			FilterService filterService,
			BioSamplesAapService bioSamplesAapService) {
		this.sampleService = sampleService;
		this.samplePageService = samplePageService;
		this.jsonLDService = jsonLDService;
		this.facetService = facetService;
		this.filterService = filterService;
		this.bioSamplesAapService = bioSamplesAapService;
	}

	@GetMapping(value = "/")
	public String index() {
		return "index";
	}

	@GetMapping(value = "/about")
	public String about() {
		return "about";
	}

	@GetMapping(value = "/help")
	public String helpIndex() {
		return "help/index";
	}

	@GetMapping(value = "/help/{page}")
	public String helpIndex(@PathVariable String page) {
		return "help/"+page;
	}

	@GetMapping(value = "/test")
	public String test() {
		return "test";
	}

	@GetMapping(value = "/samples")
	public String samples(Model model, @RequestParam(name="text", required=false) String text,
			@RequestParam(name = "updatedafter", required = false) String updatedAfter,
			@RequestParam(name = "updatedbefore", required = false) String updatedBefore,
			@RequestParam(name="filter", required=false) String[] filters,
			@RequestParam(name="start", defaultValue="0") Integer start,
			@RequestParam(name="rows", defaultValue="10") Integer rows,
			HttpServletRequest request, HttpServletResponse response) {

		//force a minimum of 1 result
		if (rows < 1) {
			rows = 1;
		}
		//cap it for our protection
		if (rows > 1000) {
			rows = 1000;
		}
		
		MultiValueMap<String, String> filtersMap = filterService.getFilters(filters);
		
		Collection<String> domains = bioSamplesAapService.getDomains();

		Instant updatedAfterDate = null;
		if (updatedAfter != null) {
			try {
				updatedAfterDate = Instant.parse(updatedAfter);
			} catch (DateTimeParseException e) {
				//TODO make an exception
				throw new RuntimeException("Unable to parse date "+updatedAfter); 
			}
		}
		Instant updatedBeforeDate = null;
		if (updatedBefore != null) {
			try {
				updatedBeforeDate = Instant.parse(updatedBefore);
			} catch (DateTimeParseException e) {
				//TODO make an exception
				throw new RuntimeException("Unable to parse date "+updatedBefore);
			}
		}
						
		Pageable pageable = new PageRequest(start/rows, rows);
		Page<Sample> pageSample = samplePageService.getSamplesByText(text, filtersMap, domains, updatedAfterDate, updatedBeforeDate, pageable);
		//default to getting 10 values from 10 facets
		List<SampleFacet> sampleFacets = facetService.getFacets(text, filtersMap, 10, 10);
		
		//build URLs for the facets depending on if they are enabled or not
		UriComponentsBuilder uriBuilder = ServletUriComponentsBuilder.fromRequest(request);		
		Map<String, String> facetsUri = new HashMap<>();		
		List<String> filtersList = new ArrayList<>();
		if (filters != null) {
			filtersList.addAll(Arrays.asList(filters));
		}
		Collections.sort(filtersList);
		
		//now actually build the URLs for each facet
		URI uri;
		for (SampleFacet sampleFacet : sampleFacets) {
			//check if the filter all is on
			if (filtersList.contains(sampleFacet.getLabel())) {
				uri = getFilterUri(uriBuilder, filtersList, null, sampleFacet.getLabel());
				facetsUri.put(sampleFacet.getLabel(), uri.toString());				
			} else {
				//filter is off, add uri to turn it on
				uri = getFilterUri(uriBuilder, filtersList, sampleFacet.getLabel(), null);
				facetsUri.put(sampleFacet.getLabel(), uri.toString());
			}	
			//check for each facet
			for (SampleFacetValue sampleFacetValue : sampleFacet.getValues()) {
				//check if the filter for this facet is on
				String filter = sampleFacet.getLabel()+":"+sampleFacetValue.label;
				if (filtersList.contains(filter)) {
					//filter is on, add uri to turn it off
					uri = getFilterUri(uriBuilder, filtersList, null, filter);
					facetsUri.put(filter, uri.toString());
				} else {
					//filter is off, add uri to turn it on
					uri = getFilterUri(uriBuilder, filtersList, filter, null);
					facetsUri.put(filter, uri.toString());	
				}
			}
		}
								
		model.addAttribute("text", text);		
		model.addAttribute("start", start);
		model.addAttribute("rows", rows);
		model.addAttribute("page", pageSample);
		model.addAttribute("facets", sampleFacets);
		model.addAttribute("facetsuri", facetsUri);
		model.addAttribute("filters", filtersList);
		model.addAttribute("paginations", getPaginations(pageSample, uriBuilder));
				
		//TODO add "clear all facets" button
		//TODO title of webpage
		
		return "samples";
	}
		
	private Paginations getPaginations(Page<Sample> pageSample, UriComponentsBuilder uriBuilder) {
		
		int pageTotal = pageSample.getTotalPages();
		int pageCurrent = pageSample.getNumber()+1;
		
		Pagination previous = null;
		if (pageCurrent > 1) {
			previous = new Pagination(pageCurrent-1, false, pageCurrent, uriBuilder, pageSample);
		}
		
		Pagination next = null;
		if (pageCurrent < pageTotal) {
			next = new Pagination(pageCurrent+1, false, pageCurrent, uriBuilder, pageSample);
		}

		Paginations paginations = new Paginations(pageCurrent, pageTotal, previous, next);
		
		if (pageTotal <=6) {
			//few enough pages to fit onto a single bar
			for (int i=1; i <= pageTotal; i++ ) {
				paginations.add(new Pagination(i, false, pageCurrent, uriBuilder, pageSample));
			}
		} else {
			//need at least one ellipsis		
			//if we are in the first 4 or the last 4			
			if (pageCurrent <= 4 ) {
				paginations.add(new Pagination(1, false, pageCurrent, uriBuilder, pageSample));
				paginations.add(new Pagination(2, false, pageCurrent, uriBuilder, pageSample));
				paginations.add(new Pagination(3, false, pageCurrent, uriBuilder, pageSample));
				paginations.add(new Pagination(4, false, pageCurrent, uriBuilder, pageSample));
				paginations.add(new Pagination(5, false, pageCurrent, uriBuilder, pageSample));
				paginations.add(new Pagination(pageTotal, true, pageCurrent, uriBuilder, pageSample));
			} else if (pageTotal - pageCurrent <= 3) {
				paginations.add(new Pagination(1, false, pageCurrent, uriBuilder, pageSample));
				paginations.add(new Pagination(pageTotal-4, true, pageCurrent, uriBuilder, pageSample));
				paginations.add(new Pagination(pageTotal-3, false, pageCurrent, uriBuilder, pageSample));
				paginations.add(new Pagination(pageTotal-2, false, pageCurrent, uriBuilder, pageSample));
				paginations.add(new Pagination(pageTotal-1, false, pageCurrent, uriBuilder, pageSample));
				paginations.add(new Pagination(pageTotal, false, pageCurrent, uriBuilder, pageSample));			
			} else {
				//will need two sets of ellipsis
				paginations.add(new Pagination(1, false, pageCurrent, uriBuilder, pageSample));
				paginations.add(new Pagination(pageCurrent-1, true, pageCurrent, uriBuilder, pageSample));	
				paginations.add(new Pagination(pageCurrent, false, pageCurrent, uriBuilder, pageSample));	
				paginations.add(new Pagination(pageCurrent+1, false, pageCurrent, uriBuilder, pageSample));	
				paginations.add(new Pagination(pageTotal, true, pageCurrent, uriBuilder, pageSample));
			}
		}
						
		return paginations;
	}
	
	private static class Paginations implements Iterable<Pagination> {

		private final List<Pagination> paginations = new ArrayList<>();
		public final Pagination previous;
		public final Pagination next;
		public final int current;
		public final int total;
		
		public Paginations(int current, int total, Pagination previous, Pagination next) {
			this.current = current;
			this.total = total;
			this.previous = previous;
			this.next = next;
		}
		
		public void add(Pagination pagination) {
			paginations.add(pagination);
		}
		
		@Override
		public Iterator<Pagination> iterator() {
			return paginations.iterator();
		}
		
	}
	
	private static class Pagination {
		public final int page;
		public final String url;
		public final boolean skip;
		public final boolean current;
		
		public Pagination(int pageNo, boolean skip, int currentNo, UriComponentsBuilder uriBuilder, Page<Sample> pageSample) {
			this.page = pageNo;
			this.skip = skip;
			this.current = (currentNo == pageNo);
			this.url = uriBuilder.cloneBuilder()
					.replaceQueryParam("start", (pageNo-1)*pageSample.getSize())
					.build().toUriString();
		}
	}
	
	private URI getFilterUri(UriComponentsBuilder uriBuilder, List<String> filters, String filterAdd, String filterRemove) {
		List<String> tempFiltersList = new ArrayList<>(filters);
		if (filterAdd != null) {
			tempFiltersList.add(filterAdd);
			// if turning on a facet-all filter, remove facet-value filters for that facet
			// if turning on a facet-value filter, remove facet-all filters for that facet
			if (filterAdd.contains(":")) {
				//remove facet-all filters when adding a specific facet
				tempFiltersList.remove(filterAdd.split(":")[0]);
			} else {
				//remove facet-specific filters when adding a filter-all facet
				Iterator<String> it = tempFiltersList.iterator();
				while (it.hasNext()) {
					if (it.next().startsWith(filterAdd+":")) {
						it.remove();
					}
				}
			}
		}
		if (filterRemove != null) {
			tempFiltersList.remove(filterRemove);
		}
		Collections.sort(tempFiltersList);
		String[] tempFiltersArray = new String[tempFiltersList.size()];
		tempFiltersArray = tempFiltersList.toArray(tempFiltersArray);
		URI uri = uriBuilder.cloneBuilder()
				.replaceQueryParam("filter", (Object[])tempFiltersArray)
				.replaceQueryParam("start") //reset back to page 1
				.build(false).toUri();
		return uri;
	}
	
	

	@GetMapping(value = "/samples/{accession}")
	public String samplesAccession(Model model, @PathVariable String accession, HttpServletRequest request,
			HttpServletResponse response) {
		Optional<Sample> sample = sampleService.fetch(accession);
		if (!sample.isPresent()) {
			// did not exist, throw 404
			//TODO do as an exception
			log.info("Returning a 404 for " + request.getRequestURL());
			response.setStatus(HttpStatus.NOT_FOUND.value());
			return "error/404";
		}

		if (sample == null || !sample.isPresent()) {
			// throw internal server error
			throw new RuntimeException("Unable to retrieve " + accession);
		}

		bioSamplesAapService.checkAccessible(sample.get());

		//response.setHeader(HttpHeaders.LAST_MODIFIED, String.valueOf(sample.getUpdate().toEpochSecond(ZoneOffset.UTC)));
		//response.setHeader(HttpHeaders.ETAG, String.valueOf(sample.hashCode()));

		String jsonLDString = jsonLDService.jsonLDToString(jsonLDService.sampleToJsonLD(sample.get()));
		model.addAttribute("sample", sample.get());
		model.addAttribute("jsonLD", jsonLDString);
		//becuase thymleaf can only work with timezoned temporals, not instant
		//we need to do the conversion
		model.addAttribute("update", sample.get().getUpdate().atOffset(ZoneOffset.UTC));
		model.addAttribute("release", sample.get().getRelease().atOffset(ZoneOffset.UTC));

		return "sample";
	}
}
