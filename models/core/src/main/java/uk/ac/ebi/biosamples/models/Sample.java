package uk.ac.ebi.biosamples.models;

import java.time.LocalDateTime;
import java.util.Set;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

@JsonSerialize(using = SampleSerializer.class)
@JsonDeserialize(using = SampleDeserializer.class)
public interface Sample {

	public String getAccession();
	public String getName();

	public LocalDateTime getRelease();
	public LocalDateTime getUpdate();

	public Set<String> getAttributeKeys();
	public Set<String> getAttributeValues(String key);
	public String getAttributeUnit(String key, String value);
	public String getAttributeOntologyTerm(String key, String value);


	public Set<String> getRelationshipTypes();
	public Set<String> getRelationshipTargets(String type);
}
