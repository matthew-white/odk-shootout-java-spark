package org.opendatakit.thin.models;

import org.sql2o.Connection;
import org.sql2o.Query;
import org.sql2o.ResultSetIterable;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.io.StringReader;
import java.util.List;

public class Submission extends AbstractModel {
	private static final TableMetadata TABLE =
		new TableMetadata("submissions", "formId", "instanceId", "xml");

	private static final DocumentBuilder XML_PARSER;
	static {
		try {
			XML_PARSER = DocumentBuilderFactory.newInstance().newDocumentBuilder();
		}
		catch (ParserConfigurationException e) {
			throw new RuntimeException(e);
		}
	}

	private Element xmlRoot;

	public TableMetadata table() {
		return TABLE;
	}

	public String getFormId() {
		return (String) value("formId");
	}

	public void setFormId(String formId) {
		value("formId", formId);
	}

	public String getInstanceId() {
		return (String) value("instanceId");
	}

	public void setInstanceId(String instanceId) {
		value("instanceId", instanceId);
	}

	public Element getXmlRoot() {
		return xmlRoot;
	}

	private void setXmlRoot(String xml) {
		xmlRoot = null;
		if (xml != null) {
			Document document = null;
			InputSource source = new InputSource(new StringReader(xml));
			try {
				document = XML_PARSER.parse(source);
			}
			catch (IOException | SAXException e) {
				// Do nothing: xmlRoot will be set to null.
			}
			if (document != null && document.getChildNodes().getLength() == 1) {
				Node rootNode = document.getChildNodes().item(0);
				if (rootNode instanceof Element)
					xmlRoot = (Element) rootNode;
			}
		}
	}

	private String getXmlFormId() {
		return getXmlRoot() != null ? getXmlRoot().getAttribute("id") : null;
	}

	private String getXmlInstanceId() {
		if (getXmlRoot() != null)
			return getXmlRoot().getAttribute("instanceID");
		else
			return null;
	}

	public String getXml() {
		return (String) value("xml");
	}

	public void setXml(String xml) {
		setXmlRoot(xml);
		value("xml", getXmlRoot() != null ? xml : null);
		setFormId(getXmlFormId());
		setInstanceId(getXmlInstanceId());
	}

	public boolean isValid() {
		boolean valid = true;
		valid = valid && getFormId() != null && !getFormId().isEmpty();
		valid = valid && getInstanceId() != null && !getInstanceId().isEmpty();
		valid = valid && getXml() != null && !getXml().isEmpty();
		valid = valid && getFormId().equals(getXmlFormId());
		valid = valid && getInstanceId().equals(getXmlInstanceId());
		return valid;
	}

	private static class ApiRepresentation {
		private String formId, instanceId, xml;

		// Needed for Gson to deserialize.
		public ApiRepresentation() { }

		public ApiRepresentation(Submission submission) {
			this.formId = submission.getFormId();
			this.instanceId = submission.getInstanceId();
			this.xml = submission.getXml();
		}
	}

	public Object forApi() {
		return new ApiRepresentation(this);
	}

	private interface Queries {
		// Leaving a trailing space so that it's easy to append additional SQL.
		String FOR_FORM_ID = "SELECT * FROM submissions WHERE formId = :formId ";
		String SAMPLE_FOR_FORM_ID = FOR_FORM_ID + "LIMIT 1000 ";
		String FIND_BY_INSTANCE_ID =
			"SELECT * FROM submissions    " +
			"WHERE                        " +
			"    formId = :formId AND     " +
			"    instanceId = :instanceId " ;
	}

	public static ResultSetIterable<Submission> forFormId(Connection connection,
		String formId) {
		Query query = connection
			.createQuery(Queries.FOR_FORM_ID)
			.addParameter("formId", formId);
		log(query, "formId", formId);
		return query.executeAndFetchLazy(Submission.class);
	}

	public static List<Submission> sampleForFormId(String formId) {
		try (Connection connection = connection()) {
			Query query = connection
				.createQuery(Queries.SAMPLE_FOR_FORM_ID)
				.addParameter("formId", formId);
			log(query, "formId", formId);
			return query.executeAndFetch(Submission.class);
		}
	}

	public static Submission findByFormAndInstance(String formId,
		String instanceId) {
		try (Connection connection = connection()) {
			Query query = connection
				.createQuery(Queries.FIND_BY_INSTANCE_ID)
				.addParameter("formId", formId)
				.addParameter("instanceId", instanceId);
			log(query, "formId", formId, "instanceId", instanceId);
			return query.executeAndFetchFirst(Submission.class);
		}
	}
}
