package com.koitda.project.repository;

import java.math.BigDecimal;

/** 도안별 게이지 분포 집계. */
public interface GaugeStatView {
	BigDecimal getStitches();

	BigDecimal getRows();

	long getCnt();
}
