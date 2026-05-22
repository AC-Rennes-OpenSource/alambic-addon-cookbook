package fr.gouv.education.acrennes.alambic.addon.cookbook;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
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
import org.xmlunit.builder.DiffBuilder;
import org.xmlunit.diff.Diff;

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
public class QuicheAuxLegumesAnonymiseeTest {

	private static final String RUN_ID = "1";
	private static final String UNIT_TEST_PERSISTENCE_UNIT = "TEST_PERSISTENCE_UNIT";
	private static final String TEST_USERS_DICTIONARY_ARCHIVE = "src/test/resources/quiche-aux-legumes-anonymisee/user-dictionaries-testu.tar.gz";

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
		try (final InputStream is = QuicheAuxLegumesAnonymiseeTest.class.getClassLoader().getResourceAsStream("variables.xml")) {
			Element variablesElt = (new SAXBuilder()).build(is).getRootElement();
			variables.loadFromXmlNode(variablesElt.getChild("variables").getChildren("variable"));
		}

		// Clean the output directory content (in order to not disturb the next unit test)
		Files.list(Paths.get("output/")).forEach(i -> {
			try {
				if (!i.endsWith(".donotremove")) {
					Files.delete(i);
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		});

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
	 * - Recette : quiche-aux-legumes-anonymisee.xml
	 * - Job : BuildAnonymizationRecipe
	 * - Anonymiser une recette xml
	 **/
	@Test
	public void test1() {
		try {
			Jobs jobs = new Jobs(".", "jobs/quiche-aux-legumes-anonymisee.xml", variables, new Properties());
			List<Future<ActivityMBean>> futures;
            futures = jobs.executeJobList(Arrays.asList("BuildAnonymizationRecipe"), RUN_ID);

            Path fichierAnonymise = Paths.get("output/quicheAuxLegumesAnonymisee.xml");
			final File source = new File("src/test/resources/quiche-aux-legumes-anonymisee/quicheAuxLegumesAAnonymiser1.xml");
			final File output = new File(fichierAnonymise.toString());
			Reader expectedFileReader = new BufferedReader(new FileReader(source));
			Reader outputFileReader = new BufferedReader(new FileReader(output));

			// Assertions
			assertTrue("Le fichier n'existe pas.", Files.exists(fichierAnonymise));
			assertTrue("Le fichier est vide.", Files.size(fichierAnonymise)>0);
            assertFalse("Le fichier créé est identique au fichier source.", IOUtils.contentEqualsIgnoreEOL(expectedFileReader, outputFileReader));
			assertEquals(1, futures.size());
            assertTrue(futures.get(0).isDone());
			assertEquals(ActivityTrafficLight.GREEN, futures.get(0).get().getTrafficLight());
		} catch (Exception e) {
			e.printStackTrace();
			Assert.fail();
		}
	}

	/* test use case :
	 * - Recette : quiche-aux-legumes-anonymisee.xml
	 * - Job : BuildAnonymizationRecipe
	 * - Anonymiser deux recettes xml, avec les mêmes ingrédients (un même contexte et une même clé)
	 **/
	@Test
	public void test2() {
		try {
			Jobs jobs = new Jobs(".", "jobs/quiche-aux-legumes-anonymisee.xml", variables, new Properties());
			List<Future<ActivityMBean>> futures = jobs.executeJobList(Arrays.asList("BuildAnonymizationRecipeWithIdenticalContextsAndKeys"), RUN_ID);

			Path fichierAnonymise1 = Paths.get("output/quicheAuxLegumesAnonymisee1.xml");
			Path fichierAnonymise2 = Paths.get("output/quicheAuxLegumesAnonymisee2.xml");
			final File source = new File("src/test/resources/quiche-aux-legumes-anonymisee/quicheAuxLegumesAAnonymiser1.xml");
			final File output1 = new File(fichierAnonymise1.toString());
			final File output2 = new File(fichierAnonymise2.toString());
			Reader expectedFileReader1 = new BufferedReader(new FileReader(source));
			Reader outputFileReader1 = new BufferedReader(new FileReader(output1));
			Reader outputFileReader2 = new BufferedReader(new FileReader(output2));

			// Assertions
			assertTrue("Le fichier n'existe pas.", Files.exists(fichierAnonymise1));
			assertTrue("Le fichier n'existe pas.", Files.exists(fichierAnonymise2));
			assertTrue("Le fichier est vide.", Files.size(fichierAnonymise1)>0);
			assertTrue("Le fichier est vide.", Files.size(fichierAnonymise2)>0);
			assertFalse("Le fichier créé est identique au fichier source.", IOUtils.contentEqualsIgnoreEOL(expectedFileReader1, outputFileReader1));
			assertFalse("Le fichier créé est identique au fichier source.", IOUtils.contentEqualsIgnoreEOL(expectedFileReader1, outputFileReader2));
			Diff diff = DiffBuilder.compare(new String(Files.readAllBytes(fichierAnonymise1)))
					.withTest(new String(Files.readAllBytes(fichierAnonymise2)))
					.ignoreWhitespace()
					.checkForSimilar()
					.build();
			assertFalse("Les fichiers XML sont différents : " + diff.toString(), diff.hasDifferences());
			assertEquals(1, futures.size());
			assertTrue(futures.get(0).isDone());
			assertEquals(ActivityTrafficLight.GREEN, futures.get(0).get().getTrafficLight());

		} catch (Exception e) {
			e.printStackTrace();
			Assert.fail();
		}
	}

	/* test use case :
	 * - Recette : quiche-aux-legumes-anonymisee.xml
	 * - Job : BuildAnonymizationRecipe
	 * - Anonymiser deux recettes xml, avec des ingrédients différents (un même contexte et deux clés distinctes)
	 **/
	@Test
	public void test3() {
		try {
			Jobs jobs = new Jobs(".", "jobs/quiche-aux-legumes-anonymisee.xml", variables, new Properties());
			List<Future<ActivityMBean>> futures = jobs.executeJobList(Collections.singletonList("BuildAnonymizationRecipeWithIdenticalContextsAndDistinctKeys"), RUN_ID);

			Path fileAnonymized1 = Paths.get("output/quicheAuxLegumesAnonymisee3.xml");
			Path fileAnonymized2 = Paths.get("output/quicheAuxLegumesAnonymisee4.xml");
			final File source = new File("src/test/resources/quiche-aux-legumes-anonymisee/quicheAuxLegumesAAnonymiser2.xml");
			Reader sourceFileReader = new BufferedReader(new FileReader(source));
			Reader anonymizedFileReader1 = new BufferedReader(new FileReader(fileAnonymized1.toString()));
			Reader anonymizedFileReader2 = new BufferedReader(new FileReader(fileAnonymized2.toString()));

			// Assertions
			assertTrue("Le fichier n'existe pas.", Files.exists(fileAnonymized1));
			assertTrue("Le fichier n'existe pas.", Files.exists(fileAnonymized2));
			assertTrue("Le fichier est vide.", Files.size(fileAnonymized1)>0);
			assertTrue("Le fichier est vide.", Files.size(fileAnonymized2)>0);
			assertNotSame("Le fichier quicheAuxLegumesAnonymisee3 créé est identique au fichier source.", sourceFileReader, anonymizedFileReader1);
			assertNotSame("Le fichier quicheAuxLegumesAnonymisee4 créé est identique au fichier source.", sourceFileReader, anonymizedFileReader2);
			assertNotSame("Les fichiers créés sont identiques.", anonymizedFileReader1, anonymizedFileReader2);
			Diff diff = DiffBuilder.compare(new String(Files.readAllBytes(fileAnonymized1)))
					.withTest(new String(Files.readAllBytes(fileAnonymized2)))
					.ignoreWhitespace()
					.checkForSimilar()
					.build();
			assertTrue("Les fichiers XML sont identiques : " + diff.toString(), diff.hasDifferences());
			assertEquals(1, futures.size());
			assertTrue(futures.get(0).isDone());
			assertEquals(ActivityTrafficLight.GREEN, futures.get(0).get().getTrafficLight());

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