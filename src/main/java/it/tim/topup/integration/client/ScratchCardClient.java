package it.tim.topup.integration.client;

import org.springframework.cloud.netflix.feign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import it.tim.topup.model.integration.ScratchCardRequest;
import it.tim.topup.model.integration.ScratchCardResponse;

@FeignClient(
        name="scratchcard",
        url = "${integration.sdp.scpath}"
)
public interface ScratchCardClient {
    @PostMapping(value = "/v1/clienti/{rifCliente}/consistenze/{numLinea}/credito/ricarica/scratchCard")
    ResponseEntity<ScratchCardResponse> rechargeWithScratchCard(
    		@PathVariable("rifCliente") String rifCliente,
            @PathVariable("numLinea") String numLinea,
    		@RequestBody ScratchCardRequest request);
    
}
