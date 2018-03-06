package org.opencds.cqf.builders;

import org.hl7.fhir.dstu3.model.*;
import org.hl7.fhir.dstu3.model.Enumerations.AdministrativeGender;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class PatientBuilder extends BaseBuilder<Patient> {

    public PatientBuilder() {
        super(new Patient());
    }

    public PatientBuilder buildId(String id) {
        complexProperty.setId(id);
        return this;
    }

    public PatientBuilder buildIdentifier(List<Identifier> identifiers) {
        complexProperty.setIdentifier(identifiers);
        return this;
    }

    public PatientBuilder buildIdentifier(Identifier identifier) {
        if (!complexProperty.hasIdentifier()) {
            complexProperty.setIdentifier(new ArrayList<>());
        }

        complexProperty.addIdentifier(identifier);
        return this;
    }

    public PatientBuilder buildActive(boolean active) {
        complexProperty.setActive(active);
        return this;
    }

    public PatientBuilder buildName(List<HumanName> names) {
        complexProperty.setName(names);
        return this;
    }

    public PatientBuilder buildName(HumanName name) {
        if (!complexProperty.hasName()) {
            complexProperty.setName(new ArrayList<>());
        }

        complexProperty.addName(name);
        return this;
    }

    public PatientBuilder buildTelecom(List<ContactPoint> references) {
        complexProperty.setTelecom(references);
        return this;
    }

    public PatientBuilder buildTelecom(ContactPoint contactPoint) {
        if (!complexProperty.hasTelecom()) {
            complexProperty.setTelecom(new ArrayList<>());
        }

        complexProperty.addTelecom(contactPoint);
        return this;
    }

    public PatientBuilder buildGender(AdministrativeGender gender) {
        complexProperty.setGender(gender);
        return this;
    }

    public PatientBuilder buildBirthDate(Date date) {
        complexProperty.setBirthDate(date);
        return this;
    }

    public PatientBuilder buildDeceased(Type type) {
        complexProperty.setDeceased(type);
        return this;
    }

    public PatientBuilder buildAddress(List<Address> addresses) {
        complexProperty.setAddress(addresses);
        return this;
    }

    public PatientBuilder buildAddress(Address address) {
        if (!complexProperty.hasAddress()) {
            complexProperty.setAddress(new ArrayList<>());
        }

        complexProperty.addAddress(address);
        return this;
    }

    public PatientBuilder buildMaritalStatus(CodeableConcept status) {
        complexProperty.setMaritalStatus(status);
        return this;
    }

    public PatientBuilder buildMultipleBirth(Type type) {
        complexProperty.setMultipleBirth(type);
        return this;
    }

    public PatientBuilder buildPhoto(List<Attachment> photos) {
        complexProperty.setPhoto(photos);
        return this;
    }

    public PatientBuilder buildPhoto(Attachment photo) {
        if (!complexProperty.hasPhoto()) {
            complexProperty.setPhoto(new ArrayList<>());
        }

        complexProperty.addPhoto(photo);
        return this;
    }

    public PatientBuilder buildGeneralPractitioner(List<Reference> references) {
        complexProperty.setGeneralPractitioner(references);
        return this;
    }

    public PatientBuilder buildGeneralPractitioner(Practitioner practitioner) {
        Reference reference = new Reference(practitioner);

        if (!complexProperty.hasGeneralPractitioner()) {
            complexProperty.setGeneralPractitioner(new ArrayList<>());
        }

        complexProperty.addGeneralPractitioner(reference);
        return this;
    }

    public PatientBuilder buildManagingOrganization(Reference reference) {
        complexProperty.setManagingOrganization(reference);
        return this;
    }
}