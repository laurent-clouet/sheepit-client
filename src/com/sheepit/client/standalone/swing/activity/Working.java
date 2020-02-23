/*
 * Copyright (C) 2015 Laurent CLOUET
 * Author Laurent CLOUET <laurent.clouet@nopnop.net>
 *
 * This program is free software; you can redistribute it and/or 
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; version 2
 * of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */

package com.sheepit.client.standalone.swing.activity;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.DecimalFormat;
import java.util.Date;

import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.Spring;
import javax.swing.SpringLayout;
import javax.swing.border.TitledBorder;

import com.sheepit.client.Client;
import com.sheepit.client.Job;
import com.sheepit.client.Server;
import com.sheepit.client.Stats;
import com.sheepit.client.Utils;

import com.sheepit.client.Configuration;
import com.sheepit.client.standalone.GuiSwing;
import com.sheepit.client.standalone.GuiSwing.ActivityType;

public class Working implements Activity {
	private GuiSwing parent;
	
	private JLabel statusContent;
	private JLabel renderedFrameContent;
	private JLabel remainingFrameContent;
	private JLabel lastRenderTime;
	private JLabel lastRender;
	private JLabel creditEarned;
	private JButton pauseButton;
	private JButton exitAfterFrame;
	private JLabel currentProjectNameValue;
	private JLabel currentProjectDurationValue;
	private JLabel currrentProjectProgressionValue;
	private JLabel currentProjectComputeMethodValue;
	private JLabel userInfoPointsTotalValue;
	private JLabel renderableProjectsValue;
	private JLabel waitingProjectsValue;
	private JLabel connectedMachinesValue;
	private JLabel userInfoTotalRenderTimeThisSessionValue;
	
	public Working(GuiSwing parent_) {
		parent = parent_;

		statusContent                           = new JLabel("Init");
		currentProjectNameValue                 = new JLabel("");
		currentProjectDurationValue             = new JLabel("");
		currrentProjectProgressionValue         = new JLabel("");
		currentProjectComputeMethodValue        = new JLabel("");

		waitingProjectsValue                    = new JLabel("");
		connectedMachinesValue                  = new JLabel("");
		remainingFrameContent                   = new JLabel("");
		userInfoPointsTotalValue                = new JLabel("");


		creditEarned                            = new JLabel("");
		renderedFrameContent                    = new JLabel("");
		renderableProjectsValue                 = new JLabel("");
		userInfoTotalRenderTimeThisSessionValue = new JLabel("");

		lastRenderTime                          = new JLabel("");
		lastRender                              = new JLabel("");
	}
	
