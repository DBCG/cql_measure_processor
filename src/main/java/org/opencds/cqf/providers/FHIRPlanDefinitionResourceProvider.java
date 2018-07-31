package org.opencds.cqf.providers;

import ca.uhn.fhir.jpa.dao.SearchParameterMap;
import ca.uhn.fhir.jpa.rp.dstu3.LibraryResourceProvider;
import ca.uhn.fhir.jpa.rp.dstu3.PlanDefinitionResourceProvider;
import ca.uhn.fhir.rest.annotation.*;
import ca.uhn.fhir.rest.api.server.IBundleProvider;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.cqframework.cql.cql2elm.LibraryManager;
import org.cqframework.cql.cql2elm.ModelManager;
import org.cqframework.cql.elm.execution.*;
import org.hl7.fhir.dstu3.model.*;
import org.hl7.fhir.exceptions.FHIRException;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.opencds.cqf.builders.*;
import org.opencds.cqf.cdshooks.providers.Discovery;
import org.opencds.cqf.cdshooks.providers.DiscoveryDataProvider;
import org.opencds.cqf.cdshooks.providers.DiscoveryDataProviderDstu2;
import org.opencds.cqf.cdshooks.providers.DiscoveryDataProviderStu3;
import org.opencds.cqf.config.STU3LibraryLoader;
import org.opencds.cqf.cql.execution.Context;
import org.opencds.cqf.cql.execution.LibraryLoader;
import org.opencds.cqf.cql.runtime.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.bind.JAXBException;
import java.io.IOException;
import java.util.*;
import java.util.List;

public class FHIRPlanDefinitionResourceProvider extends PlanDefinitionResourceProvider {

    private JpaDataProvider provider;
    private CqlExecutionProvider executionProvider;

    private static final Logger logger = LoggerFactory.getLogger(FHIRPlanDefinitionResourceProvider.class);

    public FHIRPlanDefinitionResourceProvider(JpaDataProvider provider) {
        this.provider = provider;
        this.executionProvider = new CqlExecutionProvider(provider);
    }

