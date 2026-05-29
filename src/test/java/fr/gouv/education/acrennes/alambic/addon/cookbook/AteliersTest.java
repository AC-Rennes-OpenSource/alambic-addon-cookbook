package fr.gouv.education.acrennes.alambic.addon.cookbook;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.Future;

import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;

import org.apache.commons.io.IOUtils;
import org.apache.commons.vfs.FileObject;
import org.apache.commons.vfs.FileSystemException;
import org.apache.commons.vfs.FileSystemManager;
import org.apache.commons.vfs.VFS;
import org.apache.commons.vfs.impl.StandardFileSystemManager;
import org.jdom2.Element;
import org.jdom2.input.SAXBuilder;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;

import fr.gouv.education.acrennes.alambic.exception.AlambicException;
import fr.gouv.education.acrennes.alambic.generator.service.RandomGenerator;
import fr.gouv.education.acrennes.alambic.generator.service.RandomGeneratorService;
import fr.gouv.education.acrennes.alambic.jobs.JobHelper;
import fr.gouv.education.acrennes.alambic.jobs.Jobs;
import fr.gouv.education.acrennes.alambic.monitoring.ActivityMBean;
import fr.gouv.education.acrennes.alambic.monitoring.ActivityTrafficLight;
import fr.gouv.education.acrennes.alambic.persistence.EntityManagerHelper;
import fr.gouv.education.acrennes.alambic.random.persistence.RandomDictionaryEntity;
import fr.gouv.education.acrennes.alambic.random.persistence.RandomDictionaryEntityPK;
import fr.gouv.education.acrennes.alambic.utils.Variables;

@RunWith(PowerMockRunner.class)
@PrepareForTest({JobHelper.class, VFS.class})
@PowerMockIgnore({ "javax.management.*", "javax.net.ssl.*" })
public class AteliersTest {
	private static final String RUN_ID = "1";
	private static final String UNIT_TEST_PERSISTENCE_UNIT = "TEST_PERSISTENCE_UNIT";
	private static final String TEST_USERS_DICTIONARY_ARCHIVE = "src/test/resources/user-dictionaries-testu.tar.gz";

	private RandomGenerator rg = null;
	private FileSystemManager fsManager;
	private Variables variables;

	@BeforeClass
	public static void setProperties() {
		System.setProperty("log4j.configurationFile", "log4j2.xml");
	}

	@Before
	public void setUp() throws Exception {
		// Mock the entity manager helper so that the embedded persistence unit (h2) is used
		EntityManagerHelper.getInstance(UNIT_TEST_PERSISTENCE_UNIT, null);
				
		// Initialize variables
	    PowerMockito.mockStatic(JobHelper.class, invocation -> {
	        if (invocation.getMethod().getName().equals("getVariables")) {
	            return Collections.emptyList();
	        }
	        return invocation.callRealMethod();
	    });		
		variables = new Variables();
		try (final InputStream is = AteliersTest.class.getClassLoader().getResourceAsStream("variables.xml")) {
			Element variablesElt = (new SAXBuilder()).build(is).getRootElement();
			variables.loadFromXmlNode(variablesElt.getChild("variables").getChildren("variable"));
		}

		// Clean the output directory content (in order to not disturb the next unit test)
//		Files.list(Paths.get("output/")).forEach(i -> {
//			try {
//				if (!i.endsWith(".donotremove")) {
//					Files.delete(i);
//				}
//			} catch (IOException e) {
//				e.printStackTrace();
//			}
//		});

		/**
		 * Mock so that the dictionary is used for unit testing purpose
		 */
		fsManager = VFS.getManager();
		PowerMockito.mockStatic(VFS.class);
		PowerMockito.when(VFS.getManager()).thenReturn(new InnerFileSystemManager());

		// Set up the persistence data for the unit tests
		setUpPersistenceData();

	}

	private void setUpPersistenceData() throws AlambicException, JsonProcessingException, IOException {
		// load the random users dictionaries
		EntityManager em = EntityManagerHelper.getEntityManager();

		try {
			EntityTransaction transac = em.getTransaction();
			FileSystemManager fsManager = VFS.getManager();
			FileObject archive = fsManager.resolveFile("tgz:file:/");

			// List the children of the archive file
			FileObject[] children = archive.getChildren();
			ObjectMapper mapper = new ObjectMapper();

			Map<String, Long> idmap = new HashMap<>();
			idmap.put(RandomDictionaryEntityPK.IDENTITY_ELEMENT.FIRSTNAME_MALE.toString(), (long) 1);
			idmap.put(RandomDictionaryEntityPK.IDENTITY_ELEMENT.FIRSTNAME_FEMALE.toString(), (long) 1);
			idmap.put(RandomDictionaryEntityPK.IDENTITY_ELEMENT.LASTNAME.toString(), (long) 1);
			idmap.put(RandomDictionaryEntityPK.IDENTITY_ELEMENT.ADDRESS_TYPE.toString(), (long) 1);
			idmap.put(RandomDictionaryEntityPK.IDENTITY_ELEMENT.ADDRESS_LABEL.toString(), (long) 1);
			idmap.put(RandomDictionaryEntityPK.IDENTITY_ELEMENT.ADDRESS_CITY.toString(), (long) 1);
			for (int i = 0; i < children.length; i++) {
				transac.begin();
				FileObject fo = children[i];
				JsonNode rootNode = mapper.readTree(fo.getContent().getInputStream());
				ArrayNode dictionaryNode = (ArrayNode) rootNode.get("dictionary");
				for (JsonNode node : dictionaryNode) {
					RandomDictionaryEntityPK.IDENTITY_ELEMENT element = RandomDictionaryEntityPK.IDENTITY_ELEMENT.valueOf(node.get("element").textValue().toUpperCase());
					Long index = idmap.get(element.toString());
					RandomDictionaryEntityPK pk = new RandomDictionaryEntityPK(element, index);
					RandomDictionaryEntity rie = new RandomDictionaryEntity(pk, node.get("value").textValue());
					em.persist(rie);
					idmap.put(element.toString(), ++index);
				}

				/**
				 * Add flush and clear methods so that persistence entities objects are made available
				 * for garbage collection.
				 * (see : http://www.eclipse.org/eclipselink/documentation/2.4/jpa/extensions/p_persistence_context_referencemode.htm)
				 */
				em.flush();
				em.clear();
				transac.commit();
			}
		} finally {
			em.close();
		}
	}

