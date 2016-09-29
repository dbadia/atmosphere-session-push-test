package org.atmosphere.samples.chat;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.atmosphere.cpr.AtmosphereResource;
import org.atmosphere.cpr.BroadcasterFactory;

public class Worker implements Runnable {
	private final Map<Long, OurObject> monitorTable = new ConcurrentHashMap<>();
	private final Map<String, AtmosphereResource> currentResourceTable = new ConcurrentHashMap<>();

	@Override
	public void run() {
		final Iterator<Long> iter = monitorTable.keySet().iterator();
		while (iter.hasNext()) {
			final Long time = iter.next();
			if (time > System.currentTimeMillis()) {
				final OurObject ourObject = monitorTable.remove(time);
				sendAtmostphereResponse(ourObject.getResource().uuid(), SqrlAuthenticationStatus.AUTH_COMPLETE);
			}
		}
	}

	public void sendAtmostphereResponse(final String uuid,
			final SqrlAuthenticationStatus newAuthStatus) {
		final AtmosphereResource resource = currentResourceTable.get(uuid);
		final BroadcasterFactory broadcasterFactory = resource.getAtmosphereConfig().getBroadcasterFactory();
		broadcasterFactory.lookup(resource.uuid()).broadcast(newAuthStatus.toString());
	}

	public void monitorCorrelatorForChange(final OurObject object) {
		monitorTable.put(object.getTriggerAt(), object);
	}

	public void storeLatestResource(final AtmosphereResource resource) {
		currentResourceTable.put(resource.uuid(), resource);

	}
}
