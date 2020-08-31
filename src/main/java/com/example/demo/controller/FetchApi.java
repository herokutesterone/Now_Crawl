package com.example.demo.controller;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.example.demo.model.Quote;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

@RestController
public class FetchApi {
    private static final Logger log = LoggerFactory.getLogger(FetchApi.class);

    // @Bean
    @GetMapping("/FetchApi")
    public Quote FetchApiMethod(RestTemplate restTemplate) {
        Quote quote = restTemplate.getForObject("https://gturnquist-quoters.cfapps.io/api/random", Quote.class);
        log.info(quote.toString());
        return quote;
    }

    private static final String APIGetNear = "https://gappapi.deliverynow.vn/api/collection/get_delivery_ids?id=10895&sort_type=3&latitude=10.765260&longitude=106.673302";
    private static final String APIGetDetail = "https://gappapi.deliverynow.vn/api/collection/get_delivery_restaurant_infos";
    private static final String TemplateDetail = "{\"delivery_ids\":[%s]}";
    private static final MultiValueMap<String, String> authNow = new LinkedMultiValueMap();
    private static ArrayList<Integer> ids = null;
    static {
        System.out.println("Run once");
        authNow.add("x-foody-api-version", "1");
        authNow.add("x-foody-app-type", "1004");
        authNow.add("x-foody-client-id", "");
        authNow.add("x-foody-client-type", "1");
        authNow.add("x-foody-client-version", "3.0.0");
        GetAllStore(new RestTemplate());
    }

    public static void GetAllStore(RestTemplate restTemplate) {
        HttpHeaders headers = new HttpHeaders((MultiValueMap) authNow);
        HttpEntity<String> entity = new HttpEntity<String>("", headers);
        ResponseEntity<String> response = restTemplate.exchange(APIGetNear, HttpMethod.GET, entity, String.class);

        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = null;
        try {
            root = mapper.readTree(response.getBody());
        } catch (JsonProcessingException e) {
            e.printStackTrace();
            log.error("Something went wrong!");
        }
        JsonNode reply = root.path("reply");
        JsonNode collection = reply.path("collection");
        JsonNode delivery_ids = collection.path("delivery_ids");

        ids = mapper.convertValue(delivery_ids, ArrayList.class);
        System.out.println("Called in static");
    }

    @GetMapping("/GetStore")
    public String GetStore(
            @RequestParam(name = "lat", defaultValue = "10.765260f") float latitude,
            @RequestParam(name = "lng", defaultValue = "106.673302f") float longitude,

            RestTemplate restTemplate) {

        StringBuilder sb = new StringBuilder();
        int st = ids.size() / 300 + 1;
        for (int i = 0; i < st; ++i) {
            sb.append(GetStoreIn(latitude,longitude,300 * (i + 1), i * 300, restTemplate));
        }
        return sb.toString();
    }

    public String GetStoreIn(
            float la,float lo,
            int limit, int index, RestTemplate restTemplate) {

        HttpHeaders headers = new HttpHeaders((MultiValueMap) authNow);

        if (limit > ids.size())
            limit = ids.size();

        StringBuilder sb = new StringBuilder();
        for (int i = index; i < limit; ++i) {
            sb.append(String.valueOf(ids.get(i))).append(",");
        }
        sb.deleteCharAt(sb.length() - 1);
        String jsonPost = String.format(TemplateDetail, sb.toString());

        HttpEntity entity = new HttpEntity<String>(jsonPost, headers);
        ResponseEntity<String> response = restTemplate.exchange(APIGetDetail, HttpMethod.POST, entity, String.class);

        ObjectMapper mapper = new ObjectMapper();
        JsonNode listStore;
        try {
            listStore = mapper.readTree(response.getBody());
        } catch (JsonProcessingException e) {
            e.printStackTrace();
            return "Nothing2";
        }
        JsonNode reply = listStore.path("reply");
        JsonNode delivery_restaurant_infos = reply.path("delivery_restaurant_infos");

        ArrayList<LinkedHashMap<String, String>> Storeids = mapper.convertValue(delivery_restaurant_infos,
                ArrayList.class);

        ArrayList<JsonNode> result = new ArrayList();

        sb.setLength(0);

        String temp2 = "<a href=\"%s\">%d, %s: %s (%.1f km)</a>";
        int index2 = 0;
        for (LinkedHashMap x : Storeids) {
            ++index2;

            ArrayList xgroup = (ArrayList) x.get("promotion_groups");

            if (xgroup.size() > 0) {
                LinkedHashMap<String, String> gr0 = (LinkedHashMap) xgroup.get(0);
                LinkedHashMap<String, Double>position =(LinkedHashMap) x.get("position");
                String desp = gr0.get("text");

                float dis = getDistanceFromLatLonInKm(la,lo,
                
                position.get("latitude").floatValue() ,position.get("longitude").floatValue() );

                if (dis > 3) continue;

                if (desp.toLowerCase().contains("deal")) {
                    sb.append(String.format(temp2, x.get("url"), index2 + index, x.get("name"),desp,dis)).append("<br>\n");
                }
            }

        }

        return sb.toString();
    }

    private float getDistanceFromLatLonInKm(float lat1, float lon1, float lat2, float lon2) {
        float R = 6371; // Radius of the earth in km
        float dLat = deg2rad(lat2 - lat1); // deg2rad below
        float dLon = deg2rad(lon2 - lon1);
        float a = (float) (Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(deg2rad(lat1)) * Math.cos(deg2rad(lat2)) * Math.sin(dLon / 2) * Math.sin(dLon / 2));
        float c = (float) (2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a)));
        float d = R * c; // Distance in km
        return d;
    }

    float deg2rad(float deg) {
        return (float) (deg * (Math.PI / 180));
    }

}