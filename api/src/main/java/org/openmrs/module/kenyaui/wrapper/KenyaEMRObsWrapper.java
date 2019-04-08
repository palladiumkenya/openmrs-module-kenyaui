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

package org.openmrs.module.kenyaui.wrapper;

import org.openmrs.Concept;
import java.util.Date;

/**
 * This is a clone of the Obs object. It should only be used for plotting of numeric obs values on patient chart
 * This model acts as proxy to help unify LDL(qualitative) and numeric (quantitative) VL results.
 */
public class KenyaEMRObsWrapper {

    protected Integer obsId;
    protected Concept concept;
    protected Date obsDatetime;
    protected Double valueNumeric;

    public KenyaEMRObsWrapper(Integer obsId, Concept concept, Date obsDatetime, Double valueNumeric) {
        this.obsId = obsId;
        this.concept = concept;
        this.obsDatetime = obsDatetime;
        this.valueNumeric = valueNumeric;
    }

    public Integer getObsId() {
        return obsId;
    }

    public void setObsId(Integer obsId) {
        this.obsId = obsId;
    }

    public Concept getConcept() {
        return concept;
    }

    public void setConcept(Concept concept) {
        this.concept = concept;
    }

    public Date getObsDatetime() {
        return obsDatetime;
    }

    public void setObsDatetime(Date obsDatetime) {
        this.obsDatetime = obsDatetime;
    }

    public Double getValueNumeric() {
        return valueNumeric;
    }

    public void setValueNumeric(Double valueNumeric) {
        this.valueNumeric = valueNumeric;
    }
}
