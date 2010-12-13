/*******************************************************************************
Copyright (C) 2010
by 52 North Initiative for Geospatial Open Source Software GmbH

Contact: Andreas Wytzisk
52 North Initiative for Geospatial Open Source Software GmbH
Martin-Luther-King-Weg 24
48155 Muenster, Germany
info@52north.org

This program is free software; you can redistribute and/or modify it under 
the terms of the GNU General Public License version 2 as published by the 
Free Software Foundation.

This program is distributed WITHOUT ANY WARRANTY; even without the implied
WARRANTY OF MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
General Public License for more details.

You should have received a copy of the GNU General Public License along with
this program (see gnu-gpl v2.txt). If not, write to the Free Software
Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA or
visit the Free Software Foundation web page, http://www.fsf.org.

Author: Daniel Nüst
 
 ******************************************************************************/
package org.n52.owsSupervisor.checkImpl;

import java.io.IOException;
import java.net.URL;
import java.text.ParseException;
import java.util.Date;

import javax.xml.namespace.QName;

import net.opengis.gml.AbstractTimeObjectType;
import net.opengis.gml.TimeInstantType;
import net.opengis.gml.TimePositionType;
import net.opengis.ogc.BinaryTemporalOpType;
import net.opengis.ogc.TEqualsDocument;
import net.opengis.om.x10.ObservationCollectionDocument;
import net.opengis.om.x10.ObservationPropertyType;
import net.opengis.sos.x10.GetObservationDocument;
import net.opengis.sos.x10.GetObservationDocument.GetObservation;
import net.opengis.sos.x10.GetObservationDocument.GetObservation.EventTime;
import net.opengis.swe.x101.TimeObjectPropertyType;

import org.apache.log4j.Logger;
import org.apache.xmlbeans.XmlCursor;
import org.apache.xmlbeans.XmlObject;
import org.n52.owsSupervisor.ICheckResult.ResultType;
import org.n52.owsSupervisor.util.Client;
import org.n52.owsSupervisor.util.XmlTools;

/**
 * @author Daniel Nüst
 * 
 */
public class SosLatestObservationCheck extends AbstractServiceCheck {

	private static final String SOS_SERVICE = "SOS";

	private static Logger log = Logger
			.getLogger(SosLatestObservationCheck.class);

	private String observedProp;

	private String off;

	private long checkInterval;

	private long maximumAgeOfObservation;

	private String proc;

	protected static final String POSITIVE_TEXT = "Successfully requested latest observation.";

	protected static final String NEGATIVE_TEXT = "Request for latest observation FAILED.";

	private static final String LATEST_OBSERVATION_VALUE = "latest";

	private static final String TEMP_OP_PROPERTY_NAME = "om:samplingTime";

	private static final String GET_OBS_RESPONSE_FORMAT = "text/xml;subtype=\"om/1.0.0\""; // text/xml;subtype="om/1.0.0

	private static final String SOS_VERSION = "1.0.0";

	private static final QName GET_OBS_RESULT_MODEL = new QName(
			XmlTools.OM_NAMESPACE_URI, "Measurement",
			XmlTools.OM_NAMESPACE_PREFIX);

