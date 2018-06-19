package it.tim.topup.common.headers;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class TimSession {

    private String userAccount;
    @JsonProperty("cf_piva")
    private String userReference;
    private String dcaCoockie;
    private String accountType;

}
