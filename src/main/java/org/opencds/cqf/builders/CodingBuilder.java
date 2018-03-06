package org.opencds.cqf.builders;

import org.hl7.fhir.dstu3.model.Coding;

public class CodingBuilder extends BaseBuilder<Coding> {

    public CodingBuilder() {
        super(new Coding());
    }

    public CodingBuilder buildSystem(String system) {
        complexProperty.setSystem(system);
        return this;
    }

    public CodingBuilder buildVersion(String version) {
        complexProperty.setVersion(version);
        return this;
    }

    public CodingBuilder buildCode(String code) {
        complexProperty.setCode(code);
        return this;
    }

    public CodingBuilder buildDisplay(String display) {
        complexProperty.setDisplay(display);
        return this;
    }

    public CodingBuilder buildUserSelected(boolean selected) {
        complexProperty.setUserSelected(selected);
        return this;
    }

    public CodingBuilder buildCode(String system, String code, String display) {
        buildCode( system, code);
        complexProperty.setDisplay(display);
        return this;
    }

    public CodingBuilder buildCode(String system, String code) {
        complexProperty.setSystem(system);
        complexProperty.setCode(code);
        return this;
    }

    public CodingBuilder buildSnomedCode(String code, String display) {
        return this.buildCode( "http://snomed.info/sct", code, display );
    }

    public CodingBuilder buildCptCode(String code, String display ) {
        return this.buildCode( "http://www.ama-assn.org/go/cpt", code, display );
    }

    public CodingBuilder buildCptCode(String code, String display, String version) {
        complexProperty.setVersion(version);
        return this.buildCptCode( code, display );
    }

    public CodingBuilder buildLoincCode(String code) {
        return this.buildCode( "http://loinc.org", code  );
    }
}
