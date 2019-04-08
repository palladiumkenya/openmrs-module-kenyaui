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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.Concept;
import org.openmrs.Obs;
import org.openmrs.Patient;
import org.openmrs.Person;
import org.openmrs.api.AdministrationService;
import org.openmrs.api.ConceptService;
import org.openmrs.api.context.Context;
import org.openmrs.module.kenyaui.KenyaUiUtils;
import org.openmrs.module.kenyaui.wrapper.KenyaEMRObsWrapper;
import org.openmrs.ui.framework.annotation.FragmentParam;
import org.openmrs.ui.framework.annotation.SpringBean;
import org.openmrs.ui.framework.fragment.FragmentModel;


import java.util.*;

/**
 * Controller for the obsGraphByDate fragment
 */
public class ObsHistoryGraphFragmentController {

	private final Log log = LogFactory.getLog(this.getClass());
	public void controller(@FragmentParam("patient") Patient patient, @FragmentParam("concepts") List<Concept> concepts, FragmentModel model, @SpringBean KenyaUiUtils kenyaUi) {
		if (concepts.size() < 1) {
			throw new IllegalArgumentException("Concept list must be non-empty");
		}

		model.addAttribute("concepts", concepts);
		model.addAttribute("data", getObsAsSeries(patient, concepts));
	}

	/**
	 * Loads the obs for each of the specified concepts for the given person
	 * @param person the person
	 * @param concepts the concepts
	 * @return the map of concepts to lists of obs
	 */
	private Map<Concept, List<KenyaEMRObsWrapper>> getObsAsSeries(Person person, List<Concept> concepts) {
		Map<Concept, List<KenyaEMRObsWrapper>> series = new HashMap<Concept, List<KenyaEMRObsWrapper>>();
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
		List<Obs> ldl = Context.getObsService().getObservationsByPersonAndConcept(person, LDLQuestion);

		for (Obs obs: ldl) {
			if (obs.getConcept().equals(LDLQuestion) && obs.getValueCoded().equals(LDLAnswer)) {
				KenyaEMRObsWrapper obsw = new KenyaEMRObsWrapper(obs.getObsId(), vl, obs.getObsDatetime(), ldl_default_value);
				ldlObs.add(obsw);
			}
		}

		// get ordinary VL obs
		List<Obs> vlObss =  Context.getObsService().getObservationsByPersonAndConcept(person, vl);
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

		//sort VLs

		if (allVls.size() > 1) {
			Collections.sort(allVls, new Comparator<KenyaEMRObsWrapper>() {
				@Override
				public int compare(final KenyaEMRObsWrapper object1, final KenyaEMRObsWrapper object2) {
					return object1.getObsDatetime().compareTo(object2.getObsDatetime());
				}
			});
		}

		if(allVls.size() > 0) {
			series.put(vl, allVls);
		}

		for (Concept concept : concepts) {
			if(!concept.equals(LDLQuestion) && !concept.equals(vl)) {
				List<Obs> otherObs = Context.getObsService().getObservationsByPersonAndConcept(person, concept);
				List<KenyaEMRObsWrapper> l = new ArrayList<KenyaEMRObsWrapper>();
				for (Obs obs : otherObs) {
					KenyaEMRObsWrapper obsw = new KenyaEMRObsWrapper(obs.getObsId(), obs.getConcept(), obs.getObsDatetime(), obs.getValueNumeric());
					l.add(obsw);
				}
				series.put(concept, l);
			}
		}
		return series;
	}
}