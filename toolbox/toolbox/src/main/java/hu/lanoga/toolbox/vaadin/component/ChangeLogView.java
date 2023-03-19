package hu.lanoga.toolbox.vaadin.component;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.apache.commons.io.IOUtils;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import com.vaadin.navigator.View;
import com.vaadin.ui.VerticalLayout;

import hu.lanoga.toolbox.exception.ToolboxGeneralException;
import hu.lanoga.toolbox.i18n.I18nUtil;
import hu.lanoga.toolbox.vaadin.component.markdown.MarkdownDisplayLabel;

public class ChangeLogView extends VerticalLayout implements View {

	public ChangeLogView() {

		try {

			final Locale loggedInUserLocale = I18nUtil.getLoggedInUserLocale();
			String userLanguage = loggedInUserLocale.getLanguage();

			Resource[] sourceResources = new PathMatchingResourcePatternResolver().getResources("classpath:changelog/" + userLanguage + "_*");

			// ha üres a keresett nyelvre, akkor az 1. fájl nyelve alapján betöltjük a többit
			if (sourceResources.length == 0) {

				sourceResources = new PathMatchingResourcePatternResolver().getResources("classpath:changelog/*");

				for (Resource sourceResource : sourceResources) {
					String filename = sourceResource.getFilename();
					userLanguage = filename.substring(0, 2);
					break;
				}

				sourceResources = new PathMatchingResourcePatternResolver().getResources("classpath:changelog/" + userLanguage + "_*");

			}

			if (sourceResources.length > 0) {

				final List<String> filenameList = new ArrayList<>();

				for (final Resource sourceResource : sourceResources) {
					filenameList.add(sourceResource.getFilename());
				}

				filenameList.sort((o1, o2) -> {
					final String version1 = o1.split("_")[1];
					final String version2 = o2.split("_")[1];
					return version2.compareTo(version1);
				});

				for (final String filename : filenameList) {

					final Resource sourceResource = new PathMatchingResourcePatternResolver().getResources("classpath:changelog/" + filename)[0];

					String content = null;

					try (InputStream is = sourceResource.getInputStream()) {
						content = IOUtils.toString(is, "UTF-8");
					}

					final MarkdownDisplayLabel lblMkDisplay = new MarkdownDisplayLabel();
					lblMkDisplay.setValue(content);

					this.addComponent(lblMkDisplay);

				}
			}

		} catch (final Exception e) {
			throw new ToolboxGeneralException("Missing sourceFile in changelog folder: ", e);
		}

	}
}
