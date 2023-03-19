package hu.lanoga.toolbox.repository;

import java.sql.Timestamp;

import org.springframework.data.domain.Persistable;

import com.fasterxml.jackson.annotation.JsonIgnore;

public interface ToolboxPersistable extends Persistable<Integer> {

	@Override
	@JsonIgnore
	default boolean isNew() {
		return getId() == null;
	}
	
	public void setId(Integer id);

	default public Integer getCreatedBy() {
		return null;
	}

	default public Integer getModifiedBy() {
		return null;
	}
	
	default public Timestamp getCreatedOn() {
		return null;
	}
	
	default public Timestamp getModifiedOn() {
		return null;
	}
	
}
