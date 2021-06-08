package org.opencds.cqf.ruler.core.dstu3.helpers;

import org.hl7.fhir.dstu3.model.CodeableConcept;
import org.hl7.fhir.dstu3.model.Coding;
import org.hl7.fhir.dstu3.model.OperationOutcome;

public class OperationOutcomeHelper {

    public static OperationOutcome createErrorOutcome(String display) {
        Coding code = new Coding().setDisplay(display);
        return new OperationOutcome().addIssue(
                new OperationOutcome.OperationOutcomeIssueComponent()
                        .setSeverity(OperationOutcome.IssueSeverity.ERROR)
                        .setCode(OperationOutcome.IssueType.PROCESSING)
                        .setDetails(new CodeableConcept().addCoding(code))
        );
    }
}
