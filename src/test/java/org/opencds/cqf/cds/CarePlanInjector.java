package org.opencds.cqf.cds;

import org.hl7.fhir.dstu3.model.*;
import org.hl7.fhir.dstu3.model.codesystems.*;
import org.hl7.fhir.utilities.xhtml.XhtmlNode;
import org.junit.Test;
import org.opencds.cqf.TestServer;

import java.util.ArrayList;
import java.util.List;

public class CarePlanInjector {

    @Test
    public void injectCarePlan() throws Exception {
        TestServer testServer = new TestServer();
        testServer.start();
        getResources().stream().forEach(
            resource-> {
                try {
                    testServer.putResource(resource);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        );
        testServer.stop();
    }

    /****************************************************
     *
     */
    List<Resource>  getResources(){
        List<Resource> resources = new ArrayList<>();

        CarePlan carePlan = (CarePlan) new CarePlan();

        Patient patient = (Patient) new Patient()
            .addName( new HumanName()
                .addGiven("The")
                .setFamily("Patient")
            )
            .setId("PatientId");
        carePlan.addContained(patient);

        Procedure procedure = (Procedure) new Procedure()
            .setSubject(new Reference().setReference("Patient/"+patient.getId()))
            .setText( new Narrative().setDiv( new XhtmlNode().setValue("Some Procedure")))
            .setId("someProcedure");
        carePlan.addContained(procedure);

        int n = 0;
        RequestGroup requestGroup = (RequestGroup) new RequestGroup()
            .addAction( new RequestGroup.RequestGroupActionComponent()
                .setTitle("The Title "+n++)
                .setDescription("An action "+n)
            )
            .addAction( new RequestGroup.RequestGroupActionComponent()
                .setTitle("The Title "+n++)
                .setDescription("An action no ")
                .addAction( new RequestGroup.RequestGroupActionComponent()
                    .setTitle("The Title "+n++)
                    .setDescription("An action "+n)
                )
                .addAction( new RequestGroup.RequestGroupActionComponent()
                    .setTitle("The Title "+n++)
                    .setDescription("An action "+n)
                )
            )
            .addAction( new RequestGroup.RequestGroupActionComponent()
                .setTitle("At most one ")
                .setDescription("An action - visualgroup")
                .setGroupingBehavior( RequestGroup.ActionGroupingBehavior.VISUALGROUP )
                .setSelectionBehavior(RequestGroup.ActionSelectionBehavior.ATMOSTONE)
                .addAction( new RequestGroup.RequestGroupActionComponent()
                    .setTitle("The Title "+n++)
                    .setDescription("An action "+n)
                    .addAction( new RequestGroup.RequestGroupActionComponent().setDescription("An action "+ ++n))
                    .addAction( new RequestGroup.RequestGroupActionComponent().setDescription("An action "+ ++n))
                )
                .addAction( new RequestGroup.RequestGroupActionComponent()
                    .setTitle("The Title "+n++)
                    .setDescription("An action - visualgroup")
                    .setGroupingBehavior( RequestGroup.ActionGroupingBehavior.VISUALGROUP )
                    .setSelectionBehavior(RequestGroup.ActionSelectionBehavior.ANY)
                )
                .addAction( new RequestGroup.RequestGroupActionComponent()
                    .setTitle("The Title "+n++)
                    .setDescription("An action - logical group")
                    .setGroupingBehavior( RequestGroup.ActionGroupingBehavior.LOGICALGROUP )
                    .addAction( new RequestGroup.RequestGroupActionComponent().setDescription("An action "+ ++n))
                    .addAction( new RequestGroup.RequestGroupActionComponent().setDescription("An action "+ ++n))
                )
            )
            .addAction( new RequestGroup.RequestGroupActionComponent()
                .setTitle("All or none")
                .setDescription("An action - visualgroup")
                .setGroupingBehavior( RequestGroup.ActionGroupingBehavior.VISUALGROUP )
                .setSelectionBehavior(RequestGroup.ActionSelectionBehavior.ALLORNONE)
                .addAction( new RequestGroup.RequestGroupActionComponent()
                    .setTitle("The Title "+n++)
                    .setDescription("An action "+n)
                    .addAction( new RequestGroup.RequestGroupActionComponent().setDescription("An action "+ ++n))
                    .addAction( new RequestGroup.RequestGroupActionComponent().setDescription("An action "+ ++n))
                )
                .addAction( new RequestGroup.RequestGroupActionComponent()
                    .setTitle("The Title "+n++)
                    .setDescription("An action - visualgroup")
                    .setGroupingBehavior( RequestGroup.ActionGroupingBehavior.VISUALGROUP )
                    .setSelectionBehavior(RequestGroup.ActionSelectionBehavior.ANY)
                )
                .addAction( new RequestGroup.RequestGroupActionComponent()
                    .setTitle("The Title "+n++)
                    .setDescription("An action - logical group")
                    .setGroupingBehavior( RequestGroup.ActionGroupingBehavior.LOGICALGROUP )
                    .addAction( new RequestGroup.RequestGroupActionComponent().setDescription("An action "+ ++n))
                    .addAction( new RequestGroup.RequestGroupActionComponent().setDescription("An action "+ ++n))
                )
            )
            .addAction( new RequestGroup.RequestGroupActionComponent()
                .setTitle("any")
                .setDescription("An action - visualgroup")
                .setGroupingBehavior( RequestGroup.ActionGroupingBehavior.VISUALGROUP )
                .setSelectionBehavior(RequestGroup.ActionSelectionBehavior.ANY)
                .addAction( new RequestGroup.RequestGroupActionComponent()
                    .setTitle("The Title "+n++)
                    .setDescription("An action "+n)
                    .addAction( new RequestGroup.RequestGroupActionComponent().setDescription("An action "+ ++n))
                    .addAction( new RequestGroup.RequestGroupActionComponent().setDescription("An action "+ ++n))
                )
                .addAction( new RequestGroup.RequestGroupActionComponent()
                    .setTitle("The Title "+n++)
                    .setDescription("An action - visualgroup")
                    .setGroupingBehavior( RequestGroup.ActionGroupingBehavior.VISUALGROUP )
                    .setSelectionBehavior(RequestGroup.ActionSelectionBehavior.ANY)
                )
                .addAction( new RequestGroup.RequestGroupActionComponent()
                    .setTitle("The Title "+n++)
                    .setDescription("An action - logical group")
                    .setGroupingBehavior( RequestGroup.ActionGroupingBehavior.LOGICALGROUP )
                    .addAction( new RequestGroup.RequestGroupActionComponent().setDescription("An action "+ ++n))
                    .addAction( new RequestGroup.RequestGroupActionComponent().setDescription("An action "+ ++n))
                )
            )
            .addAction( new RequestGroup.RequestGroupActionComponent()
                .setTitle("Exactly One "+n++)
                .setDescription("An action - visual group")
                .setGroupingBehavior( RequestGroup.ActionGroupingBehavior.VISUALGROUP )
                .setSelectionBehavior(RequestGroup.ActionSelectionBehavior.EXACTLYONE)
                .addAction( new RequestGroup.RequestGroupActionComponent()
                    .setDescription("An prechecked action "+ n++)
                    .setPrecheckBehavior(RequestGroup.ActionPrecheckBehavior.YES)
                )
                .addAction( new RequestGroup.RequestGroupActionComponent()
                    .setDescription("An action "+ n++)
                    .setPrecheckBehavior(RequestGroup.ActionPrecheckBehavior.NO)
                )
                .addAction( new RequestGroup.RequestGroupActionComponent()
                    .setDescription("An action "+ n++)
                    .setDescription("An action "+n)
                )
            )
            .addAction( new RequestGroup.RequestGroupActionComponent()
                .setTitle("One or more "+n++)
                .setDescription("An action visual group")
                .setGroupingBehavior( RequestGroup.ActionGroupingBehavior.VISUALGROUP )
                .setSelectionBehavior(RequestGroup.ActionSelectionBehavior.ONEORMORE)
                .addAction( new RequestGroup.RequestGroupActionComponent()
                    .setTitle("The Title "+n++)
                    .setDescription("creat procedure")
                    .setType( new Coding().setCode(ActionType.CREATE.toCode()).setSystem(ActionType.CREATE.getSystem()).setDisplay(ActionType.CREATE.getDisplay()))
                    .setResource( new Reference().setReference("#"+procedure.getId()))
                )
                .addAction( new RequestGroup.RequestGroupActionComponent().setDescription("An prechecked action "+ ++n)
                    .setPrecheckBehavior(RequestGroup.ActionPrecheckBehavior.YES)
                )
                .addAction( new RequestGroup.RequestGroupActionComponent().setDescription("An action "+ ++n))
                .addAction( new RequestGroup.RequestGroupActionComponent().setDescription("An action "+ ++n))
            )
            .setId("TheRequestGroup");
        carePlan.addContained(requestGroup);

        carePlan.setTitle("test title")
            .setDescription("Test description")
            .setSubject( new Reference().setReference("#"+patient.getId()))
            .setStatus( CarePlan.CarePlanStatus.DRAFT )
            .setIntent( CarePlan.CarePlanIntent.OPTION )
            .addActivity( new CarePlan.CarePlanActivityComponent()
                .setReference(new Reference().setReference("#"+requestGroup.getId()))
                .setDetail( new CarePlan.CarePlanActivityDetailComponent().setDescription("CarePlan activity description"))
            )
            .setId("CP1")
        ;
        resources.add( carePlan );
        return resources;
    }
}
