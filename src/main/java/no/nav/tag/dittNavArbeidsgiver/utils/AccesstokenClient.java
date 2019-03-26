package no.nav.security;


import com.nimbusds.oauth2.sdk.token.AccessToken;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

@Component
@ConfigurationProperties("aad")
public class AccesstokenClient {


    public void setAadAccessTokenURL(String aadAccessTokenURL) {
        this.aadAccessTokenURL = aadAccessTokenURL;
    }

    private String aadAccessTokenURL;

    @Autowired
    public AccesstokenClient( ){

    }

    public AccessToken hentAccessToken(){

        RestTemplate template = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> map= new LinkedMultiValueMap<String, String>();
        map.add("client_id", "1b1bf278-3c28-4003-a528-b595d800afb0");
        map.add("scope", "https://trygdeetaten.no/syfoarbeidsgivertilgang/.default");
        map.add("grant_type", "client_credentials");
        map.add("client_secret", "2jMO[N/_ID$M{jQ5d+b5HQeAEkvsXOqt(CkcI}6]h)");


        HttpEntity<MultiValueMap<String, String>> entity = new HttpEntity<MultiValueMap<String, String>>(map, headers);

        return template.exchange(aadAccessTokenURL, HttpMethod.POST,entity, AccessToken.class).getBody();

    }

@Data
    private class AadAccessToken

    {
        String access_token;
        String token_type;
        String expires_in;
        String ext_expires_in;
        String expires_on;
        String not_before;
        String resource;
    }


}
