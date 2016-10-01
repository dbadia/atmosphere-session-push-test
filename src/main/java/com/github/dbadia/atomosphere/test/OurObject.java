package com.github.dbadia.atomosphere.test;

import org.atmosphere.cpr.AtmosphereResource;

public class OurObject {
	private final AtmosphereResource resource;
	private final String correlator;
	private final long wait = 2000;
	private final long triggerAt = computeTriggerAt();
	private SqrlAuthenticationStatus status = SqrlAuthenticationStatus.CORRELATOR_ISSUED;

	public OurObject(final AtmosphereResource resource, final String correlator) {
		super();
		this.resource = resource;
		this.correlator = correlator;
	}

	private long computeTriggerAt() {
		return System.currentTimeMillis() + wait;
	}

	public AtmosphereResource getResource() {
		return resource;
	}

	public String getId() {
		return correlator;
	}

	public SqrlAuthenticationStatus incrementStatusAndResetTime() {
		if (status == null) {
			return null;
		}
		final SqrlAuthenticationStatus toReturn = status;
		if (toReturn.ordinal() == SqrlAuthenticationStatus.values().length - 1) {
			return null;
		} else {
			status = SqrlAuthenticationStatus.values()[toReturn.ordinal() + 1];
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
