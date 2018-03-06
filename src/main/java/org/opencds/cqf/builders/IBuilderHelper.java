package org.opencds.cqf.builders;

import org.hl7.fhir.dstu3.model.CodeableConcept;

public interface IBuilderHelper {
    default CodeableConcept buildCodeableConcept(String... args) {
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
