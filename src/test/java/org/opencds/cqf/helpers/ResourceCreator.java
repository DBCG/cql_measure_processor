package org.opencds.cqf.helpers;

import org.ajbrown.namemachine.Gender;
import org.ajbrown.namemachine.Name;
import org.ajbrown.namemachine.NameGenerator;
import org.hl7.fhir.dstu3.model.*;
import org.hl7.fhir.dstu3.model.Enumerations.AdministrativeGender;
import org.opencds.cqf.builders.*;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.hl7.fhir.dstu3.model.Enumerations.AdministrativeGender.FEMALE;
import static org.hl7.fhir.dstu3.model.Enumerations.AdministrativeGender.MALE;

public class ResourceCreator {
    private Date createDate(String dateString) {
        LocalDate localeDate = LocalDate.parse(dateString);

        return Date.from(localeDate.atStartOfDay(ZoneId.systemDefault()).toInstant());
    }

    private Map<String, String> getCodeCodeSystemAndDisplayByOperationType(String procedureType) {
        String code, display;
        String codeSystem = "http://snomed.info/sct"; // default code system
        Map<String, String> codeDetails = new HashMap<>();

        switch (procedureType) {
            case "fobt":
                code = "27396-1";
                codeSystem = "http://loinc.org";
                display = "Hemoglobin.gastrointestinal [Mass/mass] in Stool";
                break;
            case "sigmo":
                code = "44441009";
                display = "Flexible fiberoptic sigmoidoscopy";
                break;
            case "colonoscopy":
                code = "447021001";
                display = "Colonoscopy and tattooing";
                break;
            case "systolic":
                code = "8480-6";
                codeSystem = "http://loinc.org";
                display = "Systolic blood pressure";
                break;
            case "diastolic":
                code = "8462-4";
                codeSystem = "http://loinc.org";
                display = "Diastolic blood pressure";
                break;
            case "fit-dna":
                code = "77353-1";
                codeSystem = "http://loinc.org";
                display = "Noninvasive colorectal cancer DNA and occult blood screening [Interpretation] in Stool Narrative";
                break;
            case "colonography":
                code = "418714002";
                display = "Virtual computed tomography colonoscopy";
                break;
            case "colectomy":
                code = "44156";
                codeSystem = "http://www.ama-assn.org/go/cpt";
                display = "Colectomy, total, abdominal, with proctectomy; with continent ileostomy";
                break;
            default:
                code = "";
                display = "";
                break;
        }

        codeDetails.put("code", code);
        codeDetails.put("codeSystem", codeSystem);
        codeDetails.put("display", display);

        return codeDetails;
    }

    public Procedure createProcedure(String procedureType, long index, Patient patient, Practitioner practitioner, String dateString) {
        ProcedureBuilder procedureBuilder = new ProcedureBuilder();
        String status = "completed";
        String id = "Procedure-" + procedureType + "-" + index + "-" + patient.getId();

        Map<String, String> codeDetails = getCodeCodeSystemAndDisplayByOperationType(procedureType);

        String code = codeDetails.get("code");
        String codeSystem = codeDetails.get("codeSystem");
        String display = codeDetails.get("display");

        procedureBuilder.buildCode(code, codeSystem, display);
        procedureBuilder.buildCategory(code, codeSystem);
        procedureBuilder.buildId(id);
        procedureBuilder.buildStatus(status);
        procedureBuilder.buildPerformedPeriod(createDate(dateString), createDate(dateString));
        procedureBuilder.buildSubject(patient);
        procedureBuilder.buildPerformer(practitioner);

        return procedureBuilder.build();
    }

    public Patient createPatient(String id, String birthDateString, Practitioner practitioner) {
        NameGenerator generator = new NameGenerator();

        Name name = generator.generateName();
        Gender nameMachineGender = name.getGender();
        AdministrativeGender gender = nameMachineGender.equals(Gender.MALE) ? MALE : FEMALE;
        PatientBuilder builder = new PatientBuilder();
        HumanName humanName = new HumanName();
        Date birthDate = createDate(birthDateString);
        List<StringType> givenNames = Stream.of(new StringType(name.getFirstName())).collect(Collectors.toList());

        humanName.setGiven(givenNames);
        humanName.setFamily(name.getLastName());

        builder.buildId("Patient-" + id);
        builder.buildName(humanName);
        builder.buildGender(gender);
        builder.buildBirthDate(birthDate);
        builder.buildGeneralPractitioner(practitioner);

        return builder.build();
    }

    public Practitioner createPractitioner(String id, String birthDateString) {
        NameGenerator generator = new NameGenerator();

        Name name = generator.generateName();
        Gender nameMachineGender = name.getGender();
        AdministrativeGender gender = nameMachineGender.equals(Gender.MALE) ? MALE : FEMALE;
        PractitionerBuilder builder = new PractitionerBuilder();
        HumanName humanName = new HumanName();
        Date birthDate = createDate(birthDateString);
        List<StringType> givenNames = Stream.of(new StringType(name.getFirstName())).collect(Collectors.toList());

        humanName.setGiven(givenNames);
        humanName.setFamily(name.getLastName());

        builder.buildId("Practitioner-" + id);
        builder.buildName(humanName);
        builder.buildGender(gender);
        builder.buildBirthDate(birthDate);

        return builder.build();
    }

    protected Observation createObservation(String observationType, long index, Patient patient, Practitioner practitioner, String dateString) {
        ObservationBuilder observationBuilder = new ObservationBuilder();
        String status = "final";
        String id = "Observation-" + observationType + "-" + index + "-" + patient.getId();

        Map<String, String> codeDetails = getCodeCodeSystemAndDisplayByOperationType(observationType);

        String code = codeDetails.get("code");
        String codeSystem = codeDetails.get("codeSystem");
        String display = codeDetails.get("display");

        observationBuilder.buildCode(code, codeSystem, display);
        observationBuilder.buildId(id);
        observationBuilder.buildStatus(status);
        observationBuilder.buildEffectiveDateTime(createDate(dateString));
        observationBuilder.buildSubject(patient);
        observationBuilder.buildPerformer(practitioner);

        return observationBuilder.build();
    }

    protected Condition createCondition(Patient patient, Practitioner practitioner, long index) {
        ConditionBuilder conditionBuilder = new ConditionBuilder();
        String clinicalStatus = "active";
        String verificationStatus = "confirmed";
        String code = "363414004";
        String display = "Malignant tumor of rectosigmoid junction (disorder)";
        String id = "Condition-" + index + "-" + patient.getId();
        String codeSystem = "http://snomed.info/sct";

        conditionBuilder.buildCode(code, codeSystem, display);
        conditionBuilder.buildId(id);
        conditionBuilder.buildClinicalStatus(clinicalStatus);
        conditionBuilder.buildVerificationStatus(verificationStatus);
        conditionBuilder.buildSubject(patient);
        conditionBuilder.buildAsserter(practitioner);

        return conditionBuilder.build();
    }

    public Encounter createEncounter( Patient patient, int index ) {
        Encounter encounter = (Encounter) new Encounter()
            .setStatus(Encounter.EncounterStatus.INPROGRESS)
            .setSubject( new Reference().setReference( "Patient/"+patient.getId()) )
            .setId("Encounter"+index+"-"+patient.getId());
        return encounter;
    }
}
