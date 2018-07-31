package org.opencds.cqf.directory;

import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.OperationOutcome;
import org.hl7.fhir.dstu3.model.Resource;
import org.hl7.fhir.instance.model.api.IBaseResource;

import java.util.TreeMap;

public class AsyncResult {
    TreeMap<String, TreeMap<String,Resource>> resultTreeMap = new TreeMap<>();
    Resource resultResource = null;
    public Bundle resultBundle = null;
    private OperationOutcome operationOutcome = null;

    public void addBundle(Bundle patientDataBundle) {
        System.out.println("add Bundle");
        boolean initial = false;
        if ( resultBundle==null){
            resultBundle = patientDataBundle;
            initial = true;
        }

        for (Bundle.BundleEntryComponent bundleEntryComponent : patientDataBundle.getEntry()) {
            Resource resource = bundleEntryComponent.getResource();
            addResourceToMap( resource );
            if ( !initial ){
                resultBundle.getEntry().add( bundleEntryComponent );
            }
        }
        System.out.println("add Bundle Done");
    }

    public void addResource(Resource resource) {
        resultResource=resource;
        addResourceToMap(resource);
    }

    private void addResourceToMap(Resource resource) {
        if (!resultTreeMap.containsKey(resource.fhirType())) {
            resultTreeMap.put(resource.fhirType(), new TreeMap<String, Resource>());
        }
        TreeMap<String, Resource> resourceTreeMap = resultTreeMap.get(resource.fhirType());
        String key = (resource.hasId()? resource.getId():"HC"+resource.hashCode());
        resourceTreeMap.put(key, resource);
    }

    public IBaseResource getResultResource() {
        if ( resultResource != null ){
            return resultResource;
        } else {
            resultBundle.getLink().clear();
            return resultBundle;
        }
    }

    public void addOperationOutcome(OperationOutcome operationOutcome) {
        this.operationOutcome = operationOutcome;
    }

    public OperationOutcome getOperationOutcome() {
        return this.operationOutcome;
    }
}
