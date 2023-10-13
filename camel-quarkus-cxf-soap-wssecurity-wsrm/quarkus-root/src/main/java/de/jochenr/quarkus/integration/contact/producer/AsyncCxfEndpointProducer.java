/**
 *
 */
package de.jochenr.quarkus.integration.contact.producer;

import java.io.IOException;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;

import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;

import org.apache.camel.CamelContext;
import org.apache.camel.component.cxf.common.DataFormat;
import org.apache.camel.component.cxf.jaxws.CxfEndpoint;
import org.apache.camel.support.jsse.SSLContextParameters;
import org.apache.camel.support.jsse.TrustManagersParameters;
import org.apache.http.conn.ssl.NoopHostnameVerifier;

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

			CxfEndpoint cxfEndpoint = builder.build();

			 /* this is now need start*/
            SSLContextParameters sslContextParameters = new SSLContextParameters();
            TrustManagersParameters trustManagersParameters = new TrustManagersParameters();
            trustManagersParameters.setTrustManager(createTrustManagers()[0]);
            sslContextParameters.setTrustManagers(trustManagersParameters);
            /* this is now needs end*/
            cxfEndpoint.setSslContextParameters(sslContextParameters);

		cxfEndpoint.setHostnameVerifier(new NoopHostnameVerifier());
		return cxfEndpoint;
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
	

	static TrustManager[] createTrustManagers() {

        TrustManager[] trustManagers = null;

        try {
            KeyStore trustStore = KeyStore.getInstance("pkcs12");
            try (InputStream is = Thread.currentThread().getContextClassLoader()
                    .getResourceAsStream("truststore-client.jks")) {
                trustStore.load(is, "password".toCharArray());

                TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
                tmf.init(trustStore);
                trustManagers = tmf.getTrustManagers();
            }
        } catch (KeyStoreException | NoSuchAlgorithmException | CertificateException
                | IOException e) {
            throw new RuntimeException(e);
        }
        return trustManagers;

    }
}
