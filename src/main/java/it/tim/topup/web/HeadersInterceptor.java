package it.tim.topup.web;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.tim.topup.common.headers.TimHeaders;
import it.tim.topup.common.headers.TimHeadersExtractor;
import lombok.extern.slf4j.Slf4j;

import org.jboss.logging.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.configurationprocessor.json.JSONObject;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Created by alongo on 10/05/18.
 */
@Component
@Slf4j
public class HeadersInterceptor extends HandlerInterceptorAdapter {

    private TimHeaders headers;
    private ObjectMapper objectMapper;

    @Autowired
    public HeadersInterceptor(TimHeaders headers, ObjectMapper objectMapper) {
        this.headers = headers;
        this.objectMapper = objectMapper;
        try {
    		InputStream inputStream = TypeReference.class.getResourceAsStream("/promotions.json");
        	BufferedReader bR = new BufferedReader(  new InputStreamReader(inputStream));
        	String line = "";

        	StringBuilder responseStrBuilder = new StringBuilder();
        	while((line =  bR.readLine()) != null){

        	    responseStrBuilder.append(line);
        	}
    		inputStream.close();
			JSONObject result= new JSONObject(responseStrBuilder.toString());
			log.info("AAAAAAAAAAAA-->" + result.toString());
			
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}     

    }


    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        try{
            TimHeadersExtractor.populateHeaders(headers, objectMapper, request);
        }catch (Exception e){
            log.error("An error occured while extracting headers from request",e);
        }
        long startTime = System.currentTimeMillis();
        request.setAttribute("startTime", startTime);
        return true;
    }
    
    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        super.afterCompletion(request, response, handler, ex);
        long startTime = (Long)request.getAttribute("startTime");

        long endTime = System.currentTimeMillis();

        long executeTime = endTime - startTime;
        MDC.put("processElapsed", String.valueOf(executeTime));
        MDC.put("endpoint", request.getRequestURI());
        MDC.put("method", request.getMethod());
        MDC.put("URL", request.getRequestURL());
        MDC.put("remotePort", request.getRemotePort());
        MDC.put("remoteHost", request.getRemoteHost());
        MDC.put("status", response.getStatus());
        log.info("Request completed.");
    }

}
