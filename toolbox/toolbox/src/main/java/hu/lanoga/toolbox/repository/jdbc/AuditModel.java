package hu.lanoga.toolbox.repository.jdbc;

import java.sql.Timestamp;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AuditModel {

	public AuditModel() {
		//
	}
	
	public AuditModel(final Integer auditUserId, final Timestamp auditTs) {
		super();
		this.auditUserId = auditUserId;
		this.auditTs = auditTs;
	}

	/**
	 * null esetén nincs felülírás
	 * 
	 * (értsd ha csak pl. a {@link Timestamp}-pel van dolgod, 
	 * akkor hagyd ezt null-on)
	 */
	private Integer auditUserId;
	
	/**
	 * null esetén nincs felülírva 
	 * 
	 * (értsd, ha csak pl. a userId-val van dolgod, 
	 * akkor hagyd ezt null-on)
	 */
	private Timestamp auditTs;

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((this.auditTs == null) ? 0 : this.auditTs.hashCode());
		result = prime * result + ((this.auditUserId == null) ? 0 : this.auditUserId.hashCode());
		return result;
	}

	@Override
	public boolean equals(final Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (this.getClass() != obj.getClass()) {
			return false;
		}
		final AuditModel other = (AuditModel) obj;
		if (this.auditTs == null) {
			if (other.auditTs != null) {
				return false;
			}
		} else if (!this.auditTs.equals(other.auditTs)) {
			return false;
		}
		if (this.auditUserId == null) {
			if (other.auditUserId != null) {
				return false;
			}
		} else if (!this.auditUserId.equals(other.auditUserId)) {
			return false;
		}
		return true;
	}

	@Override
	public String toString() {
		final StringBuilder builder = new StringBuilder();
		builder.append("AuditModel [auditUserId=");
		builder.append(this.auditUserId);
		builder.append(", auditTs=");
		builder.append(this.auditTs);
		builder.append("]");
		return builder.toString();
	}

}
