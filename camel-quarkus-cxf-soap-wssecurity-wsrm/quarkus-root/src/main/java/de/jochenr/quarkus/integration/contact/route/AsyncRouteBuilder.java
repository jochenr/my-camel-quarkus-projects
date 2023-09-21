package de.jochenr.quarkus.integration.contact.route;

import org.apache.camel.Exchange;
import org.apache.camel.LoggingLevel;
import org.apache.camel.Processor;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.cxf.jaxws.CxfEndpoint;
import org.apache.camel.converter.jaxb.JaxbDataFormat;
import org.apache.camel.model.dataformat.JsonLibrary;
import org.apache.camel.model.rest.RestBindingMode;

import camel_quarkus.jochenr.de.cxf_soap.contactservice.AddContact;
import camel_quarkus.jochenr.de.cxf_soap.contactservice.Contact;
import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.security.runtime.SecurityIdentityAssociation;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;

@ApplicationScoped
public class AsyncRouteBuilder extends RouteBuilder {

	private static final String DIRECT_INPUT_ENDPOINT = "direct:input";

	@Inject
	@Named("asyncCxfConsumer")
	CxfEndpoint asyncCxfConsumer;

	@Inject
	@Named("asyncCxfProducer")
	CxfEndpoint asyncCxfProducer;

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

		restConfiguration()
				.component("platform-http")
				.bindingMode(RestBindingMode.off)
		;

		onException(Exception.class)
			.handled(false)
			.useOriginalMessage()
			.log(LoggingLevel.ERROR, logCategory, "Exception caused by Message:\n"
					+ " \tHeaders: ${headers}\n"
					+ " \tBody: ${body}")
		;


		rest("/contacts")
				.post().routeId("consume--post-rest")
				.to(DIRECT_INPUT_ENDPOINT)
		;


		

		from(DIRECT_INPUT_ENDPOINT).routeId("send-to-backend-route")

			.log(LoggingLevel.INFO, logCategoryBusiness, "REST-Message received:\n"
					+ " \tHeaders: ${headers}\n"
					+ " \tBody: ${body}")

			.removeHeaders("*")

			.unmarshal().json(JsonLibrary.Jackson, Contact.class)
			
			.process(new Processor() {
				@Override
				public void process(Exchange exchange) throws Exception {

					Contact contact = exchange.getIn().getBody(Contact.class);
					System.out.println(contact.getName());

					AddContact adContact = new AddContact();
					adContact.setArg0(contact);

					exchange.getIn().setBody(adContact);
				}
			})

			.marshal(getDataFormatAdContact())

			.to(this.asyncCxfProducer)
		;


		

		// this route simulates a backend
		from(this.asyncCxfConsumer).routeId("backend-simulation-route")

			.log(LoggingLevel.INFO, logCategoryBusiness, "Message received:\n"
					+ " \tHeaders: ${headers}\n"
					+ " \tBody: ${body}")

			.removeHeaders("*")
			.process(new Processor() {
						@Override
						public void process(Exchange exchange) throws Exception {
							// GetContactResponse getContactResponse = new GetContactResponse();

							// Contact contact = new Contact();
							// Address address = new Address();
							// address.setCity("Karlsruhe");
							// address.setStreet("main Street 42");
                            // contact.setAddress(address);
							// contact.setName("Jochen");
							// contact.setType(ContactType.PERSONAL);
                            // getContactResponse.setReturn(contact);

							// exchange.getIn().setBody(getContactResponse, GetContactResponse.class);

						}	
				})

				// .marshal(dataFormatGetContactResponse);
		;



		// @formatter:on
	}


	protected JaxbDataFormat getDataFormatContact() {
		try {
			JAXBContext jaxbContact = JAXBContext.newInstance(Contact.class);
			JaxbDataFormat dataFormatContact = new JaxbDataFormat(jaxbContact);

			// because of missing @xmlRootElement in Contact.class
			dataFormatContact.setPartClass(Contact.class);
			dataFormatContact.setFragment(true);
			return dataFormatContact;
		} catch (JAXBException e) {
			throw new IllegalStateException(e);
		}
	}

	protected JaxbDataFormat getDataFormatAdContact() {
		try {
			JAXBContext jaxbAddContact = JAXBContext.newInstance(AddContact.class);
			JaxbDataFormat dataFormatAddContact = new JaxbDataFormat(jaxbAddContact);

			// because of missing @xmlRootElement in Contact.class
			dataFormatAddContact.setPartClass(AddContact.class);
			dataFormatAddContact.setFragment(true);
			return dataFormatAddContact;
		} catch (JAXBException e) {
			throw new IllegalStateException(e);
		}
	}

}