    @Operation(name = "$apply", idempotent = true)
    public CarePlan applyPlanDefinition(
            @IdParam IdType theId,
            @RequiredParam(name="patient") String patientId,
            @OptionalParam(name="encounter") String encounterId,
            @OptionalParam(name="practitioner") String practitionerId,
            @OptionalParam(name="organization") String organizationId,
            @OptionalParam(name="userType") String userType,
            @OptionalParam(name="userLanguage") String userLanguage,
            @OptionalParam(name="userTaskContext") String userTaskContext,
            @OptionalParam(name="setting") String setting,
            @OptionalParam(name="settingContext") String settingContext)
            throws IOException, JAXBException, FHIRException
    {
        PlanDefinition planDefinition = this.getDao().read(theId);

        if (planDefinition == null) {
            throw new IllegalArgumentException("Couldn't find PlanDefintion " + theId);
        }

        logger.info("Performing $apply operation on PlanDefinition/" + theId);

        CarePlanBuilder carePlanBuilder = new CarePlanBuilder();

        carePlanBuilder
                .buildDefinition(new Reference("PlanDefinition/"+ planDefinition.getIdElement().getIdPart())) // TODO instantiates in R4
                .buildSubject(new Reference(patientId))
                .buildStatus(CarePlan.CarePlanStatus.ACTIVE)
                .buildTitle("Application of "+(planDefinition.hasTitle()?planDefinition.getTitle():planDefinition.getId()))
                ;

        if (encounterId != null) carePlanBuilder.buildContext(new Reference(encounterId));
        if (practitionerId != null) carePlanBuilder.buildAuthor(new Reference(practitionerId));
        if (organizationId != null) carePlanBuilder.buildAuthor(new Reference(organizationId));
        if (userLanguage != null) carePlanBuilder.buildLanguage(userLanguage);

        RequestGroupBuilder requestGroupBuilder = new RequestGroupBuilder()
            .buildStatus( RequestGroup.RequestStatus.ACTIVE)
            .buildId("#requestgroup")
            .buildIntent()
            //instantiates
            .buildSubject(new Reference(patientId))
            .buildAuthoredOnToNow()
            ;

        if (encounterId != null) requestGroupBuilder.buildContext(new Reference(encounterId));



        Session session =
            new Session(planDefinition, carePlanBuilder, requestGroupBuilder, patientId, encounterId, practitionerId,
                organizationId, userType, userLanguage, userTaskContext, setting, settingContext, null);

        // links
        if (planDefinition.hasRelatedArtifact()) {
            List<Extension> extensions = new ArrayList<>();
            for (RelatedArtifact relatedArtifact : planDefinition.getRelatedArtifact()) {
                AttachmentBuilder attachmentBuilder = new AttachmentBuilder();
                ExtensionBuilder extensionBuilder = new ExtensionBuilder();
                if (relatedArtifact.hasDisplay()) { // label
                    attachmentBuilder.buildTitle(relatedArtifact.getDisplay());
                }
                if (relatedArtifact.hasUrl()) { // url
                    attachmentBuilder.buildUrl(relatedArtifact.getUrl());
                }
                if (relatedArtifact.hasExtension()) { // type
                    attachmentBuilder.buildExtension(relatedArtifact.getExtension());
                }
                extensionBuilder.buildUrl("http://example.org");
                extensionBuilder.buildValue(attachmentBuilder.build());
                extensions.add(extensionBuilder.build());
            }
            requestGroupBuilder.buildExtension(extensions);
        }

        resolveActions(planDefinition.getAction(), session, patientId, requestGroupBuilder, new ArrayList<>());

        CarePlanActivityBuilder carePlanActivityBuilder = new CarePlanActivityBuilder();
        carePlanActivityBuilder.buildReferenceTarget(requestGroupBuilder.build());
        carePlanBuilder.buildActivity(carePlanActivityBuilder.build());

        //////////////////////////////////////////////////////////

        carePlanBuilder
            .buildActivity(
                new CarePlanActivityBuilder()
                    .buildReference( new Reference().setReference(  requestGroupBuilder.build().getId() ))
                    .build()
            )
            .buildContained( requestGroupBuilder.build() );

        return carePlanBuilder.build();

    }


    // For library use
    public CarePlan resolveCdsHooksPlanDefinition(Context context, PlanDefinition planDefinition, String patientId) throws FHIRException {

        CarePlanBuilder carePlanBuilder = new CarePlanBuilder();
        RequestGroupBuilder requestGroupBuilder = new RequestGroupBuilder().buildStatus().buildIntent();

        // links
        if (planDefinition.hasRelatedArtifact()) {
            List<Extension> extensions = new ArrayList<>();
            for (RelatedArtifact relatedArtifact : planDefinition.getRelatedArtifact()) {
                AttachmentBuilder attachmentBuilder = new AttachmentBuilder();
                ExtensionBuilder extensionBuilder = new ExtensionBuilder();
                if (relatedArtifact.hasDisplay()) { // label
                    attachmentBuilder.buildTitle(relatedArtifact.getDisplay());
                }
                if (relatedArtifact.hasUrl()) { // url
                    attachmentBuilder.buildUrl(relatedArtifact.getUrl());
                }
                if (relatedArtifact.hasExtension()) { // type
                    attachmentBuilder.buildExtension(relatedArtifact.getExtension());
                }
                extensionBuilder.buildUrl("http://example.org");
                extensionBuilder.buildValue(attachmentBuilder.build());
                extensions.add(extensionBuilder.build());
            }
            requestGroupBuilder.buildExtension(extensions);
        }

        Session session =
            new Session(planDefinition, carePlanBuilder, requestGroupBuilder, patientId, context );

        resolveActions(planDefinition.getAction(), session, patientId, requestGroupBuilder, new ArrayList<>());

        CarePlanActivityBuilder carePlanActivityBuilder = new CarePlanActivityBuilder();
        carePlanActivityBuilder.buildReferenceTarget(requestGroupBuilder.build());
        carePlanBuilder.buildActivity(carePlanActivityBuilder.build());

        return carePlanBuilder.build();
    }