	/**
	 * 
	 * @param service
	 * @param offering
	 * @param observedProperty
	 * @param procedure
	 * @param maximumAge
	 * @param notifyEmail
	 * @param checkIntervalMillis
	 */
	public SosLatestObservationCheck(URL service, String offering,
			String observedProperty, String procedure, long maximumAge,
			String notifyEmail, long checkIntervalMillis) {
		super(notifyEmail);
		this.off = offering;
		this.observedProp = observedProperty;
		this.checkInterval = checkIntervalMillis;
		this.serviceUrl = service;
		this.maximumAgeOfObservation = maximumAge;
		this.proc = procedure;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.n52.owsSupervisor.IServiceChecker#check()
	 */
	@Override
	public boolean check() {
		// max age
		Date maxAge = new Date(System.currentTimeMillis()
				- this.maximumAgeOfObservation);

		log.debug("Checking for lates observation " + this.off + "/"
				+ this.observedProp + " which must be after " + maxAge);

		this.results.clear();

		// build the request
		GetObservationDocument getObsDoc = buildRequest();

		// send the document
		try {
			XmlObject response = Client.xSendPostRequest(
					this.serviceUrl.toString(), getObsDoc);
			getObsDoc = null;

			// check it!
			if (response instanceof ObservationCollectionDocument) {
				ObservationCollectionDocument obsColl = (ObservationCollectionDocument) response;
				ObservationPropertyType observation = obsColl
						.getObservationCollection().getMemberArray(0);

				TimeObjectPropertyType samplingTime = observation
						.getObservation().getSamplingTime();
				AbstractTimeObjectType timeObj = samplingTime.getTimeObject();

				if (timeObj instanceof TimeInstantType) {
					TimeInstantType timeInstant = (TimeInstantType) timeObj;
					String timeString = timeInstant.getTimePosition()
							.getStringValue();
					log.debug("Parsed response, latest observation was at "
							+ timeString);

					try {
						Date timeToCheck = ISO8601LocalFormat.parse(timeString);
						if (timeToCheck.after(maxAge)) {
							// save the result
							this.results.add(new CheckResultImpl(new Date(),
									this.serviceUrl.toString(), POSITIVE_TEXT
											+ " " + this.observedProp + " "
											+ this.proc, ResultType.POSITIVE));
							return true;
						}

						// to old!
						this.results.add(new CheckResultImpl(new Date(),
								this.serviceUrl.toString(), NEGATIVE_TEXT + " "
										+ this.observedProp + " " + this.proc
										+ " -- latest observation is too old ("
										+ timeString + ")!",
								ResultType.NEGATIVE));
						return false;
					} catch (ParseException e) {
						log.error(
								"Could not parse sampling time " + timeString,
								e);
						this.results.add(new CheckResultImpl(new Date(),
								this.serviceUrl.toString(), NEGATIVE_TEXT + " "
										+ this.observedProp + " " + this.proc
										+ " -- could not parse the given time "
										+ timeString, ResultType.NEGATIVE));
						return false;
					}
				}
				log.warn("Response does not contain time instant, not handling this!");
				this.results
						.add(new CheckResultImpl(
								new Date(),
								this.serviceUrl.toString(),
								NEGATIVE_TEXT
										+ " ... Response did not contain TimeInstant as samplingTime!",
								ResultType.NEGATIVE));
				return false;
			}
			this.results.add(new CheckResultImpl(new Date(), this.serviceUrl
					.toString(), NEGATIVE_TEXT
					+ " ... Response was not the correct document!",
					ResultType.NEGATIVE));
			return false;
		} catch (IOException e) {
			log.error("Could not send request", e);
			this.results.add(new CheckResultImpl(new Date(), this.serviceUrl
					.toString(),
					NEGATIVE_TEXT + " ... Could not send request!",
					ResultType.NEGATIVE));
			return false;
		}
	}

	private GetObservationDocument buildRequest() {
		// build the request
		GetObservationDocument getObs = GetObservationDocument.Factory
				.newInstance();
		GetObservation obs = getObs.addNewGetObservation();
		EventTime eventTime = obs.addNewEventTime();

		// TM_Equals
		TEqualsDocument tmEqDoc = TEqualsDocument.Factory.newInstance();

		BinaryTemporalOpType tmEq = tmEqDoc.addNewTEquals();
		AbstractTimeObjectType timeInstType = tmEq.addNewTimeObject();
		TimeInstantType timeInst = net.opengis.gml.TimeInstantType.Factory
				.newInstance();
		TimePositionType timePos = timeInst.addNewTimePosition();
		timePos.setStringValue(LATEST_OBSERVATION_VALUE);
		timeInstType.set(timeInst);
		tmEq.addNewPropertyName();

		// workaround
		XmlCursor tmEqualsCursor = tmEq.newCursor();
		if (tmEqualsCursor.toChild(new QName("http://www.opengis.net/gml",
				"_TimeObject"))) {
			tmEqualsCursor.setName(new QName("http://www.opengis.net/gml",
					"TimeInstant"));
		}
		XmlCursor tmEqualsCursor2 = tmEq.newCursor();
		if (tmEqualsCursor2.toChild(new QName("http://www.opengis.net/ogc",
				"PropertyName"))) {
			tmEqualsCursor2.setTextValue(TEMP_OP_PROPERTY_NAME);
		}

		eventTime.addNewTemporalOps();

		eventTime.setTemporalOps(tmEqDoc.getTemporalOps());

		XmlCursor cursor = eventTime.newCursor();
		if (cursor.toChild(new QName("http://www.opengis.net/ogc",
				"temporalOps"))) {
			cursor.setName(new QName("http://www.opengis.net/ogc", "TM_Equals"));
		}
		// binOp.set(tmEqDoc);

		// rest
		obs.addProcedure(this.proc);
		obs.setOffering(this.off);
		obs.addObservedProperty(this.observedProp);
		obs.setService(SOS_SERVICE);
		obs.setVersion(SOS_VERSION);
		// obs.addNewFeatureOfInterest().addNewObjectID().setStringValue(foi);
		obs.setResponseFormat(GET_OBS_RESPONSE_FORMAT);
		obs.setResultModel(GET_OBS_RESULT_MODEL);

		return getObs;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.n52.owsSupervisor.IServiceChecker#getCheckIntervalMillis()
	 */
	@Override
	public long getCheckIntervalMillis() {
		return this.checkInterval;
	}

	@Override
	public String getService() {
		return this.serviceUrl.toString();
	}

	@Override
	public String toString() {
		return "SosLatestObservationCheck [" + getService()
				+ ", check interval=" + getCheckIntervalMillis()
				+ ", offering/bserved property/procedure=" + this.observedProp
				+ "/" + this.off + "/" + this.proc + ", maximum age="
				+ this.maximumAgeOfObservation + "]";
	}
}