package org.atmosphere.samples.chat;

import org.atmosphere.cpr.AtmosphereResource;

public class OurObject {
	private final AtmosphereResource resource;
	private final String id;
	private long wait = 2000;
	private long triggerAt = computeTriggerAt();
	private SqrlAuthenticationStatus status = SqrlAuthenticationStatus.CORRELATOR_ISSUED;

	public OurObject(final AtmosphereResource resource, final String id) {
		super();
		this.resource = resource;
		this.id = id;
	}

	private long computeTriggerAt() {
		return System.currentTimeMillis() + wait;
	}

	public AtmosphereResource getResource() {
		return resource;
	}

	public String getId() {
		return id;
	}

	public SqrlAuthenticationStatus incrementStatusAndResetTime() {
		if (status == null) {
			return null;
		}
		final SqrlAuthenticationStatus toReturn = status;
		if (toReturn.ordinal() == SqrlAuthenticationStatus.values().length - 1) {
			status = null; // we're done
		} else {
			status = SqrlAuthenticationStatus.values()[toReturn.ordinal() + 1];
			wait *= 2;
			triggerAt = computeTriggerAt();
		}
		return toReturn;
	}

	public SqrlAuthenticationStatus getStatus() {
		return status;
	}

	public long getTriggerAt() {
		return triggerAt;
	}
}
