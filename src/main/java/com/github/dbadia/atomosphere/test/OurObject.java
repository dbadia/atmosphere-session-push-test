package com.github.dbadia.atomosphere.test;

public class OurObject {
	/**
	 * Once the atmosphere reosurce is invalid, the sessionID may not be available, so persist it here
	 */
	private final String atmosphereSessionId;

	public String getAtmosphereSessionId() {
		return atmosphereSessionId;
	}

	/**
	 * The atmosphere UUID of the post data request that sent the correlator. Only useful for tracking
	 */
	private final String dataResourceUuid;
	private final String correlator;
	private final long triggerAt = computeTriggerAt();
	private long wait = 2000;
	private SqrlAuthenticationStatus status = SqrlAuthenticationStatus.CORRELATOR_ISSUED;

	public OurObject(final String dataResourceUuid, final String atmosphereSessionId, final String correlator) {
		super();
		this.dataResourceUuid = dataResourceUuid;
		this.correlator = correlator;
		this.atmosphereSessionId = atmosphereSessionId;
		if (atmosphereSessionId == null) {
			throw new IllegalArgumentException("sessionID cannot be null");
		}
	}

	public String getDataResourceUuid() {
		return dataResourceUuid;
	}

	private long computeTriggerAt() {
		return System.currentTimeMillis() + wait;
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
			wait += 2000;
			computeTriggerAt();
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
