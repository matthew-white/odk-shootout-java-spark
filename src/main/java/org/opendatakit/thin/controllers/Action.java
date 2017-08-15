package org.opendatakit.thin.controllers;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.opendatakit.thin.ApiRepresentable;
import spark.Request;
import spark.Response;

import java.io.IOException;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static java.net.HttpURLConnection.HTTP_BAD_REQUEST;

// Action encapsulates a controller action. It is instantiated with Request and
// Response objects, then returns a String response body from body(). It may
// also mutate the Response object.
public abstract class Action {
	private final Request request;
	private final Response response;

	public Action(Request request, Response response) {
		this.request = request;
		this.response = response;
	}

	public Request request() {
		return request;
	}

	public Response response() {
		return response;
	}

	// Returns the response body (the same thing that Route.handle() returns).
	public abstract String body();

	/* ------------------------------------------------------------------------ */
				/* simple responses */

	protected String respond(int status, String body) {
		response.status(status);
		return body;
	}

	protected String badRequest(String body) {
		return respond(HTTP_BAD_REQUEST, body);
	}

	protected String invalidFormat() {
		return badRequest("Invalid format");
	}

	/* ------------------------------------------------------------------------ */
				/* JSON response */

	private static final Gson GSON = new GsonBuilder()
		.setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
		.create();

	private Object transformForJson(ApiRepresentable body) {
		return body.forApi();
	}

	private List<?> transformForJson(List<?> body) {
		return body
			.stream()
			.map(element -> {
				if (element instanceof ApiRepresentable)
					return ((ApiRepresentable) element).forApi();
				else
					return element;
			})
			.collect(Collectors.toList());
	}

	/*
	jsonResponse() attempts to convert an object to JSON. If the object is
	ApiRepresentable, its forApi() method is called before the conversion. If the
	object is a List that contains one or more ApiRepresentable elements,
	jsonResponse() calls forApi() for each such element.

	jsonResponse() also sets the response type to application/json.
	 */
	protected String jsonResponse(Object body) {
		response.type("application/json");
		Object forJson;
		if (body instanceof ApiRepresentable)
			forJson = transformForJson((ApiRepresentable) body);
		else if (body instanceof List)
			forJson = transformForJson((List<?>) body);
		else
			forJson = body;
		return GSON.toJson(forJson);
	}

	/* ------------------------------------------------------------------------ */
				/* format parameter */

	/*
	Some endpoints accept an optional format parameter at the end of the path. For
	example, for the path /data.csv, "csv" is the format parameter. More complex
	paths may contain a parameter directly before the format parameter, for
	example, /submission/:formId:format. Spark doesn't seem to support this out of
	the box. Instead, we specify a single combination parameter, then use
	ParamWithFormat to parse it. For example:

		get("/submission/:formIdWithFormat", FormatConsciousAction::new);

		class FormatConsciousAction extends Action {
			public FormatConsciousAction(Request request, Response response) { ... }

			public String body() {
				ParamWithFormat formIdWithFormat =
					new ParamWithFormat(request().params("formIdWithFormat"));
				String formId = formIdWithFormat.param(),
					format = formIdWithFormat.format();
				...
			}
		}

	Note that ParamWithFormat supports requests both with and without a format
	parameter: "with format" means "with optional format."
	 */
	protected static class ParamWithFormat {
		// Matches a combination parameter that specifies a format parameter, where
		// the format parameter is not blank.
		private static final Pattern SPECIFIES_FORMAT =
			Pattern.compile("(.+)\\.(\\w+)");

		private final String param, format;

		public ParamWithFormat(String combinationParam) {
			if (combinationParam == null)
				throw new NullPointerException();
			Matcher matcher = SPECIFIES_FORMAT.matcher(combinationParam);
			if (matcher.matches()) {
				param = matcher.group(1);
				format = matcher.group(2);
			}
			else {
				param = combinationParam;
				format = null;
			}
		}

		// Returns the first parameter, that is, the combination parameter stripped
		// of the format parameter.
		public String param() {
			return param;
		}

		// Returns the format parameter if there is one and null otherwise.
		public String format() {
			return format;
		}
	}

	/* ------------------------------------------------------------------------ */
				/* streaming */

	public void stream(byte[] b) {
		try {
			response().raw().getOutputStream().write(b);
		}
		catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public void stream(byte[] b, int off, int len) {
		try {
			response().raw().getOutputStream().write(b, off, len);
		}
		catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public void stream(int b) {
		try {
			response().raw().getOutputStream().write(b);
		}
		catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public void stream(CharSequence sequence) {
		stream(sequence.toString().getBytes());
	}
}
