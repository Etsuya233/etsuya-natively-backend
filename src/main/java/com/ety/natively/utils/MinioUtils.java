package com.ety.natively.utils;

import com.ety.natively.properties.MinioProperties;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

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


}
