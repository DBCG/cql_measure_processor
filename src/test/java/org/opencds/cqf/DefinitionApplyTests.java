package org.opencds.cqf;

import ca.uhn.fhir.model.primitive.IdDt;
import org.hl7.fhir.dstu3.model.*;
import org.junit.Assert;

import java.util.List;

import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

class DefinitionApplyTests {

    private TestServer server;
    private final String definitionApplyLocation = "definition-apply/";

    DefinitionApplyTests(TestServer server) {
        this.server = server;
        this.server.putResource("general-practitioner.json", "Practitioner-12208");
        this.server.putResource("general-patient.json", "Patient-12214");
        this.server.putResource("general-fhirhelpers-3.json", "FHIRHelpers");
    }

    void PlanDefinitionApplyTest() throws ClassNotFoundException {
        server.putResource(definitionApplyLocation + "plandefinition-apply-library.json", "plandefinitionApplyTest");
        server.putResource(definitionApplyLocation + "plandefinition-apply.json", "apply-example");

        Parameters inParams = new Parameters();
        inParams.addParameter().setName("patient").setValue(new StringType("Patient-12214"));

        Parameters outParams = server.ourClient
                .operation()
                .onInstance(new IdDt("PlanDefinition", "apply-example"))
                .named("$apply")
                .withParameters(inParams)
                .useHttpGet()
                .execute();

        List<Parameters.ParametersParameterComponent> response = outParams.getParameter();

        Assert.assertTrue(!response.isEmpty());

        Resource resource = response.get(0).getResource();

        Assert.assertTrue(resource instanceof CarePlan);

        CarePlan carePlan = (CarePlan) resource;

        assertEquals( 1, carePlan.getContained().size() );
        assertTrue( carePlan.getContained().get(0) instanceof RequestGroup  );
        RequestGroup requestGroup = (RequestGroup)carePlan.getContained().get(0);
        assertEquals( 1, requestGroup.getAction().size() );

        RequestGroup.RequestGroupActionComponent actionComponent = requestGroup.getActionFirstRep();

        assertEquals( "This is a dynamic definition!", actionComponent.getTitle() );
    }

    void ActivityDefinitionApplyTest() {
        server.putResource(definitionApplyLocation + "activitydefinition-apply-library.json", "activityDefinitionApplyTest");
        server.putResource(definitionApplyLocation + "activitydefinition-apply.json", "ad-apply-example");

        Parameters inParams = new Parameters();
        inParams.addParameter().setName("patient").setValue(new StringType("Patient-12214"));

        Parameters outParams = server.ourClient
                .operation()
                .onInstance(new IdDt("ActivityDefinition", "ad-apply-example"))
                .named("$apply")
                .withParameters(inParams)
                .useHttpGet()
                .execute();

        List<Parameters.ParametersParameterComponent> response = outParams.getParameter();

        Assert.assertTrue(!response.isEmpty());

        Resource resource = response.get(0).getResource();

        Assert.assertTrue(resource instanceof ProcedureRequest);

        ProcedureRequest procedureRequest = (ProcedureRequest) resource;

        Assert.assertTrue(procedureRequest.getDoNotPerform());
    }
}
