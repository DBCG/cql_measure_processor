package org.opencds.cqf.servlet;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.FhirVersionEnum;
import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.Resource;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.opencds.cqf.async.AsyncHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

/**
 * Created by Bas van den Heuvel.
 */
@WebServlet(name = "async-services")
public class AsyncServicesServlet extends HttpServlet {
    private static final String searchString = "async-services/";
    private static FhirContext fhirContext = new FhirContext(FhirVersionEnum.DSTU3);
    private static final Logger logger = LoggerFactory.getLogger(AsyncServicesServlet.class );


    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

        //Request(GET //localhost:9001/cqf-ruler/bulkdata-services)@2375c4f6
        //Request(GET //localhost:9001/cqf-ruler/bulkdata-services/93523457-147582349)@456bc3d9
        String requestUrl = request.getRequestURI();

        if (!requestUrl.endsWith(searchString)) {
            String bulkdataserviceId = requestUrl.substring(requestUrl.indexOf(searchString) + searchString.length(), requestUrl.length());
            String resourceName = null;
            if (bulkdataserviceId.contains("/")) {
                // get session data
                resourceName = bulkdataserviceId.substring(bulkdataserviceId.indexOf("/") + 1, bulkdataserviceId.length());
                bulkdataserviceId = bulkdataserviceId.substring(0, bulkdataserviceId.indexOf("/"));
            }

            switch (AsyncHelper.getSessionStatus(bulkdataserviceId)) {
                case PROCESSING: {
                        response.setStatus(202);
                        String linkHeader = "";
                        JSONObject json = new JSONObject();

                        Date transactionTime = AsyncHelper.getSessionTransActionTime(bulkdataserviceId);
                        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                        json.put("transactionTime", sdf.format(transactionTime).toString());

                        json.put("request", AsyncHelper.getRequestUrl(bulkdataserviceId));
                        json.put("secure", "false");
                        response.setContentType("text/x-json;charset=UTF-8");
                        response.setHeader("Cache-Control", "no-cache");
    //                    response.setHeader("X-Progress",bulkDataSession.getStatusDescription());
                        try {
                            response.getWriter().write(json.toJSONString());
                        } catch (IOException e) {
                            logger.error("Oeps something went wrong");
                        }
                    }
                break;
                case UNKNOWN:
                    response.setStatus(404);
                break;
                case READY: {
                    if (resourceName == null) {
                        response.setStatus(200);
                            /*LINK IN HDR*/
                        String linkHeader = "";
                        JSONObject json = new JSONObject();

                        Date transactionTime = AsyncHelper.getSessionTransActionTime(bulkdataserviceId);
                        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                        json.put("transactionTime", sdf.format(transactionTime).toString());

                        json.put("request", AsyncHelper.getRequestUrl(bulkdataserviceId));
                        json.put("secure", "false");

                        //                        IBaseResource resource = AsyncHelper.getResource( bulkdataserviceId );
                        //
                        //                        String linkedResourceName = "resource";
                        //                        JSONObject ouput = new JSONObject();
                        ////                        linkObject.put("type", linkedResourceName);
                        //                        String resourceUrl = request.getRequestURL().toString() + "/resource" ;
                        //                        ouput.put("url", resourceUrl);
                        //                        json.put( "output", ouput );

                        JSONArray outputLinks = new JSONArray();
                        Iterator<String> resourceNameIterator = AsyncHelper.getSessionResultResourceNames(bulkdataserviceId).iterator();

                        while (resourceNameIterator.hasNext()) {
                            String linkedResourceName = resourceNameIterator.next();
                            JSONObject linkObject = new JSONObject();
                            linkObject.put("type", linkedResourceName);
                            String resourceUrl = request.getRequestURL().toString() + "/" + linkedResourceName;
                            linkObject.put("url", resourceUrl);
                            outputLinks.add(linkObject);
                                /*LINK IN HDR*/
                            linkHeader += "<" + resourceUrl + ">";
                            if (resourceNameIterator.hasNext()) {
                                linkHeader += ",";
                            }
                        }
                        json.put("output", outputLinks);
                        response.setContentType("text/x-json;charset=UTF-8");
                        response.setHeader("Cache-Control", "no-cache");
                        try {
                            response.getWriter().write(json.toJSONString());
                        } catch (IOException e) {
                            logger.error("Oeps something went wrong");
                        }
                    } else {
                        List<Resource> resourceList = AsyncHelper.getResources(bulkdataserviceId, resourceName);

                        if ( request.getHeader("Content-type")!=null && request.getHeader("Content-Type").equals("application/fhir+ndjson") ){
                            String ndjosn = "";

                            Iterator<Resource> resourceIterator = resourceList.iterator();
                            while (resourceIterator.hasNext()) {
                                Resource resource = resourceIterator.next();
                                String resourceJson = fhirContext.newJsonParser().encodeResourceToString(resource);
                                ndjosn += resourceJson.replace("\n", " ") + "\n";
                                response.setContentType("application/fhir+ndjson;charset=UTF-8");
                                response.setHeader("Cache-Control", "no-cache");
                            }
                            try {
                                response.getWriter().write(ndjosn);
                            } catch (IOException e) {
                                logger.error("Oeps something went wrong");
                            }
                        } else {
                            // download file
                            Bundle bundle = new Bundle();
                            resourceList.stream().forEach( resource -> bundle.addEntry().setResource(resource));
                            String resourceJson = fhirContext.newJsonParser().encodeResourceToString(bundle);
                            response.setContentType("text/json;charset=UTF-8");
                            response.setHeader("Cache-Control", "no-cache");
                            try {
                                response.getWriter().write(resourceJson);
                            } catch (IOException e) {
                                logger.error("Oeps something went wrong");
                            }
                        }
                    }
                }
                break;
            }
        }
    }

    @Override
    protected void doDelete(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String requestUrl = request.getRequestURI();
        String resourceName = null;

        String sessionId = requestUrl.substring(requestUrl.indexOf(searchString) + searchString.length(), requestUrl.length());

        if (!requestUrl.endsWith(searchString)) {
            if (sessionId.contains("/")) {
                sessionId = sessionId.substring(0, sessionId.indexOf("/"));
            }
        }
        AsyncHelper.deleteSession(sessionId);
        response.setStatus(200);
    }
}