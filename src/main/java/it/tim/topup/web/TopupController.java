package it.tim.topup.web;

import io.swagger.annotations.Api;
import com.fasterxml.jackson.core.type.TypeReference;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import it.tim.topup.aspects.Loggable;
import it.tim.topup.common.headers.TimHeaders;
import it.tim.topup.common.headers.TransientHeaderName;
import it.tim.topup.model.domain.Amounts;
import it.tim.topup.model.domain.CreditCardData;
import it.tim.topup.model.domain.TermsAndConditions;
import it.tim.topup.model.web.*;
import it.tim.topup.service.CreditCardTopupService;
import it.tim.topup.service.ScratchCardService;
import it.tim.topup.service.StaticContentService;
import it.tim.topup.validation.TopupControllerValidator;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.Base64.Encoder;
import java.util.Properties;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.configurationprocessor.json.JSONObject;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;


import static it.tim.topup.service.IdsGenerator.generateId10;
import static it.tim.topup.service.IdsGenerator.generateId;
import static it.tim.topup.service.IdsGenerator.generateTransactionId;

/**
 * Created by alongo on 13/04/18.
 */
@RestController
@RequestMapping("/api/topup")
@Api("Controller exposing topup operations")
public class TopupController {

    private ScratchCardService scratchCardService;
    private StaticContentService staticContentService;
    private CreditCardTopupService creditCardTopupService;

    private TimHeaders headers;

    private static final DateTimeFormatter AUTH_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
    
    @Autowired
    public TopupController(TimHeaders timHeaders, ScratchCardService scratchCardService, StaticContentService staticContentService,
                           CreditCardTopupService creditCardTopupService) {
    	       	this.headers = timHeaders;

        this.scratchCardService = scratchCardService;
        this.staticContentService = staticContentService;
        this.creditCardTopupService = creditCardTopupService;
    }

    @RequestMapping(method = RequestMethod.POST, value = "/ricaricard")
    @ApiOperation(value = "Refill operation with scratch card", response = ScratchCardResponse.class)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Refill success"),
            @ApiResponse(code = 400, message = "Missing or wrong mandatory parameters"),
            @ApiResponse(code = 404, message = "Wrong card number or card not found"),
            @ApiResponse(code = 401, message = "Not authorized due to max attempts reached"),
            @ApiResponse(code = 500, message = "Generic error"),
    })
    @Loggable
    public ScratchCardResponse scratchCard(@RequestBody ScratchCardRequest request)
    {
        TopupControllerValidator.validateScratchCardRequest(request.getCardNumber(), request.getFromMsisdn()
                , request.getToMsisdn(), request.getSubSys(), headers);
        String userReference = headers.getSession().getUserReference();
        String transactionId = headers.getHeader(TransientHeaderName.TRANSACTIONID);

        return new ScratchCardResponse(
                "OK",
                scratchCardService.rechargeWithScratchCard(userReference, request.getCardNumber(), request.getFromMsisdn()
                        , request.getFromMsisdn(), request.getSubSys(), transactionId)
        );

    }

    @RequestMapping(method = RequestMethod.POST, value = "/checkout")
    @ApiOperation(value = "Refill operation checkout", response = CheckoutResponse.class)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Refill success"),
            @ApiResponse(code = 400, message = "Missing or wrong mandatory parameters"),
            @ApiResponse(code = 503, message = "Service unavailable")
    })
    @Loggable
    public CheckoutResponse checkout(@RequestBody CheckoutRequest request) {

        CreditCardData cdc = new CreditCardData(request.getCardNumber(), request.getExpiryMonth(), request.getExpiryYear()
                , request.getCvv(), request.getBuyerName());

        TopupControllerValidator.validateCheckoutRequest(request.getAmount(), cdc, request.getFromMsisdn()
                , request.getToMsisdn(), request.getSubSys(), headers);

        String userReference = headers.getSession().getUserReference();
        //String transactionId = headers.getHeader(TransientHeaderName.TRANSACTIONID);
        //String transactionId = generateTransactionId() + "00000000";
        String transactionId = generateTransactionId(30);

        String dateTime = composeHeaderDateTime();
        String deviceType = headers.getHeader(TransientHeaderName.DEVICE_TYPE);

        String out = creditCardTopupService.checkout(
                userReference,
                request.getToMsisdn(),
                request.getAmount(),
                cdc,
                request.getSubSys(),
                request.getTiid(),
                deviceType,
                dateTime,
                transactionId
        );
        return new CheckoutResponse("OK", out);

    }

    @RequestMapping(method = RequestMethod.GET, value = "/termsAndConditions")
    @ApiOperation(value = "Get terms and condition to display", response = TermsAndConditionsResponse.class)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Terms and conditions succesfully retrieved"),
            @ApiResponse(code = 404, message = "Terms and conditions not found"),
            @ApiResponse(code = 408, message = "Time out"),
            @ApiResponse(code = 422, message = "Cannot parse source JSON"),
            @ApiResponse(code = 500, message = "Generic error"),
            @ApiResponse(code = 503, message = "Service unavailable")
    })
    @Loggable
    public TermsAndConditionsResponse termsAndConditions(){

        TermsAndConditions termsAndConditions = staticContentService.getTermsAndConditions();
        return new TermsAndConditionsResponse(
                "OK",
                termsAndConditions.getTitle(),
                termsAndConditions.getText()
        );

    }

    @RequestMapping(method = RequestMethod.GET, value = "/amounts")
    @ApiOperation(value = "Get topup amounts", response = AmountsResponse.class)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Terms and conditions succesfully retrieved"),
            @ApiResponse(code = 404, message = "Resource not found"),
            @ApiResponse(code = 408, message = "Time out"),
            @ApiResponse(code = 422, message = "Cannot parse source JSON"),
            @ApiResponse(code = 500, message = "Generic error"),
            @ApiResponse(code = 503, message = "Service unavailable")
    })
    @Loggable
    public AmountsResponse amounts(){

        Amounts amounts = staticContentService.getAmounts();
        return new AmountsResponse(
                "OK",
                amounts.getValues(),
                amounts.getDefaultValue(),
                amounts.getPromoInfo()
        );

    }

    public String composeHeaderDateTime(){
    	
        LocalDateTime bankAuthDate = LocalDateTime.now();
        String authDate = bankAuthDate.format(AUTH_DATE_FORMATTER);
        return authDate;
    }

    
    
}
