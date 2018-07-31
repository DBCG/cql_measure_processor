package org.opencds.cqf.cdshooks.request;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.hl7.fhir.dstu3.model.Reference;
import org.hl7.fhir.dstu3.model.Resource;
import org.hl7.fhir.exceptions.FHIRException;
import org.opencds.cqf.cdshooks.providers.CdsHooksProviders;
import org.opencds.cqf.cdshooks.response.CarePlanToCdsCard;
import org.opencds.cqf.cdshooks.response.CdsCard;
import org.opencds.cqf.exceptions.InvalidFieldTypeException;
import org.opencds.cqf.providers.FHIRBundleResourceProvider;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

public abstract class CdsRequest {

    private JsonObject requestJson;
    private String hook;
    private String hookInstance;
    private URL fhirServer;
    private FhirAuthorization fhirAuthorization;
    private Reference user;
    // this is not a standard element - used for testing
    private Boolean applyCql;
    Context context;
    private Prefetch prefetch;
    private JsonElement prefetchObject;

    public abstract void setContext(JsonObject context);

    public CdsRequest(JsonObject requestJson) {
        this.requestJson = requestJson;
        hook = JsonFieldResolution.getStringField(requestJson, "hook", true);
        hookInstance = JsonFieldResolution.getStringField(requestJson, "hookInstance", true);
        String fhirServerString = JsonFieldResolution.getStringField(requestJson, "fhirServer", false);
        if (fhirServerString != null) {
            try {
                fhirServer = new URL(fhirServerString);
            } catch (MalformedURLException e) {
                throw new InvalidFieldTypeException("Invalid URL provided for fhirServer field: " + fhirServerString);
            }
        }
        fhirAuthorization = new FhirAuthorization(JsonFieldResolution.getObjectField(requestJson, "fhirAuthorization", false));
        user = new Reference(JsonFieldResolution.getStringField(requestJson, "user", true));
        applyCql = JsonFieldResolution.getBooleanField(requestJson, "applyCql", false);
        if (applyCql == null) {
            applyCql = false;
        }
        setContext(JsonFieldResolution.getObjectField(requestJson, "context", true));

        if (requestJson.has("prefetch")) {
            prefetchObject = requestJson.get("prefetch");
        }
    }

    private boolean isFhirServerLocal() {
        return fhirServer == null || fhirServer.toString().contains("cqf-ruler");
    }

    // TODO - override for cdc-opioid-guidance base call - runs every recommendation
    public List<CdsCard> process(CdsHooksProviders providers) throws FHIRException {
        // cannot use this server for DSTU2 resource retrieval TODO - support DSTU2
        if (isFhirServerLocal() && providers.getVersion() == CdsHooksProviders.FhirVersion.DSTU2) {
            throw new RuntimeException("A DSTU2 fhirServer endpoint or the prefetch resources must be provided in order to evaluate a DSTU2 library");
        }

        // resolve remote data provider
        if (fhirServer != null && !isFhirServerLocal()) {
            providers.resolveRemoteDataProvider(fhirServer.toString());
        }

        // resolve context resources library parameter
        List<Object> contextResources = context.getResources(providers.getVersion());
        providers.resolveContextParameter(applyCql ? applyCqlToResources(contextResources, providers) : contextResources);

        // resolve prefetch urls and resources
        if (prefetchObject == null) {
            prefetch = new Prefetch(null, providers, context.getPatientId());
        }
        else if (prefetchObject.isJsonNull()) {
            prefetch = new Prefetch(providers.nullifyPrefetch(), providers, context.getPatientId());
        }
        else {
            prefetch = new Prefetch(JsonFieldResolution.getObjectField(requestJson, "prefetch", false), providers, context.getPatientId());
        }

        // resolve prefetch data provider
        List<Object> prefetchResources = prefetch.getResources();
        providers.resolvePrefetchDataProvider(applyCql ? applyCqlToResources(prefetchResources, providers) : prefetchResources);

        // apply plandefinition and return cards
        return CarePlanToCdsCard.convert(
                providers.getPlanDefinitionProvider()
                        .resolveCdsHooksPlanDefinition(
                                providers.getContext(), providers.getPlanDefinition(), context.getPatientId()
                        )
        );
    }

    private List<Object> applyCqlToResources(List<Object> resources, CdsHooksProviders providers) {
        FHIRBundleResourceProvider bundleResourceProvider = new FHIRBundleResourceProvider(providers.getJpaDataProvider());
        for (Object res : resources) {
            try {
                bundleResourceProvider.applyCql((Resource) res);
            } catch (FHIRException e) {
                e.printStackTrace();
                throw new RuntimeException("Error applying cql expression extensions: " + e.getMessage());
            }
        }
        return resources;
    }
}
