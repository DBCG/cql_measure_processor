package org.opencds.cqf.providers;

import ca.uhn.fhir.jpa.dao.IFhirResourceDaoPatient;
import ca.uhn.fhir.jpa.dao.SearchParameterMap;
import ca.uhn.fhir.jpa.rp.dstu3.PatientResourceProvider;
import ca.uhn.fhir.model.api.annotation.Description;
import ca.uhn.fhir.model.valueset.BundleTypeEnum;
import ca.uhn.fhir.rest.annotation.IdParam;
import ca.uhn.fhir.rest.annotation.Operation;
import ca.uhn.fhir.rest.annotation.OperationParam;
import ca.uhn.fhir.rest.annotation.OptionalParam;
import ca.uhn.fhir.rest.api.server.IBundleProvider;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.param.*;
import ca.uhn.fhir.rest.server.IResourceProvider;
import org.hl7.fhir.dstu3.model.*;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.opencds.cqf.exceptions.NotImplementedException;
import org.opencds.cqf.helpers.BulkDataHelper;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by Bryn on 1/16/2017.
 */
public class FHIRPatientResourceProvider extends PatientResourceProvider {

    private JpaDataProvider provider;

    public FHIRPatientResourceProvider(Collection<IResourceProvider> providers) {
        super();
        this.provider = new JpaDataProvider(providers);
    }


    /**
     * Patient/123/$export
     * @param theRequestDetails
     */
    //@formatter:off
    @Operation(name = "export", idempotent = true, bundleType=BundleTypeEnum.SEARCHSET)
    public IBaseResource patientInstanceEverything(

            javax.servlet.http.HttpServletRequest theServletRequest,
            RequestDetails theRequestDetails,

            @IdParam IdType theId,

            @Description(formalDefinition="Indicates the preferred output form for the call.")
            @OptionalParam(name="outputFormat") String outputFormat,

            @Description(formalDefinition="Resources updated after this period will be included in the response.")
            @OptionalParam(name="since") InstantType since,

            @Description(formalDefinition="string of comma-delimited FHIR resource types.")
            @OperationParam(name = "type", min=0, max=OperationParam.MAX_UNLIMITED) String  type

    ) {
        // TODO refactor to include $export approach
        if (theRequestDetails.getHeader("Accept") == null) {
            return createErrorOutcome("Please provide the Accept header, which must be set to application/fhir+json");
        } else if (!theRequestDetails.getHeader("Accept").equals("application/fhir+json")) {
            return createErrorOutcome("Only the application/fhir+json value for the Accept header is currently supported");
        }

        DateRangeParam theLastUpdated = null;
        if ( since !=null ) {
            theLastUpdated = new DateRangeParam();
            theLastUpdated.setLowerBound(new DateParam().setValue(since.getValue()));
        }

        startRequest(theServletRequest);
        try {
//            return ((IFhirResourceDaoPatient<Patient>) getDao()).patientInstanceEverything(theServletRequest, theId, theCount, theLastUpdated, theSortSpec, toStringAndList(theContent), toStringAndList(theNarrative), theRequestDetails);

            IBundleProvider iBundleProvider = ((IFhirResourceDaoPatient<Patient>) getDao()).patientInstanceEverything(theServletRequest, theId, null, theLastUpdated, null, null, null, theRequestDetails);
            return( createExportBundle( iBundleProvider, type, theRequestDetails.getFhirServerBase() ));
        } finally {
            endRequest(theServletRequest);
        }
    }



    /**
     * /Patient/$export
     * @param theRequestDetails
     */
    //@formatter:off
    @Operation(name = "export", idempotent = true, bundleType=BundleTypeEnum.SEARCHSET)
    public IBaseResource patientTypeEverything(

            javax.servlet.http.HttpServletRequest theServletRequest,

            @Description(formalDefinition="Resources updated after this period will be included in the response.")
            @OptionalParam(name="since") InstantType since,

            @Description(formalDefinition="string of comma-delimited FHIR resource types.")
            @OperationParam(name="type") StringAndListParam type,

            RequestDetails theRequestDetails
    ) {
        if (theRequestDetails.getHeader("Accept") == null) {
            return createErrorOutcome("Please provide the Accept header, which must be set to application/fhir+json");
        } else if (!theRequestDetails.getHeader("Accept").equals("application/fhir+json")) {
            return createErrorOutcome("Only the application/fhir+json value for the Accept header is currently supported");
        }

        BulkDataHelper helper = new BulkDataHelper(provider);

        if (theRequestDetails.getHeader("Accept") == null) {
            return helper.createErrorOutcome("Please provide the Accept header, which must be set to application/fhir+json");
        } else if (!theRequestDetails.getHeader("Accept").equals("application/fhir+json")) {
            return helper.createErrorOutcome("Only the application/fhir+json value for the Accept header is currently supported");
        }

        DateRangeParam theLastUpdated = null;
        if ( since !=null ) {
            theLastUpdated = new DateRangeParam();
            theLastUpdated.setLowerBound(new DateParam().setValue(since.getValue()));
        }

        SearchParameterMap searchMap = new SearchParameterMap();
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
                    resolvedResources = helper.resolveType(theType.getValue(), searchMap);
                    if (!resolvedResources.isEmpty()) {
                        resources.add(resolvedResources);
                    }
                }
            }
        }
        else {
            for (String theType : helper.compartmentPatient) {
                resolvedResources = helper.resolveType(theType, searchMap);
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

    private Bundle createExportBundle(IBundleProvider iBundleProvider, String type, String fhirServerBase) {
        List<String> typeFilter = new ArrayList<>();
        if ( type!=null ) {
            for (String str : type.split(",")) {
                if (!str.isEmpty()) {
                    typeFilter.add(str);
                }
            }
        }
        Bundle bundle = new Bundle();
        List<Resource> resourceList = iBundleProvider.getResources(0,iBundleProvider.size()).stream()
                .filter( obj -> obj instanceof Resource )
                .map( obj -> (Resource)obj )
                .filter( resource -> type==null || typeFilter.stream().anyMatch( typeFilterStr -> typeFilterStr.equals( resource.fhirType())))
                .collect(Collectors.toList());
        bundle.setTotal(resourceList.size());
        resourceList.stream()
                .forEach( resource ->
                        bundle.addEntry()
                                .setResource(resource)
                                .setFullUrl(fhirServerBase+resource.getId())
                );

        return bundle;
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
}
