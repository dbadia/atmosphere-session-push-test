package com.github.dbadia.atomosphere.test;

import java.io.IOException;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.atmosphere.cpr.AtmosphereResource;
import org.atmosphere.cpr.AtmosphereResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Worker implements Runnable {
	private static final Logger logger = LoggerFactory.getLogger(Worker.class);
	private static final String EOL = System.getProperty("line.separator");
	private static final Object MONITOR_TABLE_LOCK = new Object();
	private final Map<Long, OurObject> monitorTable = new ConcurrentHashMap<>();
	private final Map<String, AtmosphereResource> currentResourceTable = new ConcurrentHashMap<>();

	@Override
	public void run() {
		try {
			synchronized (MONITOR_TABLE_LOCK) {
				final Iterator<Long> iter = monitorTable.keySet().iterator();
				while (iter.hasNext()) {
					final Long time = iter.next();
					if (System.currentTimeMillis() > time.longValue()) {
						final OurObject ourObject = monitorTable.get(time);
						final String sessionId = ourObject.getAtmosphereSessionId();
						if (sessionId == null) {
							logger.error("Got null session id for atmosphere UUID {}", ourObject.getAtmosphereSessionId());
						} else {
							final AtmosphereResource resource = currentResourceTable.get(sessionId);
							if (resource != null) {
								if (resource.isCancelled()) {
									// The user is on another page or a new request will come in

									if (currentResourceTable.get(sessionId).equals(resource)) {
										currentResourceTable.remove(sessionId);
									}
								}
								sendAtmostphereResponse(ourObject, ourObject.getStatus());
								ourObject.incrementStatusAndResetTime();
							}
						}
					}
				}
			}
		} catch (final Throwable t) {
			// Don't let worker thread die
			logger.error("Caught error in " + getClass().getSimpleName() + ".run()", t);
		}
	}

	public void sendAtmostphereResponse(final OurObject ourObject,
			final SqrlAuthenticationStatus newAuthStatus) {
		final String sessionId = ourObject.getAtmosphereSessionId();
		final AtmosphereResource resource = currentResourceTable.get(sessionId);
		if (resource == null) {
			logger.error("AtmosphereResource not found for sessionId {}", sessionId);
			return;
		}
		final AtmosphereResponse res = resource.getResponse();

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
			switch (resource.transport()) {
				case JSONP:
				case LONG_POLLING:
					resource.resume();
					break;
				case WEBSOCKET:
					break;
				case SSE: // this is not in the original examples but is necessary for SSE
				case STREAMING:
					res.getWriter().flush();
					break;
				default:
					throw new IOException("Don't know how to handle transport " + resource.transport());
			}
			logger.info("Status update of {} sent to atmosphere sessionId {}", newAuthStatus, sessionId);
		} catch (final IOException e) {
			logger.error("Error sending status update to atmosphere sessionId " + sessionId, e);
		}
	}

	public void stopMonitoringSessionId(final String atmospheresSessionId) {
		synchronized (MONITOR_TABLE_LOCK) {
			final Iterator<OurObject> iter = monitorTable.values().iterator();
			while (iter.hasNext()) {
				final OurObject clientObject = iter.next();
				if (clientObject.getAtmosphereSessionId().equals(atmospheresSessionId)) {
					iter.remove();
					return;
				}
			}
		}
		logger.error("Tried to remove atmosphere session Id {} from monitorTable but it wasn't there",
				atmospheresSessionId);
	}

	public void monitorCorrelatorForChange(final OurObject object) {
		synchronized (MONITOR_TABLE_LOCK) {
			monitorTable.put(object.getTriggerAt(), object);
		}
	}

	public void storeLatestResource(final AtmosphereResource resource) {
		final String sessionId = OurAtmosphereHandler.getSessionId(resource);
		if (sessionId == null) {
			logger.error(
					"atmosphere session id was null for current resource, see https://github.com/Atmosphere/atmosphere/wiki/Enabling-HttpSession-Support or put the following in your web.xml?"
							+ EOL
							+ "    <listener><listener-class>org.atmosphere.cpr.SessionSupport</listener-class></listener>"
							+ EOL
							+
					"    <context-param><param-name>org.atmosphere.cpr.sessionSupport</param-name><param-value>true</param-value></context-param>");
		} else {
			logger.debug("Updating current resource to {} for atmosphere sessionId {}", resource.uuid(), sessionId);
			currentResourceTable.put(sessionId, resource);
		}
	}

}
