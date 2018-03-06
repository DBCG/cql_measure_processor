package org.opencds.cqf.builders;

import org.hl7.fhir.dstu3.model.*;
import org.hl7.fhir.exceptions.FHIRException;

import java.util.ArrayList;
import java.util.List;

public class ConditionBuilder extends BaseBuilder<Condition> implements IBuilderHelper {

    public ConditionBuilder() {
        super(new Condition());
    }

    public ConditionBuilder buildId(String id) {
        complexProperty.setId(id);
        return this;
    }

    public ConditionBuilder buildIdentifier(List<Identifier> identifiers) {
        complexProperty.setIdentifier(identifiers);
        return this;
    }

    public ConditionBuilder buildIdentifier(Identifier identifier) {
        if (!complexProperty.hasIdentifier()) {
            complexProperty.setIdentifier(new ArrayList<>());
        }

        complexProperty.addIdentifier(identifier);
        return this;
    }

    public ConditionBuilder buildClinicalStatus(String status) {
        Condition.ConditionClinicalStatus clinicalStatus;

        try {
            clinicalStatus = Condition.ConditionClinicalStatus.fromCode(status);
        } catch (FHIRException e) {
            clinicalStatus = Condition.ConditionClinicalStatus.valueOf(status);
        }

        complexProperty.setClinicalStatus(clinicalStatus);
        return this;
    }

    public ConditionBuilder buildVerificationStatus(String status) {
        Condition.ConditionVerificationStatus verificationStatus;

        try {
            verificationStatus = Condition.ConditionVerificationStatus.fromCode(status);
        } catch (FHIRException e) {
            verificationStatus = Condition.ConditionVerificationStatus.valueOf(status);
        }

        complexProperty.setVerificationStatus(verificationStatus);
        return this;
    }

    public ConditionBuilder buildCode(String code, String system, String display) {
        complexProperty.setCode(buildCodeableConcept(code, system, display));
        return this;
    }

    public ConditionBuilder buildSubject(Patient patient) {
        Reference reference = new Reference(patient);

        complexProperty.setSubject(reference);
        return this;
    }

    public ConditionBuilder buildAsserter(Practitioner practitioner) {
        Reference reference = new Reference(practitioner);

        complexProperty.setAsserter(reference);
        return this;
    }
}