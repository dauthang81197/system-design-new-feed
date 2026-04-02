package com.pv.common.s3;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.io.InputStream;
import java.util.UUID;

/**
 * Dùng chung cho mọi service cần upload/download file.
 * Inject S3Client được cấu hình từ S3Config của từng service.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class S3StorageService {

    private final S3Client s3Client;

    @Value("${aws.s3.bucket}")
    private String bucket;

    /** Upload bytes, trả về object key. */
    public String upload(String folder, String originalFilename, byte[] data, String contentType) {
        String key = folder + "/" + UUID.randomUUID() + "-" + originalFilename;
        s3Client.putObject(
            PutObjectRequest.builder()
                .bucket(bucket).key(key)
                .contentType(contentType)
                .contentLength((long) data.length)
                .build(),
            RequestBody.fromBytes(data)
        );
        log.info("Uploaded to S3: bucket={} key={}", bucket, key);
        return key;
    }

    /** Download object, trả về InputStream. */
    public InputStream download(String key) {
        ResponseInputStream<GetObjectResponse> response = s3Client.getObject(
            GetObjectRequest.builder().bucket(bucket).key(key).build()
        );
        log.info("Downloaded from S3: bucket={} key={}", bucket, key);
        return response;
    }

    /** Xóa object. */
    public void delete(String key) {
        s3Client.deleteObject(
            DeleteObjectRequest.builder().bucket(bucket).key(key).build()
        );
        log.info("Deleted from S3: bucket={} key={}", bucket, key);
    }
}

