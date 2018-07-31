package org.opencds.cqf.cds;

import ca.uhn.fhir.model.primitive.IdDt;
import com.google.common.base.Charsets;
import com.google.common.io.Resources;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.opencds.cqf.TestServer;
import org.hl7.fhir.dstu3.model.*;
import org.hl7.fhir.dstu3.model.codesystems.LibraryType;
import org.hl7.fhir.dstu3.model.codesystems.PlanDefinitionType;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.codesystems.ActionType;
import org.junit.Assert;
import org.opencds.cqf.builders.*;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.Assert.*;

public class BmiRequired {
    private static TestServer testServer;
    static String rootDir = "bmi";
    static String planDefinitionId =  "bmiProtocol";
    private static Practitioner practitioner;

    private String cqlString;
    private PlanDefinition planDefinition;
    private PlanDefinition planDefinitionPlain;
    private ActivityDefinition createBmiAd;

    public BmiRequired(TestServer testServer) throws IOException {
        this.testServer = testServer;
        testServer.putResource("general-fhirhelpers-3.json", "FHIRHelpers");
        List<IBaseResource> resources = loadResources();
        for (IBaseResource baseResource : resources) {
            testServer.putResource(baseResource);
        }
    }

    public void testPatientNoData() throws Exception {
        Patient      patient      = new PatientBuilder("BMI-Pa1", "1980-01-15", practitioner ).build() ;
        Encounter    encounter    = new EncounterBuilder( "BMI-Enc", patient ).build();

        testServer.putResource( patient );

        CarePlan carePlan = applyPlanDefinition(patient);
        carePlan.setId("bmiCarePlanHeightWeight");

        RequestGroup requestGroup = getRequestGroup( carePlan );
        assertEquals( 2, requestGroup.getAction().size() );

        testServer.putResource(carePlan);

    }

    public void testPatientWeightData() throws Exception {
        Patient      patient           = new PatientBuilder("BMI-Pa2", "1980-02-15", practitioner ).build() ;
        Encounter    encounter         = new EncounterBuilder( "BMI-Enc", patient ).build();
        Observation  weigthObservation = createWeightObservation( patient, "BMI-Obs-Pa2-1", 80 );

        testServer.putResource( patient );
        testServer.putResource(weigthObservation);

        CarePlan carePlan = applyPlanDefinition(patient);
        carePlan.setId("bmiCarePlanWeight");

        RequestGroup requestGroup = getRequestGroup( carePlan );
        assertEquals( 1, requestGroup.getAction().size() );

        testServer.putResource(carePlan);
    }


    public void testPatientHeightData() throws Exception {
        Patient      patient           = new PatientBuilder("BMI-Pa3", "1980-03-15", practitioner ).build() ;
        Encounter    encounter         = new EncounterBuilder( "BMI-Enc", patient ).build();
        Observation  heigthObservation = createHeightObservation( patient, "BMI-Obs-Pa3-1", 1.80 );

        testServer.putResource( patient );
        testServer.putResource(heigthObservation);

        CarePlan carePlan = applyPlanDefinition(patient);
        carePlan.setId("bmiCarePlanHeight");

        RequestGroup requestGroup = getRequestGroup(carePlan);
        assertEquals( 1, requestGroup.getAction().size() );

        testServer.putResource(carePlan);
    }

    public void testPatientWeightHeightData() throws Exception {
        Patient      patient           = new PatientBuilder("BMI-Pa4", "1980-04-15", practitioner ).build() ;
        Encounter    encounter         = new EncounterBuilder( "BMI-Enc", patient ).build();
        Observation  weigthObservation = createWeightObservation( patient, "BMI-Obs-Pa4-1", 70 );
        Observation  heigthObservation = createHeightObservation( patient, "BMI-Obs-Pa4-2", 1.70 );

        testServer.putResource( patient );
        testServer.putResource( weigthObservation);
        testServer.putResource( heigthObservation);

        CarePlan carePlan = applyPlanDefinition(patient);
        carePlan.setId("bmiCarePlanHeightWeight");

        RequestGroup requestGroup = getRequestGroup(carePlan);
        assertEquals( 1, requestGroup.getAction().size() );

        testServer.putResource(carePlan);
    }

    private CarePlan applyPlanDefinition(Patient patient) {
        Parameters outParams = testServer.getOurClient()
            .operation()
            .onInstance(new IdDt("PlanDefinition", planDefinitionId ))
            .named("$apply")
            .withParameters(getParameters( patient ))
            .useHttpGet()
            .execute();

        List<Parameters.ParametersParameterComponent> response = outParams.getParameter();

        Assert.assertTrue(!response.isEmpty());

        Resource resource = response.get(0).getResource();

        Assert.assertTrue(resource instanceof CarePlan);

        CarePlan carePlan = (CarePlan) resource;

        assertNotNull( carePlan );
        assertTrue( carePlan.hasActivity() );

        return carePlan;
    }

