package com.koitda.project.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "project_yarn")
public class ProjectYarn {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "project_id", nullable = false)
	private Long projectId;

	@Column(name = "brand")
	private String brand;

	@Column(name = "yarn_name")
	private String yarnName;

	@Column(name = "color")
	private String color;

	@Column(name = "amount")
	private String amount;

	@Column(name = "unit")
	private String unit;

	@Column(name = "note")
	private String note;

	@Column(name = "sort_order", nullable = false)
	private int sortOrder;

	@Enumerated(EnumType.STRING)
	@Column(name = "input_source", nullable = false)
	private InputSource inputSource = InputSource.MANUAL;

	protected ProjectYarn() {
	}

	public ProjectYarn(Long projectId, String brand, String yarnName, String color, String amount,
			String unit, String note, int sortOrder) {
		this.projectId = projectId;
		this.brand = brand;
		this.yarnName = yarnName;
		this.color = color;
		this.amount = amount;
		this.unit = unit;
		this.note = note;
		this.sortOrder = sortOrder;
	}

	public String getBrand() {
		return brand;
	}

	public String getYarnName() {
		return yarnName;
	}

	public String getColor() {
		return color;
	}

	public String getAmount() {
		return amount;
	}

	public String getUnit() {
		return unit;
	}

	public String getNote() {
		return note;
	}
}
