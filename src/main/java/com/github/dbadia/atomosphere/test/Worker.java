package com.github.dbadia.atomosphere.test;

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
		try {
			final Iterator<Long> iter = monitorTable.keySet().iterator();
			while (iter.hasNext()) {
				final Long time = iter.next();
				if (System.currentTimeMillis() > time.longValue()) {
					final OurObject ourObject = monitorTable.remove(time);
					final String sessionId = OurAtmosphereHandler.getSessionId(ourObject.getResource());
					final AtmosphereResource r = currentResourceTable.get(sessionId);
					if (r != null) {
						sendAtmostphereResponse(ourObject, ourObject.getStatus());
						// prep the next update
						if (ourObject.incrementStatusAndResetTime() != null) {
							monitorTable.put(ourObject.getTriggerAt(), ourObject);
						}
					}
				}
			}
		} catch (final Throwable t) {
			// Don't let worker thread die
			t.printStackTrace();
		}
	}

	public void sendAtmostphereResponse(final OurObject ourObject,
			final SqrlAuthenticationStatus newAuthStatus) {
		final String sessionId = ourObject.getResource().getRequest().getRequestedSessionId();
		final AtmosphereResource r = currentResourceTable.get(sessionId);
		if (r == null) {
			System.err.println("AtmosphereResource not found for sessionId " + sessionId);
			return;
		}
		final AtmosphereResponse res = r.getResponse();
		System.out.println("Broadcasting to " + sessionId);

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
			final String data = "{ \"author:\":\"" + ourObject.getId() + "\", \"message\" : \""
					+ newAuthStatus.toString() + "\" }";
			res.getWriter().write(data);
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
			System.out.println("Response sent to " + sessionId);
		} catch (final IOException e) {
			e.printStackTrace();
		}
	}

	public void monitorCorrelatorForChange(final OurObject object) {
		monitorTable.put(object.getTriggerAt(), object);
	}

	public void storeLatestResource(final AtmosphereResource resource) {
		final String sessionId = OurAtmosphereHandler.getSessionId(resource);
		System.out.println("Storing sessionId " + sessionId);
		currentResourceTable.put(sessionId, resource);
	}
}
