package org.opencds.cqf.qdm.resources;

import ca.uhn.fhir.model.api.annotation.ResourceDef;
import org.hl7.fhir.dstu3.model.ResourceType;

@ResourceDef(name="NegativePatientCharacteristicSex", profile="TODO")
public class NegativePatientCharacteristicSex extends PatientCharacteristicSex {
    @Override
    public NegativePatientCharacteristicSex copy() {
        NegativePatientCharacteristicSex retVal = new NegativePatientCharacteristicSex();
        super.copyValues(retVal);

        return retVal;
    }

    @Override
    public ResourceType getResourceType() {
        return null;
    }

    @Override
    public String getResourceName() {
        return "NegativePatientCharacteristicSex";
    }
}
