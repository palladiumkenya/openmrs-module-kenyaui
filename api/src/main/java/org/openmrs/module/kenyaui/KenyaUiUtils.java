/**
 * The contents of this file are subject to the OpenMRS Public License
 * Version 1.0 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://license.openmrs.org
 *
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the
 * License for the specific language governing rights and limitations
 * under the License.
 *
 * Copyright (C) OpenMRS, LLC.  All Rights Reserved.
 */

package org.openmrs.module.kenyaui;

import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.joda.time.DateTime;
import org.joda.time.Period;
import org.joda.time.PeriodType;
import org.joda.time.format.ISODateTimeFormat;
import org.ocpsoft.prettytime.PrettyTime;
import org.openmrs.Concept;
import org.openmrs.ConceptDatatype;
import org.openmrs.ConceptNumeric;
import org.openmrs.Obs;
import org.openmrs.Person;
import org.openmrs.PersonName;
import org.openmrs.User;
import org.openmrs.Visit;
import org.openmrs.api.context.Context;
import org.openmrs.module.appframework.domain.AppDescriptor;
import org.openmrs.module.kenyaui.wrapper.KenyaEMRObsWrapper;
import org.openmrs.ui.framework.fragment.FragmentActionRequest;
import org.openmrs.ui.framework.page.PageRequest;
import org.openmrs.util.OpenmrsUtil;
import org.openmrs.web.WebConstants;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpSession;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * UI utility methods
 */
@Component
public class KenyaUiUtils {

	private static final DateFormat dateFormatter = new SimpleDateFormat(KenyaUiConstants.DATE_FORMAT);

	private static final DateFormat timeFormatter = new SimpleDateFormat(KenyaUiConstants.TIME_FORMAT);

	private static final DateFormat iso8601Formatter = new SimpleDateFormat("yyyy-MM-dd");

	private static final String DURATION_FORMAT = "%02d:%02d:%02d"; // HH:MM:SS

	/**
	 * Sets the notification success message
	 * @param session the session
	 * @param message the message
	 */
	public void notifySuccess(HttpSession session, String message) {
		session.setAttribute(WebConstants.OPENMRS_MSG_ATTR, message);
	}

	/**
	 * Sets the notification error message
	 * @param session the session
	 * @param message the message
	 */
	public void notifyError(HttpSession session, String message) {
		session.setAttribute(WebConstants.OPENMRS_ERROR_ATTR, message);
	}

	/**
	 * Formats a date time
	 * @param date the date
	 * @return the string value
	 */
	public String formatDateTime(Date date) {
		if (date == null) {
			return "";
		}

		return dateFormatter.format(date) + " " + timeFormatter.format(date);
	}

	/**
	 * Formats a date ignoring any time information
	 * @param date the date
	 * @return the string value
	 * @should format date as a string without time information
	 * @should format null date as empty string
	 */
	public String formatDate(Date date) {
		if (date == null) {
			return "";
		}

		return dateFormatter.format(date);
	}

	/**
	 * Formats a date as ISO 8601 for use as a query parameter
	 * @param date the date
	 * @return the string value
	 * @should format null date as empty string
	 */
	public String formatDateParam(Date date) {
		if (date == null) {
			return "";
		}
		if (!dateHasTime(date)) { // don't include time if there isn't any
			return iso8601Formatter.format(date);
		}
		else { // return a full ISO8601 representation
			DateTime dt = new DateTime(date);
			return ISODateTimeFormat.dateTime().print(dt);
		}
	}

	/**
	 * Formats a date, automatically inferring the best format
	 * @param date the date
	 * @return the string value
	 */
	public String formatDateAuto(Date date) {
		if (!dateHasTime(date)) { // don't print time if there isn't any
			return formatDate(date);
		} else if (DateUtils.isSameDay(date, new Date())) { // don't print date if it's today
			return formatTime(date);
		} else {
			return formatDateTime(date);
		}
	}

	/**
	 * Formats a date as a time value only
	 * @param date the date
	 * @return the string value
	 * @should format date as a string without time information
	 * @should format null date as empty string
	 */
	public String formatTime(Date date) {
		if (date == null) {
			return "";
		}

		return timeFormatter.format(date);
	}