    private void resolveActions(List<PlanDefinition.PlanDefinitionActionComponent> actions, Session session,
                                String patientId, RequestGroupBuilder requestGroupBuilder,
                                List<RequestGroup.RequestGroupActionComponent> actionComponents) throws FHIRException {
        for (PlanDefinition.PlanDefinitionActionComponent action : actions) {
            requestGroupBuilder.buildAction( resolveAction(session, patientId, action) );
        }
    }

    private RequestGroup.RequestGroupActionComponent resolveAction(Session session, String patientId, PlanDefinition.PlanDefinitionActionComponent action) throws FHIRException {
        boolean conditionsMet = true;
        logger.info("Process action "+action.getTitle());
        for ( PlanDefinition.PlanDefinitionActionConditionComponent condition: action.getCondition()) {
            if (condition.getKind() == PlanDefinition.ActionConditionKind.APPLICABILITY) {
                if (!condition.hasExpression()) {
                    continue;
                }

                Object result = null;
                if ( session.getContext()!= null ){
                    result = session.getContext().resolveExpressionRef(condition.getExpression()).getExpression().evaluate(session.getContext());
                } else {
                    result = executionProvider
                            .evaluateInContext(session.getPlanDefinition(), condition.getLanguage(), condition.getExpression(), session.getPatientId());
                }

                if (!(result instanceof Boolean)) {
                    continue;
                }

                if (!(Boolean) result) {
                    conditionsMet = false;
                }
                logger.info("Check Condition ("+condition.getExpression()+") => "+conditionsMet);

            }
        }
        if (conditionsMet) {
            RequestGroupActionBuilder requestGroupActionBuilder = new RequestGroupActionBuilder();
            if (action.hasTitle()) {
                requestGroupActionBuilder.buildTitle(action.getTitle());
            }
            if (action.hasDescription()) {
                requestGroupActionBuilder.buildDescripition(action.getDescription());
            }
            if ( action.hasTextEquivalent() ){
                requestGroupActionBuilder.build().setTextEquivalent(action.getTextEquivalent());
            }

            // source
            if (action.hasDocumentation()) {
                RelatedArtifact artifact = action.getDocumentationFirstRep();
                RelatedArtifactBuilder artifactBuilder = new RelatedArtifactBuilder();
                if (artifact.hasDisplay()) {
                    artifactBuilder.buildDisplay(artifact.getDisplay());
                }
                if (artifact.hasUrl()) {
                    artifactBuilder.buildUrl(artifact.getUrl());
                }
                if (artifact.hasDocument() && artifact.getDocument().hasUrl()) {
                    AttachmentBuilder attachmentBuilder = new AttachmentBuilder();
                    attachmentBuilder.buildUrl(artifact.getDocument().getUrl());
                    artifactBuilder.buildDocument(attachmentBuilder.build());
                }
                requestGroupActionBuilder.buildDocumentation(Collections.singletonList(artifactBuilder.build()));
            }

            // suggestions
            // TODO - uuid
            if (action.hasLabel()) {
                requestGroupActionBuilder.buildLabel(action.getLabel());
            }
            if (action.hasType()) {
                requestGroupActionBuilder.buildType(action.getType());
            }
            if (action.hasCardinalityBehavior()){
                requestGroupActionBuilder.setCardinalityBehavior( action.getCardinalityBehavior() );
            }
            if (action.hasGroupingBehavior()){
                requestGroupActionBuilder.setGroupingBehavior( action.getGroupingBehavior() );
            }
            if( action.hasPrecheckBehavior() ){
                requestGroupActionBuilder.setPreCheckBehavior( action.getPrecheckBehavior() );
            }
            if( action.hasSelectionBehavior() ){
                requestGroupActionBuilder.setSelectionBehavior( action.getSelectionBehavior() );
            }
            if (action.hasDefinition()) {
                if (action.getDefinition().getReferenceElement().getResourceType().equals("ActivityDefinition")) {
                    if (action.getDefinition().getResource() != null) {
                        ActivityDefinition activityDefinition = (ActivityDefinition) action.getDefinition().getResource();
                        ReferenceBuilder referenceBuilder = new ReferenceBuilder();
                        referenceBuilder.buildDisplay(activityDefinition.getDescription());
                        requestGroupActionBuilder.buildResource(referenceBuilder.build());
                    }

                    // TODO - fix this
                    FHIRActivityDefinitionResourceProvider activitydefinitionProvider = (FHIRActivityDefinitionResourceProvider) provider.resolveResourceProvider("ActivityDefinition");
                    ActivityDefinition activityDefinition =
                            activitydefinitionProvider.getDao().read(action.getDefinition().getReferenceElement());
                    if (activityDefinition.hasDescription()) {
                        requestGroupActionBuilder.buildDescripition(activityDefinition.getDescription());
                    }
                    Resource resource = null;
                    try {
                        resource = activitydefinitionProvider.apply(
                                new IdType(action.getDefinition().getReferenceElement().getIdPart()), patientId,
                                null, null, null, null,
                                null, null, null, null
                        ).setId(UUID.randomUUID().toString());
                    } catch (FHIRException | ClassNotFoundException | InstantiationException | IllegalAccessException e) {
                        throw new RuntimeException("Error applying ActivityDefinition " + e.getMessage());
                    }

                    requestGroupActionBuilder.buildResourceTarget(resource);
                    session.getCarePlanBuilder().buildContained(resource);
                    requestGroupActionBuilder.buildResource(new ReferenceBuilder().buildReference("#"+resource.getId()).build());
                }
            }

            // Dynamic values populate the RequestGroup - there is a bit of hijacking going on here...
            if (action.hasDynamicValue()) {
                // TODO BvdH Replace with proper dynamic value management
                for (PlanDefinition.PlanDefinitionActionDynamicValueComponent dynamicValue : action.getDynamicValue()) {
                    if (dynamicValue.hasPath() && dynamicValue.hasExpression()) {

                        Object result;
                        if ( session.getContext()!= null ){
                            result = session.getContext().resolveExpressionRef(dynamicValue.getExpression()).getExpression().evaluate(session.getContext());
                        } else {
                            result = executionProvider
                                    .evaluateInContext(session.getPlanDefinition(), dynamicValue.getLanguage(), dynamicValue.getExpression(), session.getPatientId());
                        }

                        // TODO address this in the standard or change
                        if (dynamicValue.getPath().endsWith("title")) { // summary
                            String title = (String)result;
                            requestGroupActionBuilder.buildTitle(title);
                        }
                        else if (dynamicValue.getPath().endsWith("description")) { // detail
                            String description = (String)result;
                            requestGroupActionBuilder.buildDescripition(description);
                        }
                        else if (dynamicValue.getPath().endsWith("extension")) { // indicator
                            String extension = (String)result;
                            requestGroupActionBuilder.buildExtension(extension);
                        } else {
                            // TODO is this correct or should &this refer to RequestGroup?
                            if (dynamicValue.hasPath() && dynamicValue.getPath().equals("$this"))
                            {
                                session.setCarePlan((CarePlan) result);
                            }
                            else {
                                // TODO - likely need more date tranformations
                                if (result instanceof DateTime) {
                                    result =
                                            new JavaDateBuilder()
                                                    .buildFromDateTime((DateTime) result)
                                                    .build();
                                }
                                else if (result instanceof String) {
                                    result = new StringType((String) result);
                                }
                                provider.setValue(session.getCarePlan(), dynamicValue.getPath(), result);
                            }
                        }
                    }
                }
            }
            for ( PlanDefinition.PlanDefinitionActionComponent planDefinitionActionComponent: action.getAction() ){
                requestGroupActionBuilder.buildAction( resolveAction(session,patientId,planDefinitionActionComponent));
            }
            return requestGroupActionBuilder.build();
        }
        return null;
    }

