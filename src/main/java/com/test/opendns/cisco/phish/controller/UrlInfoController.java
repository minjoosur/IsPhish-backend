package com.test.opendns.cisco.phish.controller;

import com.test.opendns.cisco.phish.model.UrlInfo;
import com.test.opendns.cisco.phish.service.UrlInfoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;


@CrossOrigin
@RestController
public class UrlInfoController {

    @Autowired
    private UrlInfoService urlInfoService;

    @PostMapping
    @CrossOrigin
    public boolean readByUrl(@RequestBody UrlInfo urlInfo) {
        return urlInfoService.isPhish(urlInfo);
    }

}
