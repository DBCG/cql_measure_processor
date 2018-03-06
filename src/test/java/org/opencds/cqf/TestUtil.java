package org.opencds.cqf;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.model.primitive.IdDt;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.client.api.ServerValidationModeEnum;
import ca.uhn.fhir.rest.client.interceptor.LoggingInterceptor;
import com.google.common.base.Charsets;
import com.google.common.io.Resources;
import org.apache.commons.io.IOUtils;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.webapp.WebAppContext;
import org.hl7.fhir.dstu3.hapi.ctx.HapiWorkerContext;
import org.hl7.fhir.dstu3.hapi.validation.DefaultProfileValidationSupport;
import org.hl7.fhir.dstu3.model.*;
import org.hl7.fhir.dstu3.utils.FHIRPathEngine;
import org.hl7.fhir.exceptions.FHIRException;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.junit.Assert;

import java.io.*;
import java.net.URL;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import static org.junit.Assert.assertNotNull;

public class TestUtil {
    private static FhirContext ourCtx = FhirContext.forDstu3();

    public static void printResourceToXml(Resource resource) {
        String xml = ourCtx.newXmlParser().setPrettyPrint(true).encodeResourceToString(resource);
        System.out.println("------------------------------------------------");
        System.out.println(xml);
        System.out.println("------------------------------------------------");
    }

    public static void printResourceToJson(Resource resource) {
        String xml = ourCtx.newJsonParser().setPrettyPrint(true).encodeResourceToString(resource);
        System.out.println("------------------------------------------------");
        System.out.println(xml);
        System.out.println("------------------------------------------------");
    }

    public static FhirContext getCtx() {
        return ourCtx;
    }

    public static InputStream loadResource(String resourceFileName ) throws IOException {
        URL url = Resources.getResource(resourceFileName);
        return url.openStream();
    }

    public static IBaseResource loadJsonResource(String resourceFileName ) throws IOException {
        Scanner scanner = new Scanner(loadResource(resourceFileName)).useDelimiter("\\A");
        String json = scanner.hasNext() ? scanner.next() : "";
        return ourCtx.newJsonParser().parseResource(json);
    }

    public static IBaseResource loadXmlResource(String resourceFileName ) throws IOException {
        Scanner scanner = new Scanner(loadResource(resourceFileName)).useDelimiter("\\A");
        String json = scanner.hasNext() ? scanner.next() : "";
        return ourCtx.newXmlParser().parseResource(json);
    }

