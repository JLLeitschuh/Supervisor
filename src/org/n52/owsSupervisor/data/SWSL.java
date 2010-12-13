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
package org.n52.owsSupervisor.data;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;

import org.apache.log4j.Logger;
import org.n52.owsSupervisor.IServiceChecker;
import org.n52.owsSupervisor.checkImpl.SirCapabilitiesCheck;
import org.n52.owsSupervisor.checkImpl.SorCapabilitiesCheck;
import org.n52.owsSupervisor.checkImpl.SosCapabilitiesCheck;
import org.n52.owsSupervisor.checkImpl.SosLatestObservationCheck;

/**
 * @author Daniel Nüst
 * 
 */
@SuppressWarnings("unused")
public abstract class SWSL {

	private static Logger log = Logger.getLogger(SWSL.class);

	public static Collection<IServiceChecker> checkers = new ArrayList<IServiceChecker>();

	private static final String EMAIL_DN = "daniel.nuest@uni-muenster.de";

	private static final long EVERY_HALF_HOUR = 1000 * 60 * 30;

	private static final long EVERY_HOUR = 1000 * 60 * 60;

	private static final long EVERY_12_HOURS = 1000 * 60 * 60 * 12;

	private static final long EVERY_24_HOURS = 1000 * 60 * 60 * 24;

	static {
		// WeatherSOS
		try {
			URL weathersos = new URL(
					"http://v-swe.uni-muenster.de:8080/WeatherSOS/sos");
			IServiceChecker checker02 = new SosCapabilitiesCheck(weathersos,
					EMAIL_DN, EVERY_12_HOURS);

			String[] offerings = new String[] { "RAIN_GAUGE", "LUMINANCE",
					"HUMIDITY", "ATMOSPHERIC_PRESSURE",
					"ATMOSPHERIC_TEMPERATURE", "WIND_SPEED", "WIND_DIRECTION" };
			String[] obsProps = new String[] {
					"urn:ogc:def:property:OGC::Precipitation1Hour",
					"urn:ogc:def:property:OGC::Luminance",
					"urn:ogc:def:property:OGC::RelativeHumidity",
					"urn:ogc:def:property:OGC::BarometricPressure",
					"urn:ogc:def:property:OGC::Temperature",
					"urn:ogc:def:property:OGC::WindSpeed",
					"urn:ogc:def:property:OGC::WindDirection" };
			String proc = "urn:ogc:object:feature:OSIRIS-HWS:3d3b239f-7696-4864-9d07-15447eae2b93";
			long maximumAge = 1000 * 60 * 30;

			for (int i = 0; i < offerings.length; i++) {
				IServiceChecker checker01 = new SosLatestObservationCheck(
						weathersos, offerings[i], obsProps[i], proc,
						maximumAge, EMAIL_DN, EVERY_HALF_HOUR);

				checkers.add(checker01);
			}

		} catch (MalformedURLException e) {
			log.error("Could not create URL for checker.", e);
		}

		// SIR @ giv-genesis
		try {
			URL sir = new URL(
					"http://giv-genesis.uni-muenster.de:8080/SIR2/sir");
			IServiceChecker checker03 = new SirCapabilitiesCheck(sir, EMAIL_DN,
					EVERY_12_HOURS);
			checkers.add(checker03);
		} catch (MalformedURLException e) {
			log.error("Could not create URL for checker.", e);
		}

		// SOR @ giv-genesis
		try {
			URL sir = new URL("http://giv-genesis.uni-muenster.de:8080/SOR/sor");
			IServiceChecker checker04 = new SorCapabilitiesCheck(sir, EMAIL_DN,
					EVERY_12_HOURS);
			checkers.add(checker04);
		} catch (MalformedURLException e) {
			log.error("Could not create URL for checker.", e);
		}

	}
}