    private Observation createHeightObservation(Patient patient, String id, double height) {
        Observation observation = new ObservationBuilder()
            .buildId(id)
            .buildEffectiveDateTime( new Date() )
            .buildSubject(patient)
            .buildCode( "8302-2", "http://loinc.org", "Height")
            .build();
        observation.setValue( new Quantity()
            .setValue( height )
            .setCode("m")
            .setUnit("m")
            .setSystem("http://unitsofmeasure.org")
        );
        return observation;
    }

    private Observation createWeightObservation(Patient patient, String id, double weigth ) {
        Observation observation = new ObservationBuilder()
            .buildId(id)
            .buildEffectiveDateTime( new Date() )
            .buildSubject(patient)
            .buildCode( "27113001", "http://snomed.info/sct", "Weight")
            .build();
        observation.setValue( new Quantity()
            .setValue( weigth )
            .setCode("kg")
            .setUnit("kg")
            .setSystem("http://unitsofmeasure.org")
        );
        return observation;
    }

    private Observation createBmiObservation(Patient patient, String id, double bmi ) {
        Observation observation = new ObservationBuilder()
            .buildId(id)
            .buildEffectiveDateTime( new Date() )
            .buildSubject(patient)
            .buildCode( "39156-5", "http://loinc.org", "BMI observation")
            .build();
        observation.setValue( new Quantity()
            .setValue( bmi )
            .setCode("kg/m2")
            .setUnit("kg/m2")
            .setSystem("http://unitsofmeasure.org")
        );
        return observation;
    }

    private RequestGroup getRequestGroup(CarePlan carePlan) {
        assertTrue( carePlan.hasActivity() );
        Reference reference = carePlan.getActivity().get(0).getReference();

        Resource resource = getContainedResource( carePlan, reference.getReference() );
        assertTrue( resource instanceof  RequestGroup );
        RequestGroup requestGroup = (RequestGroup)resource;

        return requestGroup;
    }

    private Resource getContainedResource(CarePlan carePlan, String reference) {
        List<Resource> resources = carePlan.getContained().stream()
                .filter( resource -> resource.hasId() )
                .filter( resource -> resource.getId().equals(reference))
                .collect(Collectors.toList());
        assertEquals( 1, resources.size());

        return resources.get(0);
    }

    ///////////////////////////////////////////////////////////////////////

