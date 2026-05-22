package fr.gouv.education.acrennes.alambic.addon.cookbook;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.Future;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpStatus;
import org.apache.http.HttpVersion;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicStatusLine;
import org.jdom2.Element;
import org.jdom2.input.SAXBuilder;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Matchers;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.xmlunit.builder.Input;
import org.xmlunit.matchers.CompareMatcher;

import fr.gouv.education.acrennes.alambic.exception.AlambicException;
import fr.gouv.education.acrennes.alambic.jobs.ExecutorFactory;
import fr.gouv.education.acrennes.alambic.jobs.JobHelper;
import fr.gouv.education.acrennes.alambic.jobs.Jobs;
import fr.gouv.education.acrennes.alambic.jobs.extract.clients.WSToStateBase;
import fr.gouv.education.acrennes.alambic.jobs.load.StateBaseToWS;
import fr.gouv.education.acrennes.alambic.monitoring.ActivityMBean;
import fr.gouv.education.acrennes.alambic.monitoring.ActivityTrafficLight;
import fr.gouv.education.acrennes.alambic.utils.Variables;

@RunWith(PowerMockRunner.class)
@PrepareForTest({WSToStateBase.class, StateBaseToWS.class, JobHelper.class, HttpClientBuilder.class, CloseableHttpClient.class})
@PowerMockIgnore({ "javax.management.*", "javax.net.ssl.*" })
public class CarpaccioDeWebserviceTest {
	
	private static final String RUN_ID = "1";

	private HttpClientBuilder mockedHttpClientBuilder;
	private CloseableHttpClient mockedHttpClient;
	private CloseableHttpResponse mockedHttpResponse;
	private HttpEntity mockedHttpEntity;
	private Variables variables;

	@BeforeClass
	public static void setProperties() {
		System.setProperty("log4j.configurationFile", "log4j2.xml");
	}

