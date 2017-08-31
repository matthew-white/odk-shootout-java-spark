package org.opendatakit.thin.controllers;

import com.google.common.base.CharMatcher;
import org.opendatakit.thin.models.AbstractModel;
import org.opendatakit.thin.models.Submission;
import org.sql2o.Connection;
import org.sql2o.ResultSetIterable;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import spark.Request;
import spark.Response;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

// At least for now, a controller is essentially just a class that encloses
// static nested Action subclasses.
public class SubmissionsController {
	public static class GetFormSubmissions extends Action {
		private static final CharMatcher REQUIRES_QUOTES =
			CharMatcher.anyOf(",\"\r\n");

		public GetFormSubmissions(Request request, Response response) {
			super(request, response);
		}

		private List<String> streamHeaders(Submission template) {
			List<String> headers = new ArrayList<>();
			NodeList children = template.getXmlRoot().getChildNodes();
			boolean first = true;
			for (int i = 0; i < children.getLength(); i++) {
				Node child = children.item(i);
				if (child.getNodeType() != Node.ELEMENT_NODE)
					continue;
				String header = child.getNodeName();
				headers.add(header);
				if (!first)
					stream(',');
				stream(header);
				first = false;
			}
			if (!headers.isEmpty())
				stream('\n');
			return headers;
		}

		// TODO: Handle additional problem characters.
		private CharSequence escapeValue(String value) {
			boolean requiresQuotes = REQUIRES_QUOTES.matchesAnyOf(value);
			StringBuilder escaped = new StringBuilder();
			if (requiresQuotes)
				escaped.append('"');
			escaped.append(value.replace("\"", "\"\""));
			if (requiresQuotes)
				escaped.append('"');
			return escaped;
		}

		private void streamValue(Submission submission, String field) {
			NodeList fieldNodes = submission.getXmlRoot().getElementsByTagName(field);
			if (fieldNodes.getLength() == 0)
				return;
			String value = null;
			NodeList contentNodes = fieldNodes.item(0).getChildNodes();
			if (contentNodes.getLength() == 1) {
				Node contentNode = contentNodes.item(0);
				if (contentNode.getNodeType() == Node.TEXT_NODE)
					value = contentNode.getTextContent();
			}
			else if (contentNodes.getLength() > 1) {
				value = "(repeat group)";
			}
			if (value != null)
				stream(escapeValue(value));
		}

		private void streamSubmission(Submission submission, List<String> fields) {
			boolean first = true;
			for (String field : fields) {
				if (!first)
					stream(',');
				streamValue(submission, field);
				first = false;
			}
			stream('\n');
		}

		private void streamCsv(ResultSetIterable<Submission> submissions) {
			Iterator<Submission> iterator = submissions.iterator();
			if (!iterator.hasNext())
				return;
			Submission first = iterator.next();
			List<String> headers = streamHeaders(first);
			if (headers.isEmpty())
				return;
			streamSubmission(first, headers);
			while (iterator.hasNext())
				streamSubmission(iterator.next(), headers);
		}

		private void streamCsv(String formId) {
			response().type("text/csv");
			try (Connection connection = AbstractModel.connection()) {
				try (ResultSetIterable<Submission> submissions =
					Submission.forFormId(connection, formId)) {
					streamCsv(submissions);
				}
			}
		}

		public String body() {
			ParamWithFormat formIdWithFormat =
				new ParamWithFormat(request().params("formIdWithFormat"));
			String formId = formIdWithFormat.param(),
				format = formIdWithFormat.format();
			if (format == null)
				return jsonResponse(Submission.sampleForFormId(formId));
			else if (format.equals("csv")) {
				streamCsv(formId);
				return "";
			}
			else {
				return invalidFormat();
			}
		}
	}

	public static class GetSubmission extends Action {
		public GetSubmission(Request request, Response response) {
			super(request, response);
		}

		public String body() {
			Submission submission = Submission.findByFormAndInstance(
				request().params("formId"),
				request().params("instanceId")
			);
			if (submission == null)
				return badRequest("Submission not found");
			return jsonResponse(submission);
		}
	}

	public static class Create extends Action {
		public Create(Request request, Response response) {
			super(request, response);
		}

		public String body() {
			Submission submission = new Submission();
			submission.setData(request().body());
			if (submission.save())
				return "";
			else {
				// TODO: Currently we always respond with a 400 error, but the save
				// failure could be a 500 error. Can we use Sql2o to distinguish between
				// the two?
				return badRequest("Failed to save submission");
			}
		}
	}

	public static class Update extends Action {
		public Update(Request request, Response response) {
			super(request, response);
		}

		public String body() {
			Submission submission = Submission.findByFormAndInstance(
				request().params("formId"),
				request().params("instanceId")
			);
			if (submission == null)
				return badRequest("Submission not found");
			submission.setData(request().body());
			return submission.save() ? "" : badRequest("Failed to update submission");
		}
	}
}