	@Override
	public void show() {
		Configuration config = parent.getConfiguration();

		Color backgroundColor = config.getThemedBackgroundColor();
		Color foregroundColor = config.getThemedForegroundColor();

		// Image logo
		ImageIcon image   = new ImageIcon(getClass().getResource(config.getThemedSheepItLogo()));
		JLabel labelImage = new JLabel(image);
		labelImage.setAlignmentX(Component.CENTER_ALIGNMENT);

		parent.getContentPane().add(labelImage);

		// current project
		JPanel currentProjectPanel = new JPanel(new SpringLayout());

		TitledBorder titledBorder = BorderFactory.createTitledBorder("Project");
		titledBorder.setTitleColor(foregroundColor);

		currentProjectPanel.setBorder(titledBorder);
		currentProjectPanel.setBackground(backgroundColor);

		// Configure the right theme for Project panel's data fields
		statusContent.setForeground(foregroundColor);
		currentProjectNameValue.setForeground(foregroundColor);
		currentProjectDurationValue.setForeground(foregroundColor);
		currrentProjectProgressionValue.setForeground(foregroundColor);
		currentProjectComputeMethodValue.setForeground(foregroundColor);

		JLabel currentProjectStatusLabel = new JLabel("Status: ", JLabel.TRAILING);
		currentProjectStatusLabel.setForeground(foregroundColor);

		JLabel currentProjectNameLabel = new JLabel("Name: ", JLabel.TRAILING);
		currentProjectNameLabel.setForeground(foregroundColor);

		JLabel currentProjectDurationLabel = new JLabel("Rendering for: ", JLabel.TRAILING);
		currentProjectDurationLabel.setForeground(foregroundColor);

		JLabel currentProjectProgressionLabel = new JLabel("Remaining: ", JLabel.TRAILING);
		currentProjectProgressionLabel.setForeground(foregroundColor);

		JLabel currentProjectComputeMethodLabel = new JLabel("Compute method: ", JLabel.TRAILING);
		currentProjectComputeMethodLabel.setForeground(foregroundColor);

		currentProjectPanel.add(currentProjectStatusLabel);
		currentProjectPanel.add(statusContent);
		
		currentProjectPanel.add(currentProjectNameLabel);
		currentProjectPanel.add(currentProjectNameValue);
		
		currentProjectPanel.add(currentProjectDurationLabel);
		currentProjectPanel.add(currentProjectDurationValue);
		
		currentProjectPanel.add(currentProjectProgressionLabel);
		currentProjectPanel.add(currrentProjectProgressionValue);
		
		currentProjectPanel.add(currentProjectComputeMethodLabel);
		currentProjectPanel.add(currentProjectComputeMethodValue);

		// global stats
		JPanel globalStatsPanel = new JPanel(new SpringLayout());

		titledBorder = BorderFactory.createTitledBorder("Global stats");
		titledBorder.setTitleColor(foregroundColor);

		globalStatsPanel.setBorder(titledBorder);
		globalStatsPanel.setBackground(backgroundColor);

		// Data fields theme
		waitingProjectsValue.setForeground(foregroundColor);
		connectedMachinesValue.setForeground(foregroundColor);
		remainingFrameContent.setForeground(foregroundColor);
		userInfoPointsTotalValue.setForeground(foregroundColor);

		JLabel globalStatsMachineConnected = new JLabel("Machines connected: ", JLabel.TRAILING);
		globalStatsMachineConnected.setForeground(foregroundColor);

		JLabel globalStatsRemainingFrame = new JLabel("Remaining frames: ", JLabel.TRAILING);
		globalStatsRemainingFrame.setForeground(foregroundColor);

		JLabel globalStatsWaitingProject = new JLabel("Active projects: ", JLabel.TRAILING);
		globalStatsWaitingProject.setForeground(foregroundColor);

		JLabel globalStatsUserPoints = new JLabel("User's points: ", JLabel.TRAILING);
		globalStatsUserPoints.setForeground(foregroundColor);

		globalStatsPanel.add(globalStatsWaitingProject);
		globalStatsPanel.add(waitingProjectsValue);

		globalStatsPanel.add(globalStatsMachineConnected);
		globalStatsPanel.add(connectedMachinesValue);

		globalStatsPanel.add(globalStatsRemainingFrame);
		globalStatsPanel.add(remainingFrameContent);

		globalStatsPanel.add(globalStatsUserPoints);
		globalStatsPanel.add(userInfoPointsTotalValue);

		// user info
		JPanel session_info_panel = new JPanel(new SpringLayout());
		session_info_panel.setBorder(BorderFactory.createTitledBorder("Session infos"));
		
		JLabel user_info_credits_this_session = new JLabel("Points earned: ", JLabel.TRAILING);
		JLabel user_info_total_rendertime_this_session = new JLabel("Duration: ", JLabel.TRAILING);
		JLabel user_info_rendered_frame_this_session = new JLabel("Rendered frames: ", JLabel.TRAILING);
		JLabel global_static_renderable_project = new JLabel("Renderable projects: ", JLabel.TRAILING);
		
		session_info_panel.add(user_info_credits_this_session);
		session_info_panel.add(creditEarned);
		
		session_info_panel.add(user_info_rendered_frame_this_session);
		session_info_panel.add(renderedFrameContent);
		
		session_info_panel.add(global_static_renderable_project);
		session_info_panel.add(renderableProjectsValue);
		
		session_info_panel.add(user_info_total_rendertime_this_session);
		session_info_panel.add(userInfoTotalRenderTimeThisSessionValue);
		
		// last frame
		JPanel last_frame_panel = new JPanel();
		last_frame_panel.setLayout(new BoxLayout(last_frame_panel, BoxLayout.Y_AXIS));
		last_frame_panel.setBorder(BorderFactory.createTitledBorder("Last rendered frame"));
		lastRender.setIcon(new ImageIcon(new BufferedImage(200, 120, BufferedImage.TYPE_INT_ARGB)));
		lastRender.setAlignmentX(Component.CENTER_ALIGNMENT);
		lastRenderTime.setAlignmentX(Component.CENTER_ALIGNMENT);
		last_frame_panel.add(lastRenderTime);
		last_frame_panel.add(lastRender);
		

		
		JPanel buttonsPanel = new JPanel(new GridLayout(2, 2));
		
		JButton settingsButton = new JButton("Settings");
		settingsButton.addActionListener(new SettingsAction());
		
		pauseButton = new JButton("Pause");
		Client client = parent.getClient();
		if (client != null && client.isSuspended()) {
			pauseButton.setText("Resume");
		}
		
		pauseButton.addActionListener(new PauseAction());
		
		JButton blockJob = new JButton("Block this project");
		blockJob.addActionListener(new blockJobAction());
		
		exitAfterFrame = new JButton("Exit after this frame");
		exitAfterFrame.addActionListener(new ExitAfterAction());
		
		buttonsPanel.add(settingsButton);
		buttonsPanel.add(pauseButton);
		buttonsPanel.add(blockJob);
		buttonsPanel.add(exitAfterFrame);
		
		parent.getContentPane().setLayout(new GridBagLayout());
		GridBagConstraints global_constraints = new GridBagConstraints();
		global_constraints.fill = GridBagConstraints.HORIZONTAL;
		global_constraints.weightx = 1;
		global_constraints.gridx = 0;
		
		parent.getContentPane().add(currentProjectPanel, global_constraints);
		parent.getContentPane().add(globalStatsPanel, global_constraints);
		parent.getContentPane().add(session_info_panel, global_constraints);
		parent.getContentPane().add(last_frame_panel, global_constraints);
		parent.getContentPane().add(buttonsPanel, global_constraints);
		
		Spring widthLeftColumn = getBestWidth(currentProjectPanel, 4, 2);
		widthLeftColumn = Spring.max(widthLeftColumn, getBestWidth(globalStatsPanel, 4, 2));
		widthLeftColumn = Spring.max(widthLeftColumn, getBestWidth(session_info_panel, 3, 2));
		alignPanel(currentProjectPanel, 5, 2, widthLeftColumn);
		alignPanel(globalStatsPanel, 4, 2, widthLeftColumn);
		alignPanel(session_info_panel, 4, 2, widthLeftColumn);
	}
	
