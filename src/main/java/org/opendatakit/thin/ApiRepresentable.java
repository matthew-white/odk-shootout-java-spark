package org.opendatakit.thin;

public interface ApiRepresentable {
	// Returns a representation of the object to serve through the API. This
	// allows us to distinguish between an object and its API representation.
	Object forApi();
}
