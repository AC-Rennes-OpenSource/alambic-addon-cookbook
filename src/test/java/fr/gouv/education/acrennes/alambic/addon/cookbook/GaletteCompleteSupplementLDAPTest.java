package fr.gouv.education.acrennes.alambic.addon.cookbook;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.Future;

import javax.naming.NamingEnumeration;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.BasicAttribute;
import javax.naming.directory.BasicAttributes;
import javax.naming.directory.InitialDirContext;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.directory.shared.ldap.message.ArrayNamingEnumeration;
import org.jdom2.Element;
import org.jdom2.input.SAXBuilder;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Matchers;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.xmlunit.builder.Input;
import org.xmlunit.matchers.CompareMatcher;

import fr.gouv.education.acrennes.alambic.jobs.JobHelper;
import fr.gouv.education.acrennes.alambic.jobs.Jobs;
import fr.gouv.education.acrennes.alambic.jobs.extract.clients.LdapToStateBase;
import fr.gouv.education.acrennes.alambic.jobs.load.StateBaseToLdap;
import fr.gouv.education.acrennes.alambic.monitoring.ActivityMBean;
import fr.gouv.education.acrennes.alambic.monitoring.ActivityTrafficLight;
import fr.gouv.education.acrennes.alambic.utils.Variables;

@RunWith(PowerMockRunner.class)
@PrepareForTest({LdapToStateBase.class, StateBaseToLdap.class, JobHelper.class})
@PowerMockIgnore({ "javax.management.*", "javax.net.ssl.*" })
public class GaletteCompleteSupplementLDAPTest {
	
	private static final String RUN_ID = "1";
	private static final String LDAP_ENTRY_NAME_SPACE = "ou=personnes,dc=ent-bretagne,dc=fr";

    private InitialDirContext mockedDirContext;
	private Variables variables;

	@BeforeClass
	public static void setProperties() {
		System.setProperty("log4j.configurationFile", "log4j2.xml");
	}

