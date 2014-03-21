package com.oltpbenchmark.multitenancy.gui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.Toolkit;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.Vector;

import javax.swing.BorderFactory;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.TitledBorder;

import org.jfree.ui.RefineryUtilities;

import com.oltpbenchmark.multitenancy.ResultCollector;

public class Gui {

	private Diagram overviewChart;
	private Diagram tenantChart;
	private JComboBox metricBox;
	private JComboBox tenantBox;
	private JComboBox visBox;
	private JCheckBox relPerformanceBox;
	private Vector<Integer> relevantTenants;
	private JFrame frame = new JFrame("MuTeBench - Result Visualizer");
	private JPanel overviewPanel = new JPanel();
	private JPanel tenantPanel = new JPanel();
	private JPanel controlPanel = new JPanel(new GridLayout(2, 4, 150, 0));
	private JPanel headerPanel = new JPanel();
	int windowSize;
	ResultCollector rCol;

	public Gui(int windowSize, ResultCollector rCol, String output) {
		this.windowSize = windowSize;
		this.rCol = rCol;

		// create panels
		createHeaderPanel();
		createOverviewPanel();
		createTenantPanel();
		createControlPanel();

		// create gui
		frame.getContentPane().add(headerPanel, BorderLayout.PAGE_START);
		frame.getContentPane().add(overviewPanel, BorderLayout.LINE_START);
		frame.getContentPane().add(tenantPanel, BorderLayout.LINE_END);
		frame.getContentPane().add(controlPanel, BorderLayout.PAGE_END);
		frame.getContentPane().setVisible(true);
		frame.pack();
		RefineryUtilities.centerFrameOnScreen(frame);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setPreferredSize(new Dimension((int) Math.floor(Toolkit
				.getDefaultToolkit().getScreenSize().width * 0.8), Toolkit
				.getDefaultToolkit().getScreenSize().height));
		frame.setResizable(false);
		frame.toFront();
		frame.setVisible(true);
		frame.setState(java.awt.Frame.ICONIFIED);
		frame.setState(java.awt.Frame.NORMAL);
	}

	private void createHeaderPanel() {
		JLabel header = new JLabel("MuTeBench - Result Visualizer");
		Font f = new Font("Dialog", Font.BOLD, 20);
		header.setFont(f);
		headerPanel.add(header);
	}

	private void createOverviewPanel() {
		// create overview chart
		overviewChart = new Diagram(windowSize, rCol);
		overviewChart.updateDatasets();
		overviewChart.createChart();
		overviewPanel.add(overviewChart.getChartPanel());
		overviewPanel.setVisible(true);
	}

	private void createControlPanel() {
		// add labels
		final JLabel metricLabel = new JLabel("Chosen Metric: ");
		final JLabel visLabel = new JLabel("Chosen Visualization: ");
		final JLabel tenantLabel = new JLabel("Choose Tenant: ");
		final JLabel relativePerfLabel = new JLabel(
				"Show Relative Performance: ");
		controlPanel.add(metricLabel);
		controlPanel.add(visLabel);
		controlPanel.add(tenantLabel);
		controlPanel.add(relativePerfLabel);

		// add combo box for metrics
		String metricList[] = new String[Metric.values().length];
		for (int i = 0; i < Metric.values().length; i++) {
			metricList[i] = Metric.values()[i].getValues().get(0);
		}
		metricBox = new JComboBox(metricList);
		metricBox.addItemListener(new ItemListener() {
			@Override
			public void itemStateChanged(ItemEvent arg0) {
				// update overview chart
				overviewChart.setMetric(Metric.getMetric(metricBox
						.getSelectedItem().toString()));
				updateGui(overviewChart, overviewPanel);

				// update tenant chart
				tenantChart.setMetric(Metric.getMetric(metricBox
						.getSelectedItem().toString()));
				updateGui(tenantChart, tenantPanel);
			}
		});
		controlPanel.add(metricBox);

		// add combo box for visualization
		String visList[] = new String[Visualization.values().length];
		for (int i = 0; i < Visualization.values().length; i++) {
			visList[i] = Visualization.values()[i].toString();
		}
		visBox = new JComboBox(visList);
		visBox.setSelectedItem(Visualization.StackedBarChart.toString());
		visBox.addItemListener(new ItemListener() {
			@Override
			public void itemStateChanged(ItemEvent arg0) {
				// update overview chart
				overviewChart.setVisualisation(Visualization.valueOf(visBox
						.getSelectedItem().toString()));
				updateGui(overviewChart, overviewPanel);

				// update tenant chart
				tenantChart.setVisualisation(Visualization.valueOf(visBox
						.getSelectedItem().toString()));
				updateGui(tenantChart, tenantPanel);
			}
		});
		controlPanel.add(visBox);

		// create combo box for tenants
		tenantBox = new JComboBox(relevantTenants);
		tenantBox.addItemListener(new ItemListener() {
			@Override
			public void itemStateChanged(ItemEvent arg0) {
				// update tenant chart
				tenantChart.setTenantID(Integer.parseInt(tenantBox
						.getSelectedItem().toString()));
				updateGui(tenantChart, tenantPanel);
			}
		});
		controlPanel.add(tenantBox);

		// create check box for relative performance
		relPerformanceBox = new JCheckBox();
		relPerformanceBox.addItemListener(new ItemListener() {
			@Override
			public void itemStateChanged(ItemEvent arg0) {
				// update tenant chart
				tenantChart.setShowRelativePerformance(relPerformanceBox
						.isSelected());
				updateGui(tenantChart, tenantPanel);
			}
		});
		controlPanel.add(relPerformanceBox);

		// create control panel
		TitledBorder centerBorder = BorderFactory
				.createTitledBorder("Chart Controls");
		centerBorder.setTitleJustification(TitledBorder.CENTER);
		controlPanel.setBorder(centerBorder);
		controlPanel.setVisible(true);
	}

	private void createTenantPanel() {
		// create tenant list
		relevantTenants = new Vector<Integer>();
		for (Integer tenant : rCol.getTenantList()) {
			if (rCol.getResults(tenant) != null)
				relevantTenants.add(tenant);
		}

		// create tenant chart
		tenantChart = new Diagram(windowSize, rCol);
		tenantChart.setTenantID(relevantTenants.firstElement());
		tenantChart.updateDatasets();
		tenantChart.createChart();
		tenantPanel.add(tenantChart.getChartPanel());
		tenantPanel.setVisible(true);
	}

	private void updateGui(Diagram chart, JPanel panel) {
		chart.updateDatasets();
		chart.createChart();
		panel.removeAll();
		panel.add(chart.getChartPanel());
		panel.validate();
	}
}