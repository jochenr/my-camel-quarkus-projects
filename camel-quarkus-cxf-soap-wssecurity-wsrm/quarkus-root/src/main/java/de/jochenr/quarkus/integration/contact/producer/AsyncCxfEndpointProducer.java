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


public class AsyncCxfEndpointProducer {

	public final static String CONSUMER_ENDPOINT_ROLE = "testRole";

	@Inject
	CamelContext context;
	

	@Named("asyncCxfProducer")
	@Produces
	public CxfEndpoint toLProfiCxfProducer() {
		CxfProducerEndpointBuilder builder = CxfProducerEndpointBuilder.getNewInstance("asyncLProfi", this.context)
				.withAllowStreaming(true)
				.withDataFormat(DataFormat.PAYLOAD)
				.withMtomEnabled(true)
				.withLoginSecurity(LoginSecurityType.SAML_SENDER_VOUCHES, "dummy", CONSUMER_ENDPOINT_ROLE)
				.withWrappedStyle(true)
				.withSchemaValidationEnabled(true)
				.withWSRM()
				.withWsdlURL("wsdl/ContactService.wsdl");
		return builder.build();
	}

	@Named("asyncCxfConsumer")
	@Produces
	public CxfEndpoint simulatedBackendCxfConsumer() {
		CxfConsumerEndpointBuilder builder = CxfConsumerEndpointBuilder.getNewInstance("/contact", this.context)
				.withAllowStreaming(true)
				.withDataFormat(DataFormat.PAYLOAD)
				.withMtomEnabled(true)
				.withLoginSecurity(LoginSecurityType.SAML_SENDER_VOUCHES, "dummy", CONSUMER_ENDPOINT_ROLE)
				.withWrappedStyle(true)
				.withSchemaValidationEnabled(true)
				.withWSRM()
				.withWsdlURL("wsdl/ContactService.wsdl");
		return builder.build();
	}

}
