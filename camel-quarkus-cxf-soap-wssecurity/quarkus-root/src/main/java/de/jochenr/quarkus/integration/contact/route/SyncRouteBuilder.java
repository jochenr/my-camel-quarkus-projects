package de.jochenr.quarkus.integration.contact.route;

import org.apache.camel.Exchange;
import org.apache.camel.LoggingLevel;
import org.apache.camel.Processor;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.cxf.jaxws.CxfEndpoint;
import org.apache.camel.converter.jaxb.JaxbDataFormat;

import camel_quarkus.jochenr.de.cxf_soap.contactservice.Address;
import camel_quarkus.jochenr.de.cxf_soap.contactservice.Contact;
import camel_quarkus.jochenr.de.cxf_soap.contactservice.ContactType;
import camel_quarkus.jochenr.de.cxf_soap.contactservice.GetContactResponse;
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

	// @Inject
	// @Named("syncCxfProducer")
	// CxfEndpoint syncCxfProducer;

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

		//<from id="from01" uri="cxf:bean:syncCxfConsumer"/>
		from(this.syncCxfConsumer).routeId("sync-route-cxf-outside")

			.log(LoggingLevel.INFO, logCategoryBusiness, "Message received:\n"
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