	@Before
	public void setUp() throws Exception {
		// Prepare LDAP mock
        mockedDirContext = PowerMockito.mock(InitialDirContext.class);
        PowerMockito.whenNew(InitialDirContext.class).withAnyArguments().thenReturn(mockedDirContext);
		PowerMockito.when(mockedDirContext.getNameInNamespace()).thenReturn(LDAP_ENTRY_NAME_SPACE);
		PowerMockito.when(mockedDirContext.lookup(Matchers.anyString())).thenReturn(mockedDirContext);
				
		// Initialize variables
	    PowerMockito.mockStatic(JobHelper.class, invocation -> {
	        if (invocation.getMethod().getName().equals("getVariables")) {
	            return Collections.emptyList();
	        }
	        return invocation.callRealMethod();
	    });		
		variables = new Variables();
		try (final InputStream is = GaletteCompleteSupplementLDAPTest.class.getClassLoader().getResourceAsStream("variables.xml")) {
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
	 * - Recette : galette-complete-supplement-LDAP.xml
	 * - Job : InterrogationLDAPVersXML
	 **/
	@Test
	public void test1() {
		try {
			// Initialize LDAP mocked result sets
			String rsSetItem1 = getFileContent("src/test/resources/galette-complete-supplement-LDAP/GaletteCompleteSupplementLDAP-test1-data1.json");
			String rsSetItem2 = getFileContent("src/test/resources/galette-complete-supplement-LDAP/GaletteCompleteSupplementLDAP-test1-data2.json");
            PowerMockito.when(mockedDirContext.search(Matchers.anyString(), Matchers.anyString(), Matchers.any(SearchControls.class)))
            	.thenReturn(buildResultSet("uid=warren.gomez001,ou=personnes,dc=ent-bretagne,dc=fr", Arrays.asList(rsSetItem1, rsSetItem2)));

			Jobs jobs = new Jobs(".", "jobs/galette-complete-supplement-LDAP.xml", variables, new Properties());
			List<Future<ActivityMBean>> futures = jobs.executeJobList(Arrays.asList("InterrogationLDAPVersXML"), RUN_ID);

			// Assertions
			assertEquals(1, futures.size());
			assertEquals(true, futures.get(0).isDone());
			assertEquals(ActivityTrafficLight.GREEN, futures.get(0).get().getTrafficLight());
			
			assertThat(Input.from(getFileContent("output/InterrogationLDAPVersXML.xml")), 
					CompareMatcher.isSimilarTo(Input.from(getFileContent("src/test/resources/galette-complete-supplement-LDAP/GaletteCompleteSupplementLDAP-test1-expected.xml"))));
		} catch (Exception e) {
			e.printStackTrace();
			Assert.fail();
		}
	}

	/* test use case :
	 * - Recette : galette-complete-supplement-LDAP.xml
	 * - Job : InterrogationDynamiqueLDAPVersXML
	 **/
	@Test
	public void test2() {
		try {
			// Initialize LDAP mocked result sets
			String rsSetItem = getFileContent("src/test/resources/galette-complete-supplement-LDAP/GaletteCompleteSupplementLDAP-test1-data2.json");
            PowerMockito.when(mockedDirContext.search(Matchers.anyString(), Matchers.anyString(), Matchers.any(SearchControls.class)))
            	.thenReturn(buildResultSet("uid=warren.gomez001,ou=personnes,dc=ent-bretagne,dc=fr", Arrays.asList(rsSetItem)));
            
			Jobs jobs = new Jobs(".", "jobs/galette-complete-supplement-LDAP.xml", variables, new Properties());
			List<Future<ActivityMBean>> futures = jobs.executeJobList(Arrays.asList("InterrogationDynamiqueLDAPVersXML"), RUN_ID);

			// Assertions
			assertEquals(1, futures.size());
			assertEquals(true, futures.get(0).isDone());
			assertEquals(ActivityTrafficLight.GREEN, futures.get(0).get().getTrafficLight());
			
			assertThat(Input.from(getFileContent("output/InterrogationDynamiqueLDAPVersXML.xml")), 
					CompareMatcher.isSimilarTo(Input.from(getFileContent("src/test/resources/galette-complete-supplement-LDAP/GaletteCompleteSupplementLDAP-test2-expected.xml"))));
		} catch (Exception e) {
			e.printStackTrace();
			Assert.fail();
		}
	}

	/* test use case :
	 * - Recette : galette-complete-supplement-LDAP.xml
	 * - Job : InterrogationFichiersVersPivotLDAP
	 **/
	@Test
	public void test3() {
		try {
			// Paramètre du job
			variables.put("c1", "src/test/resources/galette-complete-supplement-LDAP/");

			Jobs jobs = new Jobs(".", "jobs/galette-complete-supplement-LDAP.xml", variables, new Properties());
			List<Future<ActivityMBean>> futures = jobs.executeJobList(Arrays.asList("InterrogationFichiersVersPivotLDAP"), RUN_ID);

			// Assertions
			assertEquals(1, futures.size());
			assertEquals(true, futures.get(0).isDone());
			assertEquals(ActivityTrafficLight.GREEN, futures.get(0).get().getTrafficLight());
			
			assertThat(Input.from(getFileContent("output/InterrogationFichiersVersPivotLDAP.xml")), 
					CompareMatcher.isSimilarTo(Input.from(getFileContent("src/test/resources/galette-complete-supplement-LDAP/GaletteCompleteSupplementLDAP-test3-expected.xml"))));
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
	public void test4() {
		try {
            PowerMockito.when(mockedDirContext.search(Matchers.anyString(), Matchers.anyString(), Matchers.any(SearchControls.class)))
        		.thenReturn(buildResultSet("ou=035000X,ou=structures,dc=ent-bretagne,dc=fr", Collections.emptyList()));
			
			// Paramètre du job
			variables.put("c1", "src/test/resources/galette-complete-supplement-LDAP/GaletteCompleteSupplementLDAP-test3-expected.xml");

			Jobs jobs = new Jobs(".", "jobs/galette-complete-supplement-LDAP.xml", variables, new Properties());
			List<Future<ActivityMBean>> futures = jobs.executeJobList(Arrays.asList("InterrogationPivotLDAPVersLDAP"), RUN_ID);

			// Assertions
			assertEquals(1, futures.size());
			assertEquals(true, futures.get(0).isDone());
			assertEquals(ActivityTrafficLight.GREEN, futures.get(0).get().getTrafficLight());
			
			// Vérifier qu'il y a bien 10 écritures dans l'annuaire
			verify(mockedDirContext, times(10)).createSubcontext(Matchers.anyString(), Matchers.any());
		} catch (Exception e) {
			e.printStackTrace();
			Assert.fail();
		}
	}

    // Utility function : converts the input JSON resultSet string representation into LDAP attributes structure
    private NamingEnumeration<SearchResult> buildResultSet(final String ns, final List<String> results) {
        List<SearchResult> resultSet = new ArrayList<>();

        for (String result : results) {
            SearchResult sr = new SearchResult(result, null, buildAttributes(result));
            sr.setNameInNamespace(ns);
            resultSet.add(sr);
        }

        return new ArrayNamingEnumeration<>(resultSet.toArray(new SearchResult[0]));
    }

    // Utility function : converts the input JSON entry string representation into LDAP attributes structure
    private Attributes buildAttributes(final String attrs) {
        Attributes attributes = new BasicAttributes();

        if (StringUtils.isNotBlank(attrs)) {
            JSONObject jsonObj = new JSONObject(attrs);
            Iterator<String> keys = jsonObj.keys();
            while (keys.hasNext()) {
                String attrName = keys.next();
                Attribute attribute = new BasicAttribute(attrName);
                Object attrValues = jsonObj.get(attrName);
                if (attrValues instanceof JSONArray) {
                    List<Object> values = ((JSONArray) attrValues).toList();
                    for (Object value : values) {
                        if (StringUtils.isNotEmpty((String) value)) {
                            attribute.add(value);
                        }
                    }
                } else {
                    if (StringUtils.isNotEmpty((String) attrValues)) {
                        attribute.add(attrValues);
                    }
                }
                attributes.put(attribute);
            }
        }
        return attributes;
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