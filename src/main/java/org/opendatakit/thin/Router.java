package org.opendatakit.thin;

import com.google.common.collect.ImmutableSet;
import org.opendatakit.thin.controllers.Action;
import org.opendatakit.thin.controllers.SubmissionsController;
import spark.Request;
import spark.Response;
import spark.Route;
import spark.Spark;

import java.util.Set;
import java.util.function.BiConsumer;

import static spark.Spark.notFound;
import static spark.Spark.path;

public class Router {
	// routeAll() below only specifies Route objects that return Action objects.
	// Those Route objects are instances of ActionRoute, which has a slightly
	// stricter contract than Route.
	private interface ActionRoute extends Route {
		Action handle(Request request, Response response);
	}

	/*
	Multiple paths may have the same canonical path, which is to say, a single
	canonical path may be associated with multiple path versions. pathVersions()
	takes a path (not necessarily canonical) and returns the set of path versions
	for the specified path's canonical path. We use this process to handle
	trailing slashes in paths.
	 */
	private Set<String> pathVersions(String path) {
		if (path == null)
			throw new NullPointerException();
		if (path.endsWith("//"))
			throw new IllegalArgumentException();

		String withSlash, withoutSlash;
		if (path.endsWith("/")) {
			withSlash = path;
			withoutSlash = path.substring(0, path.length() - 1);
		}
		else {
			withSlash = path + '/';
			withoutSlash = path;
		}

		return ImmutableSet.of(withSlash, withoutSlash);
	}

	/*
	route() routes requests with the specified HTTP method and path to an
	ActionRoute. The ActionRoute will return an Action object, which route() first
	converts to String via Action.body(), then returns as the response body.
	httpMethod should be one of the Spark HTTP-method methods, for example,
	Spark::get.
	 */
	private void route(BiConsumer<String, Route> httpMethod, String path,
		ActionRoute actionRoute) {
		Route route = (request, response) ->
			actionRoute.handle(request, response).body();
		for (String version : pathVersions(path))
			httpMethod.accept(version, route);
	}

	private void post(String path, ActionRoute route) {
		route(Spark::post, path, route);
	}

	private void post(ActionRoute route) {
		post("", route);
	}

	private void get(String path, ActionRoute route) {
		route(Spark::get, path, route);
	}

	private void get(ActionRoute route) {
		get("", route);
	}

	private void patch(String path, ActionRoute route) {
		route(Spark::patch, path, route);
	}

	private void patch(ActionRoute route) {
		patch("", route);
	}

	private void delete(String path, ActionRoute route) {
		route(Spark::delete, path, route);
	}

	private void delete(ActionRoute route) {
		delete("", route);
	}

	// Configures all routes. Add new routes here.
	public void routeAll() {
		// All routes should return an Action object. body() is called on the Action
		// object before it is returned as the response body.

		path("/submission", () -> {
			post(SubmissionsController.Create::new);
			get("/:formIdWithFormat", SubmissionsController.GetFormSubmissions::new);

			path("/:formId/:instanceId", () -> {
				get(SubmissionsController.GetSubmission::new);
				patch(SubmissionsController.Update::new);
			});
		});
		notFound("404 Not found");
	}
}
