package org.opencds.cqf.helpers;

import ca.uhn.fhir.jpa.dao.SearchParameterMap;
import ca.uhn.fhir.rest.api.server.IBundleProvider;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import org.hl7.fhir.dstu3.model.CodeableConcept;
import org.hl7.fhir.dstu3.model.Coding;
import org.hl7.fhir.dstu3.model.OperationOutcome;
import org.hl7.fhir.dstu3.model.Resource;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.opencds.cqf.providers.JpaDataProvider;

import java.util.*;

public class BulkDataHelper {

    public final Set<String> compartmentPatient = new HashSet<>(
            Arrays.asList(
                    "Account", "AdverseEvent", "AllergyIntolerance", "Appointment", "AppointmentResponse",
                    "AuditEvent", "Basic", "BodySite", "CarePlan", "CareTeam", "ChargeItem", "Claim",
                    "ClaimResponse", "ClinicalImpression", "Communication", "CommunicationRequest", "Composition",
                    "Condition", "Consent", "Coverage", "DetectedIssue", "DeviceRequest", "DeviceUseStatement",
                    "DiagnosticReport", "DocumentManifest", "DocumentReference", "EligibilityRequest", "Encounter",
                    "EnrollmentRequest", "EpisodeOfCare", "ExplanationOfBenefit", "FamilyMemberHistory", "Flag",
                    "Goal", "Group", "ImagingManifest", "ImagingStudy", "Immunization", "ImmunizationRecommendation",
                    "ListResource", "MeasureReport", "Media", "MedicationAdministration", "MedicationDispense",
                    "MedicationRequest", "MedicationStatement", "NutritionOrder", "Observation", "Patient", "Person",
                    "Procedure", "ProcedureRequest", "Provenance", "QuestionnaireResponse", "ReferralRequest",
                    "RelatedPerson", "RequestGroup", "ResearchSubject", "RiskAssessment", "Schedule", "Specimen",
                    "SupplyDelivery", "SupplyRequest", "VisionPrescription"
            )
    );

    private JpaDataProvider provider;

    public BulkDataHelper(JpaDataProvider provider) {
        this.provider = provider;
    }

    public List<Resource> resolveResourceList(List<IBaseResource> resourceList) {
        List<Resource> ret = new ArrayList<>();
        for (IBaseResource res : resourceList) {
            Class clazz = res.getClass();
            ret.add((Resource) clazz.cast(res));
        }
        return ret;
    }

    public List<Resource> resolveType(String type, SearchParameterMap searchMap) {
        if (type.equals("List")) {
            type = "ListResource";
        }
        if (compartmentPatient.contains(type)) {
            IBundleProvider bundleProvider = provider.resolveResourceProvider(type).getDao().search(searchMap);
            List<IBaseResource> resources = bundleProvider.getResources(0, 10000);
            return resolveResourceList(resources);
        }
        else {
            throw new IllegalArgumentException("Invalid _type parameter: " + type);
        }
    }

    public OperationOutcome createErrorOutcome(String display) {
        Coding code = new Coding().setDisplay(display);
        return new OperationOutcome().addIssue(
                new OperationOutcome.OperationOutcomeIssueComponent()
                        .setSeverity(OperationOutcome.IssueSeverity.ERROR)
                        .setCode(OperationOutcome.IssueType.PROCESSING)
                        .setDetails(new CodeableConcept().addCoding(code))
        );
    }

