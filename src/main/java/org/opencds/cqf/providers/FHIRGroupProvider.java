package org.opencds.cqf.providers;

import ca.uhn.fhir.jpa.dao.SearchParameterMap;
import ca.uhn.fhir.jpa.provider.dstu3.JpaResourceProviderDstu3;
import ca.uhn.fhir.model.api.Include;
import ca.uhn.fhir.model.api.annotation.*;
import ca.uhn.fhir.rest.annotation.Operation;
import ca.uhn.fhir.rest.annotation.OperationParam;
import ca.uhn.fhir.rest.annotation.Search;
import ca.uhn.fhir.rest.annotation.*;
import ca.uhn.fhir.rest.param.*;
import ca.uhn.fhir.rest.api.SortSpec;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.param.DateParam;
import ca.uhn.fhir.rest.param.DateRangeParam;
import ca.uhn.fhir.rest.param.StringParam;
import ca.uhn.fhir.rest.server.IResourceProvider;
import org.hl7.fhir.dstu3.model.*;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.opencds.cqf.helpers.BulkDataHelper;

import javax.servlet.ServletException;
import java.io.IOException;
import java.util.*;

public class FHIRGroupProvider extends JpaResourceProviderDstu3<Group> {

    private JpaDataProvider provider;

    public FHIRGroupProvider(Collection<IResourceProvider> providers) {
        provider = new JpaDataProvider(providers);
    }

    @Operation(name = "$export", idempotent = true)
    public IBaseResource exportGroupData(
            javax.servlet.http.HttpServletRequest theServletRequest,
            RequestDetails theRequestDetails,
            @IdParam IdType theId,
            @OperationParam(name="since") DateParam since,
            @OperationParam(name="type") StringAndListParam type) throws ServletException, IOException
    {
        BulkDataHelper helper = new BulkDataHelper(provider);

        if (theRequestDetails.getHeader("Accept") == null) {
            return helper.createErrorOutcome("Please provide the Accept header, which must be set to application/fhir+json");
        } else if (!theRequestDetails.getHeader("Accept").equals("application/fhir+json")) {
            return helper.createErrorOutcome("Only the application/fhir+json value for the Accept header is currently supported");
        }

        Group group = this.getDao().read(theId);

        if (group == null) {
            return helper.createErrorOutcome("Group with id " + theId + " could not be found");
        }

        SearchParameterMap searchMap = new SearchParameterMap();

        ReferenceOrListParam patientParams = new ReferenceOrListParam();
        Reference reference;
        for (Group.GroupMemberComponent member : group.getMember()) {
            reference = member.getEntity();
            if (reference.getReferenceElement().getResourceType().equals("Patient")) {
                patientParams.addOr(new ReferenceParam().setValue(reference.getReference()));
            }
        }

        if (patientParams.getValuesAsQueryTokens().isEmpty()) {
            return helper.createErrorOutcome("No patients found in the Group with id: " + theId.getIdPart());
        }

        searchMap.setLastUpdated(new DateRangeParam());
        if (since != null) {
            DateRangeParam rangeParam = new DateRangeParam(since.getValue(), new Date());
            searchMap.setLastUpdated(rangeParam);
        }

        List<List<Resource> > resources = new ArrayList<>();
        List<Resource> resolvedResources;
        if (type != null) {
            for (StringOrListParam stringOrListParam : type.getValuesAsQueryTokens()) {
                for (StringParam theType : stringOrListParam.getValuesAsQueryTokens()) {
                    SearchParameterMap newMap = (SearchParameterMap) searchMap.clone();
                    for (String param : helper.getPatientInclusionPath(theType.getValue())) {
                        newMap.add(param, patientParams);
                    }
                    resolvedResources = helper.resolveType(theType.getValue(), newMap);
                    if (!resolvedResources.isEmpty()) {
                        resources.add(resolvedResources);
                    }
                }
            }
        }
        else {
            for (String theType : helper.compartmentPatient) {
                SearchParameterMap newMap = (SearchParameterMap) searchMap.clone();
                for (String param : helper.getPatientInclusionPath(theType)) {
                    newMap.add(param, patientParams);
                }
                resolvedResources = helper.resolveType(theType, newMap);
                if (!resolvedResources.isEmpty()) {
                    resources.add(resolvedResources);
                }
            }
        }

        Bundle bundle = new Bundle();
        resources.stream()
                .forEach( resourcesList -> resourcesList.stream()
                        .forEach( resource -> bundle.addEntry()
                                .setResource(resource)
                                .setFullUrl( theRequestDetails.getFhirServerBase()+"/"+resource.getId()))
                );


        return bundle;
    }

}
