package de.jochenr.quarkus.integration.jms;

import org.apache.camel.LoggingLevel;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.model.TransactedDefinition;
import org.apache.camel.model.rest.RestBindingMode;

import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class JmsRouteBuilder extends RouteBuilder {

	private static final String DIRECT_INPUT_ENDPOINT = "direct:input";


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
				.bindingMode(RestBindingMode.auto)
		;

		onException(Exception.class)
			.handled(false)
			.useOriginalMessage()
			.log(LoggingLevel.ERROR, logCategory, "Exception caused by Message:\n"
					+ " \tHeaders: ${headers}\n"
					+ " \tBody: ${body}")
		;


		rest("/say")
			.get("/hello/{greeting}").routeId("consume--get-say-hello")
			.to(DIRECT_INPUT_ENDPOINT)
		;


		

		from(DIRECT_INPUT_ENDPOINT).routeId("send-to-backend-route")

			.log(LoggingLevel.INFO, logCategoryBusiness, "REST-Message received:\n"
					+ " \tHeaders: ${headers}\n"
					+ " \tBody: ${body}")
			
			.setBody(header("greeting"))

			.log(LoggingLevel.INFO, logCategoryBusiness, "BODY now is:\n"
					+ " \tBody: ${body}")

			.removeHeaders("*")

			.to("xajms:queue:xaTestInputQueue?jmsMessageType=Text")

		;

		from("xajms:queue:xaTestInputQueue?jmsMessageType=Text").routeId("route-queue-to-cxf")

			.transacted(TransactedDefinition.PROPAGATION_REQUIRED)

			.log(LoggingLevel.INFO, logCategoryBusiness, "RECEIVED FROM Queue:\n"
					+ " \tBody: ${body}")

		;


		

		// @formatter:on
	}


}
