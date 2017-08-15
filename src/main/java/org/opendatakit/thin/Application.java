package org.opendatakit.thin;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Application {
	private static final Logger LOGGER =
		LoggerFactory.getLogger(Application.class.getPackage().getName());

	public static Logger logger() {
		return LOGGER;
	}

	public static void main(String[] args) {
		Config.threadPool();
		Config.route();
	}
}
