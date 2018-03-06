package org.opencds.cqf.async;

import org.hl7.fhir.dstu3.model.Resource;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class AsyncHelper {
    static final Logger logger = LoggerFactory.getLogger(AsyncHelper.class );
    private static Map<String, AsyncSession> sessionMap = new TreeMap<>();

    public static IBaseResource getResource(String sessionId ) {
        AsyncSession asyncSession = sessionMap.get( sessionId );
        return ( asyncSession!=null? asyncSession.getResultResource():null);

    }
    public static List<Resource> getResources(String sessionId, String resourceName) {
        AsyncSession asyncSession = sessionMap.get( sessionId );
        List<Resource> resources = asyncSession.getResultResources( resourceName );

        return resources;
    }
    public static List<String> getSessionResultResourceNames(String sessionId) {
        AsyncSession asyncSession = sessionMap.get( sessionId );
        List<String> resourceNameList = asyncSession.getResultResourceNames();

        return resourceNameList;
    }

    public static Date getSessionTransActionTime(String sessionId) {
        AsyncSession bulkDataSession = sessionMap.get( sessionId );
        return (bulkDataSession!=null ? bulkDataSession.getTransActionTime(): null );
    }

    public static void deleteSession(String bulkdataserviceId) {
        sessionMap.remove(bulkdataserviceId);
    }

    public static String getRequestUrl(String sessionId) {
        AsyncSession session = sessionMap.get( sessionId );
        return (session!=null? session.getCallUrl() : null );
    }

    public enum Status {PROCESSING,UNKNOWN,READY}

    public static String newAyncGetSession( HttpServletRequest request, HttpServletResponse response) {
        String sessionId = ""+ System.currentTimeMillis();
        AsyncSession asyncSession = new AsyncSession( request );
        sessionMap.put( sessionId, asyncSession );

        return sessionId;
    }

    public static Status getSessionStatus(String bulkdataserviceId) {
        AsyncSession bulkDataSession = sessionMap.get( bulkdataserviceId );
        if ( bulkDataSession==null){
            return Status.UNKNOWN;
        }
        if ( bulkDataSession.isReady()){
            return Status.READY;
        } else
        {
            return Status.PROCESSING;
        }

    }

}
