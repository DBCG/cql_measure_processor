package org.opencds.cqf.builders;

import org.ajbrown.namemachine.Gender;
import org.ajbrown.namemachine.Name;
import org.ajbrown.namemachine.NameGenerator;
import org.hl7.fhir.dstu3.model.*;
import org.hl7.fhir.dstu3.model.Enumerations.AdministrativeGender;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.hl7.fhir.dstu3.model.Enumerations.AdministrativeGender.FEMALE;
import static org.hl7.fhir.dstu3.model.Enumerations.AdministrativeGender.MALE;

public class PractitionerBuilder extends BaseBuilder<Practitioner> {

    // TODO

    public PractitionerBuilder() {
        super(new Practitioner());
    }

    public PractitionerBuilder(String id, String birthDateString) {
        this();
        NameGenerator generator = new NameGenerator();

        Name name = generator.generateName();
        Gender nameMachineGender = name.getGender();
        AdministrativeGender gender = nameMachineGender.equals(Gender.MALE) ? MALE : FEMALE;
        HumanName humanName = new HumanName();
        List<StringType> givenNames = Stream.of(new StringType(name.getFirstName())).collect(Collectors.toList());

        humanName.setGiven(givenNames);
        humanName.setFamily(name.getLastName());

        buildId("Practitioner-" + id);
        buildName(humanName);
        buildGender(gender);

        buildBirthDate( createDate(birthDateString));
    }

    public PractitionerBuilder buildId(String id) {
        complexProperty.setId(id);
        return this;
    }

    public PractitionerBuilder buildIdentifier(List<Identifier> identifiers) {
        complexProperty.setIdentifier(identifiers);
        return this;
    }

    public PractitionerBuilder buildIdentifier(Identifier identifier) {
        if (!complexProperty.hasIdentifier()) {
            complexProperty.setIdentifier(new ArrayList<>());
        }

        complexProperty.addIdentifier(identifier);
        return this;
    }

    public PractitionerBuilder buildActive(boolean active) {
        complexProperty.setActive(active);
        return this;
    }

    public PractitionerBuilder buildName(List<HumanName> names) {
        complexProperty.setName(names);
        return this;
    }

    public PractitionerBuilder buildName(HumanName name) {
        if (!complexProperty.hasName()) {
            complexProperty.setName(new ArrayList<>());
        }

        complexProperty.addName(name);
        return this;
    }

    public PractitionerBuilder buildTelecom(List<ContactPoint> references) {
        complexProperty.setTelecom(references);
        return this;
    }

    public PractitionerBuilder buildTelecom(ContactPoint contactPoint) {
        if (!complexProperty.hasTelecom()) {
            complexProperty.setTelecom(new ArrayList<>());
        }

        complexProperty.addTelecom(contactPoint);
        return this;
    }

    public PractitionerBuilder buildGender(AdministrativeGender gender) {
        complexProperty.setGender(gender);
        return this;
    }

    public PractitionerBuilder buildBirthDate(Date date) {
        complexProperty.setBirthDate(date);
        return this;
    }

    public PractitionerBuilder buildAddress(List<Address> addresses) {
        complexProperty.setAddress(addresses);
        return this;
    }

    public PractitionerBuilder buildAddress(Address address) {
        if (!complexProperty.hasAddress()) {
            complexProperty.setAddress(new ArrayList<>());
        }

        complexProperty.addAddress(address);
        return this;
    }

    public PractitionerBuilder buildPhoto(List<Attachment> photos) {
        complexProperty.setPhoto(photos);
        return this;
    }

    public PractitionerBuilder buildPhoto(Attachment photo) {
        if (!complexProperty.hasPhoto()) {
            complexProperty.setPhoto(new ArrayList<>());
        }

        complexProperty.addPhoto(photo);
        return this;
    }
}