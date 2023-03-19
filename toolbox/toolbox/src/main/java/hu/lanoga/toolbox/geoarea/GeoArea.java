package hu.lanoga.toolbox.geoarea;

import hu.lanoga.toolbox.repository.ToolboxPersistable;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class GeoArea implements ToolboxPersistable {

	private Integer id;

	private Integer geoAreaType;

	private String geoAreaName;

	private String geoAreaUrlName;

	private Integer parentArea;

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("GeoArea [id=");
		builder.append(id);
		builder.append(", geoAreaType=");
		builder.append(geoAreaType);
		builder.append(", geoAreaName=");
		builder.append(geoAreaName);
		builder.append(", geoAreaUrlName=");
		builder.append(geoAreaUrlName);
		builder.append(", parentArea=");
		builder.append(parentArea);
		builder.append("]");
		return builder.toString();
	}

}
