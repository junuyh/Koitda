package com.koitda.file.repository;

import com.koitda.file.domain.FileAsset;
import org.springframework.data.jpa.repository.JpaRepository;

/** 파일 메타데이터 접근. 등록 시 이미지·PDF 참조(file_id)의 존재 검증에 쓴다. */
public interface FileAssetRepository extends JpaRepository<FileAsset, Long> {
}
