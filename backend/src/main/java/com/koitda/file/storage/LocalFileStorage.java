package com.koitda.file.storage;

import com.koitda.common.error.ApiException;
import com.koitda.common.error.ErrorCode;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * 로컬 파일시스템 저장 구현. key 는 상대 경로처럼 쓰되(예: pattern_image/uuid.jpg),
 * 루트 밖으로 벗어나지 못하게 정규화해 경로 탈출(../)을 막는다.
 */
@Component
public class LocalFileStorage implements StorageService {

	private final Path root;

	public LocalFileStorage(@Value("${koitda.storage.dir:storage-data}") String dir) {
		this.root = Paths.get(dir).toAbsolutePath().normalize();
		try {
			Files.createDirectories(root);
		} catch (IOException e) {
			throw new IllegalStateException("저장 디렉토리를 만들 수 없습니다: " + root, e);
		}
	}

	@Override
	public void put(String key, byte[] data, String contentType) {
		Path target = resolve(key);
		try {
			Files.createDirectories(target.getParent());
			Files.write(target, data);
		} catch (IOException e) {
			throw new ApiException(ErrorCode.INTERNAL_ERROR, "파일 저장에 실패했습니다.");
		}
	}

	@Override
	public byte[] read(String key) {
		Path target = resolve(key);
		try {
			return Files.readAllBytes(target);
		} catch (IOException e) {
			throw new ApiException(ErrorCode.FILE_NOT_FOUND, "파일을 찾을 수 없습니다.");
		}
	}

	@Override
	public void delete(String key) {
		try {
			Files.deleteIfExists(resolve(key));
		} catch (IOException ignored) {
			// 삭제 실패는 치명적이지 않다.
		}
	}

	/** key 를 루트 기준으로 해석하고 루트 밖(경로 탈출)이면 거부한다. */
	private Path resolve(String key) {
		Path p = root.resolve(key).normalize();
		if (!p.startsWith(root)) {
			throw new ApiException(ErrorCode.VALIDATION_ERROR, "잘못된 파일 경로입니다.");
		}
		return p;
	}
}
