package hu.lanoga.toolbox.file.remote;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

import hu.lanoga.toolbox.export.ExporterIgnore;
import hu.lanoga.toolbox.repository.ToolboxPersistable;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RemoteFile implements ToolboxPersistable {

	private Integer id;

	@ExporterIgnore
	private Integer tenantId;

	@NotNull
	private Integer remoteProviderType;

	@NotNull
	private Integer fileId;

	@NotNull
	private Integer priority;

	@NotEmpty
	private String publicUrl;

	private String remoteId;

	@NotNull
	private Boolean anyoneWithLink;

	private Boolean anyoneWithLinkRequested;

	@NotNull
	private Integer status;

	private Integer attempt;

	private Integer createdBy;

	private java.sql.Timestamp createdOn;

	private Integer modifiedBy;

	private java.sql.Timestamp modifiedOn;

	public void incrementAttempt() {

		if (this.attempt == null) {
			this.attempt = 1;
		} else {
			this.attempt = this.attempt + 1;
		}

	}

}