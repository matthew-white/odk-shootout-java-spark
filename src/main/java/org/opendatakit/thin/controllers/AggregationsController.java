package org.opendatakit.thin.controllers;

import org.opendatakit.thin.models.Submission;
import spark.Request;
import spark.Response;

public class AggregationsController {
	public static class GetCountsByAge extends Action {
		public GetCountsByAge(Request request, Response response) {
			super(request, response);
		}

		public String body() {
			return jsonResponse(Submission.countsByAge(request().params("formId")));
		}
	}
}
