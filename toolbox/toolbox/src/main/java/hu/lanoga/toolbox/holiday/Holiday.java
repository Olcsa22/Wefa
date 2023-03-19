package hu.lanoga.toolbox.holiday;

import com.fasterxml.jackson.annotation.JsonRawValue;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import hu.lanoga.toolbox.json.jackson.StringValueDeserializer;
import hu.lanoga.toolbox.repository.ToolboxPersistable;
import lombok.Getter;
import lombok.Setter;

/**
 * Állami/hivatalos ünnepek és hétvégi/spec. munkanapok (kvázi "fordított" ünnep)... 
 * jelenleg Google-től szedjük... 
 * tenant független, nincs benne tenantId... 
 */
@Getter
@Setter
public class Holiday implements ToolboxPersistable {
	
	private Integer id;

	private java.sql.Date eventDate;

	@JsonRawValue // a DB-ben JSON az érték, azért kell ez
	@JsonDeserialize(using = StringValueDeserializer.class) // a DB-ben JSON az érték, azért kell ez
	private String summary;

	private Boolean workday;

	private String eventCountry;
}
