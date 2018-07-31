package org.opencds.cqf.providers;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.jpa.dao.SearchParameterMap;
import ca.uhn.fhir.jpa.provider.dstu3.JpaResourceProviderDstu3;
import ca.uhn.fhir.rest.api.server.IBundleProvider;
import ca.uhn.fhir.rest.client.api.ServerValidationModeEnum;
import ca.uhn.fhir.rest.param.*;
import ca.uhn.fhir.rest.server.IResourceProvider;
import org.hl7.fhir.dstu3.model.Enumeration;
import org.hl7.fhir.dstu3.model.InstantType;
import org.hl7.fhir.instance.model.api.IAnyResource;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.opencds.cqf.cql.data.fhir.FhirDataProviderStu3;
import org.opencds.cqf.cql.runtime.Code;
import org.opencds.cqf.cql.runtime.DateTime;
import org.opencds.cqf.cql.runtime.Interval;
import org.opencds.cqf.cql.terminology.ValueSetInfo;

import java.util.*;

public class JpaDataProvider extends FhirDataProviderStu3 {

    // need these to access the dao
    private Collection<IResourceProvider> collectionProviders;

    public Collection<IResourceProvider> getCollectionProviders() {
        return this.collectionProviders;
    }

    public JpaDataProvider(Collection<IResourceProvider> providers) {
        this.collectionProviders = providers;
        setPackageName("org.hl7.fhir.dstu3.model");
        setFhirContext(FhirContext.forDstu3());
    }

    public Iterable<Object> retrieve(String context, Object contextValue, String dataType, String templateId,
                                     String codePath, Iterable<Code> codes, String valueSet, String datePath,
                                     String dateLowPath, String dateHighPath, Interval dateRange)
    {

        SearchParameterMap map = new SearchParameterMap();
        map.setLastUpdated(new DateRangeParam());

        if (templateId != null && !templateId.equals("")) {
            // do something?
        }

        if (valueSet != null && valueSet.startsWith("urn:oid:")) {
            valueSet = valueSet.replace("urn:oid:", "");
        }

        if (codePath == null && (codes != null || valueSet != null)) {
            throw new IllegalArgumentException("A code path must be provided when filtering on codes or a valueset.");
        }

        if (dataType == null) {
            throw new IllegalArgumentException("A data type (i.e. Procedure, Valueset, etc...) must be specified for clinical data retrieval");
        }

        if (context != null && context.equals("Patient") && contextValue != null) {
            ReferenceParam patientParam = new ReferenceParam(contextValue.toString());
            map.add(getPatientSearchParam(dataType), patientParam);
        }

        if (codePath != null && !codePath.equals("")) {

            if (valueSet != null && terminologyProvider != null && expandValueSets) {
                ValueSetInfo valueSetInfo = new ValueSetInfo().withId(valueSet);
                codes = terminologyProvider.expand(valueSetInfo);
            }
            else if (valueSet != null) {
                map.add(convertPathToSearchParam(dataType, codePath), new TokenParam(null, valueSet).setModifier(TokenParamModifier.IN));
            }
            if (codes != null) {
                TokenOrListParam codeParams = new TokenOrListParam();
                for (Code code : codes) {
                    codeParams.addOr(new TokenParam(code.getSystem(), code.getCode()));
                }
                map.add(convertPathToSearchParam(dataType, codePath), codeParams);
            }
        }

        if (dateRange != null) {
            DateParam low = null;
            DateParam high = null;
            if (dateRange.getLow() != null) {
                low = new DateParam(ParamPrefixEnum.GREATERTHAN_OR_EQUALS, ((DateTime) dateRange.getLow()).getJodaDateTime().toDate());
            }

            if (dateRange.getHigh() != null) {
                high = new DateParam(ParamPrefixEnum.LESSTHAN_OR_EQUALS, ((DateTime) dateRange.getHigh()).getJodaDateTime().toDate());
            }

            DateRangeParam rangeParam;
            if (low == null && high != null) {
                rangeParam = new DateRangeParam(high);
            }
            else if (high == null && low != null) {
                rangeParam = new DateRangeParam(low);
            }
            else {
                rangeParam = new DateRangeParam(low, high);
            }

            map.add(convertPathToSearchParam(dataType, datePath), rangeParam);
        }

        JpaResourceProviderDstu3<? extends IAnyResource> jpaResProvider = resolveResourceProvider(dataType);
        IBundleProvider bundleProvider = jpaResProvider.getDao().search(map);
        List<IBaseResource> resourceList = bundleProvider.getResources(0, 10000);
        return resolveResourceList(resourceList);
    }

    public Iterable<Object> resolveResourceList(List<IBaseResource> resourceList) {
        List<Object> ret = new ArrayList<>();
        for (IBaseResource res : resourceList) {
            Class clazz = res.getClass();
            ret.add(clazz.cast(res));
        }
        // ret.addAll(resourceList);
        return ret;
    }

    public JpaResourceProviderDstu3<? extends IAnyResource> resolveResourceProvider(String datatype) {
        for (IResourceProvider resource : collectionProviders) {
            if (resource.getResourceType().getSimpleName().toLowerCase().equals(datatype.toLowerCase())) {
                return (JpaResourceProviderDstu3<? extends IAnyResource>) resource;
            }
        }
        throw new RuntimeException("Could not find resource provider for type: " + datatype);
    }

    public void setValue(Object target, String path, Object value) {
        if ( target instanceof InstantType && value instanceof DateTime ){
            DateTime dateTime = (DateTime)value;
            Date date =((DateTime)value).getJodaDateTime().toDate();
            super.setValue( target, path, date );
        } else {
            super.setValue( target, path, value );
        }

    }
}
