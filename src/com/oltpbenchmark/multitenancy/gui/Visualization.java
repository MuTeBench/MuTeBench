package com.oltpbenchmark.multitenancy.gui;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.category.CategoryDataset;

public enum Visualization {
	AreaChart, BarChart, LineChart, LineChart3D, StackedAreaChart, StackedBarChart, StackedBarChart3D;

	public static JFreeChart createChart(Visualization vis, String title,
			Metric metric, CategoryDataset dataset) {
		switch (vis) {
		case BarChart:
			return ChartFactory.createBarChart(title, "Bucket start time",
					metric.getValues().get(0), dataset,
					PlotOrientation.VERTICAL, true, true, false);
		case StackedBarChart:
			return ChartFactory.createStackedBarChart(title,
					"Bucket start time", metric.getValues().get(0), dataset,
					PlotOrientation.VERTICAL, true, true, false);
		case StackedBarChart3D:
			return ChartFactory.createStackedBarChart3D(title,
					"Bucket start time", metric.getValues().get(0), dataset,
					PlotOrientation.VERTICAL, true, true, false);
		case AreaChart:
			return ChartFactory.createAreaChart(title, "Bucket start time",
					metric.getValues().get(0), dataset,
					PlotOrientation.VERTICAL, true, true, false);
		case StackedAreaChart:
			return ChartFactory.createStackedAreaChart(title,
					"Bucket start time", metric.getValues().get(0), dataset,
					PlotOrientation.VERTICAL, true, true, false);
		case LineChart:
			return ChartFactory.createLineChart(title, "Bucket start time",
					metric.getValues().get(0), dataset,
					PlotOrientation.VERTICAL, true, true, false);
		case LineChart3D:
			return ChartFactory.createLineChart3D(title, "Bucket start time",
					metric.getValues().get(0), dataset,
					PlotOrientation.VERTICAL, true, true, false);
		}
		return null;
	}
}
