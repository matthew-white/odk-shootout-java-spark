package org.opendatakit.thin.controllers;

import com.google.common.primitives.Ints;
import org.opendatakit.thin.models.Submission;
import spark.Request;
import spark.Response;

import java.util.List;

// At least for now, a controller is essentially just a class that encloses
// static nested Action subclasses.
public class SubmissionsController {
	public static class GetFormSubmissions extends Action {
		private static final int SUBMISSIONS_PER_PAGE = 100;

		public GetFormSubmissions(Request request, Response response) {
			super(request, response);
		}

		private int page() {
			String pageParam = request().queryParams("page");
			Integer page = pageParam != null ? Ints.tryParse(pageParam) : null;
			return page != null ? page : 1;
		}

		public String body() {
			List<?> submissions = Submission.pageOfFields(
				request().params("formId"),
				request().queryParams("sort"),
				SUBMISSIONS_PER_PAGE,
				page()
			);
			return jsonResponse(submissions);
		}
	}
}
