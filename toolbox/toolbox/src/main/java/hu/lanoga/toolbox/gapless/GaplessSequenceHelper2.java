package hu.lanoga.toolbox.gapless;

import java.util.concurrent.locks.Lock;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RestController;

import com.google.common.util.concurrent.Striped;

import hu.lanoga.toolbox.exception.ToolboxGeneralException;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

/**
 * Java {@link Lock} ({@link Striped}) + {@link ThreadLocal} alapú megoldás... 
 * 
 * a) viszonylag hatékony 
 * b) nem minden esetben alkalmazható (több Java server esetén nem (cluster stb.)!) 
 * c) fontos, hogy a lock egy finally blockban el legyen "engedeve" 
 * d) egy része {@link Transactional}-on belül kell legyen, egy része pedig nem (lásd metdod kommentek) 
 */
public class GaplessSequenceHelper2 {

	@Getter
	@Setter
	@RequiredArgsConstructor
	static class GaplessSequenceHelper2Model {

		private final Supplier<Long> currentValueSupplier;
		private final Consumer<Long> valueSaveConsumer;

		private final Lock lock;
	}
	
	private static GaplessSequenceHelper2Model getModelFromTl() {
		final GaplessSequenceHelper2Model gaplessSequenceHelper2Model = tlGaplessSequenceHelper2Model.get();
		if (gaplessSequenceHelper2Model == null /*|| gaplessSequenceHelper2Model.getLock().tryLock()*/) { // úgy néz ki a tryLock true lesz, ha ugyanaz a Thread még
			throw new ToolboxGeneralException("Imporper usage (not locked etc.)!");
		}
		return gaplessSequenceHelper2Model;
	}

	private static ThreadLocal<GaplessSequenceHelper2Model> tlGaplessSequenceHelper2Model = new ThreadLocal<>();
	private static final Striped<Lock> stripedLock = Striped.lazyWeakLock(50);

	/**
	 * első lépés, {@link Transactional} kezdete előtt (pl.: {@link RestController}-ben, Vaadin Button click stb.)
	 * 
	 * @param sequenceName
	 * @param currentValueSupplier
	 * @param valueSaveConsumer
	 * 		olyan kell legyen, hogy erre hasson a meghívásakor hatályos {@link Transactional} (értsd DB update kell legyen)
	 */
	public static void lock(final String sequenceName, final Supplier<Long> currentValueSupplier, final Consumer<Long> valueSaveConsumer) {
		final Lock lock = stripedLock.get(sequenceName);
		lock.lock();
		tlGaplessSequenceHelper2Model.set(new GaplessSequenceHelper2Model(currentValueSupplier, valueSaveConsumer, lock));
		
	}
	
	/**
	 * @return
	 * 
	 * @see #incrementAndGetSequenceValue()
	 */
	public static int incrementAndGetSequenceValueInt() {
		return Math.toIntExact(incrementAndGetSequenceValue());
	}

	/**
	 * második (harmadik, negyedik stb.) lépés, {@link Transactional} közegben (a {@link Service} save-ben stb.)
	 * 
	 * @return
	 */
	public static long incrementAndGetSequenceValue() {
		final GaplessSequenceHelper2Model gaplessSequenceHelper2Model = getModelFromTl();

		final Long sequenceValue = gaplessSequenceHelper2Model.currentValueSupplier.get();
		final Long incrementedSequenceValue = sequenceValue + 1L;

		gaplessSequenceHelper2Model.valueSaveConsumer.accept(incrementedSequenceValue);

		return incrementedSequenceValue;
	}

	/**
	 * végső lépés, {@link Transactional} után (pl.: {@link RestController}-ben, Vaadin Button click stb.)
	 */
	public static void unlock() {
		
		final GaplessSequenceHelper2Model gaplessSequenceHelper2Model = getModelFromTl();

		gaplessSequenceHelper2Model.getLock().unlock();
		tlGaplessSequenceHelper2Model.remove();

	}

}
