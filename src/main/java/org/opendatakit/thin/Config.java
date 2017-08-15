package org.opendatakit.thin;

import org.sql2o.Sql2o;
import spark.Spark;

public class Config {
	public static class ConfigurationException extends IllegalStateException {
		public ConfigurationException() {
			super();
		}

		public ConfigurationException(String message) {
			super(message);
		}

		public ConfigurationException(Throwable cause) {
			super(cause);
		}

		public ConfigurationException(String message, Throwable cause) {
			super(message, cause);
		}
	}

	public static void threadPool() {
		// TODO: Allow these values to be configured through environment variables.
		Spark.threadPool(16, 2, 30000);
	}

	// TODO: Configure connection pool.
	public static Sql2o newSql2o() {
		String url = System.getenv("DATABASE_URL");
		if (url == null)
			throw new ConfigurationException("DATABASE_URL not set");
		return new Sql2o(
			"jdbc:" + url,
			System.getenv("DATABASE_USERNAME"),
			System.getenv("DATABASE_PASSWORD")
		);
	}

	public static void route() {
		new Router().routeAll();
	}
}
