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
package org.atmosphere.samples.chat;

import java.io.IOException;
import java.util.Date;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.atmosphere.config.service.AtmosphereHandlerService;
import org.atmosphere.cpr.AtmosphereHandler;
import org.atmosphere.cpr.AtmosphereRequest;
import org.atmosphere.cpr.AtmosphereResource;
import org.atmosphere.cpr.AtmosphereResourceEvent;
import org.atmosphere.cpr.AtmosphereResponse;

/**
 * Simple AtmosphereHandler that implement the logic to build a Server Side Events Chat application.
 *
 * @author Jeanfrancois Arcand
 */

@AtmosphereHandlerService(path = "/chat")
public class OurAtmosphereHandler implements AtmosphereHandler {
	private static ScheduledExecutorService scheduledExecutor = Executors.newScheduledThreadPool(1);
	private final Worker processor;

	public OurAtmosphereHandler() {
		processor = new Worker();
		scheduledExecutor.scheduleAtFixedRate(processor, 1, 1, TimeUnit.SECONDS); // TODO: configurable
	}

	@Override
	public void onRequest(final AtmosphereResource resource) throws IOException {

		final AtmosphereRequest req = resource.getRequest();
		System.out.println(resource.uuid());
		// First, tell Atmosphere to allow bi-directional communication by suspending.
		if (req.getMethod().equalsIgnoreCase("GET")) {
			// We are using HTTP long-polling with a timeout
			resource.suspend();
			processor.storeLatestResource(resource);
			// Second, broadcast message to all connected users.
		} else if (req.getMethod().equalsIgnoreCase("POST")) {
			// Post means we're being sent data
			final String message = req.getReader().readLine().trim();
			final OurObject object = new OurObject(resource, message);
			processor.monitorCorrelatorForChange(object);
		}
	}

	@Override
	public void onStateChange(final AtmosphereResourceEvent event) throws IOException {
		final AtmosphereResource r = event.getResource();
		final AtmosphereResponse res = r.getResponse();

		if (r.isSuspended()) {
			final Object o = event.getMessage();
			if (o != null) {
				final String body = event.getMessage().toString();

				// Simple JSON -- Use Jackson for more complex structure
				// Message looks like { "author" : "foo", "message" : "bar" }

				res.getWriter().write("REPLY!");
				switch (r.transport()) {
					case JSONP:
					case LONG_POLLING:
						event.getResource().resume();
						break;
					case WEBSOCKET:
						break;
					case STREAMING:
						res.getWriter().flush();
						break;
				}
			}
		} else if (!event.isResuming()) {
			event.broadcaster().broadcast(new Data("Someone", "say bye bye!").toString());
		}
	}

	@Override
	public void destroy() {
	}

	private final static class Data {

		private final String text;
		private final String author;

		public Data(final String author, final String text) {
			this.author = author;
			this.text = text;
		}

		@Override
		public String toString() {
			return "{ \"text\" : \"" + text + "\", \"author\" : \"" + author + "\" , \"time\" : " + new Date().getTime()
					+ "}";
		}
	}
}