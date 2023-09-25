package de.jochenr.integration.contact;

import static io.restassured.RestAssured.given;

import java.net.URL;
import java.util.Map;
import java.util.Properties;

import org.apache.cxf.ws.security.SecurityConstants;
import org.hamcrest.CoreMatchers;
import org.hamcrest.Matchers;
import org.jboss.logging.Logger;
import org.junit.jupiter.api.Test;

import camel_quarkus.jochenr.de.cxf_soap.contactservice.ContactService;
import camel_quarkus.jochenr.de.cxf_soap.contactservice.ContactWS;
import camel_quarkus.jochenr.de.cxf_soap.contactservice.GetContact;
import camel_quarkus.jochenr.de.cxf_soap.contactservice.NoSuchContactException;
import de.jochenr.quarkus.framework.camel.cxfsoap.client.SamlStandaloneCallbackHandler;
import io.quarkus.test.common.http.TestHTTPResource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.config.RestAssuredConfig;
import io.restassured.config.SSLConfig;
import jakarta.xml.ws.BindingProvider;
import jakarta.xml.ws.Service;

@QuarkusTest
public class HealthCheckSyncTest extends BaseTest {



	public static final String SAML_ASSERTION_USER_NAME = "TestUser";


	private static final Logger logger = Logger.getLogger(HealthCheckSyncTest.class);

	private static final String WS_BASE_PATH = "/cxfservices/contact";
	ContactWS contactService = null;

	// @TestHTTPResource
	// URL url;

	public HealthCheckSyncTest() {

		this.contactService = createCXFClient();

	}

	protected ContactWS createCXFClient() {

        final URL serviceUrl = Thread.currentThread().getContextClassLoader().getResource("wsdl/ContactService.wsdl");
        final Service service = Service.create(serviceUrl, ContactService.SERVICE);

        ContactWS port = service.getPort(ContactWS.class);
        BindingProvider bp = (BindingProvider) port;

		// this has to be done AFTER    BindingProvider.ENDPOINT_ADDRESS_PROPERTY   with new version.....
        // // to ignore wrong hostname in TLS cert
        // initTLS(port);

        Map<String, Object> requestContext = bp.getRequestContext();

        requestContext.put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY, getServerUrl() + WS_BASE_PATH);

		// to ignore wrong hostname in TLS cert
        initTLS(port);


        Properties samlProps = new Properties();
        samlProps.put("org.apache.wss4j.crypto.provider", "org.apache.wss4j.common.crypto.Merlin");
        samlProps.put("org.apache.wss4j.crypto.merlin.keystore.type", "pkcs12");
        samlProps.put("org.apache.wss4j.crypto.merlin.keystore.file", "alice.jks");
        samlProps.put("org.apache.wss4j.crypto.merlin.keystore.password", "password");
        samlProps.put("org.apache.wss4j.crypto.merlin.keystore.alias", "alice");
        samlProps.put("org.apache.wss4j.crypto.merlin.keystore.private.password", "password");
        samlProps.put("org.apache.wss4j.crypto.merlin.keystore.private.caching", "true");

        requestContext.put(SecurityConstants.SIGNATURE_PROPERTIES, samlProps);
        requestContext.put(SecurityConstants.SAML_CALLBACK_HANDLER, new SamlStandaloneCallbackHandler(SAML_ASSERTION_USER_NAME));

        requestContext.put(SecurityConstants.STORE_BYTES_IN_ATTACHMENT, false);

        // List<Interceptor<? extends Message>> inInterceptors = ClientProxy.getClient(port).getInInterceptors();
        // List<Interceptor<? extends Message>> outInterceptors = ClientProxy.getClient(port).getOutInterceptors();

        // LoggingInInterceptor loggingInInterceptor = new LoggingInInterceptor();
        // loggingInInterceptor.setPrettyLogging(true);
        // inInterceptors.add(loggingInInterceptor);

        // LoggingOutInterceptor loggingOutInterceptor = new LoggingOutInterceptor();
        // loggingOutInterceptor.setPrettyLogging(true);
        // outInterceptors.add(loggingOutInterceptor);

        return port;
    }


	@Test
	public void wsdlAvailableTest() {


		RestAssuredConfig config = RestAssured.config().sslConfig(new SSLConfig()
				.allowAllHostnames()
				.relaxedHTTPSValidation());
		config.getXmlConfig().namespaceAware(false);
		given()
				.config(config)
				.when().get(this.getServerUrl() + WS_BASE_PATH +"?wsdl")
				.then()
				.statusCode(200)
				.body(
						Matchers.hasXPath(
								"/*[local-name()='definitions']/*[local-name()='Policy']/@*[local-name() = 'Id']",
								CoreMatchers.is("BN_Policy")));

	}

	@Test
	public void testGetContact() throws NoSuchContactException {

		GetContact getContact = new GetContact();
		getContact.setArg0("id4711");

		this.contactService.getContact(getContact);
	}

	

	// private void healthCheckSync(String param) throws BusinessFault,
	// 		TechnicalFault {
	// 	MTOMFeature mtom = new MTOMFeature(false);
	// 	AddressingFeature adressing = new AddressingFeature(false, false);
	// 	URL url = Thread.currentThread().getContextClassLoader().getResource(WSDL_URL);
	// 	HealthCheckSyncWS wsPort = SamlSenderVouchesWSInitializer.initStandaloneClientWSPort(HealthCheckSyncWS.class,
	// 			url, HealthCheckSyncWS_Service.SERVICE, CONFIGURATION_PROPERTIES_PREFIX,
	// 			SAML_ASSERTION_USER_NAME, adressing, mtom);

	// 	CheckHealth parameters = new CheckHealth();
	// 	parameters.setRequestTimestamp(new Date());
	// 	parameters.setTextPayload("Hallo hallo &apos;S.O.S");
	// 	parameters.setTid("erfundeneTID");
	// 	;
	// 	parameters.setTo(EndpointSystemType.SAP); // wogegen soll dieser Test gehen??
	// 	parameters.setFrom(EndpointSystemType.ESB);
	// 	parameters.setOperation("NONE");
	// 	parameters.setParam(param);

	// 	CheckHealthResponse checkHealthResp = wsPort.checkHealth(parameters);
	// 	String paramReturn = checkHealthResp.getParam();
	// 	Assertions.assertEquals(param, paramReturn);
	// }

}