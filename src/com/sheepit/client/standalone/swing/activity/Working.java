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

import java.awt.Component;
import java.awt.Container;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.awt.Image;
import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.Date;

import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.UIManager;
import javax.swing.Spring;
import javax.swing.SpringLayout;

import com.sheepit.client.Client;
import com.sheepit.client.Job;
import com.sheepit.client.Stats;
import com.sheepit.client.Utils;
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
	private JLabel current_project_name_value;
	private JLabel current_project_duration_value;
	private JLabel currrent_project_progression_value;
	private JLabel current_project_compute_method_value;
	private JLabel user_info_points_total_value;
	private JLabel renderable_projects_value;
	private JLabel waiting_projects_value;
	private JLabel connected_machines_value;
	private JLabel user_info_total_rendertime_this_session_value;
	private String currentTheme;
	
	public Working(GuiSwing parent_) {
		parent = parent_;
		
		statusContent = new JLabel("Init");
		renderedFrameContent = new JLabel("");
		remainingFrameContent = new JLabel("");
		creditEarned = new JLabel("");
		current_project_name_value = new JLabel("");
		current_project_duration_value = new JLabel("");
		currrent_project_progression_value = new JLabel("");
		current_project_compute_method_value = new JLabel("");
		user_info_points_total_value = new JLabel("");
		renderable_projects_value = new JLabel("");
		waiting_projects_value = new JLabel("");
		connected_machines_value = new JLabel("");
		user_info_total_rendertime_this_session_value = new JLabel("");
		lastRenderTime = new JLabel("");
		lastRender = new JLabel("");
		currentTheme = UIManager.getLookAndFeel().getName();    // Capture the theme on component instantiation
	}
	
	@Override
	public void show() {
		// If the stored theme and the UIManager's theme doesn't match is bc the user has changed it
		if (!currentTheme.equals(UIManager.getLookAndFeel().getName())) {
			// And, as the user has changed the theme, then we must recreate all the UI elements with session data
			// Reason being they are defined as class variables and therefore created once when the object
			// is created the first time.
			// As the Java swing engine applies the "look & feel" at creation time, we need to "re-create" the
			// objects to ensure they have the right theme colors.
			statusContent = new JLabel(statusContent.getText());
			renderedFrameContent = new JLabel(renderedFrameContent.getText());
			remainingFrameContent = new JLabel(remainingFrameContent.getText());
			creditEarned = new JLabel(creditEarned.getText());
			current_project_name_value = new JLabel(current_project_name_value.getText());
			current_project_duration_value = new JLabel(current_project_duration_value.getText());
			currrent_project_progression_value = new JLabel(currrent_project_progression_value.getText());
			current_project_compute_method_value = new JLabel(current_project_compute_method_value.getText());
			user_info_points_total_value = new JLabel(user_info_points_total_value.getText());
			renderable_projects_value = new JLabel(renderable_projects_value.getText());
			waiting_projects_value = new JLabel(waiting_projects_value.getText());
			connected_machines_value = new JLabel(connected_machines_value.getText());
			user_info_total_rendertime_this_session_value = new JLabel(user_info_total_rendertime_this_session_value.getText());
			lastRenderTime = new JLabel(lastRenderTime.getText());
			lastRender = new JLabel(lastRender.getText());

			// set the new theme as the current one
			currentTheme = UIManager.getLookAndFeel().getName();
		}

		// current project
		JPanel current_project_panel = new JPanel(new SpringLayout());
		current_project_panel.setBorder(BorderFactory.createTitledBorder("Project"));
		
		JLabel current_project_status = new JLabel("Status: ", JLabel.TRAILING);
		JLabel current_project_name = new JLabel("Name: ", JLabel.TRAILING);
		JLabel current_project_duration = new JLabel("Rendering for: ", JLabel.TRAILING);
		JLabel current_project_progression = new JLabel("Remaining: ", JLabel.TRAILING);
		JLabel current_project_compute_method_label = new JLabel("Compute method: ", JLabel.TRAILING);
		
		current_project_panel.add(current_project_status);
		current_project_panel.add(statusContent);
		
		current_project_panel.add(current_project_name);
		current_project_panel.add(current_project_name_value);
		
		current_project_panel.add(current_project_duration);
		current_project_panel.add(current_project_duration_value);
		
		current_project_panel.add(current_project_progression);
		current_project_panel.add(currrent_project_progression_value);
		
		current_project_panel.add(current_project_compute_method_label);
		current_project_panel.add(current_project_compute_method_value);
		
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
		session_info_panel.add(renderable_projects_value);
		
		session_info_panel.add(user_info_total_rendertime_this_session);
		session_info_panel.add(user_info_total_rendertime_this_session_value);
		
		// global stats
		JPanel global_stats_panel = new JPanel(new SpringLayout());
		global_stats_panel.setBorder(BorderFactory.createTitledBorder("Global stats"));
		
		JLabel global_stats_machine_connected = new JLabel("Machines connected: ", JLabel.TRAILING);
		JLabel global_stats_remaining_frame = new JLabel("Remaining frames: ", JLabel.TRAILING);
		JLabel global_stats_waiting_project = new JLabel("Active projects: ", JLabel.TRAILING);
		JLabel global_stats_user_points = new JLabel("User's points: ", JLabel.TRAILING);
		
		global_stats_panel.add(global_stats_waiting_project);
		global_stats_panel.add(waiting_projects_value);
		
		global_stats_panel.add(global_stats_machine_connected);
		global_stats_panel.add(connected_machines_value);
		
		global_stats_panel.add(global_stats_remaining_frame);
		global_stats_panel.add(remainingFrameContent);
		
		global_stats_panel.add(global_stats_user_points);
		global_stats_panel.add(user_info_points_total_value);
		
		// last frame
		JPanel last_frame_panel = new JPanel();
		last_frame_panel.setLayout(new BoxLayout(last_frame_panel, BoxLayout.Y_AXIS));
		last_frame_panel.setBorder(BorderFactory.createTitledBorder("Last rendered frame"));
		lastRender.setIcon(new ImageIcon(new BufferedImage(200, 120, BufferedImage.TYPE_INT_ARGB)));
		lastRender.setAlignmentX(Component.CENTER_ALIGNMENT);
		lastRenderTime.setAlignmentX(Component.CENTER_ALIGNMENT);
		last_frame_panel.add(lastRenderTime);
		last_frame_panel.add(lastRender);
		
		ImageIcon image = new ImageIcon(getClass().getResource("/sheepit-logo.png"));
		JLabel labelImage = new JLabel(image);
		labelImage.setAlignmentX(Component.CENTER_ALIGNMENT);
		parent.getContentPane().add(labelImage);
		
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
		
		parent.getContentPane().add(new JLabel(" "), global_constraints);		// Add a separator between logo and first panel
		parent.getContentPane().add(current_project_panel, global_constraints);
		parent.getContentPane().add(global_stats_panel, global_constraints);
		parent.getContentPane().add(session_info_panel, global_constraints);
		parent.getContentPane().add(last_frame_panel, global_constraints);
		parent.getContentPane().add(new JLabel(" "), global_constraints);		// Add a separator between last panel and buttons
		parent.getContentPane().add(buttonsPanel, global_constraints);
		
		Spring widthLeftColumn = getBestWidth(current_project_panel, 4, 2);
		widthLeftColumn = Spring.max(widthLeftColumn, getBestWidth(global_stats_panel, 4, 2));
		widthLeftColumn = Spring.max(widthLeftColumn, getBestWidth(session_info_panel, 3, 2));
		alignPanel(current_project_panel, 5, 2, widthLeftColumn);
		alignPanel(global_stats_panel, 4, 2, widthLeftColumn);
		alignPanel(session_info_panel, 4, 2, widthLeftColumn);
	}
	
	public void setStatus(String msg_) {
		statusContent.setText("<html>" + msg_ + "</html>");
	}
	
	public void setRenderingProjectName(String msg_) {
		current_project_name_value.setText("<html>" + (msg_.length() > 26 ? msg_.substring(0, 26) : msg_) + "</html>");
	}
	
	public void setRemainingTime(String time_) {
		currrent_project_progression_value.setText("<html>" + time_ + "</html>");
	}
	
	public void setRenderingTime(String time_) {
		current_project_duration_value.setText("<html>" + time_ + "</html>");
	}
	
	public void setComputeMethod(String computeMethod_) {
		this.current_project_compute_method_value.setText(computeMethod_);
	}
	
	public void displayStats(Stats stats) {
		DecimalFormat df = new DecimalFormat("##,##,##,##,##,##,##0");
		remainingFrameContent.setText(df.format(stats.getRemainingFrame()));
		creditEarned.setText(df.format(stats.getCreditsEarnedDuringSession()));
		user_info_points_total_value.setText(df.format(stats.getCreditsEarned()));
		renderable_projects_value.setText(df.format(stats.getRenderableProject()));
		waiting_projects_value.setText(df.format(stats.getWaitingProject()));
		connected_machines_value.setText(df.format(stats.getConnectedMachine()));
		updateTime();
	}
	
	public void updateTime() {
		if (this.parent.getClient().getStartTime() != 0) {
			user_info_total_rendertime_this_session_value.setText(Utils.humanDuration(new Date((new Date().getTime() - this.parent.getClient().getStartTime()))));
		}
		Job job = this.parent.getClient().getRenderingJob();
		if (job != null && job.getProcessRender() != null && job.getProcessRender().getStartTime() > 0) {
			current_project_duration_value.setText("<html>" + Utils.humanDuration(new Date((new Date().getTime() - job.getProcessRender().getStartTime()))) + "</html>");
		}
		else {
			current_project_duration_value.setText("");
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
			if (lastJob != null) {
				ImageIcon icon = null;
				int idInt = Integer.parseInt(lastJob.getId());
				if (idInt == 1) {
					icon = new ImageIcon(getClass().getResource("/frame_compute_method.jpg"));
				}
				else if (idInt < 20) {
					icon = new ImageIcon(getClass().getResource("/frame_power_detection.jpg"));
				}
				else {
					try {
						String path = lastJob.getOutputImagePath();

						BufferedImage img = ImageIO.read(new File(path));
						float width = img.getWidth();
						float height = img.getHeight();
						float factor = 1.0f;
						if (height > 200) {
							factor = 200f / height;
						}
						if (width * factor > 200) {
							factor = Math.min(factor, 200f / width);
						}
						icon = new ImageIcon(img.getScaledInstance((int)(width * factor), (int)(height * factor), Image.SCALE_FAST));
					}
					catch (IOException e) {
						System.out.println("Working::showLastRender() exception " + e);
						e.printStackTrace();
					}
				}

				if (icon != null) {
					lastRender.setIcon(icon);
					// don't use lastJob.getProcessRender().getDuration() due to timezone
					if (lastJob.getProcessRender().getDuration() > 1) {
						lastRenderTime.setText("Render time : " + Utils.humanDuration(new Date(lastJob.getProcessRender().getEndTime() - lastJob.getProcessRender().getStartTime())));
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
