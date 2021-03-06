package uk.ac.ebi.biosamples.neo.model;

import java.util.Collection;
import java.util.Set;
import java.util.TreeSet;

import org.neo4j.ogm.annotation.GraphId;
import org.neo4j.ogm.annotation.Index;
import org.neo4j.ogm.annotation.NodeEntity;
import org.neo4j.ogm.annotation.Property;
import org.neo4j.ogm.annotation.Relationship;

import com.google.common.hash.Hasher;
import com.google.common.hash.Hashing;

@NodeEntity(label = "Curation")
public class NeoCuration {

	@GraphId
	private Long id;

    @Relationship(type = "HAS_PRE_ATTRIBUTE")
	private Set<NeoAttribute> attributesPre;
    
    @Relationship(type = "HAS_POST_ATTRIBUTE")    
	private Set<NeoAttribute> attributesPost;

    @Relationship(type = "HAS_PRE_EXTERNAL_REFERENCE")
	private Set<NeoExternalReference> externalPre;
    
    @Relationship(type = "HAS_POST_EXTERNAL_REFERENCE")    
	private Set<NeoExternalReference> externalPost;
    
    @Relationship(type = "HAS_CURATION_TARGET")
    private Set<NeoCurationLink> links;
    
	@Property
	@Index(unique=true, primary=true)
	private String hash;

	private NeoCuration() {
	}
	
	public Long getId() {
		return id;
	}
	
	public Set<NeoCurationLink> getLinks() {
		return links;
	}
	
	public String getHash() {
		return hash;
	}
	
	public Set<NeoAttribute> getAttributesPre() {
		return attributesPre;
	}
	
	public Set<NeoAttribute> getAttributesPost() {
		return attributesPost;
	}
	
	public Set<NeoExternalReference> getExternalsPre() {
		return externalPre;
	}
	
	public Set<NeoExternalReference> getExternalsPost() {
		return externalPost;
	}
	
	
	public static NeoCuration build(Collection<NeoAttribute> attributesPre, Collection<NeoAttribute> attributesPost,
			Collection<NeoExternalReference> externalPre, Collection<NeoExternalReference> externalPost) {
		NeoCuration neoCuration = new NeoCuration();
		neoCuration.attributesPre = new TreeSet<>(attributesPre);
		neoCuration.attributesPost = new TreeSet<>(attributesPost);
		neoCuration.externalPre = new TreeSet<>(externalPre);
		neoCuration.externalPost = new TreeSet<>(externalPost);

    	Hasher hasher = Hashing.sha256().newHasher();
    	for (NeoAttribute a : neoCuration.attributesPre) {
    		hasher.putUnencodedChars(a.getType());
    		hasher.putUnencodedChars(a.getValue());
    		if (a.getUnit() != null) {
    			hasher.putUnencodedChars(a.getUnit());
    		}
    		if (a.getIri() != null) {
    			hasher.putUnencodedChars(a.getIri());
    		}
    	}
    	for (NeoAttribute a : neoCuration.attributesPost) {
    		hasher.putUnencodedChars(a.getType());
    		hasher.putUnencodedChars(a.getValue());
    		if (a.getUnit() != null) {
    			hasher.putUnencodedChars(a.getUnit());
    		}
    		if (a.getIri() != null) {
    			hasher.putUnencodedChars(a.getIri());
    		}
    	}
    	for (NeoExternalReference a : externalPre) {
    		hasher.putUnencodedChars(a.getUrl());
    	}
    	for (NeoExternalReference a : externalPost) {
    		hasher.putUnencodedChars(a.getUrl());
    	}
		neoCuration.hash = hasher.hash().toString();

    	//TODO bake user id into hash
		
		return neoCuration;
	}
}
