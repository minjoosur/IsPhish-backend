package com.test.opendns.cisco.phish.service;

import com.test.opendns.cisco.phish.model.UrlInfo;
import com.test.opendns.cisco.phish.repository.PhishUrlRepository;
import org.apache.commons.io.FileUtils;
import org.apache.http.impl.client.HttpClientBuilder;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.json.JsonParser;
import org.springframework.boot.json.JsonParserFactory;
import org.springframework.http.*;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.zip.GZIPInputStream;

/**
 * Created by minjoo on 10/01/18.
 */
@Service
public class UrlInfoServiceImpl implements UrlInfoService {

    private final PhishUrlRepository phishUrlRepository;

    public UrlInfoServiceImpl(PhishUrlRepository phishUrlRespsitory){
        this.phishUrlRepository = phishUrlRespsitory;
    }

    @Value("${phishtank.baseurl}")
    private String phishtankUrl;

    @Value("${phishtank.appkey}")
    private String phishtankAppKey;

    @Override
    public boolean isPhish(UrlInfo urlInfo) {
        String urlToLookup = urlInfo.getUrl();

        Optional<UrlInfo> urlFound =  phishUrlRepository.findById(urlToLookup);
        return urlFound.isPresent() && urlFound.get().getVerified().equals("yes");
    }

    /**
     * This method takes JSON filename as an input, and convert json file to UserInto List 
     */
    private List<UrlInfo> convert(String fileName) {
        JSONParser parser = new JSONParser();
        JSONArray array = null;
        List<UrlInfo> urlInfos = new LinkedList<>();

        try {
            array = (JSONArray) parser.parse(new FileReader("db.json"));
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ParseException e) {
            e.printStackTrace();
        }

        for (Object t : array)
        {
            JSONObject phish = (JSONObject) t;
            String url = (String) phish.get("url");
            String verified = (String) phish.get("verified");

            UrlInfo urlInfo = new UrlInfo();
            urlInfo.setUrl(url);
            urlInfo.setVerified(verified);
            urlInfos.add(urlInfo);
        }

        return urlInfos;
    }

    /**
     * This method automatically updates and syncs with Phishtank Phish URLs every hour using cron
     */
    @Async
    @Scheduled(cron = "0 0 * * * *")
    public void updatePhishUrlRespository() {

        System.out.println("UPDATING Phish DB");

        //1. Send GET request to PhishTank and takes url info as gz file as a response
        URI phishtankServiceEndPoint = UriComponentsBuilder.fromHttpUrl(phishtankUrl)
                .pathSegment("online-valid.json.gz")
                .build().toUri();

        RequestEntity<Void> request = RequestEntity.get(phishtankServiceEndPoint)
                .accept(MediaType.APPLICATION_JSON)
                .build();

        HttpComponentsClientHttpRequestFactory clientHttpRequestFactory =
                new HttpComponentsClientHttpRequestFactory(HttpClientBuilder.create().build());
        RestTemplate restTemplate = new RestTemplate(clientHttpRequestFactory);

        byte[] responseBytes = restTemplate.exchange(request, byte[].class).getBody();

        //2. Since we take the response as a gz format, decompress it and save it to json file
        InputStream target = new ByteArrayInputStream(responseBytes);
        FileOutputStream fos = null;
        GZIPInputStream gzis = null;

        try {
            fos = new FileOutputStream("db.json");
            gzis = new GZIPInputStream(target);
            byte[] buffer = new byte[1024];
            int len = 0;

            while ((len = gzis.read(buffer)) > 0) {
                fos.write(buffer, 0, len);
            }

            fos.close();
            target.close();
            gzis.close();

        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("Decompressing failure");
        } finally {
            try {
                if (fos != null)
                    fos.close();
                if (gzis != null)
                    gzis.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        //3. Now convert json file to List and save all UrlInfo to the repository
        List<UrlInfo> all_info = convert("db.json");
        phishUrlRepository.saveAll(all_info);
    }
}
