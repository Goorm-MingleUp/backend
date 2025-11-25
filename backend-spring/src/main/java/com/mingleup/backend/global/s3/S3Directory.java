package com.mingleup.backend.global.s3;

import lombok.Getter;

@Getter
public enum S3Directory {

    USER_PROFILE("user/profile/"),
    PARTY_THUMBNAIL("party/thumbnail/");

    private final String path;

    S3Directory(String path) {
        this.path = path;
    }
}
