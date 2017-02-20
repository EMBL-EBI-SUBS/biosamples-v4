package uk.ac.ebi.biosamples.accession;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.stereotype.Component;

import uk.ac.ebi.biosamples.PipelinesProperties;
import uk.ac.ebi.biosamples.model.Attribute;
import uk.ac.ebi.biosamples.model.Relationship;
import uk.ac.ebi.biosamples.model.Sample;
import uk.ac.ebi.biosamples.utils.AdaptiveThreadPoolExecutor;
import uk.ac.ebi.biosamples.utils.SubmissionService;
import uk.ac.ebi.biosamples.utils.ThreadUtils;

@Component
public class Accession implements ApplicationRunner{

	private Logger log = LoggerFactory.getLogger(getClass());

	@Autowired
	private PipelinesProperties pipelinesProperties;


	@Autowired
	private AccessionDao accessionDao;
	@Autowired
	private SubmissionService submissionService;

	@Override
	public void run(ApplicationArguments args) throws Exception {
		log.info("Processing Accession pipeline...");
		

		try (AdaptiveThreadPoolExecutor executorService = AdaptiveThreadPoolExecutor.create(100, 10000, true, pipelinesProperties.getThreadCount())) {
		

			Map<String, Future<Void>> futures = new HashMap<>();
			RowCallbackHandler rch = new AccessionCallbackHandler(executorService, futures);		
			accessionDao.doAccessionCallback(rch);
		}
	}	
	
	private class AccessionCallbackHandler implements RowCallbackHandler {
		
		private final ThreadPoolExecutor executor;
		private final Map<String, Future<Void>> futures;
		
		public AccessionCallbackHandler(ThreadPoolExecutor executor, Map<String, Future<Void>> futures) {
			this.executor = executor;
			this.futures = futures;
		}

		@Override
		public void processRow(ResultSet rs) throws SQLException {
			int accessionNo = rs.getInt("ACCESSION");
			String userAccession = rs.getString("USER_ACCESSION");
			String submissionAccession = rs.getString("SUBMISSION_ACCESSION");
			Date dateAssigned = rs.getDate("DATE_ASSIGNED");
			boolean deleted = rs.getBoolean("IS_DELETED");

			String accession = "SAMEA"+accessionNo;
			log.trace(""+accessionNo+" "+userAccession+" "+submissionAccession+" "+dateAssigned+" "+deleted);
			
			Callable<Void> callable = new AccessionCallable(accession, userAccession, submissionAccession, dateAssigned, deleted);			
			Future<Void> future = executor.submit(callable);
			futures.put(accession, future);

			try {
				ThreadUtils.checkFutures(futures, 100);
			} catch (InterruptedException e) {
				log.warn("Interupted while checking for futures");
			}
		}
	}
	
	private class AccessionCallable implements Callable<Void> {
		private final String accession;
		private final String userAccession;
		private final String submissionAccession;
		private final Date dateAssigned;
		private final boolean deleted;
		
		public AccessionCallable(String accession, String userAccession, String submissionAccession, Date dateAssigned, boolean deleted) {
			this.accession = accession;
			this.userAccession = userAccession;
			this.submissionAccession = submissionAccession;
			this.dateAssigned = dateAssigned;
			this.deleted = deleted;			
		}
		
		@Override
		public Void call() throws Exception {
			String name = userAccession;
			LocalDateTime release = LocalDateTime.now().plusYears(100);
			LocalDateTime update = LocalDateTime.ofInstant(Instant.ofEpochMilli(dateAssigned.getTime()), ZoneId.systemDefault());

			SortedSet<Attribute> attributes = new TreeSet<>();
			attributes.add(Attribute.build("other", "migrated from accession database at "+LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME)));
			attributes.add(Attribute.build("user accession", userAccession));
			attributes.add(Attribute.build("submission accession", submissionAccession));
			attributes.add(Attribute.build("deleted", Boolean.toString(deleted)));
			SortedSet<Relationship> relationships = new TreeSet<>();
			
			Sample sample = Sample.build(name, accession, release, update, attributes, relationships);
			submissionService.submit(sample);
			return null;
		}
		
	}

}
