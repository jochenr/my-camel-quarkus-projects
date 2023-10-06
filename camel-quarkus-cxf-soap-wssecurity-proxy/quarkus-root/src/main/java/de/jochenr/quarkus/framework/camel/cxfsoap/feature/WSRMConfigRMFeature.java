/**
 *
 */
package de.jochenr.quarkus.framework.camel.cxfsoap.feature;

import org.apache.cxf.ws.rm.feature.RMFeature;
import org.apache.cxf.ws.rm.manager.AcksPolicyType;
import org.apache.cxf.ws.rm.manager.DeliveryAssuranceType;
import org.apache.cxf.ws.rm.manager.DeliveryAssuranceType.ExactlyOnce;
import org.apache.cxf.ws.rm.manager.DestinationPolicyType;
import org.apache.cxf.ws.rm.manager.RetryPolicyType;
import org.apache.cxf.ws.rm.manager.SequenceTerminationPolicyType;
import org.apache.cxf.ws.rm.manager.SourcePolicyType;
import org.apache.cxf.ws.rmp.v200502.RMAssertion;
import org.apache.cxf.ws.rmp.v200502.RMAssertion.AcknowledgementInterval;

public class WSRMConfigRMFeature extends RMFeature {

	// 0 müsste nun "richtig" funktionieren: https://issues.apache.org/jira/browse/CXF-5187
	protected static final int WS_RM_MAX_RETRIES = 0; // ACHTUNG: A-MQ versucht ja den "richtigen" Retry....

	/**
	 *
	 */
	public WSRMConfigRMFeature() {
		super();
		this.enabled = true;
		this.init();
	}


	/**
	 * So geht es später mit Apache CXF 3 Bei der 2.7er Version von CXF ist "org.apache.cxf.ws.rm.feature.RMFeature" dummerweise noch keine Subklasse von
	 * "javax.xml.ws.WebServiceFeature"
	 *
	 * Siehe https://docs.jboss.org/author/display/JBWS/WS-Reliable+Messaging
	 *
	 * @return
	 */
	protected void init() {

		RMAssertion rma = new RMAssertion();
		RMAssertion.BaseRetransmissionInterval bri = new RMAssertion.BaseRetransmissionInterval();
		bri.setMilliseconds(4000L);
		rma.setBaseRetransmissionInterval(bri);
		AcknowledgementInterval ai = new AcknowledgementInterval();
		ai.setMilliseconds(2000L);
		rma.setAcknowledgementInterval(ai);
		super.setRMAssertion(rma);

		SourcePolicyType sp = new SourcePolicyType();
		sp.setIncludeOffer(false);
		RetryPolicyType retryP = new RetryPolicyType();
		retryP.setMaxRetries(WS_RM_MAX_RETRIES);
		sp.setRetryPolicy(retryP);
		/*
		 * To enforce the use of a separate sequence per application message configure the WS-RM source’s sequence termination policy (setting the maximum
		 * sequence length to 1). aus:
		 * https://access.redhat.com/documentation/en-US/Red_Hat_JBoss_Fuse/6.0/html/Configuring_Web_Service_Endpoints/files/CXFDeployWSRMIntercept.html
		 */
		SequenceTerminationPolicyType sequenceTerminationPolicyType = new SequenceTerminationPolicyType();
		sequenceTerminationPolicyType.setMaxLength(1);
		sequenceTerminationPolicyType.setMaxUnacknowledged(0);
		sp.setSequenceTerminationPolicy(sequenceTerminationPolicyType);

		super.setSourcePolicy(sp);

		DestinationPolicyType dp = new DestinationPolicyType();
		AcksPolicyType ap = new AcksPolicyType();
		ap.setIntraMessageThreshold(0);
		dp.setAcksPolicy(ap);
		super.setDestinationPolicy(dp);

		DeliveryAssuranceType deliveryAssurance = new DeliveryAssuranceType();
		deliveryAssurance.setExactlyOnce(new ExactlyOnce());
		// deliveryAssurance.setInOrder(new InOrder()); // wird nur in Kombination mit den anderen, NICHT ExactlyOnce verwendet
		super.setDeliveryAssurance(deliveryAssurance);

	}
}
