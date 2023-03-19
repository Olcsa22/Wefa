package hu.lanoga.toolbox.tenant;

import java.sql.Timestamp;

import javax.validation.constraints.NotEmpty;

import org.hibernate.validator.constraints.Length;

import com.fasterxml.jackson.annotation.JsonRawValue;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import hu.lanoga.toolbox.export.ExporterIgnore;
import hu.lanoga.toolbox.json.jackson.StringValueDeserializer;
import hu.lanoga.toolbox.repository.ToolboxPersistable;
import hu.lanoga.toolbox.repository.jdbc.Column;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TenantInfo implements ToolboxPersistable {

	private Integer id;

	@ExporterIgnore
	@NotEmpty
	private Integer tenantId;

	// ---

	@Length(max = 50)
	private String companyTaxNumber;

	@Length(max = 50)
	private String companyRegistrationNumber;

	@Length(max = 50)
	private String companyBankAccountNumber;

	// ---

	@Length(max = 2)
	private String companyAddressCountry;

	@Length(max = 50)
	private String companyAddressState;

	@Length(max = 50)
	private String companyAddressCounty;

	@Length(max = 10)
	private String companyAddressZipCode;

	@Length(max = 50)
	private String companyAddressDetails;

	// ---

	@Length(max = 50)
	private String contactName;

	@Length(max = 100)
	@NotEmpty
	private String email;

	@Length(max = 30)
	private String phone;

	// ---

	@Length(max = 500)
	private String note;

	@Length(max = 100)
	@Column(name = "extra_data_1")
	private String extraData1;

	@Length(max = 100)
	@Column(name = "extra_data_2")
	private String extraData2;

	@Length(max = 100)
	@Column(name = "extra_data_3")
	private String extraData3;

	@Column(name = "extra_data_4")
	private Integer extraData4; // codeStoreItem-re mutat

	@Length(max = 100)
	@Column(name = "extra_data_5")
	private String extraData5; // extra mezo (pl: mapfre projektben)
	
	@Length(max = 100)
	@Column(name = "extra_data_6")
	private String extraData6; // extra mezo (pl: mapfre projektben)
	
	@Length(max = 200)
	@Column(name = "extra_data_7")
	private String extraData7;  // extra mezo (pl: mapfre projektben)

	@Length(max = 200)
	@Column(name = "extra_data_8")
	private String extraData8;  // extra mezo (pl: mapfre projektben)

	@Length(max = 200)
	@Column(name = "extra_data_9")
	private String extraData9;  // extra mezo (pl: mapfre projektben)
	
	@Length(max = 200)
	@Column(name = "extra_data_10")
	private String extraData10;  // extra mezo (pl: mapfre projektben)

	@JsonRawValue
	@JsonDeserialize(using = StringValueDeserializer.class)
	@Column(name = "file_ids")
	private String fileIds;

	@JsonRawValue
	@JsonDeserialize(using = StringValueDeserializer.class)
	@Column(name = "file_ids_2")
	private String fileIds2;

	@JsonRawValue
	@JsonDeserialize(using = StringValueDeserializer.class)
	@Column(name = "file_ids_3")
	private String fileIds3;

	@JsonRawValue
	@JsonDeserialize(using = StringValueDeserializer.class)
	@Column(name = "file_ids_4")
	private String fileIds4;

	@JsonRawValue
	@JsonDeserialize(using = StringValueDeserializer.class)
	@Column(name = "file_ids_5")
	private String fileIds5;

	@JsonRawValue
	@JsonDeserialize(using = StringValueDeserializer.class)
	@Column(name = "file_ids_6")
	private String fileIds6;
	
	@JsonRawValue
	@JsonDeserialize(using = StringValueDeserializer.class)
	@Column(name = "file_ids_7")
	private String fileIds7;

	@Length(max = 200)
	private String companyName;

	// ---

	@ExporterIgnore
	private Integer createdBy;

	private Timestamp createdOn;

	@ExporterIgnore
	private Integer modifiedBy;

	private Timestamp modifiedOn;

}
