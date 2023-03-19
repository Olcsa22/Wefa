package hu.lanoga.toolbox.session;

import java.lang.ref.WeakReference;

import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.stereotype.Component;

import hu.lanoga.toolbox.spring.ApplicationContextHelper;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Component
@Scope(proxyMode = ScopedProxyMode.TARGET_CLASS, value = "session")
public class LcuHelperSessionBean {

	private static ThreadLocal<WeakReference<LcuHelperSessionBean>> tlLcuHelperBeanReference = new ThreadLocal<>();

	// ---

	public static void setTlLcuHelperBeanReference() {

		final LcuHelperSessionBean t = ApplicationContextHelper.getBean(LcuHelperSessionBean.class);
		tlLcuHelperBeanReference.set(new WeakReference<>(t));

	}

	public static LcuHelperSessionBean getTlLcuHelperBeanReference() {

		final WeakReference<LcuHelperSessionBean> wr = tlLcuHelperBeanReference.get();

		if (wr == null) {
			return null;
		}

		return wr.get();

	}

	public static void clearTlLcuHelperBeanReference() {
		tlLcuHelperBeanReference.remove();
	}

	// ---

	public static String specialLcuGidRetrieve() {

		String lcuGid = null;

		try {

			lcuGid = ApplicationContextHelper.getBean(LcuHelperSessionBean.class).getLcuGid();

		} catch (org.springframework.beans.factory.BeanCreationException e) {

			if (e.getMessage().contains("Scope 'session' is not active")) {

				LcuHelperSessionBean t = LcuHelperSessionBean.getTlLcuHelperBeanReference();

				if (t != null) {
					lcuGid = t.getLcuGid();
				}

			} else {
				throw e;
			}
		}

		return lcuGid;

	}

	// ---

	/**
	 * ad-hoc (regisztráció nélküli) LCU (=vásárló) user UUID id-ja... 
	 */
	private String lcuGid;

	// ---

	/**
	* nem állítja be, itt csak meg lehet jegyeztetni és ennek alapján
	* hívni {@link JdbcRepositoryTenantManager#setTlTenantId(int)} metódust kézzel
	*/
	private Integer targetTenantId;

	// ---

	private Object order;

}
