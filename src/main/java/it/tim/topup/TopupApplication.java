package it.tim.topup;

import it.tim.topup.common.headers.TimHeaders;
import it.tim.topup.web.HeaderForwardInterceptor;
import it.tim.topup.web.HeadersInterceptor;
import lombok.extern.slf4j.Slf4j;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.UUID;

import org.apache.log4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.configurationprocessor.json.JSONObject;
import org.springframework.cloud.client.circuitbreaker.EnableCircuitBreaker;
import org.springframework.cloud.netflix.feign.EnableFeignClients;
import org.springframework.cloud.netflix.hystrix.dashboard.EnableHystrixDashboard;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;

import com.fasterxml.jackson.core.type.TypeReference;

import lombok.extern.slf4j.Slf4j;

@SpringBootApplication
@EnableFeignClients
@EnableCircuitBreaker
@EnableHystrixDashboard
@EnableAspectJAutoProxy
@Slf4j
public class TopupApplication extends WebMvcConfigurerAdapter {

	public static void main(String[] args) {
		
		SpringApplication.run(TopupApplication.class, args);
	}



	private HeadersInterceptor headersInterceptor;

	@Autowired
    public TopupApplication(HeadersInterceptor headersInterceptor) {
        this.headersInterceptor = headersInterceptor;
        try {
    		InputStream inputStream = TypeReference.class.getResourceAsStream("/promotions.json");
        	BufferedReader bR = new BufferedReader(  new InputStreamReader(inputStream));
        	String line = "";

        	StringBuilder responseStrBuilder = new StringBuilder();
        	while((line =  bR.readLine()) != null){

        	    responseStrBuilder.append(line);
        	}
    		inputStream.close();
    		log.info("AAAAAAAAAAAA-->" + responseStrBuilder.toString());
			
			
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}     
        

    }

	@Bean
    @Scope(value = WebApplicationContext.SCOPE_REQUEST, proxyMode = ScopedProxyMode.TARGET_CLASS)
	public TimHeaders timHeaders(){
		return new TimHeaders();
	}

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(headersInterceptor)
                .addPathPatterns("/**/topup*/**/");
    }

}