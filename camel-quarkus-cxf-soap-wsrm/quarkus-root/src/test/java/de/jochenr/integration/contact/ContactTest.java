package de.jochenr.integration.contact;

import static io.restassured.RestAssured.given;

import java.net.URL;
import java.util.List;
import java.util.Map;

import org.apache.cxf.ext.logging.LoggingInInterceptor;
import org.apache.cxf.ext.logging.LoggingOutInterceptor;
import org.apache.cxf.frontend.ClientProxy;
import org.apache.cxf.interceptor.Interceptor;
import org.apache.cxf.message.Message;
import org.apache.cxf.transport.http.HTTPConduit;
import org.apache.cxf.transports.http.configuration.HTTPClientPolicy;
import org.hamcrest.CoreMatchers;
import org.hamcrest.Matchers;
import org.jboss.logging.Logger;
import org.junit.jupiter.api.Test;

import camel_quarkus.jochenr.de.cxf_soap.contactservice.Address;
import camel_quarkus.jochenr.de.cxf_soap.contactservice.Contact;
import camel_quarkus.jochenr.de.cxf_soap.contactservice.ContactService;
import camel_quarkus.jochenr.de.cxf_soap.contactservice.ContactType;
import camel_quarkus.jochenr.de.cxf_soap.contactservice.ContactWS;
import de.jochenr.quarkus.framework.camel.cxfsoap.feature.WSRMConfigRMFeature;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.config.RestAssuredConfig;
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
		
		String runtimeURL = getServerHttpUrl() + WS_BASE_PATH;
		// set target address
		// FIX
		// use this instead of "requestContext.put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY,......"
		service.addPort(ContactService.ContactServicePort, SOAPBinding.SOAP11HTTP_BINDING, runtimeURL);

		ContactWS port = service.getPort(ContactWS.class);
		BindingProvider bp = (BindingProvider) port;


		Map<String, Object> requestContext = bp.getRequestContext();

		// set target address
		// requestContext.put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY, runtimeURL);

		
		HTTPConduit httpConduit = (HTTPConduit) ClientProxy.getClient(port).getConduit();
		HTTPClientPolicy httpClientPolicy = httpConduit.getClient();
		if (httpClientPolicy == null) {
			httpClientPolicy = new HTTPClientPolicy();
		}
		httpClientPolicy.setConnectionTimeout(120000);
		httpClientPolicy.setReceiveTimeout(300000);
		httpConduit.setClient(httpClientPolicy);
		// to ignore wrong hostname in TLS cert
		// this.initTLS(httpConduit);


		logger.info("SOAP Call from ContactTest will call:\t" + runtimeURL);


		// for WS-RM
		requestContext.put(Message.ROBUST_ONEWAY, Boolean.TRUE.toString());		

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

		RestAssuredConfig config = RestAssured.config();
		config.getXmlConfig().namespaceAware(false);
		given()
				.config(config)
				.when().get(this.getServerHttpUrl() + WS_BASE_PATH + "?wsdl")
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

		RestAssuredConfig config = RestAssured.config();
		given()
				.config(config)
				.contentType(ContentType.JSON)
				.body(contact)
				.when()
				.post(this.getServerHttpUrl() + REST_BASE_PATH)
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