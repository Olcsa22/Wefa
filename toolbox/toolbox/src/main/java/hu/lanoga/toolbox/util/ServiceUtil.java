package hu.lanoga.toolbox.util;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

import org.apache.commons.beanutils.BeanUtils;

import hu.lanoga.toolbox.exception.StaleDataException;
import hu.lanoga.toolbox.exception.ToolboxGeneralException;
import hu.lanoga.toolbox.repository.DefaultInMemoryRepository;
import hu.lanoga.toolbox.repository.ToolboxPersistable;
import hu.lanoga.toolbox.service.LazyEnhanceCrudService;
import hu.lanoga.toolbox.service.RapidLazyEnhanceCrudService;
import hu.lanoga.toolbox.service.ToolboxCrudService;

public class ServiceUtil {

	private ServiceUtil() {
		//
	}

	public static <T extends ToolboxPersistable> void checkStalenessWithModifiedOn(final ToolboxCrudService<T> service, final T modelObject) {
		final T oldPersisted = service.findOne(modelObject.getId());
		checkStalenessWithModifiedOn(oldPersisted, modelObject);
	}

	public static <T extends ToolboxPersistable> void checkStalenessWithModifiedOn(final T oldPersisted, final T modelObject) {
		if (modelObject.getModifiedOn() != null && oldPersisted.getModifiedOn().after(modelObject.getModifiedOn())) {
			throw new StaleDataException("Model object is stale (its modifiedOn value is smaller (older) than the current DB record modifiedOn value)!");
		}
	}

	/**
	 * @param <U>
	 * @param <V>
	 * @param list
	 * @param viewModelClass
	 * @param enhancer
	 * 		opcionális, null esetén {@link BeanUtils#copyProperties(Object, Object)} lesz használva
	 * @param enhancer
	 * 		opcionális, {@link LazyEnhanceCrudService} enhance metódus lesz belőle
	 * 		(azért {@link Consumer} mert a {@link LazyEnhanceCrudService} esetén is magát a kapott objektumot kell módosítani)
	 * @return
	 */
	public static <U, V extends ToolboxPersistable> RapidLazyEnhanceCrudService<V, DefaultInMemoryRepository<V>> createInMemoryService(final List<U> list, final Class<V> viewModelClass, final Function<U, V> copyPropFunction, final Consumer<V> enhancer) {

		final List<V> viewModelList = new ArrayList<>();

		for (final U o : list) {
			try {
				final V viewModel;
				
				if (copyPropFunction == null) {
					viewModel = viewModelClass.newInstance();
					BeanUtils.copyProperties(viewModel, o);
				} else {
					viewModel = copyPropFunction.apply(o);
				}

				viewModelList.add(viewModel);
			} catch (IllegalAccessException | InvocationTargetException | InstantiationException e) {
				throw new ToolboxGeneralException(e);
			}
		}

		final DefaultInMemoryRepository<V> inMemoryRepository = new DefaultInMemoryRepository<>(viewModelClass);
		inMemoryRepository.initWithData(viewModelList);
		final RapidLazyEnhanceCrudService<V, DefaultInMemoryRepository<V>> inMemoryService = new RapidLazyEnhanceCrudService<V, DefaultInMemoryRepository<V>>(inMemoryRepository) {

			@Override
			public V enhance(final V t) {
				if (enhancer != null) {
					enhancer.accept(t);
					return t;
				} else {
					return t;
				}
			}

		};
		return inMemoryService;

	}

}
