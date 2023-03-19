package hu.lanoga.toolbox.vaadin.component.file;

public interface FileUploadSuccessListener {

	/**
	 * Háttérből lesz meghívva (kellhet UI.access... az implementáció belsejében)!
	 *
	 * @param event
	 */
	abstract void onFinish(FileUploadSuccessEvent event);

}