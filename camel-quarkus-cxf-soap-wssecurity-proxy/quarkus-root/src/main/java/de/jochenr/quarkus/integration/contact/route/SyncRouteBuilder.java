package de.jochenr.quarkus.integration.contact.route;

import java.security.Principal;
import java.util.HashMap;
import java.util.Map;

import org.apache.camel.Exchange;
import org.apache.camel.LoggingLevel;
import org.apache.camel.Message;
import org.apache.camel.Processor;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.cxf.jaxws.CxfEndpoint;
import org.apache.camel.converter.jaxb.JaxbDataFormat;
import org.apache.camel.http.common.HttpMessage;

import camel_quarkus.jochenr.de.cxf_soap.contactservice.Address;
import camel_quarkus.jochenr.de.cxf_soap.contactservice.Contact;
import camel_quarkus.jochenr.de.cxf_soap.contactservice.ContactType;
import camel_quarkus.jochenr.de.cxf_soap.contactservice.GetContactResponse;
import de.jochenr.quarkus.framework.camel.cxfsoap.client.SamlStandaloneCallbackHandler;
import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.security.runtime.SecurityIdentityAssociation;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.xml.bind.JAXBContext;

@ApplicationScoped
public class SyncRouteBuilder extends RouteBuilder {

	@Inject
	@Named("syncCxfConsumer")
	CxfEndpoint syncCxfConsumer;

	@Inject
	@Named("syncSendToSapCxfProducer")
	CxfEndpoint syncSendToSapCxfProducer;

	@Inject
	@Named("mockSapCxfConsumer")
	CxfEndpoint mockSapCxfConsumer;

	@Inject
	SecurityIdentityAssociation securityIdentityAssociation;

	@Inject
	SecurityIdentity identity;

	@Override
	public void configure() throws Exception {

		String logCategory = this.getClass().getCanonicalName() + "." + this.getContext().getName();
		String logCategoryBusiness = logCategory + "_business";

		// @formatter:off


		// Stream Caching global einschalten
		getContext().setStreamCaching(true);
		getContext().setAllowUseOriginalMessage(true);

		onException(Exception.class)
			.handled(false)
			.useOriginalMessage()
			.log(LoggingLevel.ERROR, logCategory, "Exception caused by Message:\n"
					+ " \tHeaders: ${headers}\n"
					+ " \tBody: ${body}")
		;


		JAXBContext jaxbGetContactResponse = JAXBContext.newInstance(GetContactResponse.class);
		JaxbDataFormat dataFormatGetContactResponse = new JaxbDataFormat(jaxbGetContactResponse);

		from(this.syncCxfConsumer).routeId("sync-route-cxf-outside")

			.log(LoggingLevel.INFO, logCategoryBusiness, "Message received:\n"
					+ " \tHeaders: ${headers}\n"
					+ " \tBody: ${body}")

			.removeHeaders("*")

			.process(new Processor() {

				@Override
				public void process(Exchange exchange) throws Exception {

					Map<String, Object> requestContext = (Map)exchange.getProperty("RequestContext");
					if (requestContext == null) {
						requestContext = new HashMap();
					}

					((Map)requestContext).put("security.saml-callback-handler", new SamlStandaloneCallbackHandler("TestUser"));
					exchange.setProperty("RequestContext", requestContext);
				}
				
			})


			.to(this.syncSendToSapCxfProducer)

			.removeHeaders("*")

		;


		from(this.mockSapCxfConsumer).routeId("mock-sap-route")

			.log(LoggingLevel.INFO, logCategoryBusiness, "SAP Mock received message:\n"
					+ " \tHeaders: ${headers}\n"
					+ " \tBody: ${body}")

			.removeHeaders("*")
			.process(new Processor() {
						@Override
						public void process(Exchange exchange) throws Exception {
							GetContactResponse getContactResponse = new GetContactResponse();

							Contact contact = new Contact();
							Address address = new Address();
							address.setCity("Karlsruhe");
							address.setStreet("main Street 42");
                            contact.setAddress(address);
							contact.setName("Jochen");
							contact.setType(ContactType.PERSONAL);
                            getContactResponse.setReturn(contact);

							exchange.getIn().setBody(getContactResponse, GetContactResponse.class);

						}	
				})

				.marshal(dataFormatGetContactResponse);
		;



		// @formatter:on
	}

}
