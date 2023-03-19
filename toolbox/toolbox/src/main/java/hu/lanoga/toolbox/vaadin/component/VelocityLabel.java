package hu.lanoga.toolbox.vaadin.component;

import java.io.StringWriter;

import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.Velocity;
import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;

import com.vaadin.shared.ui.ContentMode;
import com.vaadin.ui.Label;

import hu.lanoga.toolbox.exception.ToolboxGeneralException;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class VelocityLabel extends Label {

	public VelocityLabel() {
		this.setContentMode(ContentMode.HTML);
	}

	@Override
	public void setValue(final String value) {
		// throw new UnsupportedOperationException(); // nem lehet ilyen dobni, mert az ős több helyen is meghívja (pl. ős constructor)
		log.debug("Simple string value setter is not supported (skipped)!");
	}

	public void setValue(final String templateName, final VelocityContext velocityContext, final boolean doSanitize) {

		try (final StringWriter writer = new StringWriter()) {

			if (doSanitize) {
				for (final Object key : velocityContext.getKeys()) {

					final Object value = velocityContext.get(key.toString());

					if (value instanceof CharSequence) {
						velocityContext.put(key.toString(), Jsoup.clean(value.toString(), Safelist.basic()));
					}

				}
			}

			Velocity.mergeTemplate(templateName, "UTF-8", velocityContext, writer);

			super.setValue(writer.toString());

		} catch (final Exception e) {
			throw new ToolboxGeneralException("VelocityLabel error!", e);
		}

	}

}
