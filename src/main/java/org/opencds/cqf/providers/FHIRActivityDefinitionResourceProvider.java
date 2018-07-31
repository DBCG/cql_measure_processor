package org.opencds.cqf.providers;

import ca.uhn.fhir.jpa.rp.dstu3.ActivityDefinitionResourceProvider;
import ca.uhn.fhir.rest.annotation.*;
import ca.uhn.fhir.rest.server.exceptions.InternalErrorException;
import org.hl7.fhir.dstu3.model.*;
import org.hl7.fhir.exceptions.FHIRException;
import org.opencds.cqf.exceptions.ActivityDefinitionApplyException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Created by Bryn on 1/16/2017.
 */
public class FHIRActivityDefinitionResourceProvider extends ActivityDefinitionResourceProvider {

    private JpaDataProvider provider;
    private CqlExecutionProvider executionProvider;
    private static final Logger logger = LoggerFactory.getLogger(FHIRPlanDefinitionResourceProvider.class );

    public FHIRActivityDefinitionResourceProvider(JpaDataProvider provider) {
        this.provider = provider;
        this.executionProvider = new CqlExecutionProvider(provider);
    }

    @Operation(name = "$apply", idempotent = true)
    public Resource apply(@IdParam IdType theId, @RequiredParam(name="patient") String patientId,
                          @OptionalParam(name="encounter") String encounterId,
                          @OptionalParam(name="practitioner") String practitionerId,
                          @OptionalParam(name="organization") String organizationId,
                          @OptionalParam(name="userType") String userType,
                          @OptionalParam(name="userLanguage") String userLanguage,
                          @OptionalParam(name="userTaskContext") String userTaskContext,
                          @OptionalParam(name="setting") String setting,
                          @OptionalParam(name="settingContext") String settingContext)
            throws InternalErrorException, FHIRException, ClassNotFoundException, IllegalAccessException,
            InstantiationException, ActivityDefinitionApplyException
    {
        logger.info("apply on activity definition "+theId);
        ActivityDefinition activityDefinition = this.getDao().read(theId);

        return resolveActivityDefinition(activityDefinition, patientId, practitionerId, organizationId);
    }

    // For library use
    public Resource resolveActivityDefinition(ActivityDefinition activityDefinition, String patientId,
                                              String practitionerId, String organizationId)
            throws FHIRException
    {
        Resource result = null;
        try {
            // This is a little hacky...
            result = (Resource) Class.forName("org.hl7.fhir.dstu3.model." + activityDefinition.getKind().toCode()).newInstance();
        }
        catch (Exception e) {
            e.printStackTrace();
            throw new FHIRException("Could not find org.hl7.fhir.dstu3.model." + activityDefinition.getKind().toCode());
        }

        switch (result.fhirType()) {
            case "ProcedureRequest":
                result = resolveProcedureRequest(activityDefinition, patientId, practitionerId, organizationId);
                break;

            case "MedicationRequest":
                result = resolveMedicationRequest(activityDefinition, patientId);
                break;

            case "SupplyRequest":
                result = resolveSupplyRequest(activityDefinition, practitionerId, organizationId);
                break;

            case "Procedure":
                result = resolveProcedure(activityDefinition, patientId);
                break;

            case "DiagnosticReport":
                result = resolveDiagnosticReport(activityDefinition, patientId);
                break;

            case "Communication":
                result = resolveCommunication(activityDefinition, patientId);
                break;

            case "CommunicationRequest":
                result = resolveCommunicationRequest(activityDefinition, patientId);
                break;
        }

        // TODO: Apply expression extensions on any element?

        for (ActivityDefinition.ActivityDefinitionDynamicValueComponent dynamicValue : activityDefinition.getDynamicValue())
        {
            if (dynamicValue.getExpression() != null) {
                /*
                    TODO: Passing the activityDefinition as context here because that's what will have the libraries,
                          but perhaps the "context" here should be the result resource?
                */
                Object value =
                        executionProvider
                            .evaluateInContext(activityDefinition, dynamicValue.getLanguage(), dynamicValue.getExpression(), patientId);

                // TODO need to verify type... yay
                if (value instanceof Boolean) {
                    value = new BooleanType((Boolean) value);
                }
                if (dynamicValue.hasPath() ){
                    if ( dynamicValue.getPath().equals("$this") && result instanceof Resource) {
                        result = (Resource) value;
                    } else {
                        this.provider.setValue(result, dynamicValue.getPath(), value);
                    }
                }
            }
        }

        return result;
    }

