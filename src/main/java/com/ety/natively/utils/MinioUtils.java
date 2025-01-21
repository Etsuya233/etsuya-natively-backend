package com.ety.natively.utils;

import com.ety.natively.properties.MinioProperties;
import io.minio.*;
import io.minio.errors.*;
import io.minio.messages.Tags;
import lombok.RequiredArgsConstructor;
import okhttp3.Headers;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Map;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class MinioUtils {

	private final MinioClient minioClient;
	private final MinioProperties minioProperties;

	public String uploadFile(MultipartFile file, String bucket) throws Exception {
		String extension = getFileExtension(file);
		String fileName = UUID.randomUUID() + extension;
		PutObjectArgs args = PutObjectArgs.builder()
				.bucket(bucket)
				.object(fileName)
				.stream(file.getInputStream(), file.getSize(), -1)
				.contentType("application/octet-stream")
				.build();
		minioClient.putObject(args);
		return fileName;
	}

	public String uploadFile(MultipartFile file, String bucket, Map<String, String> tags) throws Exception {
		String extension = getFileExtension(file);
		String fileName = UUID.randomUUID() + extension;
		PutObjectArgs args = PutObjectArgs.builder()
				.bucket(bucket)
				.object(fileName)
				.tags(tags)
				.stream(file.getInputStream(), file.getSize(), -1)
				.contentType("application/octet-stream")
				.build();
		minioClient.putObject(args);
		return fileName;
	}

	public String getFileExtension(MultipartFile file) {
		String originalFilename = file.getOriginalFilename();
		if (originalFilename != null && originalFilename.contains(".")) {
			return originalFilename.substring(originalFilename.lastIndexOf("."));
		}
		return "";  //那就不要拓展名
	}

	public String generateFileUrl(String bucket, String fileName) {
		return "/data" + "/" + bucket + "/" + fileName;
	}

	public Map<String, String> getObjectTags(String bucket, String fileName) {
		GetObjectTagsArgs args = GetObjectTagsArgs.builder()
				.bucket(bucket)
				.object(fileName)
				.build();
		try {
			return minioClient.getObjectTags(args).get();
		} catch (Exception e){
			throw new RuntimeException(e);
		}
	}

	public void setObjectTags(String bucket, String fileName, Map<String, String> tags) {
		SetObjectTagsArgs args = SetObjectTagsArgs.builder()
				.bucket(bucket)
				.object(fileName)
				.tags(tags)
				.build();
		try {
			minioClient.setObjectTags(args);
		} catch (Exception e){
			throw new RuntimeException(e);
		}
	}


}
