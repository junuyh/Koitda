package com.koitda.file.storage;

/**
 * 객체 저장 추상화. 지금은 로컬 파일시스템 구현이지만, 같은 인터페이스로 S3/MinIO 구현으로 교체할 수 있다.
 * DB(file_asset)에는 메타데이터와 key 만 남기고 실체는 이 저장소가 관리한다.
 */
public interface StorageService {

	/** key 위치에 바이트를 저장한다(같은 key 면 덮어쓴다). */
	void put(String key, byte[] data, String contentType);

	/** key 의 바이트를 읽는다. 없으면 예외. */
	byte[] read(String key);

	/** key 를 삭제한다(없어도 조용히 무시). */
	void delete(String key);
}