    private Map<String, Pair<PlanDefinition, Discovery> > discoveryCache = new HashMap<>();

    public List<Discovery> getDiscoveries() {
        List<Discovery> discoveries = new ArrayList<>();
        IBundleProvider bundleProvider = getDao().search(new SearchParameterMap());
        for (IBaseResource resource : bundleProvider.getResources(0, bundleProvider.size())) {
            if (resource instanceof PlanDefinition) {
                PlanDefinition planDefinition = (PlanDefinition) resource;
                if (discoveryCache.containsKey(planDefinition.getIdElement().getIdPart())) {
                    Pair<PlanDefinition, Discovery> pair = discoveryCache.get(planDefinition.getIdElement().getIdPart());
                    if (pair.getLeft().hasMeta() && pair.getLeft().getMeta().hasLastUpdated()
                            && planDefinition.hasMeta() && planDefinition.getMeta().hasLastUpdated())
                    {
                        if (pair.getLeft().getMeta().getLastUpdated().equals(planDefinition.getMeta().getLastUpdated())) {
                            discoveries.add(pair.getRight());
                        }
                        else {
                            Discovery discovery = getDiscovery(planDefinition);
                            if (discovery == null) continue;
                            discoveryCache.put(planDefinition.getIdElement().getIdPart(), new ImmutablePair<>(planDefinition, discovery));
                            discoveries.add(discovery);
                        }
                    }
                }
                else {
                    Discovery discovery = getDiscovery(planDefinition);
                    if (discovery == null) continue;
                    discoveryCache.put(planDefinition.getIdElement().getIdPart(), new ImmutablePair<>(planDefinition, discovery));
                    discoveries.add(discovery);
                }
            }
        }
        return discoveries;
    }

