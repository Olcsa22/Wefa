package hu.lanoga.toolbox.exception;

import java.util.UUID;

import org.springframework.http.HttpStatus;

import com.fasterxml.jackson.annotation.JsonIgnore;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

/**
 * description 1: fix angol általános hiba kategória;
 * description 2: fejlesztői célra további infó;
 * description 3: a user számára kiírható nyelvesített üzenet
 */
@Builder
@Getter
@Setter
public class ErrorResult {

	@Builder.Default
	private UUID uuid = UUID.randomUUID();

	@Builder.Default
	private String description1 = null;

	@Builder.Default
	private String description2 = null;

	@Builder.Default
	private String description3 = null;
	
	private HttpStatus httpStatus;
	
	@JsonIgnore
	@Builder.Default
	private Boolean mute = null;

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("ErrorResult [uuid=");
		builder.append(uuid);
		builder.append(", description1=");
		builder.append(description1);
		builder.append(", description2=");
		builder.append(description2);
		builder.append(", description3=");
		builder.append(description3);
		builder.append(", httpStatus=");
		builder.append(httpStatus);
		builder.append("]");
		return builder.toString();
	}

}