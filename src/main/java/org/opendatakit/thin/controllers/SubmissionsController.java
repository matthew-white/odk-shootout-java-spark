package org.opendatakit.thin.controllers;

import org.opendatakit.thin.models.Submission;
import spark.Request;
import spark.Response;

// At least for now, a controller is essentially just a class that encloses
// static nested Action subclasses.
public class SubmissionsController {
	public static class GetFormSubmissions extends Action {
		public GetFormSubmissions(Request request, Response response) {
			super(request, response);
		}

		public String body() {
			String formId = request().params("formId");
			return jsonResponse(Submission.sampleForFormId(formId));
		}
	}
}
