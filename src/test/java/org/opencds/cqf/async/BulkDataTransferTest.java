package org.opencds.cqf.async;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.HttpClientBuilder;
import org.hl7.fhir.dstu3.model.*;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.json.simple.JSONArray;
import org.junit.BeforeClass;
import org.junit.Test;
import org.opencds.cqf.TestUtil;
import org.opencds.cqf.helpers.ResourceCreator;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Scanner;

import static junit.framework.TestCase.assertEquals;
import static org.junit.Assert.*;

public class BulkDataTransferTest {

    private static Bundle allPatientData;
    private static Bundle allPatientDataPa2;
    private static String patientId;
    private static String groupId;
    private static List<IBaseResource> resources = new ArrayList<>();

    @BeforeClass
    public static void startServer() throws Exception {
        TestUtil.startServer();

        ResourceCreator resourceCreator = new ResourceCreator();
        {
            Practitioner practitioner = resourceCreator.createPractitioner("BD-Pr1", "1949-01-23");
            Patient patient = resourceCreator.createPatient("BD-Pa1", "1986-01-15", practitioner);
            Encounter encounter = resourceCreator.createEncounter(patient, 1);
            Procedure procedure = resourceCreator.createProcedure( "BD-Pr1", 1, patient, practitioner, "2018-01-23" );
            resources.add(practitioner);
            resources.add(patient);
            resources.add(encounter);
            resources.add(procedure);
        }
        {
            Practitioner practitioner = resourceCreator.createPractitioner("BD-Pr2", "1949-02-23");
            Patient patient = resourceCreator.createPatient("BD-Pa2", "1986-02-15", practitioner);
            patientId = patient.getId();
            Encounter encounter = resourceCreator.createEncounter(patient, 2);
            Procedure procedure = resourceCreator.createProcedure( "BD-Pr2", 2, patient, practitioner, "2018-01-23" );
            resources.add(practitioner);
            resources.add(patient);
            resources.add(encounter);
            resources.add(procedure);
        }
        {
            Practitioner practitioner = resourceCreator.createPractitioner("BD-Pr3", "1949-03-23");
            Patient patient1 = resourceCreator.createPatient("BD-Pa3", "1986-03-15", practitioner);
            Encounter encounter = resourceCreator.createEncounter(patient1, 3);
            Procedure procedure = resourceCreator.createProcedure( "BD-Pro3", 3, patient1, practitioner, "2018-01-23" );
            Patient patient2 = resourceCreator.createPatient("BD-Pa4", "1986-04-15", practitioner);
            Patient patient3 = resourceCreator.createPatient("BD-Pa5", "1986-05-15", practitioner);
            Group group = (Group) new Group()
                    .setActive(true)
                    .setType(Group.GroupType.PERSON)
                    .setName("My bulk data test group")
                    .addMember( new Group.GroupMemberComponent().setEntity( new Reference(patient1) ))
                    .addMember( new Group.GroupMemberComponent().setEntity( new Reference(patient2) ))
                    .addMember( new Group.GroupMemberComponent().setEntity( new Reference(patient3) ))
                    .addMember( new Group.GroupMemberComponent().setEntity( new Reference(practitioner) ))
                    .setId("Group-BD-Grp1");
            resources.add( practitioner );
            resources.add( patient1 );
            resources.add( patient2 );
            resources.add( patient3 );
            resources.add( encounter );
            resources.add( procedure );
            resources.add( group );
            groupId=group.getId();
        }

        for (IBaseResource baseResource : resources) {
            TestUtil.putResource(baseResource);
        }

        allPatientData = retrieveAllPatientEverything(null);
        assertNotNull(allPatientData);
        assertTrue(allPatientData.getEntry().size()>1 );

        allPatientDataPa2 = retrieveAllPatientEverything( patientId );
        assertNotNull(allPatientDataPa2);
        assertTrue(allPatientDataPa2.getEntry().size()>1 );

        TestUtil.saveResources( resources, "bulkdata");

    }

    @Test
    public void callBulkDataServer() throws Exception {
        HttpResponse response  = callBulkDataService();

        String sessionUrl = getSessionUrl( response );
        response = getSessionStatus( sessionUrl );

        response = waitForCompleteness(response, sessionUrl);

        List<String> links = getLinks(response);

        for (String link : links) {
            checkLink(link, allPatientData);
        }
    }

    @Test
    public void callBulkDataServerOutcomeFilter() throws Exception {
        String filterType = "Encounter";
        HttpResponse response  = callBulkDataService(filterType );

        String sessionUrl = getSessionUrl( response );
        response = getSessionStatus( sessionUrl );

        response = waitForCompleteness(response, sessionUrl);

        List<String> links = getLinks(response);

        // check absense of
        links.stream()
                .forEach( link ->
                        assertTrue(filterType+" is only one to be present: "+link, link.endsWith(filterType))
                );
        // check presence of Patient
        assertEquals( "Check presence of "+filterType, 1, links.stream().filter( link -> link.endsWith(filterType)).count());

        for (String link : links) {
            checkLink(link, allPatientData);
        }
    }

