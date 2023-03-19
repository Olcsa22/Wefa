package hu.lanoga.wefa.model;

import java.util.LinkedHashMap;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ExportImportDeployModel {

	private String procDefName;
	private byte[] bpmnXmlData;
	private LinkedHashMap<String, String> procDefGroovyScriptStrings;

}
