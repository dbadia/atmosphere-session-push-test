package org.atmosphere.samples.chat;

import java.io.IOException;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.atmosphere.cpr.AtmosphereResource;
import org.atmosphere.cpr.AtmosphereResponse;

public class Worker implements Runnable {
	private final Map<Long, OurObject> monitorTable = new ConcurrentHashMap<>();
	private final Map<String, AtmosphereResource> currentResourceTable = new ConcurrentHashMap<>();

	@Override
	public void run() {
		final Iterator<Long> iter = monitorTable.keySet().iterator();
		while (iter.hasNext()) {
			final Long time = iter.next();
			if (System.currentTimeMillis() > time.longValue()) {
				final OurObject ourObject = monitorTable.remove(time);
				sendAtmostphereResponse(ourObject.getResource().uuid(), SqrlAuthenticationStatus.AUTH_COMPLETE);
			}
		}
	}

	public void sendAtmostphereResponse(final String uuid,
			final SqrlAuthenticationStatus newAuthStatus) {
		final AtmosphereResource r = currentResourceTable.get(uuid);
		final AtmosphereResponse res = r.getResponse();
		System.out.println("Broadcasting to " + uuid);

		// @formatter:off
		/*
		 * In general, the Atmosphere documentation recommends using the 
		 * Broadcaster interface. However, we use the the
		 * raw resource api instead for the following reasons: 
		 * 
		 * 1. We need a 1 to 1 broadcast approach. Attempts to do so
		 * 		with 2.5.4 did not work, when we called broadcast, 
		 * 		onStateChange was never invoked
		 * 2. Even if 1. did work, each broadcaster spawns at least 3 
		 * 		threads via executor service.  There are ways around that
		 * 		but it just seems like needless overhead
		 */
		// @formatter:on
		try {
			res.getWriter().write(newAuthStatus.toString());
			switch (r.transport()) {
				case JSONP:
				case LONG_POLLING:
					r.resume();
					break;
				case WEBSOCKET:
					break;
				case SSE: // this is not in the original examples but is necessary for SSE
				case STREAMING:
					res.getWriter().flush();
					break;
				default:
					throw new IOException("Don't know how to handle transport " + r.transport());
			}
			System.out.println("Response sent to " + uuid);
		} catch (final IOException e) {
			e.printStackTrace();
		}
	}

	public void monitorCorrelatorForChange(final OurObject object) {
		monitorTable.put(object.getTriggerAt(), object);
	}

	public void storeLatestResource(final AtmosphereResource resource) {
		currentResourceTable.put(resource.uuid(), resource);

	}
}
