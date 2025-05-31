package com.auto.trader.config.entity;

import com.auto.trader.domain.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "exception_log")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExceptionLog extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	private String exceptionType;
	@Column(columnDefinition = "TEXT")
	private String message;
	@Column(columnDefinition = "TEXT")
	private String uri;
	private String httpMethod;

	@Column(columnDefinition = "TEXT")
	private String stackTrace;
}