    private ProcedureRequest resolveProcedureRequest(ActivityDefinition activityDefinition, String patientId,
                                                     String practitionerId, String organizationId)
            throws ActivityDefinitionApplyException
    {
        // status, intent, code, and subject are required
        ProcedureRequest procedureRequest = new ProcedureRequest();
        procedureRequest.setStatus(ProcedureRequest.ProcedureRequestStatus.DRAFT);
        procedureRequest.setIntent(ProcedureRequest.ProcedureRequestIntent.ORDER);
        procedureRequest.setSubject(new Reference(patientId));

        if (practitionerId != null) {
            procedureRequest.setRequester(
                    new ProcedureRequest.ProcedureRequestRequesterComponent()
                            .setAgent(new Reference(practitionerId))
            );
        }

        else if (organizationId != null) {
            procedureRequest.setRequester(
                    new ProcedureRequest.ProcedureRequestRequesterComponent()
                            .setAgent(new Reference(organizationId))
            );
        }

        if (activityDefinition.hasExtension()) {
            procedureRequest.setExtension(activityDefinition.getExtension());
        }

        if (activityDefinition.hasCode()) {
            procedureRequest.setCode(activityDefinition.getCode());
        }

        // code can be set as a dynamicValue
        else if (!activityDefinition.hasCode() && !activityDefinition.hasDynamicValue()) {
            throw new ActivityDefinitionApplyException("Missing required code property");
        }

        if (activityDefinition.hasBodySite()) {
            procedureRequest.setBodySite( activityDefinition.getBodySite());
        }

        if (activityDefinition.hasProduct()) {
            throw new ActivityDefinitionApplyException("Product does not map to "+activityDefinition.getKind());
        }

        if (activityDefinition.hasDosage()) {
            throw new ActivityDefinitionApplyException("Dosage does not map to "+activityDefinition.getKind());
        }

        return procedureRequest;
    }

    private MedicationRequest resolveMedicationRequest(ActivityDefinition activityDefinition, String patientId)
            throws ActivityDefinitionApplyException
    {
        // intent, medication, and subject are required
        MedicationRequest medicationRequest = new MedicationRequest();
        medicationRequest.setIntent(MedicationRequest.MedicationRequestIntent.ORDER);
        medicationRequest.setSubject(new Reference(patientId));

        if (activityDefinition.hasProduct()) {
            medicationRequest.setMedication( activityDefinition.getProduct());
        }

        else {
            throw new ActivityDefinitionApplyException("Missing required product property");
        }

        if (activityDefinition.hasDosage()) {
            medicationRequest.setDosageInstruction( activityDefinition.getDosage());
        }

        if (activityDefinition.hasBodySite()) {
            throw new ActivityDefinitionApplyException("Bodysite does not map to " + activityDefinition.getKind());
        }

        if (activityDefinition.hasCode()) {
            throw new ActivityDefinitionApplyException("Code does not map to " + activityDefinition.getKind());
        }

        if (activityDefinition.hasQuantity()) {
            throw new ActivityDefinitionApplyException("Quantity does not map to " + activityDefinition.getKind());
        }

        return medicationRequest;
    }

    private SupplyRequest resolveSupplyRequest(ActivityDefinition activityDefinition, String practionerId,
                                               String organizationId) throws ActivityDefinitionApplyException
    {
        SupplyRequest supplyRequest = new SupplyRequest();

        if (practionerId != null) {
            supplyRequest.setRequester(
                    new SupplyRequest.SupplyRequestRequesterComponent()
                            .setAgent(new Reference(practionerId))
            );
        }

        if (organizationId != null) {
            supplyRequest.setRequester(
                    new SupplyRequest.SupplyRequestRequesterComponent()
                            .setAgent(new Reference(organizationId))
            );
        }

        if (activityDefinition.hasQuantity()){
            supplyRequest.setOrderedItem(
                    new SupplyRequest.SupplyRequestOrderedItemComponent()
                            .setQuantity( activityDefinition.getQuantity())
            );
        }

        else {
            throw new ActivityDefinitionApplyException("Missing required orderedItem.quantity property");
        }

        if (activityDefinition.hasCode()) {
            supplyRequest.getOrderedItem().setItem(activityDefinition.getCode());
        }

        if (activityDefinition.hasProduct()) {
            throw new ActivityDefinitionApplyException("Product does not map to "+activityDefinition.getKind());
        }

        if (activityDefinition.hasDosage()) {
            throw new ActivityDefinitionApplyException("Dosage does not map to "+activityDefinition.getKind());
        }

        if (activityDefinition.hasBodySite()) {
            throw new ActivityDefinitionApplyException("Bodysite does not map to "+activityDefinition.getKind());
        }

        return supplyRequest;
    }