    public static List<IBaseResource> loadJsonResourcesNoBundle(String directory){
        ArrayList< IBaseResource> list = new ArrayList<>();
        try {
            List<String> files = IOUtils.readLines( TestUtil.class.getClassLoader().getResourceAsStream(directory), Charsets.UTF_8 );

            for ( String fileName: files ){
                if ( fileName.endsWith("json")) {
                    System.out.println("load resource "+fileName);
                    try {
                        IBaseResource baseResource = loadJsonResource(directory + "/" + fileName);
                        if (baseResource instanceof Bundle) {
                            System.out.println("ignore bundle");
                        } else {
                            list.add(baseResource);
                            System.out.println("Adding resource "+baseResource);
                        }
                    } catch (Exception e1) {
                        System.out.println("Failed to load "+fileName);
                    }
                } else if ( fileName.endsWith("xml")) {
                    System.out.println("load resource "+fileName);
                    try {
                        IBaseResource baseResource = loadXmlResource(directory + "/" + fileName);
                        if (baseResource instanceof Bundle) {
                            System.out.println("ignore bundle");
                        } else {
                            list.add(baseResource);
                            System.out.println("Adding resource "+baseResource);
                        }
                    } catch (Exception e1) {
                        System.out.println("Failed to load "+fileName);
                    }

                }

                }
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println("--- Loading Done --- ");
        return list;
    }

    public static List<IBaseResource> loadAllResources(String directory) {
        ArrayList< IBaseResource> list = new ArrayList<>();
        try {
            List<String> files = IOUtils.readLines( TestUtil.class.getClassLoader().getResourceAsStream(directory), Charsets.UTF_8 );

            for ( String fileName: files ){
                if ( fileName.endsWith("json")) {
                    System.out.println("load resource "+fileName);
                    try {
                        IBaseResource baseResource = loadJsonResource(directory + "/" + fileName);
                        if (baseResource instanceof Bundle) {
                            Bundle bundle = (Bundle)baseResource;
                            bundle.getEntry().forEach(
                                bundleEntryComponent -> {
                                    list.add(bundleEntryComponent.getResource());
                                    System.out.println("Adding bundle resource "+bundleEntryComponent.getResource().getId());
                                }
                            );
                        } else {
                            list.add(baseResource);
                        }
                    } catch (Exception e1) {
                        System.out.println("Failed to load "+fileName);
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println("--- Loading Done --- ");
        return list;
    }

    public static List<Base> executeFhirPath(Resource baseResource, String expression ) throws FHIRException {
        HapiWorkerContext hapiWorkerContext = new HapiWorkerContext(TestUtil.getCtx(), new DefaultProfileValidationSupport());
        FHIRPathEngine fhirPathEngine = new FHIRPathEngine(hapiWorkerContext);

        return fhirPathEngine.evaluate(baseResource, expression );
    }

    private static int ourPort;
    private static Server ourServer=null;
    private static String ourServerBase;
    private static String ourCdsServerBase;
    private static IGenericClient ourClient;

    public static synchronized void startServer() throws Exception {
        if ( ourServer==null ) {

            String path = Paths.get("").toAbsolutePath().toString();

            // changing from random to hard coded
            ourPort = 9000;
            ourServer = new Server(ourPort);

            WebAppContext webAppContext = new WebAppContext();
            webAppContext.setContextPath("/cqf-ruler");
            webAppContext.setDescriptor(path + "/src/main/webapp/WEB-INF/web.xml");
            webAppContext.setResourceBase(path + "/target/cqf-ruler");
            webAppContext.setParentLoaderPriority(true);

            ourServer.setHandler(webAppContext);
            ourServer.start();

            ourCtx.getRestfulClientFactory().setServerValidationMode(ServerValidationModeEnum.NEVER);
            ourCtx.getRestfulClientFactory().setSocketTimeout(1200 * 1000);
            ourServerBase = "http://localhost:" + ourPort + "/cqf-ruler/baseDstu3";
            ourCdsServerBase = "http://localhost:" + ourPort + "/cqf-ruler/cds-services";
            ourClient = ourCtx.newRestfulGenericClient(ourServerBase);
            ourClient.registerInterceptor(new LoggingInterceptor(true));

            putResource("/org/opencds/cqf/general-fhirhelpers-3.json", "FHIRHelpers");
        }
    }

    public static synchronized void stopServer() throws Exception {
        if ( ourServer!=null){
            ourServer.stop();
            ourServer=null;
        }
    }

    private static void putResource(String resourceFileName, String id) throws Exception {
        startServer();

        InputStream is = RulerTestBase.class.getResourceAsStream(resourceFileName);
        Scanner scanner = new Scanner(is).useDelimiter("\\A");
        String json = scanner.hasNext() ? scanner.next() : "";
        IBaseResource resource = ourCtx.newJsonParser().parseResource(json);

        if (resource instanceof Bundle) {
            ourClient.transaction().withBundle((Bundle) resource).execute();
        }
        else {
            ourClient.update().resource(resource).withId(id).execute();
        }
    }

    public static void putResource(IBaseResource resource) throws Exception {

        startServer();
        if (resource instanceof Bundle) {
            ourClient.transaction().withBundle((Bundle) resource).execute();
        }
        else {
            ourClient.update().resource(resource).execute();
        }
    }

    public static IGenericClient getOurClient() throws Exception {
        startServer();
        return ourClient;
    }

    public static Resource applyActivityDefinition( String activityDefinitionId, Parameters parameters ) {
            Parameters outParams = ourClient
                .operation()
                .onInstance(new IdDt("ActivityDefinition", activityDefinitionId))
                .named("$apply")
                .withParameters(parameters)
                .useHttpGet()
                .execute();

            List<Parameters.ParametersParameterComponent> response = outParams.getParameter();

            Assert.assertTrue(!response.isEmpty());

            Resource resource = response.get(0).getResource();

            return resource;
    }

    public static CarePlan applyPlanDefinition( String planDefinitionId, Parameters parameters) {
        Parameters outParams = ourClient
            .operation()
            .onInstance(new IdDt("PlanDefinition", planDefinitionId))
            .named("$apply")
            .withParameters(parameters)
            .useHttpGet()
            .execute();

        List<Parameters.ParametersParameterComponent> response = outParams.getParameter();

        Assert.assertTrue(!response.isEmpty());

        Resource resource = response.get(0).getResource();

        Assert.assertTrue(resource instanceof CarePlan);

        CarePlan carePlan = (CarePlan) resource;

        assertNotNull( carePlan );

        return carePlan;
    }

    public static void saveResources(List<IBaseResource> resources, String directory) throws IOException {
        String root = "./examples/"+directory;
        new File(root).mkdirs();

        for ( IBaseResource iBaseResource: resources ){
            Resource resource = (Resource)iBaseResource;
            String json = TestUtil.getCtx().newJsonParser().setPrettyPrint(true).encodeResourceToString(resource);

            BufferedWriter bufferedWriter = null;
            FileWriter fileWriter = null;
            String filename= String.format("%s/%s_%s.json", root, resource.fhirType(), resource.getId());
            fileWriter = new FileWriter(filename);
            bufferedWriter = new BufferedWriter(fileWriter);
            bufferedWriter.write(json);
            bufferedWriter.close();
        }
    }

}
