/**
 *
 */
package de.jochenr.quarkus.integration.contact.producer;

import org.apache.camel.CamelContext;
import org.apache.camel.component.cxf.common.DataFormat;
import org.apache.camel.component.cxf.jaxws.CxfEndpoint;

import de.jochenr.quarkus.framework.camel.cxfsoap.CxfConsumerEndpointBuilder;
import de.jochenr.quarkus.framework.camel.cxfsoap.CxfProducerEndpointBuilder;
import de.jochenr.quarkus.framework.camel.cxfsoap.LoginSecurityType;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;
import jakarta.inject.Named;


public class SyncCxfEndpointProducer {

	public final static String CONSUMER_ENDPOINT_ROLE = "testRole";

	@Inject
	CamelContext context;

	@Named("syncCxfConsumer")
	@Produces
	public CxfEndpoint createCxfConsumer() {
		CxfConsumerEndpointBuilder builder = CxfConsumerEndpointBuilder.getNewInstance("/contact", this.context)
				.withAllowStreaming(true)
				.withDataFormat(DataFormat.PAYLOAD)
				.withMtomEnabled(true)
				.withLoginSecurity(LoginSecurityType.SAML_SENDER_VOUCHES, "dummy", CONSUMER_ENDPOINT_ROLE)
				.withWrappedStyle(true)
				.withSchemaValidationEnabled(true)
				.withWsdlURL("wsdl/ContactService.wsdl");
		return builder.build();
	}

	@Named("syncSendToSapCxfProducer")
	@Produces
	public CxfEndpoint toLProfiCxfProducer() {
		CxfProducerEndpointBuilder builder = CxfProducerEndpointBuilder.getNewInstance("sapMock", this.context)
				.withAllowStreaming(true)
				.withDataFormat(DataFormat.PAYLOAD)
				.withMtomEnabled(true)
				.withLoginSecurity(LoginSecurityType.SAML_SENDER_VOUCHES)
				.withWrappedStyle(true)
				.withSchemaValidationEnabled(true)
				.withWsdlURL("wsdl/ContactService.wsdl");
		return builder.build();
	}

	@Named("mockSapCxfConsumer")
	@Produces
	public CxfEndpoint createSAPMockingCxfConsumer() {
		CxfConsumerEndpointBuilder builder = CxfConsumerEndpointBuilder.getNewInstance("/sap/mock/contact", this.context)
				.withAllowStreaming(true)
				.withDataFormat(DataFormat.PAYLOAD)
				.withMtomEnabled(true)
				.withLoginSecurity(LoginSecurityType.SAML_SENDER_VOUCHES, "dummy", CONSUMER_ENDPOINT_ROLE)
				.withWrappedStyle(true)
				.withSchemaValidationEnabled(true)
				.withWsdlURL("wsdl/ContactService.wsdl");
		return builder.build();
	}

}
