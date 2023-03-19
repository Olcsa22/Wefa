package hu.lanoga.toolbox.session;

import hu.lanoga.toolbox.file.FileDescriptor;
import lombok.Getter;
import lombok.Setter;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * későbbi használathoz (pl. email küldés) lehet összeszedni fájlokat a teljes rendszerből... 
 * 
 * thread-safe {@link CopyOnWriteArrayList} alapú, ami lassú, költséges, csak ritka módosításra (és/vagy kis elemszámra) jó 
 * (értsd: user általi interaktív használat (kattintgatás) és 100 alatti elemszámra ok)
 */
@Getter
@Setter
@Component
@Scope(proxyMode = ScopedProxyMode.TARGET_CLASS, value = "session")
public class FileCartSessionBean {

	/**
	 * @see FileDescriptor
	 */
	private CopyOnWriteArrayList<Integer> fileIds = new CopyOnWriteArrayList<>();

	public void add(final int fileId) {
		this.fileIds.addIfAbsent(fileId);
	}

	public void remove(final int fileId) {
		this.fileIds.removeIf(x -> {
			if (x.equals(fileId)) {
				return true;
			}

			return false;
		});
	}
	
	public void clear() {
		this.fileIds.clear();
	}
	
	public List<Integer> getAll() {
		return new ArrayList<>(this.fileIds);
	}
	
	public List<Integer> getAllAndClear() {
		final List<Integer> currentAll = this.getAll();
		this.fileIds.removeAll(currentAll);
		return currentAll;
	}

}
