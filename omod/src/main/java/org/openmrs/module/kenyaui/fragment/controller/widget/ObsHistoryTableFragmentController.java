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
package org.openmrs.module.kenyaui.fragment.controller.widget;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.openmrs.Concept;
import org.openmrs.ConceptNumeric;
import org.openmrs.Obs;
import org.openmrs.Patient;
import org.openmrs.api.AdministrationService;
import org.openmrs.api.ConceptService;
import org.openmrs.api.context.Context;
import org.openmrs.module.kenyaui.KenyaUiUtils;
import org.openmrs.module.kenyaui.wrapper.KenyaEMRObsWrapper;
import org.openmrs.ui.framework.annotation.FragmentParam;
import org.openmrs.ui.framework.annotation.SpringBean;
import org.openmrs.ui.framework.fragment.FragmentModel;
import org.openmrs.util.OpenmrsUtil;

/**
 * Controller for the obsTableByDate fragment
 */
public class ObsHistoryTableFragmentController {

	public void controller(@FragmentParam("patient") Patient patient, @FragmentParam("concepts") List<Concept> concepts, FragmentModel model, @SpringBean KenyaUiUtils kenyaUi) {
		if (concepts.size() < 1) {
			throw new IllegalArgumentException("Concept list must be non-empty");
		}

		model.addAttribute("concepts", concepts);
		ConceptService service = Context.getConceptService();
		AdministrationService as = Context.getAdministrationService();
		Double ldl_default_value = Double.parseDouble(as.getGlobalProperty("kenyaemr.LDL_default_value"));

		Concept vl = service.getConcept(856);
		Concept LDLQuestion = service.getConcept(1305);
		Concept LDLAnswer = service.getConcept(1302);

		//get LDL obs
		//we want to combine all VL obs together while setting LDL value to that of GP (default value for LDL which is numeric)
		List<KenyaEMRObsWrapper> ldlObs = new ArrayList<KenyaEMRObsWrapper>();
		List<KenyaEMRObsWrapper> vlObs = new ArrayList<KenyaEMRObsWrapper>();
		List<KenyaEMRObsWrapper> allVls = new ArrayList<KenyaEMRObsWrapper>();
		List<Obs> ldl = Context.getObsService().getObservationsByPersonAndConcept(patient, LDLQuestion);

		for (Obs obs: ldl) {
			if (obs.getConcept().equals(LDLQuestion) && obs.getValueCoded().equals(LDLAnswer)) {
				KenyaEMRObsWrapper obsw = new KenyaEMRObsWrapper(obs.getObsId(), vl, obs.getObsDatetime(), ldl_default_value);
				ldlObs.add(obsw);
			}
		}

		// get ordinary VL obs
		List<Obs> vlObss =  Context.getObsService().getObservationsByPersonAndConcept(patient, vl);
		for (Obs obs : vlObss) {
			KenyaEMRObsWrapper obsw = new KenyaEMRObsWrapper(obs.getObsId(), obs.getConcept(), obs.getObsDatetime(), obs.getValueNumeric());
			vlObs.add(obsw);
		}

		// merge vl related obs list
		if(vlObs.size() > 0) {
			allVls.addAll(vlObs);
		}

		if(ldlObs.size() > 0) {
			allVls.addAll(ldlObs);
		}

		TableData data = new TableData();
		for (Concept concept : concepts) {
			// make sure only non-vl obs are considered
			if(!concept.equals(LDLQuestion) && !concept.equals(vl)) {
				List<Obs> obss = Context.getObsService().getObservationsByPersonAndConcept(patient, concept);
				for (Obs obs : obss) {
					KenyaEMRObsWrapper obsw = new KenyaEMRObsWrapper(obs.getObsId(), obs.getConcept(), obs.getObsDatetime(), obs.getValueNumeric());
					data.addObs(obsw);
				}
			}
		}
		// add vl obs
		for (KenyaEMRObsWrapper obs : allVls) {
			data.addObs(obs);
		}

		model.addAttribute("data", data);
	}

	/**
	 * Underlying model for the table data
	 */
	public class TableData extends TreeMap<Date, Map<Concept, List<KenyaEMRObsWrapper>>> {

		public TableData() {
			super(new Comparator<Date>() {
				@Override
				public int compare(Date left, Date right) {
					return right.compareTo(left);
				}
			});
		}

		/**
		 * Adds an obs to the table data
		 * @param obs the obs
		 */
		public void addObs(KenyaEMRObsWrapper obs) {
			Concept concept = obs.getConcept();
			Date dateNoTime = OpenmrsUtil.firstSecondOfDay(obs.getObsDatetime());

			Map<Concept, List<KenyaEMRObsWrapper>> allObsDate = get(dateNoTime);
			if (allObsDate == null) {
				allObsDate = new HashMap<Concept, List<KenyaEMRObsWrapper>>();
				put(dateNoTime, allObsDate);
			}
			List<KenyaEMRObsWrapper> obsForConceptOnDate = allObsDate.get(concept);
			if (obsForConceptOnDate == null) {
				obsForConceptOnDate = new ArrayList<KenyaEMRObsWrapper>();
				allObsDate.put(concept, obsForConceptOnDate);
			}

			obsForConceptOnDate.add(obs);
		}
	}
}