	/**
	 * Formats a date interval relative to now
	 * @param date the date relative to now
	 * @return the formatted interval
	 */
	public String formatInterval(Date date) {
		return formatInterval(date, new Date());
	}

	/**
	 * Formats a date interval relative to the given date
	 * @param date the date relative to the given now
	 * @param now the given now
	 * @return the formatted interval
	 */
	public String formatInterval(Date date, Date now) {
		PrettyTime t = new PrettyTime(now);
		return t.format(date);
	}

	/**
	 * Formats a duration
	 * @param time the time in milliseconds
	 * @return the formatted duration
	 */
	public String formatDuration(long time) {
		Period period = new Period(time);
		return String.format(DURATION_FORMAT, period.getHours(), period.getMinutes(), period.getSeconds());
	}

	/**
	 * Formats the dates of the given visit
	 * @param visit the visit
	 * @return the string value
	 */
	public String formatVisitDates(Visit visit) {
		StringBuilder sb = new StringBuilder();

		if (dateHasTime(visit.getStartDatetime())) {
			sb.append(formatDateTime(visit.getStartDatetime()));
		}
		else {
			sb.append(formatDate(visit.getStartDatetime()));
		}

		if (visit.getStopDatetime() != null) {

			// Check if stop is last second of a day, i.e. it's time is not significant
			Calendar stop = Calendar.getInstance();
			stop.setTime(visit.getStopDatetime());
			boolean isLastMoment = stop.get(Calendar.HOUR_OF_DAY) == 23 && stop.get(Calendar.MINUTE) == 59 && stop.get(Calendar.SECOND) == 59;
			boolean isSameDay = (visit.getStopDatetime() != null) && DateUtils.isSameDay(visit.getStartDatetime(), visit.getStopDatetime());

			if (!(isLastMoment && isSameDay)) {
				sb.append(" \u2192 ");

				if (isSameDay) {
					sb.append(formatTime(visit.getStopDatetime()));
				}
				else {
					sb.append(formatDateTime(visit.getStopDatetime()));
				}
			}
		}

		return sb.toString();
	}

	/**
	 * Formats an obs value
	 * @param obs the obs
	 * @return the formatted value
	 */
	public String formatObsValue(Obs obs) {
		ConceptDatatype datatype = obs.getConcept().getDatatype();

		if (datatype.isText()) {
			return "" + obs.getValueText();
		}
		else if (datatype.isNumeric()) {
			// Bug in core means Obs.getConcept() won't always return a ConceptNumeric
			ConceptNumeric numeric = (obs.getConcept() instanceof ConceptNumeric)
			 		? ((ConceptNumeric) obs.getConcept())
					: Context.getConceptService().getConceptNumeric(obs.getConcept().getId());


			String val = !numeric.getAllowDecimal() ? String.valueOf(obs.getValueNumeric()) : String.valueOf(obs.getValueNumeric().intValue());
			
			if (StringUtils.isNotEmpty(numeric.getUnits())) {
				val += " " + numeric.getUnits();
			}

			return val;
		}
		else if (datatype.isCoded()) {
			// TODO use UiUtils.format so concept names are localized
			return obs.getValueCoded().getName().getName();
		}
		else if (datatype.isDateTime()) {
			return formatDateTime(obs.getValueDatetime());
		}
		else if (datatype.isDate()) {
			return formatDate(obs.getValueDate());
		}
		else {
			throw new RuntimeException("Unsupported concept datatype");
		}
	}

	/**
	 * Formats a person name
	 * @param name the name
	 * @return the string value
	 * @should format voided person as empty string
	 */
	public String formatPersonName(PersonName name) {
		if (name != null) {
			List<String> items = new ArrayList<String>();

			if (name.getFamilyName() != null) {
				items.add(name.getFamilyName() + ",");
			}
			if (name.getGivenName() != null) {
				items.add(name.getGivenName());
			}
			if (name.getMiddleName() != null) {
				items.add(name.getMiddleName());
			}
			return OpenmrsUtil.join(items, " ");
		}
		return "";
	}

	/**
	 * Formats a person's name
	 * @param person the person
	 * @return the string value
	 * @should format voided person as empty string
	 * @deprecated use formatPersonName(PersonName name)
	 */
	@Deprecated
	public String formatPersonName(Person person) {
		return formatPersonName(person.getPersonName());
	}