    public Discovery getDiscovery(PlanDefinition planDefinition) {
        LibraryLoader libraryLoader = new STU3LibraryLoader(
                (LibraryResourceProvider) provider.resolveResourceProvider("Library"),
                new LibraryManager(new ModelManager()), new ModelManager()
        );
        if (planDefinition.hasType()) {
            for (Coding typeCode : planDefinition.getType().getCoding()) {
                if (typeCode.getCode().equals("eca-rule")) {
                    if (planDefinition.hasLibrary()) {
                        for (Reference reference : planDefinition.getLibrary()) {
                            org.cqframework.cql.elm.execution.Library library = libraryLoader.load(
                                    new VersionedIdentifier()
                                            .withId(reference.getReferenceElement().getIdPart())
                                            .withVersion(reference.getReferenceElement().getVersionIdPart())
                            );

                            DiscoveryDataProvider discoveryDataProvider = null;
                            for (UsingDef using : library.getUsings().getDef()) {
                                if (using.getLocalIdentifier().equals("FHIR") && using.getVersion().equals("3.0.0")) {
                                    discoveryDataProvider = new DiscoveryDataProviderStu3();
                                }
                                else if (using.getLocalIdentifier().equals("FHIR") && using.getVersion().equals("1.0.2")) {
                                    discoveryDataProvider = new DiscoveryDataProviderDstu2();
                                }
                            }
                            if (discoveryDataProvider == null) {
                                continue;
                            }
                            discoveryDataProvider.setExpandValueSets(true);
                            discoveryDataProvider.setTerminologyProvider(provider.getTerminologyProvider());
                            Context context = new Context(library);
                            context.registerDataProvider("http://hl7.org/fhir", discoveryDataProvider);
                            context.registerTerminologyProvider(provider.getTerminologyProvider());
                            context.registerLibraryLoader(libraryLoader
                            );
                            context.enterContext("Patient");
                            context.setContextValue(context.getCurrentContext(), "{{context.patientId}}");
                            // TODO - remove once engine issue is resolved
                            if (library.getParameters() != null) {
                                for (ParameterDef params : library.getParameters().getDef()) {
                                    if (params.getParameterTypeSpecifier() instanceof ListTypeSpecifier) {
                                        context.setParameter(null, params.getName(), new ArrayList<>());
                                    }
                                }
                            }
                            for (ExpressionDef def : library.getStatements().getDef()) {
                                try {
                                    def.getExpression().evaluate(context);
                                } catch (Exception e) {
                                    // ignore
                                }
                            }
                            return discoveryDataProvider.getDiscovery().setPlanDefinition(planDefinition);
                        }
                    }
                }
            }
        }
        return new Discovery().setPlanDefinition(planDefinition);
    }
}

