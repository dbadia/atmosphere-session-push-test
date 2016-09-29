package org.atmosphere.samples.chat;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import org.atmosphere.cpr.AtmosphereResource;
import org.atmosphere.cpr.Broadcaster;

public class Worker implements Runnable {
	private final Map<Long, OurObject> monitorTable = new ConcurrentHashMap<>();
	private final Map<String, Broadcaster> currentResourceTable = new ConcurrentHashMap<>();

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
		final Broadcaster broadcaster = currentResourceTable.get(uuid);
		System.out.println("Broadcasting to " + uuid);
		final Future<Object> future = broadcaster.broadcast(newAuthStatus);
		try {
			future.get();
			System.out.println("Broadcast complete for " + uuid);
		} catch (final InterruptedException | ExecutionException e) {
			e.printStackTrace();
		}
	}

	public void monitorCorrelatorForChange(final OurObject object) {
		monitorTable.put(object.getTriggerAt(), object);
	}

	public void storeLatestResource(final AtmosphereResource resource, final Broadcaster broadcaster) {
		currentResourceTable.put(resource.uuid(), broadcaster);

	}
}
