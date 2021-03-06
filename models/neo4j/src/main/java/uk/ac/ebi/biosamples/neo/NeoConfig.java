package uk.ac.ebi.biosamples.neo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.data.neo4j.Neo4jProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.neo4j.repository.config.EnableNeo4jRepositories;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@Configuration
@EnableNeo4jRepositories(basePackageClasses = NeoConfig.class)
@EnableTransactionManagement
public class NeoConfig {

	private Logger log = LoggerFactory.getLogger(getClass());

	public NeoConfig() {
	};
	
	
	//see AutoIndexMode for values
	@Value("${spring.data.neo4j.indexes.auto:assert}")
	private String neo4jIndexes;
	
	@Bean
	public org.neo4j.ogm.config.Configuration configuration(Neo4jProperties properties) {
		
		org.neo4j.ogm.config.Configuration config = properties.createConfiguration();
		
		//assert all the indexs we need
		config.autoIndexConfiguration().setAutoIndex(neo4jIndexes);
		
		return config;
	}
}
