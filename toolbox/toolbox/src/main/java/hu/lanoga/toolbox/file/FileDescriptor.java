package hu.lanoga.toolbox.file;

import java.io.File;
import java.sql.Timestamp;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonRawValue;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import hu.lanoga.toolbox.codestore.CodeStoreItem;
import hu.lanoga.toolbox.codestore.CodeStoreType;
import hu.lanoga.toolbox.export.ExporterIgnore;
import hu.lanoga.toolbox.json.jackson.StringValueDeserializer;
import hu.lanoga.toolbox.repository.ToolboxPersistable;
import hu.lanoga.toolbox.repository.jdbc.Column;
import hu.lanoga.toolbox.repository.jdbc.View;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class FileDescriptor implements ToolboxPersistable {

	@ExporterIgnore
	private Integer tenantId;

	private Integer id;

	private Integer locationType;

	private Integer securityType;

	/**
	 * ez az eredeti (pl. feltöltésnél lévő) fájlnév (ez Java kódban megnyitásnál nem jó, nem ilyen néven van tárolva a lemezen!)
	 */
	private String filename;

	private String filePath;

	private String mimeType;

	private Long fileSize;

	/**
	 *  "pretty name"-re van használva, tehát nem a feltöltött file név, 
	 *  hanem amire a user átírta (erre van lehetőség a filecart-nál), 
	 *  ez jelenleg email csatolmány elnevezésénél van használva
	 */
	@Column(name = "meta_1")
	private String meta1;

	/**
	 * jelenleg nem volt még használva sehol 
	 * (kivétel cactus régi, véletlenül temp statuson maradt fájlok megjelölése egy kézi DB ellenőrzésnél)
	 */
	@Column(name = "meta_2")
	private String meta2;

	/**
	 * főként cactus-ban volt használva {@link CodeStoreItem} megadására...  
	 * pl: INVOICE, ANNEX_VII 
	 * tehát a fájl irodai értelemben vett "típusának" azonosítására szólt
	 * 
	 *  @see #meta3Type
	 */
	@Column(name = "meta_3")
	private Integer meta3;

	/**
	 * meghatározza, hogy melyik {@link CodeStoreType} a {@link #meta3}
	 */
	@Column(name = "meta_3_type")
	private Integer meta3Type;

	@View
	private String meta3Caption;

	@Column(name = "meta_4")
	@JsonRawValue // a DB-ben JSON az érték, azért kell ez
	@JsonDeserialize(using = StringValueDeserializer.class) // a DB-ben JSON az érték, azért kell ez
	private String meta4;

	private Integer childType;

	private String note;

	private String hashValue;

	private Boolean remoteOnly;

	private UUID gid; // TODO: lehet, hogy itt is String-re kellene tenni inkább, a mapper nem kezeli, ha null (mondjuk itt elvileg nem lehet null soha)

	private Integer status;

	private Integer parentId;

	private Integer lockedBy;
	private Timestamp lockedOn;

	private Integer createdBy;
	private Timestamp createdOn;
	private Integer modifiedBy;
	private Timestamp modifiedOn;

	@View
	private File file;

	@Override
	public String toString() {
		final StringBuilder builder = new StringBuilder();
		builder.append("FileDescriptor [id=");
		builder.append(this.id);
		builder.append(", locationType=");
		builder.append(this.locationType);
		builder.append(", securityType=");
		builder.append(this.securityType);
		builder.append(", filename=");
		builder.append(this.filename);
		builder.append(", filePath=");
		builder.append(this.filePath);
		builder.append(", mimeType=");
		builder.append(this.mimeType);
		builder.append(", status=");
		builder.append(this.status);
		builder.append("]");
		return builder.toString();
	}

}
