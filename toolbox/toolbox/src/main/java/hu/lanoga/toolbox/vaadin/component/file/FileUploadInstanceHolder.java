package hu.lanoga.toolbox.vaadin.component.file;

import java.lang.ref.WeakReference;
import java.util.concurrent.ConcurrentHashMap;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public abstract class FileUploadInstanceHolder {

	private static ConcurrentHashMap<String, WeakReference<FileUploadComponent>> instanceMap = new ConcurrentHashMap<>();

	public static void addInstance(final FileUploadComponent fuc) {
		instanceMap.put(fuc.getUploadId(), new WeakReference<>(fuc));
	}

	/**
	 * Null-t ad vissza, ha nincs az uploadId-nak megfelelő elem!
	 *
	 * @param uploadId
	 * @return
	 */
	public static FileUploadComponent findInstance(final String uploadId) {

		final WeakReference<FileUploadComponent> wrFuc = instanceMap.get(uploadId);

		log.debug("FileUploadInstanceHolder findInstance: " + uploadId + ", " + wrFuc);

		if (wrFuc != null) {
			final FileUploadComponent fuc = wrFuc.get();
			if (fuc == null) {
				instanceMap.remove(uploadId);
			}
			return fuc;
		}

		return null;

	}

	public static void removeInstance(final FileUploadComponent rfu) {
		instanceMap.remove(rfu.getUploadId());
	}

	public static ConcurrentHashMap<String, WeakReference<FileUploadComponent>> getInstanceMap() {
		return instanceMap;
	}
}
