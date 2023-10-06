/**
 *
 */
package de.jochenr.quarkus.framework.camel.cxfsoap.security;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.security.auth.callback.CallbackHandler;

import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.interceptor.Interceptor;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.apache.cxf.phase.Phase;
import org.apache.cxf.ws.security.SecurityConstants;
import org.apache.cxf.ws.security.wss4j.PolicyBasedWSS4JStaxOutInterceptor;
import org.apache.cxf.ws.security.wss4j.WSS4JOutInterceptor;

public class CxfProducerSAMLSenderVouchesOutInterceptor<T extends Message> extends AbstractPhaseInterceptor<Message> {

	public CxfProducerSAMLSenderVouchesOutInterceptor() {
		super(Phase.SETUP);
	}

	@Override
	public void handleMessage(Message message) throws Fault {

		Map<String, Object> outProps = new HashMap<String, Object>();

		CallbackHandler callbackHandler = (CallbackHandler) message.get(SecurityConstants.SAML_CALLBACK_HANDLER);
		outProps.put(SecurityConstants.SAML_CALLBACK_HANDLER, callbackHandler);

		/*
	     * Find the WSS4J interceptor in the interceptor chain and set the configuration properties
	     */
	    for (Interceptor<? extends Message> interceptor : message.getInterceptorChain()) {
	        //set properties for WSS4JOutInterceptor
	    	if (interceptor instanceof PolicyBasedWSS4JStaxOutInterceptor) {
	    		PolicyBasedWSS4JStaxOutInterceptor wss4jInterceptor = (PolicyBasedWSS4JStaxOutInterceptor) interceptor;
	    		Set<Entry<String, Object>> outSet = outProps.entrySet();
	    		for (Entry<String, Object> entry : outSet) {
	    			wss4jInterceptor.setProperty(message, entry.getKey(), entry.getValue());
	    		}
				break;
			}
	    	if (interceptor instanceof WSS4JOutInterceptor) {
	    		WSS4JOutInterceptor wss4jInterceptor = (WSS4JOutInterceptor) interceptor;
	    		wss4jInterceptor.setProperties(outProps);
	    		break;
	    	}
	    }
	}
}
