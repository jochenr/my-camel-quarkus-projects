package de.jochenr.integration.contact;

import static io.restassured.RestAssured.given;

import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.cxf.ext.logging.LoggingInInterceptor;
import org.apache.cxf.ext.logging.LoggingOutInterceptor;
import org.apache.cxf.frontend.ClientProxy;
import org.apache.cxf.interceptor.Interceptor;
import org.apache.cxf.message.Message;
import org.apache.cxf.transport.http.HTTPConduit;
import org.apache.cxf.ws.security.SecurityConstants;
import org.hamcrest.CoreMatchers;
import org.hamcrest.Matchers;
import org.jboss.logging.Logger;
import org.junit.jupiter.api.Test;

import camel_quarkus.jochenr.de.cxf_soap.contactservice.Address;
import camel_quarkus.jochenr.de.cxf_soap.contactservice.Contact;
import camel_quarkus.jochenr.de.cxf_soap.contactservice.ContactService;
import camel_quarkus.jochenr.de.cxf_soap.contactservice.ContactType;
import camel_quarkus.jochenr.de.cxf_soap.contactservice.ContactWS;
import de.jochenr.quarkus.framework.camel.cxfsoap.client.SamlStandaloneCallbackHandler;
import de.jochenr.quarkus.framework.camel.cxfsoap.feature.WSRMConfigRMFeature;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.config.RestAssuredConfig;
import io.restassured.config.SSLConfig;
import io.restassured.http.ContentType;
import jakarta.xml.ws.BindingProvider;
import jakarta.xml.ws.Service;
import jakarta.xml.ws.soap.AddressingFeature;
import jakarta.xml.ws.soap.MTOMFeature;
import jakarta.xml.ws.soap.SOAPBinding;

@QuarkusTest
public class ContactTest extends BaseTest {

	public static final String SAML_ASSERTION_USER_NAME = "TestUser";

	private static final Logger logger = Logger.getLogger(ContactTest.class);

	private static final String WS_BASE_PATH = "/cxfservices/contact";
	private static final String REST_BASE_PATH = "/contacts";

	ContactWS contactService = null;

	// @TestHTTPResource
	// URL url;

	public ContactTest() {

		this.contactService = createCXFClient();

	}

	protected ContactWS createCXFClient() {

		final URL serviceUrl = Thread.currentThread().getContextClassLoader().getResource("wsdl/ContactService.wsdl");
		final Service service = Service.create(serviceUrl, ContactService.SERVICE, new MTOMFeature(true),
				new AddressingFeature(true, true),
				new WSRMConfigRMFeature());

		String runtimeURL = getServerHttpsUrl() + WS_BASE_PATH;
		// set target address
		// FIX
		// use this instead of "requestContext.put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY,......"
		service.addPort(ContactService.ContactServicePort, SOAPBinding.SOAP11HTTP_BINDING, runtimeURL);

		ContactWS port = service.getPort(ContactWS.class);
		BindingProvider bp = (BindingProvider) port;

		Map<String, Object> requestContext = bp.getRequestContext();

		// set target address
		requestContext.put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY, runtimeURL);

		// to ignore wrong hostname in TLS cert
		HTTPConduit httpConduit = (HTTPConduit) ClientProxy.getClient(port).getConduit();
		// httpConduit....
		this.initTLS(httpConduit);

		logger.info("SOAP Call from ContactTest will call:\t" + runtimeURL);

		// for WS-RM
		requestContext.put(Message.ROBUST_ONEWAY, Boolean.TRUE.toString());

		// SAML sender Vouches properties
		Properties samlProps = new Properties();
		samlProps.put("org.apache.wss4j.crypto.provider", "org.apache.wss4j.common.crypto.Merlin");
		samlProps.put("org.apache.wss4j.crypto.merlin.keystore.type", "pkcs12");
		samlProps.put("org.apache.wss4j.crypto.merlin.keystore.file", "alice.jks");
		samlProps.put("org.apache.wss4j.crypto.merlin.keystore.password", "password");
		samlProps.put("org.apache.wss4j.crypto.merlin.keystore.alias", "alice");
		samlProps.put("org.apache.wss4j.crypto.merlin.keystore.private.password", "password");
		samlProps.put("org.apache.wss4j.crypto.merlin.keystore.private.caching", "true");

		requestContext.put(SecurityConstants.SIGNATURE_PROPERTIES, samlProps);
		requestContext.put(SecurityConstants.SAML_CALLBACK_HANDLER,
				new SamlStandaloneCallbackHandler(SAML_ASSERTION_USER_NAME));

		requestContext.put(SecurityConstants.STORE_BYTES_IN_ATTACHMENT, false);

		List<Interceptor<? extends Message>> inInterceptors = ClientProxy.getClient(port).getInInterceptors();
		List<Interceptor<? extends Message>> outInterceptors = ClientProxy.getClient(port).getOutInterceptors();

		LoggingInInterceptor loggingInInterceptor = new LoggingInInterceptor();
		loggingInInterceptor.setPrettyLogging(true);
		inInterceptors.add(loggingInInterceptor);

		LoggingOutInterceptor loggingOutInterceptor = new LoggingOutInterceptor();
		loggingOutInterceptor.setPrettyLogging(true);
		outInterceptors.add(loggingOutInterceptor);

		return port;
	}

	@Test
	public void wsdlAvailableTest() {

		// test if the simulated backend Soap-Service is available

		RestAssuredConfig config = RestAssured.config().sslConfig(new SSLConfig()
				.allowAllHostnames()
				.relaxedHTTPSValidation());
		config.getXmlConfig().namespaceAware(false);
		given()
				.config(config)
				.when().get(this.getServerHttpsUrl() + WS_BASE_PATH + "?wsdl")
				.then()
				.statusCode(200)
				.body(
						Matchers.hasXPath(
								"/*[local-name()='definitions']/*[local-name()='Policy']/@*[local-name() = 'Id']",
								CoreMatchers.is("BN_Policy")));

	}

	@Test
	public void testAddContactRest() {

		Contact contact = createContactObject();

		RestAssuredConfig config = RestAssured.config().sslConfig(new SSLConfig()
		.allowAllHostnames()
		.relaxedHTTPSValidation());
		// RestAssuredConfig config = RestAssured.config();
		given()
				.config(config)
				.contentType(ContentType.JSON)
				.body(contact)
				.when()
				.post(this.getServerHttpsUrl() + REST_BASE_PATH)
				.then()
				.statusCode(200);
	}

	@Test
	public void testAddContactBackendSoap() {

		Contact contact = createContactObject();

		this.contactService.addContact(contact);

	}

	private Contact createContactObject() {
		Contact contact = new Contact();
		contact.setName("Jochen");
		contact.setType(ContactType.PERSONAL);
		Address address = new Address();
		address.setCity("Karlsruhe");
		address.setStreet("Main Street 42");
		contact.setAddress(address);
		return contact;
	}

}