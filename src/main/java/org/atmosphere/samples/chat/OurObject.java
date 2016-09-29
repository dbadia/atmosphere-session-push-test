package org.atmosphere.samples.chat;

import org.atmosphere.cpr.AtmosphereResource;

public class OurObject {
	private final AtmosphereResource resource;
	private final String id;
	private final long triggerAt = System.currentTimeMillis() + 2000;

	public OurObject(final AtmosphereResource resource, final String id) {
		super();
		this.resource = resource;
		this.id = id;
	}

	public AtmosphereResource getResource() {
		return resource;
	}

	public String getId() {
		return id;
	}

	public long getTriggerAt() {
		return triggerAt;
	}
}
