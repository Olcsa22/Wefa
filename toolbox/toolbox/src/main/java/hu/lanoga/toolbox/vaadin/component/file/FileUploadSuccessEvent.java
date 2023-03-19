package hu.lanoga.toolbox.vaadin.component.file;

import hu.lanoga.toolbox.file.FileDescriptor;
import lombok.Getter;

@Getter
public class FileUploadSuccessEvent {

	final FileDescriptor tmpFileDescriptor;

	public FileUploadSuccessEvent(final FileDescriptor tmpFileDescriptor) {
		this.tmpFileDescriptor = tmpFileDescriptor;
	}

}