    public List<String> getPatientInclusionPath(String dataType) {
        switch (dataType) {
            case "Account":
                return Collections.singletonList("subject");
            case "AdverseEvent":
                return Collections.singletonList("subject");
            case "AllergyIntolerance":
                return Arrays.asList("patient", "recorder", "asserter");
            case "Appointment":
                return Collections.singletonList("actor");
            case "AppointmentResponse":
                return Collections.singletonList("actor");
            case "AuditEvent":
                return Collections.singletonList("patient");
            case "Basic":
                return Arrays.asList("patient", "author");
            case "BodySite":
                return Collections.singletonList("patient");
            case "CarePlan":
                return Arrays.asList("patient", "performer");
            case "CareTeam":
                return Arrays.asList("patient", "participant");
            case "ChargeItem":
                return Collections.singletonList("subject");
            case "Claim":
                return Arrays.asList("patient", "payee");
            case "ClaimResponse":
                return Collections.singletonList("patient");
            case "ClinicalImpression":
                return Collections.singletonList("subject");
            case "Communication":
                return Arrays.asList("subject", "sender", "recipient");
            case "CommunicationRequest":
                return Arrays.asList("subject", "sender", "recipient", "requester");
            case "Composition":
                return Arrays.asList("subject", "author", "attester");
            case "Condition":
                return Arrays.asList("patient", "asserter");
            case "Consent":
                return Collections.singletonList("patient");
            case "Coverage":
                return Arrays.asList("policy-holder", "subscriber", "beneficiary", "payor");
            case "DetectedIssue":
                return Collections.singletonList("patient");
            case "DeviceRequest":
                return Arrays.asList("subject", "requester", "performer");
            case "DeviceUseStatement":
                return Collections.singletonList("subject");
            case "DiagnosticReport":
                return Collections.singletonList("subject");
            case "DocumentManifest":
                return Arrays.asList("subject", "author", "recipient");
            case "DocumentReference":
                return Arrays.asList("subject", "author");
            case "EligibilityRequest":
                return Collections.singletonList("patient");
            case "Encounter":
                return Collections.singletonList("patient");
            case "EnrollmentRequest":
                return Collections.singletonList("subject");
            case "EpisodeOfCare":
                return Collections.singletonList("patient");
            case "ExplanationOfBenefit":
                return Arrays.asList("patient", "payee");
            case "FamilyMemberHistory":
                return Collections.singletonList("patient");
            case "Flag":
                return Collections.singletonList("patient");
            case "Goal":
                return Collections.singletonList("patient");
            case "Group":
                return Collections.singletonList("member");
            case "ImagingManifest":
                return Arrays.asList("patient", "author");
            case "ImagingStudy":
                return Collections.singletonList("patient");
            case "Immunization":
                return Collections.singletonList("patient");
            case "ImmunizationRecommendation":
                return Collections.singletonList("patient");
            case "ListResource":
                return Arrays.asList("subject", "source");
            case "MeasureReport":
                return Collections.singletonList("patient");
            case "Media":
                return Collections.singletonList("subject");
            case "MedicationAdministration":
                return Arrays.asList("patient", "performer", "subject");
            case "MedicationDispense":
                return Arrays.asList("patient", "receiver", "subject");
            case "MedicationRequest":
                return Collections.singletonList("subject");
            case "MedicationStatement":
                return Collections.singletonList("subject");
            case "NutritionOrder":
                return Collections.singletonList("patient");
            case "Observation":
                return Arrays.asList("subject", "performer");
            case "Patient":
                return Collections.singletonList("link");
            case "Person":
                return Collections.singletonList("patient");
            case "Procedure":
                return Arrays.asList("patient", "performer");
            case "ProcedureRequest":
                return Arrays.asList("subject", "performer");
            case "Provenance":
                return Collections.singletonList("patient");
            case "QuestionnaireResponse":
                return Arrays.asList("subject", "author");
            case "ReferralRequest":
                return Arrays.asList("patient", "requester");
            case "RelatedPerson":
                return Collections.singletonList("patient");
            case "RequestGroup":
                return Arrays.asList("subject", "participant");
            case "ResearchSubject":
                return Collections.singletonList("individual");
            case "RiskAssessment":
                return Collections.singletonList("subject");
            case "Schedule":
                return Collections.singletonList("actor");
            case "Specimen":
                return Collections.singletonList("subject");
            case "SupplyDelivery":
                return Collections.singletonList("patient");
            case "SupplyRequest":
                return Collections.singletonList("requester");
            case "VisionPrescription":
                return Collections.singletonList("patient");
            default:
                throw new IllegalArgumentException("DataType: " + dataType + " is not a member of Compartment Patient");
        }
    }
}