	public void setStatus(String msg_) {
		statusContent.setText("<html>" + msg_ + "</html>");
	}
	
	public void setRenderingProjectName(String msg_) {
		currentProjectNameValue.setText("<html>" + (msg_.length() > 26 ? msg_.substring(0, 26) : msg_) + "</html>");
	}
	
	public void setRemainingTime(String time_) {
		currrentProjectProgressionValue.setText("<html>" + time_ + "</html>");
	}
	
	public void setRenderingTime(String time_) {
		currentProjectDurationValue.setText("<html>" + time_ + "</html>");
	}
	
	public void setComputeMethod(String computeMethod_) {
		this.currentProjectComputeMethodValue.setText(computeMethod_);
	}
	
	public void displayStats(Stats stats) {
		DecimalFormat df = new DecimalFormat("##,##,##,##,##,##,##0");
		remainingFrameContent.setText(df.format(stats.getRemainingFrame()));
		creditEarned.setText(df.format(stats.getCreditsEarnedDuringSession()));
		userInfoPointsTotalValue.setText(df.format(stats.getCreditsEarned()));
		renderableProjectsValue.setText(df.format(stats.getRenderableProject()));
		waitingProjectsValue.setText(df.format(stats.getWaitingProject()));
		connectedMachinesValue.setText(df.format(stats.getConnectedMachine()));
		updateTime();
	}
	
	public void updateTime() {
		if (this.parent.getClient().getStartTime() != 0) {
			userInfoTotalRenderTimeThisSessionValue.setText(Utils.humanDuration(new Date((new Date().getTime() - this.parent.getClient().getStartTime()))));
		}
		Job job = this.parent.getClient().getRenderingJob();
		if (job != null && job.getProcessRender() != null && job.getProcessRender().getStartTime() > 0) {
			currentProjectDurationValue.setText("<html>" + Utils.humanDuration(new Date((new Date().getTime() - job.getProcessRender().getStartTime()))) + "</html>");
		}
		else {
			currentProjectDurationValue.setText("");
		}
	}
	
	public void setRenderedFrame(int n) {
		renderedFrameContent.setText(String.valueOf(n));
		showLastRender();
	}
	
