package uk.ac.ebi.biosamples.models;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.autoconfigure.json.JsonTest;
import org.springframework.boot.test.json.JacksonTester;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.junit4.*;

import com.fasterxml.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.*;

import java.net.URISyntaxException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@RunWith(SpringRunner.class)
@JsonTest
public class MongoSerializationTest {

	private Logger log = LoggerFactory.getLogger(getClass());

	private JacksonTester<MongoSample> json;
	
    @Before
    public void setup() {
        ObjectMapper objectMapper = new ObjectMapper();
        JacksonTester.initFields(this, objectMapper);
    }

	private MongoSample getMongoSample() throws URISyntaxException {
		String name = "Test Sample";
		String accession = "TEST1";
		LocalDateTime update = LocalDateTime.of(LocalDate.of(2016, 5, 5), LocalTime.of(11, 36, 57));
		LocalDateTime release = LocalDateTime.of(LocalDate.of(2016, 4, 1), LocalTime.of(11, 36, 57));

		Map<String, Set<String>> keyValues = new HashMap<>();
		Map<String, Map<String, String>> ontologyTerms = new HashMap<>();
		Map<String, Map<String, String>> units = new HashMap<>();

		Map<String, Set<String>> relationships  = new HashMap<>();

		keyValues.put("organism", new HashSet<>());
		keyValues.get("organism").add("Homo sapiens");
		ontologyTerms.put("organism", new HashMap<>());
		ontologyTerms.get("organism").put("Homo sapiens", "http://purl.obolibrary.org/obo/NCBITaxon_9606");

		keyValues.put("age", new HashSet<>());
		keyValues.get("age").add("3");
		units.put("age", new HashMap<>());
		units.get("age").put("3", "year");

		keyValues.put("organism part", new HashSet<>());
		keyValues.get("organism part").add("lung");
		keyValues.get("organism part").add("heart");
		
		relationships.put("derived from", new HashSet<>());
		relationships.get("derived from").add("TEST2");

		return MongoSample.createFrom(null, name, accession, update, release, keyValues, ontologyTerms, units, relationships);
	}

	@Test
	public void testSerialize() throws Exception {
		MongoSample details = getMongoSample();

		System.out.println(this.json.write(details).getJson());

		// Use JSON path based assertions
		assertThat(this.json.write(details)).hasJsonPathStringValue("@.accession");
		assertThat(this.json.write(details)).extractingJsonPathStringValue("@.accession").isEqualTo("TEST1");

		// Assert against a `.json` file in the same package as the test
		log.info("testSerialize() "+this.json.write(details).getJson());
		assertThat(this.json.write(details)).isEqualToJson("/TEST1.json");
	}

	@Test
	public void testDeserialize() throws Exception {
		// Use JSON path based assertions
		assertThat(this.json.readObject("/TEST1.json").getName()).isEqualTo("Test Sample");
		assertThat(this.json.readObject("/TEST1.json").getAccession()).isEqualTo("TEST1");
		// Assert against a `.json` file
		assertThat(this.json.readObject("/TEST1.json")).isEqualTo(getMongoSample());
	}
	
	@Configuration
	public static class TestConfig {
		
	}

}