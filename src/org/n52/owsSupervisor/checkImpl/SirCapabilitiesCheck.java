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
import java.util.Date;

import org.apache.log4j.Logger;
import org.apache.xmlbeans.XmlObject;
import org.n52.owsSupervisor.ICheckResult.ResultType;
import org.n52.owsSupervisor.util.Client;
import org.n52.owsSupervisor.util.XmlTools;

import de.uniMuenster.swsl.sir.CapabilitiesDocument;
import de.uniMuenster.swsl.sir.GetCapabilitiesDocument;

/**
 * @author Daniel Nüst
 * 
 */
public class SirCapabilitiesCheck extends OwsCapabilitiesCheck {

	private static Logger log = Logger.getLogger(SirCapabilitiesCheck.class);

	/**
	 * 
	 * @param owsVersion
	 * @param service
	 * @param notifyEmail
	 * @param checkIntervalMillis
	 */
	public SirCapabilitiesCheck(String owsVersion, URL service,
			String notifyEmail, long checkIntervalMillis) {
		super(owsVersion, service, notifyEmail, checkIntervalMillis);
	}

	/**
	 * 
	 * @param service
	 * @param notifyEmail
	 * @param checkIntervalMillis
	 */
	public SirCapabilitiesCheck(URL service, String notifyEmail,
			long checkIntervalMillis) {
		super(service, notifyEmail, checkIntervalMillis);
	}

	/**
	 * 
	 * @param service
	 * @param notifyEmail
	 */
	public SirCapabilitiesCheck(URL service, String notifyEmail) {
		super(service, notifyEmail);
	}

	@Override
	public boolean check() {
		if (log.isDebugEnabled()) {
			log.debug("Checking SOS Capabilities for " + this.serviceUrl);
		}
		
		if (this.version != "1.1") {
			log.error("OWS Version not supported: " + this.version);
			this.results.add(new CheckResultImpl(new Date(), this.serviceUrl
					.toString(), NEGATIVE_TEXT
					+ " ... OWS Version not supported: " + this.version,
					ResultType.NEGATIVE));
			return false;
		}
		
		this.results.clear();

		// create get capabilities document
		GetCapabilitiesDocument getCapDoc = GetCapabilitiesDocument.Factory
				.newInstance(XmlTools.DEFAULT_OPTIONS);
		getCapDoc.addNewGetCapabilities();

		// send the document
		try {
			XmlObject response = Client.xSendPostRequest(
					this.serviceUrl.toString(), getCapDoc);
			// check it!
			if (response instanceof CapabilitiesDocument) {
				CapabilitiesDocument caps = (CapabilitiesDocument) response;
				log.debug("Parsed caps, version: "
						+ caps.getCapabilities().getVersion());

				// save the result
				this.results.add(new CheckResultImpl(new Date(),
						this.serviceUrl.toString(), POSITIVE_TEXT,
						ResultType.POSITIVE));
				return true;
			}
			this.results.add(new CheckResultImpl(new Date(), this.serviceUrl
					.toString(), NEGATIVE_TEXT
					+ " ... Response was not a Capabilities document!",
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
	
	@Override
	public String toString() {
		return "SirCapabilitiesCheck [" + getService() + ", check interval=" + getCheckIntervalMillis() + "]";
	}

}