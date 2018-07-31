package org.opencds.cqf.builders;

import org.hl7.fhir.dstu3.model.Encounter;
import org.hl7.fhir.dstu3.model.Patient;
import org.hl7.fhir.dstu3.model.Reference;

public class EncounterBuilder extends BaseBuilder<Encounter>{
    public EncounterBuilder(){ super( new Encounter());}

    public EncounterBuilder(String id, Patient patient) {
        this();
        this.complexProperty.setStatus( Encounter.EncounterStatus.INPROGRESS )
            .setSubject( new Reference().setReference( "Patient/"+patient.getId()) )
            .setId("Encounter-"+id+"-"+patient.getId());
    }
}
