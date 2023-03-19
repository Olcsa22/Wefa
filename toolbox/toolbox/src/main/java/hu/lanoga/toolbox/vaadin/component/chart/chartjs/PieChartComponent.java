package hu.lanoga.toolbox.vaadin.component.chart.chartjs;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.byteowls.vaadin.chartjs.ChartJs;
import com.byteowls.vaadin.chartjs.config.PieChartConfig;
import com.byteowls.vaadin.chartjs.data.Dataset;
import com.byteowls.vaadin.chartjs.data.PieDataset;
import com.byteowls.vaadin.chartjs.utils.ColorUtils;
import com.vaadin.ui.VerticalLayout;

import hu.lanoga.toolbox.spring.ApplicationContextHelper;
import hu.lanoga.toolbox.vaadin.component.chart.ToolboxVaadinChart;
import lombok.Getter;

/**
 * ChartJS alapú kördiagram
 */
@Getter
public class PieChartComponent extends VerticalLayout implements ToolboxVaadinChart {

	final PieChartConfig config;

	final String caption;
	final Map<String, Double> valueMap;

	final private ChartJs chartJS;

	private void setLabels() {

		final List<String> list = new ArrayList<>(this.valueMap.keySet());

		this.config.data()
				.labelsAsList(list)
				.addDataset(new PieDataset().label(""))
				.and();

	}

	private void setOptions() {
		this.config.options()
				.responsive(true)
				.title()
				.display(true)
				.text(this.caption)
				.and()
				.animation()
				.animateScale(true)
				.animateRotate(true)
				.and()
				.done();
	}

	/**
	 * @param caption
	 * @param valueMap
	 *            felirat -> érték
	 */
	public PieChartComponent(final String caption, final Map<String, Double> valueMap) {

		this.caption = caption;
		this.valueMap = valueMap;

		this.config = new PieChartConfig();

		this.setLabels();

		if (!valueMap.isEmpty()) {

			this.setOptions();

			for (final Dataset<?, ?> ds : this.config.data().getDatasets()) {

				final List<Double> data = new ArrayList<>();
				final List<String> colors = new ArrayList<>();

				for (final Double value : valueMap.values()) {
					data.add(value);
					colors.add(ColorUtils.randomColor(0.7));
				}

				final PieDataset pds = (PieDataset) ds;
				pds.backgroundColor(colors.toArray(new String[colors.size()]));
				pds.dataAsList(data);
			}

		}

		chartJS = new ChartJs(this.config);
		chartJS.setWidth("100%");

		if (ApplicationContextHelper.hasDevProfile()) {
			chartJS.setJsLoggingEnabled(true);
		}

		this.addComponent(chartJS);

		this.setSpacing(false);
		this.setMargin(true);
		this.setWidth("100%");
	}

}
