/*
 * Copyright 2016 Async-IO.org
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.atmosphere.samples.test;

import java.io.IOException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.atmosphere.config.service.AtmosphereHandlerService;
import org.atmosphere.cpr.AtmosphereHandler;
import org.atmosphere.cpr.AtmosphereRequest;
import org.atmosphere.cpr.AtmosphereResource;
import org.atmosphere.cpr.AtmosphereResourceEvent;

/**
 * Simple AtmosphereHandler that implement the logic to build a Server Side Events Chat application.
 *
 * @author Jeanfrancois Arcand
 */

@AtmosphereHandlerService(path = "/status")
public class OurAtmosphereHandler implements AtmosphereHandler {
	private static ScheduledExecutorService scheduledExecutor = Executors.newScheduledThreadPool(1);
	private final Worker processor;

	public OurAtmosphereHandler() {
		processor = new Worker();
		scheduledExecutor.scheduleAtFixedRate(processor, 1, 1, TimeUnit.SECONDS);
	}

	@Override
	public void onRequest(final AtmosphereResource resource) throws IOException {
		final AtmosphereRequest req = resource.getRequest();

		System.out
		.println(req.getMethod() + " " + resource.getRequest().getRequestedSessionId() + " " + resource.uuid());
		// First, tell Atmosphere to allow bi-directional communication by suspending.
		if (req.getMethod().equalsIgnoreCase("GET")) {
			resource.suspend();
			processor.storeLatestResource(resource);
		} else if (req.getMethod().equalsIgnoreCase("POST")) {
			// Post means we're being sent data
			final String message = req.getReader().readLine().trim();
			// Simple JSON -- Use Jackson for more complex structure
			// Message looks like { "author" : "foo", "message" : "bar" }
			final String author = message.substring(message.indexOf(":") + 2, message.indexOf(",") - 1);
			final String correlator = message.substring(message.lastIndexOf(":") + 2, message.length() - 2);

			final OurObject object = new OurObject(resource, correlator);
			processor.monitorCorrelatorForChange(object);
		}
	}

	// We don't use broadcast so this is only called when a browser disconnects
	@Override
	public void onStateChange(final AtmosphereResourceEvent event) throws IOException {
		final AtmosphereResource r = event.getResource();

		if (!event.isResuming()) {
			System.out.println("Closed " + r.uuid());
		}
	}

	@Override
	public void destroy() {
	}

	public static String getSessionId(final AtmosphereResource resource) {
		return resource.getRequest().getRequestedSessionId();
	}
}