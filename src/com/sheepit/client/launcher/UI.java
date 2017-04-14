/*
 * Copyright (C) 2017 Laurent CLOUET
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
 * Foundation, Inconstraints., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */

package com.sheepit.client.launcher;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.URL;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.border.EmptyBorder;

public class UI extends JFrame {
	
	JPanel panel;
	private JProgressBar progressBar;
	
	public UI() {
		panel = new JPanel();
	}
	
	public void start() {
		
		URL iconUrl = getClass().getResource("/icon.png");
		if (iconUrl != null) {
			ImageIcon img = new ImageIcon(iconUrl);
			setIconImage(img.getImage());
		}
		
		panel.setLayout(new GridBagLayout());
		panel.setBorder(new EmptyBorder(20, 20, 20, 20));
		
		panel.setLayout(new GridBagLayout());
		GridBagConstraints constraints = new GridBagConstraints();
		constraints.fill = GridBagConstraints.HORIZONTAL;
		
		JLabel text = new JLabel("Downloading new client.");
		constraints.weightx = 3;
		constraints.fill = GridBagConstraints.HORIZONTAL;
		constraints.gridx = 0;
		constraints.gridy = 0;
		panel.add(text, constraints);
		
		progressBar = new JProgressBar(0, 100);
		progressBar.setValue(0);
		progressBar.setStringPainted(true);
		constraints.fill = GridBagConstraints.HORIZONTAL;
		constraints.ipady = 15;
		constraints.weightx = 0.0;
		constraints.anchor = GridBagConstraints.PAGE_END;
		constraints.insets = new Insets(10, 0, 0, 0); //top padding
		constraints.gridwidth = 3;
		constraints.gridx = 0;
		constraints.gridy = 1;
		panel.add(progressBar, constraints);
		
		JButton cancelButton = new JButton("Cancel");
		cancelButton.addActionListener(new ExitAction());
		constraints.fill = GridBagConstraints.HORIZONTAL;
		constraints.ipady = 0;
		constraints.weighty = 1.0;
		constraints.anchor = GridBagConstraints.PAGE_END;
		constraints.insets = new Insets(10, 0, 0, 0);
		constraints.gridx = 1;
		constraints.gridwidth = 2;
		constraints.gridy = 2;
		panel.add(cancelButton, constraints);
		
		pack();
		setContentPane(panel);
		setTitle("SheepIt Render Farm launcher");
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setSize(500, 180);
		
		setVisible(true);
	}
	
	public void stop() {
		dispose();
	}
	
	public void error(String string) {
		JOptionPane.showMessageDialog(panel, "<html>" + string + "</html>", "Warning: Advanced Users Only!", JOptionPane.WARNING_MESSAGE);
	}
	
	public void progress(int current, int total) {
		progressBar.setValue((int) (100.0 * (float) (current) / (float) (total)));
	}
	
	class ExitAction implements ActionListener {
		
		@Override
		public void actionPerformed(ActionEvent e) {
			System.exit(4);
		}
	}
}
