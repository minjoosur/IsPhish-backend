package com.test.opendns.cisco.phish.service;

import com.test.opendns.cisco.phish.model.UrlInfo;



public interface UrlInfoService {
    boolean isPhish(UrlInfo urlInfo);
}
