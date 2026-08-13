package com.koitda.project.repository;

/** 도안별 자주 쓴 실 집계. */
public interface YarnStatView {
	String getBrand();

	String getYarnName();

	long getCnt();
}
