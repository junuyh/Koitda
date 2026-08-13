package com.koitda.project.repository;

import java.math.BigDecimal;

/** 도안별 자주 쓴 바늘(mm) 집계. */
public interface NeedleStatView {
	BigDecimal getSizeMm();

	long getCnt();
}