	/* test use case :
	 * - Recette : atelier1
	 * - Job : anonymization-job-atelier1
	 **/
	@Test
	public void test1() {
		try {
			Jobs jobs = new Jobs(".", "jobs/atelier1.xml", variables, new Properties());
			List<Future<ActivityMBean>> futures;
            futures = jobs.executeJobList(Arrays.asList("anonymization-job-atelier1"), RUN_ID);

			final File sourceFile = new File("src/test/resources/atelier1/atelier1-file-to-anonymize.xml");
			final File anonymizedFile = new File(Paths.get("output/atelier1-anonymized-file.xml").toString());

			// Assertions
			assertEquals(1, futures.size());
			assertTrue(futures.get(0).isDone());
			assertEquals(ActivityTrafficLight.GREEN, futures.get(0).get().getTrafficLight());

			assertFalse("Le fichier créé est identique au fichier attendu.", 
					IOUtils.contentEqualsIgnoreEOL(
							new BufferedReader(new FileReader(sourceFile)), 
							new BufferedReader(new FileReader(anonymizedFile))));
		} catch (Exception e) {
			e.printStackTrace();
			Assert.fail();
		}
	}

	/* test use case :
	 * - Recette : atelier2
	 * - Job : anonymization-job-atelier2
	 **/
	@Test
	public void test2() {
		try {
			Jobs jobs = new Jobs(".", "jobs/atelier2.xml", variables, new Properties());
			List<Future<ActivityMBean>> futures;
            futures = jobs.executeJobList(Arrays.asList("anonymization-job-atelier2"), RUN_ID);

			final File sourceFile1 = new File("src/test/resources/atelier2/atelier2-customers-to-anonymize.xml");
			final File anonymizedFile1 = new File(Paths.get("output/atelier2-anonymized-customers.xml").toString());

			final File sourceFile2 = new File("src/test/resources/atelier2/atelier2-orders-to-anonymize.xml");
			final File anonymizedFile2 = new File(Paths.get("output/atelier2-anonymized-orders.xml").toString());
			
			// Assertions
			assertEquals(1, futures.size());
			assertTrue(futures.get(0).isDone());
			assertEquals(ActivityTrafficLight.GREEN, futures.get(0).get().getTrafficLight());
			
            assertFalse("Le fichier 'customers' créé est identique au fichier attendu.", 
            		IOUtils.contentEqualsIgnoreEOL(
            				new BufferedReader(new FileReader(sourceFile1)), 
            				new BufferedReader(new FileReader(anonymizedFile1))));

            assertFalse("Le fichier 'orders' créé est identique au fichier attendu.", 
            		IOUtils.contentEqualsIgnoreEOL(
            				new BufferedReader(new FileReader(sourceFile2)), 
            				new BufferedReader(new FileReader(anonymizedFile2))));
		} catch (Exception e) {
			e.printStackTrace();
			Assert.fail();
		}
	}

	private class InnerFileSystemManager extends StandardFileSystemManager {

		@Override
		public FileObject resolveFile(String uri) throws FileSystemException {
			// Get archive file from resources folder
			File currentDirectory = new File(".");
			String jobAbsolutePath = currentDirectory.getAbsolutePath().replaceFirst("\\.$", "");
			File file = new File(jobAbsolutePath.concat(TEST_USERS_DICTIONARY_ARCHIVE));
			FileObject dictionary = fsManager.resolveFile("tgz:file:/" + file.getPath());
			return dictionary;
		}

	}

	@After
	public void tearDown() {
		RandomGeneratorService.close();
		if (null != rg) {
			rg.close();
		}

		/**
		 * Shutdown the derby system so that other unit tests don't run into exception because the database was not released.
		 * This is not visible when running the tests within Eclipse environment (launcher) but it is when packaging
		 * the project with maven.
		 */
		EntityManagerHelper.close();
	}

}