    @Test
    public void callBulkDataServerOutcomeFilter2() throws Exception {
        String filterType1 = "Procedure";
        String filterType2 = "Encounter";
        HttpResponse response  = callBulkDataService(filterType1+","+filterType2 );

        String sessionUrl = getSessionUrl( response );
        response = getSessionStatus( sessionUrl );

        response = waitForCompleteness(response, sessionUrl);

        List<String> links = getLinks(response);

        // check absense of
        links.stream()
                .forEach( link ->
                        assertTrue(filterType1+", "+filterType2+" are the only one to be present: "+link, link.endsWith(filterType1)||link.endsWith(filterType2))
                );
        // check presence of Patient
        assertEquals( "Check presence of "+filterType1, 1, links.stream().filter( link -> link.endsWith(filterType1)).count());
        assertEquals( "Check presence of "+filterType2, 1, links.stream().filter( link -> link.endsWith(filterType2)).count());

        for (String link : links) {
            checkLink(link, allPatientData);
        }
    }

    @Test
    public void callGroupExport() throws Exception {

        JSONArray context = new JSONArray();

        String url = TestUtil.getOurClient().getServerBase()+"/Group/"+groupId+"/$export";

        HttpClient httpClient = HttpClientBuilder.create().build();
        HttpGet get = new HttpGet( url );
        get.setHeader("Content-type", "application/fhir+ndjson");
        get.setHeader("Prefer", "respond-async");
        get.setHeader("Accept", "application/fhir+json");


        HttpResponse response = httpClient.execute(get);

        String sessionUrl = getSessionUrl( response );
        response = getSessionStatus( sessionUrl );

        response = waitForCompleteness(response, sessionUrl);

        List<String> links = getLinks(response);

        assertTrue("at least two links are expected", links.size()>1);
    }
    @Test
    public void callBulkDataServerPatientSpecific() throws Exception {
        HttpResponse response  = callBulkDataService(null, patientId );

        String sessionUrl = getSessionUrl( response );
        response = getSessionStatus( sessionUrl );

        response = waitForCompleteness(response, sessionUrl);

        List<String> links = getLinks(response);

        for (String link : links) {
            checkLink(link, allPatientDataPa2);
        }
    }

    @Test
    public void testBulkDataServerDelete() throws Exception {
        HttpResponse response  = callBulkDataService("Procedure", "Patient/BD-Pa1");
        String sessionUrl = getSessionUrl( response );

        deleteSession( sessionUrl );

        response = getSessionStatus( sessionUrl );
        assertEquals( 404,  response.getStatusLine().getStatusCode());

    }

    private void deleteSession(String sessionUrl) throws IOException {
        HttpClient httpClient = HttpClientBuilder.create().build();
        HttpDelete delete     = new HttpDelete( sessionUrl );

        HttpResponse response = httpClient.execute(delete);
    }

    private List<String> getLinks(HttpResponse response) throws IOException {
        assertNotNull(response);

        InputStream inputStream = response.getEntity().getContent();
        Scanner s = new Scanner(inputStream).useDelimiter("\\A");
        String result = s.hasNext() ? s.next() : "";

        JsonObject jobject = (JsonObject) new JsonParser().parse(result);
        JsonArray jsonArray = (JsonArray) jobject.get("output");
        ArrayList<String> links = new ArrayList<>();
        for ( int i=0; i<jsonArray.size();i++){
            String linkProto =  ((JsonObject) jsonArray.get(i)).get("url").toString();
            links.add(linkProto.substring(1, linkProto.length()-1)); // remove quotes
        }

        return links;
    }

    private void checkLink(String link, Bundle patientData) throws IOException {

        List<Resource> resourcesNdjson = new ArrayList<>();
        String resourceTypeNdjson = getResourcesFromLink(link, resourcesNdjson, true);

        List<Resource> resourcesBundleList = new ArrayList<>();
        getResourcesFromLink(link, resourcesBundleList, false);
        assertEquals( 1, resourcesBundleList.size());
        assertTrue( resourcesBundleList.get(0) instanceof  Bundle );
        Bundle resourceBundle = (Bundle)resourcesBundleList.get(0);

        // resources contains ndjson resources.
        String resourceType = resourceTypeNdjson;
        long numberOfResources  = patientData.getEntry().stream()
                .map(Bundle.BundleEntryComponent::getResource)
                .filter( bdleResource -> bdleResource.fhirType()== resourceType)
                .count();
        assertEquals( "number of "+resourceType+" resources must be equal.", resourcesNdjson.size(), numberOfResources );
        assertEquals( "number of "+resourceType+" resources must be equal.", resourceBundle.getEntry().size(), numberOfResources );

        for ( Resource resource : resourcesNdjson) {
            assertTrue( resource.fhirType()+" not present", patientData.getEntry().stream()
                    .map(Bundle.BundleEntryComponent::getResource)
                    .filter( res -> res.fhirType()==resourceType)
                    .filter( res2 -> res2.getId().endsWith(resource.getId()))
                    .findFirst().isPresent()
            );
        }

        for ( Bundle.BundleEntryComponent bundleEntryComponent : resourceBundle.getEntry()) {
            Resource resource = bundleEntryComponent.getResource();
            assertTrue( patientData.getEntry().stream()
                    .map(allPatientBundleEntryComponent -> allPatientBundleEntryComponent.getResource())
                    .filter( res -> res.fhirType()==resourceType)
                    .filter( res2 -> res2.getId().endsWith(resource.getId()))
                    .findFirst().isPresent());
            assertEquals( resourceType, resource.fhirType());
        }
    }

