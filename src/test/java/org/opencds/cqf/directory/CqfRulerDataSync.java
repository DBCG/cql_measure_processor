package org.opencds.cqf.directory;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClientBuilder;
import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.junit.Test;
import org.opencds.cqf.TestServer;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Scanner;

import static org.junit.Assert.assertTrue;

public class CqfRulerDataSync {
    private TestServer testServer;
    private FhirContext ourCtx;

    @Test
    public void syncData() throws Exception {
        String url = "http://measure.eval.kanvix.com/cqf-ruler/baseDstu3";

        testServer = new TestServer();
        testServer.start();

        ourCtx = FhirContext.forDstu3();

        IGenericClient client = ourCtx.newRestfulGenericClient(url);

        storeAll(url, "Organization");
        storeAll(url, "Practitioner");
        storeAll(url, "Patient");
        storeAll(url, "Encounter");
        storeAll(url, "Observation");
        storeAll(url, "Library");
        storeAll(url, "PlanDefinition");
        storeAll(url, "ActivityDefinition");
        storeAll(url, "Condition");
        storeAll(url, "Procedure");
        storeAll(url, "DiagnosticReport");
//        storeAll(url, "DiagnosticReport");
//        Bundle observationBundle = retrieveAllPatientEverything(url, "Observation");

        testServer.stop();
    }
    private void storeAll( String baseUrl, String resourceId ) throws Exception {
        retrieveAllPatientEverything( baseUrl, resourceId)
            .getEntry().stream()
            .map( bundleEntryComponent -> bundleEntryComponent.getResource())
            .forEach( resource -> {
                try {
                    System.out.println(resourceId+":"+resource.getId());
                    testServer.putResource(resource);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });

    }
    private Bundle retrieveAllPatientEverything( String baseUrl, String resourceId ) throws Exception {
        HttpClient httpClient = HttpClientBuilder.create().build();

        String url = baseUrl+"/"+resourceId ;
        HttpGet get        = new HttpGet( url );

        HttpResponse response = httpClient.execute(get);
        InputStream inputStream = response.getEntity().getContent();

        Scanner s = new Scanner(inputStream).useDelimiter("\\A");
        String result = s.hasNext() ? s.next() : "";

        IBaseResource iBaseResource = ourCtx.newJsonParser().parseResource(result);
        assertTrue( iBaseResource instanceof Bundle );
        org.hl7.fhir.dstu3.model.Bundle bundle = (Bundle)iBaseResource;

        AsyncResult asyncResult = new AsyncResult();
        asyncResult.addBundle(bundle);
        AsyncSession.processBundle( asyncResult, bundle, new HashMap<String,String>());

        return asyncResult.resultBundle;
    }

}
