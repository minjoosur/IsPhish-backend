package com.test.opendns.cisco.phish.repository;

import com.test.opendns.cisco.phish.model.UrlInfo;
import org.springframework.stereotype.Repository;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;


@Repository
public interface PhishUrlRepository extends MongoRepository<UrlInfo, String>{

    @Override
    Optional<UrlInfo> findById(String url);

}
