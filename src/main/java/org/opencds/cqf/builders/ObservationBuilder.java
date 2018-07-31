package org.opencds.cqf.builders;

import org.hl7.fhir.dstu3.model.*;
import org.hl7.fhir.dstu3.model.Observation.ObservationStatus;
import org.hl7.fhir.exceptions.FHIRException;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class ObservationBuilder extends BaseBuilder<Observation>  {

    public ObservationBuilder() {
        super(new Observation());
    }

    public ObservationBuilder buildId(String id) {
        complexProperty.setId(id);
        return this;
    }

    public ObservationBuilder buildIdentifier(List<Identifier> identifiers) {
        complexProperty.setIdentifier(identifiers);
        return this;
    }

    public ObservationBuilder buildIdentifier(Identifier identifier) {
        if (!complexProperty.hasIdentifier()) {
            complexProperty.setIdentifier(new ArrayList<>());
        }

        complexProperty.addIdentifier(identifier);
        return this;
    }

    public ObservationBuilder buildStatus(String procedureStatus) {
        ObservationStatus status;

        try {
            status = ObservationStatus.fromCode(procedureStatus);
        } catch (FHIRException e) {
            status = ObservationStatus.valueOf(procedureStatus);
        }

        complexProperty.setStatus(status);
        return this;
    }

    public ObservationBuilder buildCode(String code, String system, String display) {
        complexProperty.setCode(buildCodeableConcept(code, system, display));
        return this;
    }

    public ObservationBuilder buildEffectiveDateTime(Date date) {
        Type dateType = new DateTimeType(date);

        complexProperty.setEffective(dateType);
        return this;
    }

    public ObservationBuilder buildSubject(Patient patient) {
        Reference reference = new Reference(patient);

        complexProperty.setSubject(reference);
        return this;
    }

    public ObservationBuilder buildPerformer(Practitioner practitioner) {
        Reference reference = new Reference(practitioner);

        List<Reference> performers = new ArrayList<>();

        performers.add(reference);

        complexProperty.setPerformer(performers);
        return this;
    }

    CodeableConcept buildCodeableConcept(String... args) {
        CodingBuilder codingBuilder = new CodingBuilder();
        CodeableConceptBuilder codeableConceptBuilder = new CodeableConceptBuilder();
        String code = args.length > 0 ? args[0] : "";
        String system = args.length > 1 ? args[1] : "";
        String display = args.length > 2 ? args[2] : "";

        codingBuilder.buildCode(code);
        codingBuilder.buildSystem(system);

        if (!display.isEmpty()) {
            codingBuilder.buildDisplay(display);
        }

        codeableConceptBuilder.buildCoding(codingBuilder.build());

        return codeableConceptBuilder.build();
    }
}