	public void showLastRender() {
		Client client = parent.getClient();
		if (client != null) {
			Job lastJob = client.getPreviousJob();
			Server server = client.getServer();
			if (server != null) {
				byte[] data = server.getLastRender();
				if (data != null) {
					InputStream is = new ByteArrayInputStream(data);
					try {
						BufferedImage image = ImageIO.read(is);
						if (image != null) {
							lastRender.setIcon(new ImageIcon(image));
							if (lastJob != null) {
								// don't use lastJob.getProcessRender().getDuration() due to timezone
								if (lastJob.getProcessRender().getDuration() > 1) {
									lastRenderTime.setText("Render time : " + Utils.humanDuration(new Date(lastJob.getProcessRender().getEndTime() - lastJob.getProcessRender().getStartTime())));
								}
							}
						}
					}
					catch (IOException e) {
						System.out.println("Working::showLastRender() exception " + e);
						e.printStackTrace();
					}
				}
			}
		}
		
	}
	
	private void alignPanel(Container parent, int rows, int cols, Spring width) {
		SpringLayout layout;
		try {
			layout = (SpringLayout) parent.getLayout();
		}
		catch (ClassCastException exc) {
			System.err.println("The first argument to makeCompactGrid must use SpringLayout.");
			return;
		}
		
		Spring x = Spring.constant(0);
		for (int c = 0; c < cols; c++) {
			for (int r = 0; r < rows; r++) {
				SpringLayout.Constraints constraints = getConstraintsForCell(r, c, parent, cols);
				constraints.setX(x);
				constraints.setWidth(width);
			}
			x = Spring.sum(x, width);
		}
		
		Spring y = Spring.constant(0);
		for (int r = 0; r < rows; r++) {
			Spring height = Spring.constant(0);
			for (int c = 0; c < cols; c++) {
				height = Spring.max(height, getConstraintsForCell(r, c, parent, cols).getHeight());
			}
			for (int c = 0; c < cols; c++) {
				SpringLayout.Constraints constraints = getConstraintsForCell(r, c, parent, cols);
				constraints.setY(y);
				constraints.setHeight(height);
			}
			y = Spring.sum(y, height);
		}
		
		SpringLayout.Constraints pCons = layout.getConstraints(parent);
		pCons.setConstraint(SpringLayout.SOUTH, y);
		pCons.setConstraint(SpringLayout.EAST, x);
		
	}
	
	private Spring getBestWidth(Container parent, int rows, int cols) {
		Spring x = Spring.constant(0);
		Spring width = Spring.constant(0);
		for (int c = 0; c < cols; c++) {
			
			for (int r = 0; r < rows; r++) {
				width = Spring.max(width, getConstraintsForCell(r, c, parent, cols).getWidth());
			}
		}
		return width;
	}
	
	private SpringLayout.Constraints getConstraintsForCell(int row, int col, Container parent, int cols) {
		SpringLayout layout = (SpringLayout) parent.getLayout();
		Component c = parent.getComponent(row * cols + col);
		return layout.getConstraints(c);
	}
	
	class PauseAction implements ActionListener {
		
		@Override
		public void actionPerformed(ActionEvent e) {
			Client client = parent.getClient();
			if (client != null) {
				if (client.isSuspended()) {
					pauseButton.setText("Pause");
					client.resume();
				}
				else {
					pauseButton.setText("Resume");
					client.suspend();
				}
			}
		}
	}
	
	class SettingsAction implements ActionListener {
		@Override
		public void actionPerformed(ActionEvent e) {
			if (parent != null) {
				parent.showActivity(ActivityType.SETTINGS);
			}
		}
	}
	
	class ExitAfterAction implements ActionListener {
		@Override
		public void actionPerformed(ActionEvent e) {
			Client client = parent.getClient();
			if (client != null) {
				if (client.isRunning()) {
					exitAfterFrame.setText("Cancel exit");
					client.askForStop();
				}
				else {
					exitAfterFrame.setText("Exit after this frame");
					client.cancelStop();
				}
			}
		}
	}
	
	class blockJobAction implements ActionListener {
		@Override
		public void actionPerformed(ActionEvent e) {
			Client client = parent.getClient();
			if (client != null) {
				Job job = client.getRenderingJob();
				if (job != null) {
					job.block();
				}
			}
		}
	}
	
}
