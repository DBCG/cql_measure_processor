package org.opencds.cqf.async;

import ca.uhn.fhir.context.FhirContext;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClientBuilder;
import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.Resource;
import org.hl7.fhir.instance.model.api.IBaseResource;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;
import java.util.stream.Collectors;

public class AsyncSession {
    private static FhirContext ourCtx = FhirContext.forDstu3();
    private final Date transActionTime;
    private final AsyncSessionProcessor processor;
    private String callUrl;
    private final HashMap<String, String[]> parameterValues;
    private HashMap<String,String> headerValues;
    private AsyncResult asyncResult;

    public AsyncSession( HttpServletRequest httpServletRequest ) {
        this.transActionTime = new Date();
//        this.httpServletRequest = httpServletRequest;
//        this.httpServiceResponse = httpServletResponse;
        headerValues = new HashMap<>();
        Enumeration<String> headers = httpServletRequest.getHeaderNames();
        while( headers.hasMoreElements() ){
            String headerName = headers.nextElement();
            headerValues.put(headerName, httpServletRequest.getHeader(headerName));
        }

        parameterValues = new HashMap<String, String[]>();
        httpServletRequest.getParameterMap().entrySet().forEach( entry ->
            parameterValues.put( entry.getKey(), entry.getValue()));

        headerValues.remove("Prefer");
        this.callUrl = httpServletRequest.getRequestURL().toString();

        processor = new AsyncSessionProcessor();
        (new Thread(processor)).start();
    }

    public boolean isReady() {
        return processor.isDone;
    }

    public Date getTransActionTime() {
        return transActionTime;
    }

    public IBaseResource getResultResource() {
        return asyncResult.getResultResource();
    }

    public String getCallUrl() {
        return callUrl.toString();
    }

    public List<Resource> getResultResources(String resourceName) {
        return processor.getResult().resultTreeMap.get(resourceName).values()
            .stream().collect(Collectors.toList());
    }

    public List<String> getResultResourceNames() {
        return  processor.getResult().resultTreeMap.keySet().stream()
            .collect(Collectors.toList());
    }

    private class AsyncSessionProcessor implements Runnable {
        private boolean isDone = false;

        @Override
        public synchronized void run() {
            AsyncHelper.logger.info("Async processing start");
            try {
                IBaseResource iBaseResource = retrieveInitialResource( callUrl, parameterValues, headerValues);

                asyncResult = new AsyncResult();
                if ( iBaseResource instanceof Bundle) {
                    Bundle bundle = (Bundle)iBaseResource;
                    asyncResult.addBundle( bundle );
                    processBundle( asyncResult, bundle, headerValues);
                } else if ( iBaseResource instanceof Resource) {
                    asyncResult.addResource( (Resource)iBaseResource );
                }

            } catch (URISyntaxException | IOException e) {
                e.printStackTrace();
            }

            AsyncHelper.logger.info("Async processing done");
            isDone = true;
        }




        public AsyncResult getResult() {
            return asyncResult;
        }
    }
    static IBaseResource retrieveInitialResource( String callUrl, HashMap<String, String[]> parameterValues, HashMap<String,String> headerValues) throws URISyntaxException, IOException {
        HttpClient httpClient = HttpClientBuilder.create().build();
        String fhirCallUrl = callUrl;
        Iterator<Map.Entry<String, String[]>> it = parameterValues.entrySet().iterator();
        if ( it.hasNext() ){ fhirCallUrl = fhirCallUrl+"?"; }
        while( it.hasNext() ){
            Map.Entry<String, String[]> entry = it.next();
            fhirCallUrl = fhirCallUrl+entry.getKey()+"="+entry.getValue()[0];
            if ( it.hasNext()){ fhirCallUrl = fhirCallUrl+"&"; }
        }
        callUrl = fhirCallUrl;
        HttpGet get = new HttpGet( new URI( fhirCallUrl ) );
        headerValues.entrySet().stream()
                .forEach( entry -> get.setHeader( entry.getKey(), entry.getValue() ));
         HttpResponse response = httpClient.execute(get);
        InputStream inputStream = response.getEntity().getContent();

        Scanner s = new Scanner(inputStream).useDelimiter("\\A");
        String result = s.hasNext() ? s.next() : "";

        return ourCtx.newJsonParser().parseResource(result);
    }


    static void processBundle( AsyncResult asyncResult, Bundle bundle,HashMap<String,String> headerValues  ) {
        AsyncHelper.logger.debug("process Bundle");
        HttpClient httpClient = HttpClientBuilder.create().build();

        bundle.getLink().stream()
                .filter( link -> link.getRelation().equals("next"))
                .map(link ->  retrieveNextBundle( link.getUrl(), headerValues) )
                .forEach( nextBundle -> {
                    asyncResult.addBundle(nextBundle);
                    processBundle( asyncResult, nextBundle, headerValues );
                });
        AsyncHelper.logger.debug("process Bundle  done");

    }

    private static Bundle retrieveNextBundle( String bundleUrl, HashMap<String,String> headerValues)  {
        HttpClient httpClient = HttpClientBuilder.create().build();

        AsyncHelper.logger.debug("retrieve next Bundle "+bundleUrl);
        Bundle bundle = new Bundle();
        try {
            HttpGet get = new HttpGet( new URI( bundleUrl) );
            headerValues.entrySet().stream()
                    .forEach( entry -> get.setHeader( entry.getKey(), entry.getValue() ));
            HttpResponse response = httpClient.execute(get);
            InputStream inputStream = response.getEntity().getContent();

            Scanner s = new Scanner(inputStream).useDelimiter("\\A");
            String result = s.hasNext() ? s.next() : "";
            bundle = (Bundle) ourCtx.newJsonParser().parseResource(result);
        } catch (URISyntaxException | IOException e) {
            e.printStackTrace();
        }

        AsyncHelper.logger.debug("retrieve next Bundle Done");
        return bundle;
    }
}

