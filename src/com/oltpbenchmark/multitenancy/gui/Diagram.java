package com.oltpbenchmark.multitenancy.gui;

import java.awt.Color;

import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.CategoryAxis;
import org.jfree.chart.axis.CategoryLabelPositions;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.labels.StandardCategoryItemLabelGenerator;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.DatasetRenderingOrder;
import org.jfree.chart.renderer.category.CategoryItemRenderer;
import org.jfree.chart.renderer.category.LineAndShapeRenderer;
import org.jfree.data.category.CategoryDataset;
import org.jfree.data.category.DefaultCategoryDataset;

import com.oltpbenchmark.DistributionStatistics;
import com.oltpbenchmark.ThreadBench.TimeBucketIterable;
import com.oltpbenchmark.multitenancy.ResultCollector;

public class Diagram {

	private CategoryDataset absDataset;
	private CategoryDataset relDataset;
	private JFreeChart chart;
	private ChartPanel chartPanel;
	private Metric metric = Metric.values()[0];
	private boolean showRelativePerformance = false;
	private Visualization visualisation = Visualization.StackedBarChart;
	int windowSize;
	int tenantID = -1;
	ResultCollector rCol;

	public Diagram(int windowSize, ResultCollector rCol) {
		this.windowSize = windowSize;
		this.rCol = rCol;
	}

	public void updateDatasets() {
		absDataset = updateDataset(true);
		relDataset = updateDataset(false);
	}

	/**
	 * sets data set for a chart
	 * 
	 * @param absValues
	 *            use of absolute values (true) or relative values (false)
	 * @return data set
	 */
	private CategoryDataset updateDataset(boolean absValues) {
		DefaultCategoryDataset set = new DefaultCategoryDataset();
		// tenant id -1: get all results
		if (tenantID == -1) {
			for (Integer currentID : rCol.getTenantList()) {
				if (rCol.getResults(currentID) != null) {
					int i = 0;
					for (DistributionStatistics s : new TimeBucketIterable(rCol
							.getResults(currentID).getResults()
							.getLatencySamples(), windowSize)) {
						// calc absolute values
						if (absValues) {
							set.addValue(Metric.getAbsolutePerformance(metric,
									s, windowSize, rCol.getResults(currentID)
											.getPenalties(), i), "Tenant "
									+ currentID, String.valueOf(i * windowSize));
						} else {
							// calc relative values and handle NaN
							String[] base;
							try {
								base = rCol.getResults(currentID).getResults()
										.getBaselineResults()
										.get(i * windowSize).split(";");
							} catch (NullPointerException e) {
								continue;
							}
							double value = Metric.getRelativePerformance(
									metric, s, windowSize, base, rCol
											.getResults(currentID)
											.getPenalties(), i);
							/*
							 * if (Double.isNaN(value)) { if
							 * (Metrics.getAbsolutePerformance(metric, s,
							 * windowSize)==0) { value = 1; } else value = 0; }
							 */
							set.addValue(value, "Tenant " + currentID,
									String.valueOf(i * windowSize));
						}
						i += 1;
					}
				}
			}
		} else {
			// valid tenant id: get only results of this tenant
			if (rCol.getResults(tenantID) != null) {
				int i = 0;
				for (DistributionStatistics s : new TimeBucketIterable(rCol
						.getResults(tenantID).getResults().getLatencySamples(),
						windowSize)) {
					// calc absolute values
					if (absValues) {
						set.addValue(Metric.getAbsolutePerformance(metric, s,
								windowSize, rCol.getResults(tenantID)
										.getPenalties(), i), "Tenant "
								+ tenantID, String.valueOf(i * windowSize));
					} else {
						// calc relative values and handle NaN
						String[] base;
						try {
							base = rCol.getResults(tenantID).getResults()
									.getBaselineResults().get(i * windowSize)
									.split(";");
						} catch (NullPointerException e) {
							continue;
						}
						double value = Metric.getRelativePerformance(metric, s,
								windowSize, base, rCol.getResults(tenantID)
										.getPenalties(), i);

						if (Double.isNaN(value)) {
							if (Metric.getAbsolutePerformance(metric, s,
									windowSize, rCol.getResults(tenantID)
											.getPenalties(), i) == 0) {
								value = 1;
							} else
								value = 0;
						}
						set.addValue(value, "Tenant " + tenantID,
								String.valueOf(i * windowSize));
					}
					i += 1;
				}
			}
		}
		return set;
	}

	public void createChart() {
		String title = (tenantID == -1) ? "System performance"
				: "Performance of Tenant " + tenantID;
		chart = Visualization.createChart(visualisation, title, metric,
				absDataset);
		chart.setBackgroundPaint(new Color(220, 220, 220));

		final CategoryPlot plot = (CategoryPlot) chart.getPlot();
		plot.setBackgroundPaint(Color.lightGray);
		plot.setRangeGridlinePaint(Color.white);

		if (showRelativePerformance) {
			plot.setDataset(1, relDataset);
			plot.mapDatasetToRangeAxis(1, 1);

			final CategoryAxis domainAxis = plot.getDomainAxis();
			domainAxis
					.setCategoryLabelPositions(CategoryLabelPositions.DOWN_45);
			final ValueAxis axis2 = new NumberAxis(metric.getValues().get(1));
			plot.setRangeAxis(1, axis2);
			plot.setRenderer(1, new LineAndShapeRenderer());
			final CategoryItemRenderer renderer = plot.getRenderer(1);
			// show data on the bar
			renderer.setBaseItemLabelGenerator(new StandardCategoryItemLabelGenerator());
			renderer.setBaseItemLabelsVisible(true);
			plot.setDatasetRenderingOrder(DatasetRenderingOrder.FORWARD);
		}

		final NumberAxis rangeAxis = (NumberAxis) plot.getRangeAxis();
		rangeAxis.setStandardTickUnits(NumberAxis.createIntegerTickUnits());
		rangeAxis.setAutoRangeIncludesZero(true);

		chartPanel = new ChartPanel(chart);
		chartPanel.setBackground(Color.lightGray);

		// disable bar outlines and the gradient
		final CategoryItemRenderer renderer = plot.getRenderer();
		// show data on the bar
		renderer.setBaseItemLabelGenerator(new StandardCategoryItemLabelGenerator());
		renderer.setBaseItemLabelsVisible(true);
	}

	public ChartPanel getChartPanel() {
		return chartPanel;
	}

	public Metric getMetric() {
		return metric;
	}

	public void setMetric(Metric metric) {
		this.metric = metric;
	}

	public Visualization getVisualisation() {
		return visualisation;
	}

	public void setVisualisation(Visualization visualisation) {
		this.visualisation = visualisation;
	}

	public int getTenantID() {
		return tenantID;
	}

	public void setTenantID(int tenantID) {
		this.tenantID = tenantID;
	}

	public JFreeChart getChart() {
		return chart;
	}

	public void setChart(JFreeChart chart) {
		this.chart = chart;
	}

	public boolean isShowRelativePerformance() {
		return showRelativePerformance;
	}

	public void setShowRelativePerformance(boolean showRelativePerformance) {
		this.showRelativePerformance = showRelativePerformance;
	}
}