    private Procedure resolveProcedure(ActivityDefinition activityDefinition, String patientId) {
        Procedure procedure = new Procedure();

        // TODO - set the appropriate status
        procedure.setStatus(Procedure.ProcedureStatus.UNKNOWN);
        procedure.setSubject(new Reference(patientId));

        if (activityDefinition.hasCode()) {
            procedure.setCode(activityDefinition.getCode());
        }

        if (activityDefinition.hasBodySite()) {
            procedure.setBodySite(activityDefinition.getBodySite());
        }

        return procedure;
    }

    private DiagnosticReport resolveDiagnosticReport(ActivityDefinition activityDefinition, String patientId) {
        DiagnosticReport diagnosticReport = new DiagnosticReport();

        diagnosticReport.setStatus(DiagnosticReport.DiagnosticReportStatus.UNKNOWN);
        diagnosticReport.setSubject(new Reference(patientId));

        if (activityDefinition.hasCode()) {
            diagnosticReport.setCode(activityDefinition.getCode());
        }

        else {
            throw new ActivityDefinitionApplyException("Missing required ActivityDefinition.code property for DiagnosticReport");
        }

        if (activityDefinition.hasRelatedArtifact()) {
            List<Attachment> presentedFormAttachments = new ArrayList<>();
            for (RelatedArtifact artifact : activityDefinition.getRelatedArtifact()) {
                Attachment attachment = new Attachment();

                if (artifact.hasUrl()) {
                    attachment.setUrl(artifact.getUrl());
                }

                if (artifact.hasDisplay()) {
                    attachment.setTitle(artifact.getDisplay());
                }
                presentedFormAttachments.add(attachment);
            }
            diagnosticReport.setPresentedForm(presentedFormAttachments);
        }

        return diagnosticReport;
    }

    private Communication resolveCommunication(ActivityDefinition activityDefinition, String patientId) {
        Communication communication = new Communication();

        communication.setStatus(Communication.CommunicationStatus.UNKNOWN);
        communication.setSubject(new Reference(patientId));

        if (activityDefinition.hasCode()) {
            communication.setReasonCode(Collections.singletonList(activityDefinition.getCode()));
        }

        if (activityDefinition.hasRelatedArtifact()) {
            for (RelatedArtifact artifact : activityDefinition.getRelatedArtifact()) {
                if (artifact.hasUrl()) {
                    Attachment attachment = new Attachment().setUrl(artifact.getUrl());
                    if (artifact.hasDisplay()) {
                        attachment.setTitle(artifact.getDisplay());
                    }

                    Communication.CommunicationPayloadComponent payload = new Communication.CommunicationPayloadComponent();
                    payload.setContent(artifact.hasDisplay() ? attachment.setTitle(artifact.getDisplay()) : attachment);
                    communication.setPayload(Collections.singletonList(payload));
                }

                // TODO - other relatedArtifact types
            }
        }

        return communication;
    }

    // TODO - extend this to be more complete
    private CommunicationRequest resolveCommunicationRequest(ActivityDefinition activityDefinition, String patientId) {
        CommunicationRequest communicationRequest = new CommunicationRequest();

        communicationRequest.setStatus(CommunicationRequest.CommunicationRequestStatus.UNKNOWN);
        communicationRequest.setSubject(new Reference(patientId));

        // Unsure if this is correct - this is the way Motive is doing it...
        if (activityDefinition.hasCode()) {
            if (activityDefinition.getCode().hasText()) {
                communicationRequest.addPayload().setContent(new StringType(activityDefinition.getCode().getText()));
            }
        }

        return communicationRequest;
    }
}
