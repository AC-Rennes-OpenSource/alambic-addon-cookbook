package fr.gouv.education.acrennes.alambic.addon.cookbook;

import static net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson;
import static org.junit.Assert.assertEquals;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.Future;

import org.apache.commons.io.IOUtils;
import org.jdom2.Element;
import org.jdom2.input.SAXBuilder;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import fr.gouv.education.acrennes.alambic.jobs.ExecutorFactory;
import fr.gouv.education.acrennes.alambic.jobs.JobHelper;
import fr.gouv.education.acrennes.alambic.jobs.Jobs;
import fr.gouv.education.acrennes.alambic.monitoring.ActivityMBean;
import fr.gouv.education.acrennes.alambic.monitoring.ActivityTrafficLight;
import fr.gouv.education.acrennes.alambic.utils.Variables;
import net.javacrumbs.jsonunit.core.Option;

@RunWith(PowerMockRunner.class)
@PrepareForTest({JobHelper.class})
@PowerMockIgnore({ "javax.management.*", "javax.net.ssl.*" })
public class SaladeApiEtXpathTest {
	
	private static final String RUN_ID = "1";

	private Variables variables;

	@BeforeClass
	public static void setUpOnce() throws Exception {
		System.setProperty("log4j.configurationFile", "log4j2.xml");
		
        // Initialize the executor factory (multi-threading engine)
        ExecutorFactory.initialize(new Properties());
	}
	
	@Before
	public void setUp() throws Exception {
		// Initialize variables
	    PowerMockito.mockStatic(JobHelper.class, invocation -> {
	        if (invocation.getMethod().getName().equals("getVariables")) {
	            return Collections.emptyList();
	        }
	        return invocation.callRealMethod();
	    });		
		variables = new Variables();
		try (final InputStream is = SaladeApiEtXpathTest.class.getClassLoader().getResourceAsStream("variables.xml")) {
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
	}

	/* test use case :
	 * - Recette : salade-api-et-xpath.xml
	 * - Job : ConvertCSV2XML
	 * - Payload de l'appel REST : {
	 * 		"listeParametres":{
	 * 			"params.nomAddOn":"toutatice-etl-addon-cookbook",
	 * 			"params.job":"jobs/salade-api-et-xpath.xml",
	 * 			"params.task":"QueryXML",
	 * 			"xpath_query":"/villes/ville[departement='Morbihan']"
	 * 		}
	 * }
	 **/
	@Test
	public void test1() {
		try {
			// Paramètre du job
			variables.put("xpath_query", "/villes/ville[departement='Morbihan']");
			
			Jobs jobs = new Jobs(".", "jobs/salade-api-et-xpath.xml", variables, new Properties());
			List<Future<ActivityMBean>> futures = jobs.executeJobList(Arrays.asList("QueryXML"), RUN_ID);
			
			// Récupération du flux JSON de réponse de l'API
			String API_response = futures.get(0).get().getResult().toString();

			// Assertions
			assertEquals(1, futures.size());
			assertEquals(true, futures.get(0).isDone());
			assertEquals(ActivityTrafficLight.GREEN, futures.get(0).get().getTrafficLight());

			assertThatJson(API_response)
				.when(Option.IGNORING_ARRAY_ORDER)
				.isEqualTo(getFileContent("src/test/resources/salade-api-et-xpath/SaladeApiEtXpath-test1-expected.json"));
		} catch (Exception e) {
			e.printStackTrace();
			Assert.fail();
		}
	}

	/* test use case :
	 * - Recette : salade-api-et-xpath.xml
	 * - Job : ConvertCSV2XML
	 * - Payload de l'appel REST : {
	 * 		"listeParametres":{
	 * 			"params.nomAddOn":"toutatice-etl-addon-cookbook",
	 * 			"params.job":"jobs/salade-api-et-xpath.xml",
	 * 			"params.task":"QueryXML",
	 * 			"xpath_query":"/villes/ville[population > 50000]"
	 * 		}
	 * }
	 **/
	@Test
	public void test2() {
		try {
			// Paramètre du job
			variables.put("xpath_query", "/villes/ville[population > 50000]");
			
			Jobs jobs = new Jobs(".", "jobs/salade-api-et-xpath.xml", variables, new Properties());
			List<Future<ActivityMBean>> futures = jobs.executeJobList(Arrays.asList("QueryXML"), RUN_ID);

			// Récupération du flux JSON de réponse de l'API
			String API_response = futures.get(0).get().getResult().toString();

			// Assertions
			assertEquals(1, futures.size());
			assertEquals(true, futures.get(0).isDone());
			assertEquals(ActivityTrafficLight.GREEN, futures.get(0).get().getTrafficLight());
			
			assertThatJson(API_response)
				.when(Option.IGNORING_ARRAY_ORDER)
				.isEqualTo(getFileContent("src/test/resources/salade-api-et-xpath/SaladeApiEtXpath-test2-expected.json"));
		} catch (Exception e) {
			e.printStackTrace();
			Assert.fail();
		}
	}

	private String getFileContent(final String path) {
		String content = new String();
		
		try (InputStream is = new FileInputStream(path)) {
			content = IOUtils.toString(is, StandardCharsets.UTF_8.name());
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return content;
	}

}