	/**
	 * Formats a person's age
	 * @param person the person
	 * @return the string value
	 */
	public String formatPersonAge(Person person) {
		String prefix = BooleanUtils.isTrue(person.isBirthdateEstimated()) ? "~" : "";
		int ageYears = person.getAge();

		if (ageYears < 1) {
			Period p = new Period(person.getBirthdate().getTime(), System.currentTimeMillis(), PeriodType.yearMonthDay());
			return prefix + p.getMonths() + " month(s), " + p.getDays() + " day(s)";
		}
		else {
			return prefix + ageYears + " year(s)";
		}
	}

	/**
	 * Formats a person's birth date
	 * @param birthdate the birth date
	 * @param estimated whether the date is estimated
	 * @return the string value
	 */
	public String formatPersonBirthdate(Date birthdate, boolean estimated) {
		if (birthdate == null) {
			return "";
		}
		return (estimated ? "approx " : "") + formatDate(birthdate);
	}

	/**
	 * Formats a person's birth date
	 * @param person the person
	 * @return the string value
	 */
	public String formatPersonBirthdate(Person person) {
		return formatPersonBirthdate(person.getBirthdate(), BooleanUtils.isTrue(person.isBirthdateEstimated()));
	}

	/**
	 * Formats a person's gender
	 * @param person the person
	 * @return the string value
	 */
	public String formatPersonGender(Person person) {
		if (person.getGender().equals("M")) {
			return "\u2642 Male";
		}
		else if (person.getGender().equals("F")) {
			return "\u2640 Female";
		}
		return person.getGender();
	}

	/**
	 * Formats a user
	 * @param user the user
	 * @return the string value
	 */
	public String formatUser(User user) {
		return formatPersonName(user.getPerson());
	}

	/**
	 * Formats a number of bytes in a human readable format
	 * @param bytes the number of bytes
	 * @return the string value
	 */
	public String formatBytes(long bytes) {
		final int unit = 1024;
		if (bytes < unit) return bytes + " B";
		int exp = (int) (Math.log(bytes) / Math.log(unit));
		String pre = "KMGTPE".charAt(exp - 1) + ("i");
		return String.format("%.1f %sB", bytes / Math.pow(unit, exp), pre);
	}

	/**
	 * Convenience method to get the current app for the request
	 * @param request the page request
	 * @return the app descriptor
	 */
	public AppDescriptor getCurrentApp(PageRequest request) {
		return (AppDescriptor) request.getRequest().getAttribute(KenyaUiConstants.REQUEST_ATTR_CURRENT_APP);
	}

	/**
	 * Convenience method to get the current app for the request
	 * @param request the fragment action request
	 * @return the app descriptor
	 */
	public AppDescriptor getCurrentApp(FragmentActionRequest request) {
		return (AppDescriptor) request.getHttpRequest().getAttribute(KenyaUiConstants.REQUEST_ATTR_CURRENT_APP);
	}

	/**
	 * Checks if the given date has a time component
	 * @param date the date
	 * @return true if date has time
	 */
	protected boolean dateHasTime(Date date) {
		return !DateUtils.isSameInstant(date, DateUtils.truncate(date, Calendar.DATE));
	}

	/**
	 * Formats KenyaEMRObsWrapper so that concept values and units are displayed accurately
	 * @param o
	 * @return
	 */
	public String formatObsWrapper(KenyaEMRObsWrapper o) {

		if (o == null) {
			return "";
		}
		NumberFormat nf = NumberFormat.getNumberInstance(Context.getLocale());
		DecimalFormat df = (DecimalFormat) nf;
		df.applyPattern("#0.0#####"); // formatting style up to 6 digits
		String formattedValue = null;
		if (o.getConcept() instanceof ConceptNumeric) {
			String units = ((ConceptNumeric) o.getConcept()).getUnits();

			ConceptNumeric cn = (ConceptNumeric) o.getConcept();
			if (cn.getAllowDecimal() == true) {
				double d = o.getValueNumeric();
				int i = (int) d;
				formattedValue =  Integer.toString(i);
			} else {
				formattedValue = df.format(o.getValueNumeric());
			}

			return formattedValue + (org.apache.commons.lang.StringUtils.isNotBlank(units) ? " " + units : "");
		}
		return "";
	}
}