    public static List<IBaseResource> loadResources() throws IOException {
        ArrayList<IBaseResource> resources = new ArrayList<>();
        String baseDir = rootDir;

        practitioner = new PractitionerBuilder("BMI-Pr", "1969-05-01").build();
        resources.add( practitioner );

        //////////////////////////////////////////////////////////
        URL url = Resources.getResource(baseDir+"/bmi_library.cql");
        String cqlString = Resources.toString(url, Charsets.UTF_8);
        Library library = new LibraryBuilder()
            .buildId("bmi")
            .buildVersion("0.1.0")
            .buildStatus(Enumerations.PublicationStatus.ACTIVE)
            .buildExperimental( true )
            .buildType(LibraryType.LOGICLIBRARY)
            .buildCqlContent( cqlString )
            .build();
        resources.add(library);

        //////////////////////////////////////////////\
        ActivityDefinition createBmiAd =
            new ActivityDefinitionBuilder()
                .buildId( "createBmi")
                .buildNameAndTitle("Creates BMI Observation")
                .buildDescription("Body Mass Index (BMI) is a person's weight in kilograms divided by the square of height" +
                    " in meters. A high BMI can be an indicator of high body fatness. BMI can be used to screen for" +
                    " weight categories that may lead to health problems but it is not diagnostic of the body" +
                    " fatness or health of an individual.")
                .buildLibrary( "Library/"+library.getId() )
                .buildKind( ActivityDefinition.ActivityDefinitionKind.OBSERVATION)
                .buildCqlDynamicValue("Create basic Observation", "$this", "bmi.observationFromCdr")
                .buildCqlDynamicValue("Set Observation value from storage","valueQuantity", "bmi.bmivalue" )
                .build();
        resources.add(createBmiAd);

        //////////////////////////////////////////////\
        ActivityDefinition createHeightPrAd =
            (ActivityDefinition) new ActivityDefinition()
                .setName("Creates Height ProcedureRequest")
                .setTitle("Creates Height ProcedureRequest")
                .setStatus(Enumerations.PublicationStatus.DRAFT)
                .setDescription(" Create a request for a heigth measurement")
                .setKind( ActivityDefinition.ActivityDefinitionKind.PROCEDUREREQUEST)
                .setCode( new CodeableConcept().addCoding( new CodingBuilder().buildLoincCode("8302-2").build()) )
                .addDynamicValue( new ActivityDefinition.ActivityDefinitionDynamicValueComponent()
                    .setDescription("Set required time to now")
                    .setLanguage("text/fhirpath")
                    .setExpression("now()")
                    .setPath("occurrence")
                )
                .setId( "createHeightPrAd");
        resources.add(createHeightPrAd);

        ////////////////////////////////////////////////
        ActivityDefinition createWeightPrAd =
            (ActivityDefinition) new ActivityDefinition()
                .setName("Creates Weight ProcedureRequest")
                .setTitle("Creates Weight ProcedureRequest")
                .setStatus(Enumerations.PublicationStatus.DRAFT)
                .setDescription(" Create a request for a weigth measurement")
                .setKind( ActivityDefinition.ActivityDefinitionKind.PROCEDUREREQUEST)
                .setCode( new CodeableConcept().addCoding( new CodingBuilder().buildLoincCode("8302-2").build()) )
                .addDynamicValue( new ActivityDefinition.ActivityDefinitionDynamicValueComponent()
                    .setDescription("Set required time to now")
                    .setLanguage("text/fhirpath")
                    .setExpression("now()")
                    .setPath("occurrence")
                )
                .setId( "createHeightPrAd");
        resources.add(createHeightPrAd);

        ////////////////////////////////////////////\
        PlanDefinition planDefinition = (PlanDefinition) new PlanDefinition();
        planDefinition.setId(planDefinitionId);
        planDefinition
            .setVersion("0.1.0")
            .setName("BMI required")
            .setTitle("BMI required")
            .setType( new CodeableConcept()
                .addCoding( new Coding()
                    .setCode(PlanDefinitionType.ECARULE.toCode())
                    .setDisplay(PlanDefinitionType.ECARULE.getDisplay())
                    .setSystem(PlanDefinitionType.ECARULE.getSystem())
                )
            )
            .setPublisher("Philips Research")
            .setDescription("Simple sample Plan Definition. It makes sure that all patients have recent BMI observation.")
            .addLibrary( new Reference().setReference("Library/"+library.getId()))
            .addAction( new PlanDefinitionActionBuilder()
                .buildTitle("BMI observation required")
                .buildDescription("Checks whether a BMI is present if possible calculate BMI.")
                .addCondition( new PlanDefinition.PlanDefinitionActionConditionComponent()
                    .setKind( PlanDefinition.ActionConditionKind.APPLICABILITY )
                    .setDescription("No BMI, height and weight present")
                    .setLanguage("text/cql")
                    .setExpression("not(bmi.recentBmiObservation) and bmi.recentHeightMeasurement and bmi.recentWeightMeasurement")
                )
                .buildType( ActionType.CREATE )
                .buildDefinition( "ActivityDefinition/"+createBmiAd.getId())
                .build()
            )
            .addAction( new PlanDefinitionActionBuilder()
                .buildTitle("Height measurement required")
                .buildDescription("Checks whether a height measurement is present, issues procedure request if not.")
                .addCondition( new PlanDefinition.PlanDefinitionActionConditionComponent()
                    .setKind( PlanDefinition.ActionConditionKind.APPLICABILITY )
                    .setDescription("No height present")
                    .setLanguage("text/cql")
                    .setExpression("not(bmi.recentBmiObservation) and not( bmi.recentHeightMeasurement )")
                )
                .buildType( ActionType.CREATE )
                .buildDefinition( "ActivityDefinition/"+createHeightPrAd.getId())
                .build()
            )
            .addAction( new PlanDefinitionActionBuilder()
                .buildTitle("Weight measurement required")
                .buildDescription("Checks whether a weight measurement is present, issues procedure request if not.")
                .addCondition( new PlanDefinition.PlanDefinitionActionConditionComponent()
                    .setKind( PlanDefinition.ActionConditionKind.APPLICABILITY )
                    .setDescription("No weight present")
                    .setLanguage("text/cql")
                    .setExpression("not(bmi.recentBmiObservation) and not( bmi.recentWeightMeasurement )")
                )
                .buildType( ActionType.CREATE )
                .buildDefinition( "ActivityDefinition/"+createWeightPrAd.getId())
                .build()
            );

        resources.add(planDefinition);

        ///////////////////////////////////////////////////////////

        return resources;
    }

    private Parameters getParameters( Patient patient) {
        Parameters parameters = new Parameters();
        parameters.addParameter().setName("patient").setValue(new StringType("Patient/"+patient.getId()));
        parameters.addParameter().setName("practitioner").setValue(new StringType("Practitioner/performance"));
        return parameters;
    }

}