	@Before
	public void setUp() throws Exception {
		// Prepare the web service source connector mock
		mockedHttpClientBuilder = PowerMockito.mock(HttpClientBuilder.class);	
	    PowerMockito.mockStatic(HttpClientBuilder.class, invocation -> {
	        if (invocation.getMethod().getName().equals("create")) {
	            return mockedHttpClientBuilder;
	        }
	        return invocation.callRealMethod();
	    });
		mockedHttpClient = PowerMockito.mock(CloseableHttpClient.class);		
		PowerMockito.when(mockedHttpClientBuilder.useSystemProperties())
			.thenReturn(mockedHttpClientBuilder);
		PowerMockito.when(mockedHttpClientBuilder.setDefaultRequestConfig(Matchers.any()))
			.thenReturn(mockedHttpClientBuilder);
		PowerMockito.when(mockedHttpClientBuilder.build())
			.thenReturn(mockedHttpClient);

		// Mock the web service Http response
		mockedHttpEntity = PowerMockito.mock(HttpEntity.class);
		mockedHttpResponse = PowerMockito.mock(CloseableHttpResponse.class);
		PowerMockito.when(mockedHttpResponse.getStatusLine()).thenReturn(new BasicStatusLine(HttpVersion.HTTP_1_1, HttpStatus.SC_OK, "FINE!"));
		PowerMockito.when(mockedHttpResponse.getEntity()).thenReturn(mockedHttpEntity);
				
		// Initialize variables
	    PowerMockito.mockStatic(JobHelper.class, invocation -> {
	        if (invocation.getMethod().getName().equals("getVariables")) {
	            return Collections.emptyList();
	        }
	        return invocation.callRealMethod();
	    });		
		variables = new Variables();
		try (final InputStream is = CarpaccioDeWebserviceTest.class.getClassLoader().getResourceAsStream("variables.xml")) {
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
	 * - Recette : carpaccio-de-webservice.xml
	 * - Job : ListeEcolesPubliquesBretagneCSV
	 **/
	@Test
	public void test1() {
		try {
			// Initialize the web service mocked result sets
			PowerMockito.when(mockedHttpEntity.getContent())
				.thenReturn(new FileInputStream("src/test/resources/carpaccio-de-webservice/CarpaccioDeWebservice-data.json"));
			PowerMockito.when(mockedHttpClient.execute(Matchers.any()))
				.thenReturn(mockedHttpResponse);

			Jobs jobs = new Jobs(".", "jobs/carpaccio-de-webservice.xml", variables, new Properties());
			List<Future<ActivityMBean>> futures = jobs.executeJobList(Arrays.asList("ListeEcolesPubliquesBretagneCSV"), RUN_ID);

			// Assertions
			assertEquals(1, futures.size());
			assertEquals(true, futures.get(0).isDone());
			assertEquals(ActivityTrafficLight.GREEN, futures.get(0).get().getTrafficLight());
			
			final File expected = new File("src/test/resources/carpaccio-de-webservice/CarpaccioDeWebservice-expected.csv");
			final File output = new File("output/ListeEcolesPubliquesBretagne.csv");

			Reader expectedFileReader = new BufferedReader(new FileReader(expected));
			Reader outputFileReader = new BufferedReader(new FileReader(output));

			Assert.assertTrue(IOUtils.contentEqualsIgnoreEOL(expectedFileReader, outputFileReader));
		} catch (Exception e) {
			e.printStackTrace();
			Assert.fail();
		}
	}

	/* test use case :
	 * - Recette : carpaccio-de-webservice.xml
	 * - Job : ListeEcolesPubliquesBretagneXML
	 **/
	@Test
	public void test2() {
		try {
			// Initialize the web service mocked result sets
			PowerMockito.when(mockedHttpEntity.getContent())
				.thenReturn(new FileInputStream("src/test/resources/carpaccio-de-webservice/CarpaccioDeWebservice-data.json"));
			PowerMockito.when(mockedHttpClient.execute(Matchers.any()))
				.thenReturn(mockedHttpResponse);

			Jobs jobs = new Jobs(".", "jobs/carpaccio-de-webservice.xml", variables, new Properties());
			List<Future<ActivityMBean>> futures = jobs.executeJobList(Arrays.asList("ListeEcolesPubliquesBretagneXML"), RUN_ID);

			// Assertions
			assertEquals(1, futures.size());
			assertEquals(true, futures.get(0).isDone());
			assertEquals(ActivityTrafficLight.GREEN, futures.get(0).get().getTrafficLight());
			
			assertThat(Input.from(getFileContent("output/ListeEcolesPubliquesBretagne.xml")), 
					CompareMatcher.isSimilarTo(Input.from(getFileContent("src/test/resources/carpaccio-de-webservice/CarpaccioDeWebservice-expected.xml"))));
		} catch (Exception e) {
			e.printStackTrace();
			Assert.fail();
		}
	}

	/* test use case :
	 * - Recette : carpaccio-de-webservice.xml
	 * - Job : ListeEcolesPubliquesBretagneCSVAndXML
	 **/
	@Test
	public void test3() throws AlambicException {
		try {
			// Initialize the web service mocked result sets
			PowerMockito.when(mockedHttpEntity.getContent())
				.thenReturn(new FileInputStream("src/test/resources/carpaccio-de-webservice/CarpaccioDeWebservice-data.json"))
				.thenReturn(new FileInputStream("src/test/resources/carpaccio-de-webservice/CarpaccioDeWebservice-data.json"))
				.thenReturn(new FileInputStream("src/test/resources/carpaccio-de-webservice/CarpaccioDeWebservice-data.json"))
				.thenReturn(new FileInputStream("src/test/resources/carpaccio-de-webservice/CarpaccioDeWebservice-data.json"));
			PowerMockito.when(mockedHttpClient.execute(Matchers.any()))
				.thenReturn(mockedHttpResponse);

	        // Initialize the executor factory (multi-threading engine)
	        ExecutorFactory.initialize(new Properties());

			Jobs jobs = new Jobs(".", "jobs/carpaccio-de-webservice.xml", variables, new Properties());
			List<Future<ActivityMBean>> futures = jobs.executeJobList(Arrays.asList("ListeEcolesPubliquesBretagneCSVAndXML"), RUN_ID);

			// Assertions
			assertEquals(1, futures.size());
			assertEquals(true, futures.get(0).isDone());
			assertEquals(ActivityTrafficLight.GREEN, futures.get(0).get().getTrafficLight());

			// CSV file assertion
			final File expected = new File("src/test/resources/carpaccio-de-webservice/CarpaccioDeWebservice-expected.csv");
			final File output = new File("output/ListeEcolesPubliquesBretagne.csv");

			Reader expectedFileReader = new BufferedReader(new FileReader(expected));
			Reader outputFileReader = new BufferedReader(new FileReader(output));

			Assert.assertTrue(IOUtils.contentEqualsIgnoreEOL(expectedFileReader, outputFileReader));

			// XML file assertion
			assertThat(Input.from(getFileContent("output/ListeEcolesPubliquesBretagne.xml")), 
					CompareMatcher.isSimilarTo(Input.from(getFileContent("src/test/resources/carpaccio-de-webservice/CarpaccioDeWebservice-expected.xml"))));
		} catch (Exception e) {
			e.printStackTrace();
			Assert.fail();
		} finally {
	        // Shutdown the executor factory (multi-threading engine)
	        ExecutorFactory.close();
		}
	}
	
	/* test use case :
	 * - Recette : carpaccio-de-webservice.xml
	 * - Job : ListeEcolesPubliquesBretagneFichiersVersPivotWebService
	 **/
	@Test
	public void test4() {
		try {
			// Paramètre du job
			variables.put("c1", "src/test/resources/carpaccio-de-webservice/");

			Jobs jobs = new Jobs(".", "jobs/carpaccio-de-webservice.xml", variables, new Properties());
			List<Future<ActivityMBean>> futures = jobs.executeJobList(Arrays.asList("ListeEcolesPubliquesBretagneFichiersVersPivotWebService"), RUN_ID);

			// Assertions
			assertEquals(1, futures.size());
			assertEquals(true, futures.get(0).isDone());
			assertEquals(ActivityTrafficLight.GREEN, futures.get(0).get().getTrafficLight());
			
			assertThat(Input.from(getFileContent("output/ListeEcolesPubliquesBretagneFichiersVersPivotWebService.xml")), 
					CompareMatcher.isSimilarTo(Input.from(getFileContent("src/test/resources/carpaccio-de-webservice/CarpaccioDeWebservice-test4-expected.xml"))));
		} catch (Exception e) {
			e.printStackTrace();
			Assert.fail();
		}
	}

	/* test use case :
	 * - Recette : galette-complete-supplement-LDAP.xml
	 * - Job : InterrogationPivotLDAPVersLDAP
	 **/
	@Test
	public void test5() {
		try {
			// Initialize the web service mocked result sets
			PowerMockito.when(mockedHttpEntity.getContent())
				.thenReturn(new ByteArrayInputStream(new String("{}").getBytes()));
			PowerMockito.when(mockedHttpClient.execute(Matchers.any()))
				.thenReturn(mockedHttpResponse);
			
			// Paramètre du job
			variables.put("c1", "src/test/resources/carpaccio-de-webservice/CarpaccioDeWebservice-test4-expected.xml");

			Jobs jobs = new Jobs(".", "jobs/carpaccio-de-webservice.xml", variables, new Properties());
			List<Future<ActivityMBean>> futures = jobs.executeJobList(Arrays.asList("ListeEcolesPubliquesBretagnePivotWebServiceVersWebService"), RUN_ID);

			// Assertions
			assertEquals(1, futures.size());
			assertEquals(true, futures.get(0).isDone());
			assertEquals(ActivityTrafficLight.GREEN, futures.get(0).get().getTrafficLight());
			
			// Vérifier qu'il y a bien 10 écritures vers le bon WEB service
			ArgumentCaptor<HttpUriRequest> argument = ArgumentCaptor.forClass(HttpUriRequest.class);
			verify(mockedHttpClient, times(10)).execute(argument.capture());
			assertEquals("POST", argument.getValue().getMethod());
			assertEquals("http://localhost:8080", argument.getValue().getURI().toString());
			assertEquals(getFileContent("src/test/resources/carpaccio-de-webservice/CarpaccioDeWebservice-test4-expected2.json"), 
					IOUtils.toString(((HttpPost) argument.getValue()).getEntity().getContent(), StandardCharsets.UTF_8.name()));
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