class Session {
    private final String patientId;
    private final PlanDefinition planDefinition;
    private final String practionerId;
    private final String organizationId;
    private final String userType;
    private final String userLanguage;
    private final String userTaskContext;
    private final String setting;
    private final String settingContext;
    private final Context context;
    private CarePlanBuilder carePlanBuilder;
    private String encounterId;
    private RequestGroupBuilder requestGroupBuilder;

    public Session(PlanDefinition planDefinition, CarePlanBuilder builder, RequestGroupBuilder requestGroupBuilder, String patientId, String encounterId,
                   String practitionerId, String organizationId, String userType, String userLanguage,
                   String userTaskContext, String setting, String settingContext, Context context )
    {
        this.patientId = patientId;
        this.planDefinition = planDefinition;
        this.carePlanBuilder = builder;
        this.requestGroupBuilder = requestGroupBuilder;
        this.encounterId = encounterId;
        this.practionerId = practitionerId;
        this.organizationId = organizationId;
        this.userType = userType;
        this.userLanguage = userLanguage;
        this.userTaskContext = userTaskContext;
        this.setting = setting;
        this.settingContext = settingContext;
        this.context = context;
    }

    public Session(PlanDefinition planDefinition, CarePlanBuilder carePlanBuilder, RequestGroupBuilder requestGroupBuilder, String patientId) {
        this( planDefinition, carePlanBuilder, requestGroupBuilder, patientId, null, null, null, null, null, null, null, null, null );
    }

    public Session(PlanDefinition planDefinition, CarePlanBuilder carePlanBuilder, RequestGroupBuilder requestGroupBuilder, String patientId, Context context) {
        this( planDefinition, carePlanBuilder, requestGroupBuilder, patientId, null, null, null, null, null, null, null, null, context );
    }


    public PlanDefinition getPlanDefinition() {
        return this.planDefinition;
    }

    public String getPatientId() {
        return patientId;
    }

    public CarePlan getCarePlan() {
        return carePlanBuilder.build();
    }

    public void setCarePlan(CarePlan carePlan) {
        this.carePlanBuilder = new CarePlanBuilder(carePlan);
    }

    public String getEncounterId() {
        return this.encounterId;
    }

    public String getPractionerId() {
        return practionerId;
    }

    public String getOrganizationId() {
        return organizationId;
    }

    public String getUserType() {
        return userType;
    }

    public String getUserLanguage() {
        return userLanguage;
    }

    public String getUserTaskContext() {
        return userTaskContext;
    }

    public String getSetting() {
        return setting;
    }

    public String getSettingContext() {
        return settingContext;
    }

    public CarePlanBuilder getCarePlanBuilder() {
        return carePlanBuilder;
    }

    public RequestGroupBuilder getRequestGroupBuilder() {
        return requestGroupBuilder;
    }

    public Context getContext() {
        return this.context;
    }
}
