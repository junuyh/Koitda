package com.koitda.project.dto;

import jakarta.validation.constraints.NotNull;

/** 니팅로그 대표 이미지 추가 요청. 파일은 먼저 업로드(POST /files)한 뒤 그 id 를 넘긴다. */
public record AddImageRequest(@NotNull Long fileId) {
}
