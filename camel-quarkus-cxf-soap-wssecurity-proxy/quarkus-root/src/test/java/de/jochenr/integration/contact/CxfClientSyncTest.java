package de.jochenr.integration.contact;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;

import org.apache.cxf.endpoint.Client;
import org.apache.cxf.ext.logging.LoggingInInterceptor;
import org.apache.cxf.ext.logging.LoggingOutInterceptor;
import org.apache.cxf.frontend.ClientProxy;
import org.apache.cxf.helpers.IOUtils;
import org.apache.cxf.interceptor.Interceptor;
import org.apache.cxf.jaxws.DispatchImpl;
import org.apache.cxf.message.Message;
import org.apache.cxf.transport.http.HTTPConduit;
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
import de.jochenr.quarkus.framework.camel.cxfsoap.security.CxfProducerSAMLSenderVouchesOutInterceptor;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.config.RestAssuredConfig;
import io.restassured.config.SSLConfig;
import jakarta.xml.ws.BindingProvider;
import jakarta.xml.ws.Dispatch;
import jakarta.xml.ws.Service;

@QuarkusTest
public class CxfClientSyncTest extends BaseTest {

	public static final String SAML_ASSERTION_USER_NAME = "TestUser";


	private static final Logger logger = Logger.getLogger(CxfClientSyncTest.class);

	private static final String WS_BASE_PATH = "/cxfservices/contact";
	ContactWS contactService = null;

	Dispatch<Source> cxfDispatch = null;


	public CxfClientSyncTest() {

		this.contactService = createCXFClient();
		this.cxfDispatch = createDispatch();

		logger.info("Constructor \"CxfClientSyncTest()\" called!");

	}

	protected ContactWS createCXFClient() {

        final URL serviceUrl = Thread.currentThread().getContextClassLoader().getResource("wsdl/ContactService.wsdl");
        final Service service = Service.create(serviceUrl, ContactService.SERVICE);

        ContactWS port = service.getPort(ContactWS.class);
        BindingProvider bp = (BindingProvider) port;

		

		// this has to be done AFTER    BindingProvider.ENDPOINT_ADDRESS_PROPERTY   with new version.....
        // // to ignore wrong hostname in TLS cert
		// HTTPConduit httpConduit = (HTTPConduit) ClientProxy.getClient(port).getConduit();
        // initTLS(httpConduit);

        Map<String, Object> requestContext = bp.getRequestContext();

        requestContext.put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY, getServerUrl() + WS_BASE_PATH);

		// to ignore wrong hostname in TLS cert
		HTTPConduit httpConduit = (HTTPConduit) ClientProxy.getClient(port).getConduit();
        initTLS(httpConduit);

        initSamlProps(requestContext);

        List<Interceptor<? extends Message>> inInterceptors = ClientProxy.getClient(port).getInInterceptors();
        List<Interceptor<? extends Message>> outInterceptors = ClientProxy.getClient(port).getOutInterceptors();

        LoggingInInterceptor loggingInInterceptor = new LoggingInInterceptor();
        loggingInInterceptor.setPrettyLogging(true);
        inInterceptors.add(loggingInInterceptor);


		CxfProducerSAMLSenderVouchesOutInterceptor<Message> cxfProducerOutInterceptor = new CxfProducerSAMLSenderVouchesOutInterceptor<Message>();

        LoggingOutInterceptor loggingOutInterceptor = new LoggingOutInterceptor();
        loggingOutInterceptor.setPrettyLogging(true);

		outInterceptors.add(cxfProducerOutInterceptor);
        outInterceptors.add(loggingOutInterceptor);

        return port;
    }


	protected Dispatch<Source> createDispatch() {
		final URL serviceUrl = Thread.currentThread().getContextClassLoader().getResource("wsdl/ContactService.wsdl");
		final Service service = Service.create(serviceUrl, ContactService.SERVICE);

		Dispatch<Source> dispatch = service.createDispatch(ContactService.ContactServicePort, Source.class, Service.Mode.PAYLOAD);

		Map<String, Object> requestContext = dispatch.getRequestContext();

		requestContext.put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY, getServerUrl() + WS_BASE_PATH);

		Client client = ((DispatchImpl) dispatch).getClient();
		HTTPConduit httpConduit = (HTTPConduit) client.getConduit();

		initTLS(httpConduit);

		initSamlProps(requestContext);

		List<Interceptor<? extends Message>> inInterceptors = client.getInInterceptors();
        List<Interceptor<? extends Message>> outInterceptors = client.getOutInterceptors();
		
		LoggingInInterceptor loggingInInterceptor = new LoggingInInterceptor();
        loggingInInterceptor.setPrettyLogging(true);
        inInterceptors.add(loggingInInterceptor);


		CxfProducerSAMLSenderVouchesOutInterceptor<Message> cxfProducerOutInterceptor = new CxfProducerSAMLSenderVouchesOutInterceptor<Message>();

        LoggingOutInterceptor loggingOutInterceptor = new LoggingOutInterceptor();
        loggingOutInterceptor.setPrettyLogging(true);

		outInterceptors.add(cxfProducerOutInterceptor);
        outInterceptors.add(loggingOutInterceptor);


		return dispatch;
	}

	private void initSamlProps(Map<String, Object> requestContext) {
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
	}

	protected Source getSourceFromFileName(String fileName) {
		try {
			return new StreamSource(new StringReader(getStringFromUtf8FileName(fileName)));
		} catch (Exception e) {
			fail("Exception when getting source from file " + fileName + ": " + e.getMessage());
			return null;
		}
	}

	protected String getStringFromUtf8FileName(String fileName) {
		try {
			InputStream resourceAsStream = Thread.currentThread().getContextClassLoader().getResourceAsStream(fileName);
			if (resourceAsStream == null) {
				throw new IllegalStateException("Die Datei \"" + fileName + "\" konnte nicht gefunden werden!");
			}
			return new String(IOUtils.readBytesFromStream(resourceAsStream), StandardCharsets.UTF_8);
		} catch (NullPointerException | IOException e) {
			fail("Exception when getting String from file " + fileName + ": " + e.getMessage());
			throw new IllegalStateException(e);
		}
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

	@Test
	public void testGetContactDispatchMode() throws NoSuchContactException {

		Source requestSource = this.getSourceFromFileName("request.xml");

		this.cxfDispatch.invoke(requestSource);

	}

	


}