    private String getResourcesFromLink(String link, List<Resource> resources, boolean ndjson) throws IOException {
        HttpClient httpClient = HttpClientBuilder.create().build();
        HttpGet get         = new HttpGet( link );
        if ( ndjson ) {
            get.setHeader("Content-type", "application/fhir+ndjson");
        }
        else{
            get.setHeader("Content-type", "application/json");
        }
        HttpResponse response = httpClient.execute(get);
        assertNotNull(response);

        InputStream inputStream = response.getEntity().getContent();
        Scanner s = new Scanner(inputStream).useDelimiter("\\A");
        String result = s.hasNext() ? s.next() : "";

        String resourceType = null;
        for ( String str : result.split("\\n")){
            Resource resource = (Resource) TestUtil.getCtx().newJsonParser().parseResource(str);
            if ( resourceType ==null ) {
                resourceType = resource.fhirType();
            } else {
                assertEquals(resourceType, resource.fhirType());
            }
            resources.add( resource );
        }
        ;
        return resourceType;
    }

    public static HttpResponse callBulkDataService() throws Exception {
        return callBulkDataService(null, null);
    }
    public static HttpResponse callBulkDataService(String outcome) throws Exception {
        return callBulkDataService(outcome, null);
    }
    public static HttpResponse callBulkDataService(String outcome, String patientId) throws Exception {

        TestUtil.startServer();

        JSONArray context = new JSONArray();

        String url = TestUtil.getOurClient().getServerBase()+"/Patient/$export";
        if ( patientId!=null ){
            url = TestUtil.getOurClient().getServerBase()+"/Patient/"+patientId+"/$export";
        }

        URIBuilder uriBuilder = new URIBuilder(url);
        if ( outcome!=null ){
            uriBuilder.addParameter("type", outcome);
        }

        HttpClient httpClient = HttpClientBuilder.create().build();
        HttpGet get = new HttpGet( uriBuilder.build() );
        get.setHeader("Content-type", "application/fhir+ndjson");
        get.setHeader("Prefer", "respond-async");
        get.setHeader("Accept", "application/fhir+json");


        HttpResponse response = httpClient.execute(get);

        return response;
    }

    private HttpResponse waitForCompleteness(HttpResponse response, String sessionUrl ) throws InterruptedException, IOException {
        int i=0;
        while( response.getStatusLine().getStatusCode()==202 && i<3000) {
            Thread.sleep(100);
            i++;
            response = getSessionStatus(sessionUrl);
        }
        assertEquals(200, response.getStatusLine().getStatusCode());
        assertNotEquals("Session not ready in time.", 3000, i);
        return response;
    }

    private static HttpResponse getSessionStatus(String sessionUrl) throws IOException {
        HttpClient httpClient = HttpClientBuilder.create().build();
        Gson gson             = new Gson();
//        HttpGet get         = new HttpGet( TestUtil.getOurClient().getServerBase()+"/Patient/"+ patientId +"" );
        HttpGet get         = new HttpGet( sessionUrl );

        HttpResponse response = httpClient.execute(get);
        return response;
    }

    private static String getSessionUrl(HttpResponse response) throws Exception {
        InputStream inputStream = response.getEntity().getContent();

        Scanner s = new Scanner(inputStream).useDelimiter("\\A");
        String result = s.hasNext() ? s.next() : "";

        IBaseResource iBaseResource = TestUtil.getCtx().newJsonParser().parseResource(result);
        assertTrue( iBaseResource instanceof OperationOutcome );
        OperationOutcome operationOutcome = (OperationOutcome)iBaseResource;
        assertNotNull( response.getFirstHeader("Content-Location") );
        String contentLocation = response.getFirstHeader("Content-Location").getValue();

        return  contentLocation;
    }


    private static Bundle retrieveAllPatientEverything( String patientId ) throws Exception {
        HttpClient httpClient = HttpClientBuilder.create().build();

        String url = TestUtil.getOurClient().getServerBase()+( patientId==null? "/Patient/$everything":"/Patient/"+patientId+"/$everything" );
        HttpGet    get        = new HttpGet( url );

        HttpResponse response = httpClient.execute(get);
        InputStream inputStream = response.getEntity().getContent();

        Scanner s = new Scanner(inputStream).useDelimiter("\\A");
        String result = s.hasNext() ? s.next() : "";

        IBaseResource iBaseResource = TestUtil.getCtx().newJsonParser().parseResource(result);
        assertTrue( iBaseResource instanceof Bundle );
        Bundle bundle = (Bundle)iBaseResource;

        AsyncResult asyncResult = new AsyncResult();
        asyncResult.addBundle(bundle);
        AsyncSession.processBundle( asyncResult, bundle, new HashMap<String,String>());

        return asyncResult.resultBundle;
    }


}
