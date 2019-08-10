/*
 * Copyright (C) 2015 Laurent CLOUET
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
package com.sheepit.client.standalone.swing.components;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.LayoutManager;
import java.awt.event.MouseListener;
import java.awt.event.MouseEvent;
import javax.swing.BorderFactory;

import javax.swing.JPanel;
import javax.swing.border.Border;
import javax.swing.border.TitledBorder;

public class CollapsibleJPanel extends JPanel {
	
	private boolean isCompnentsVisible = true;
	private int originalHeight;
	private String borderTitle = "";
	private int COLLAPSED_HEIGHT = 22;
	private boolean[] originalVisibilty;
	
	public CollapsibleJPanel(LayoutManager layoutManager) {
		setLayout(layoutManager);
		addMouseListener(new onClickHandler());
	}
	
	public void setCollapsed(boolean aFlag) {
		if (aFlag) {
			hideComponents();
		}
		else {
			showComponents();
		}
	}
	
	public void toggleCollapsed() {
		if (isCompnentsVisible) {
			setCollapsed(true);
		}
		else {
			setCollapsed(false);
		}
	}
	
	private void hideComponents() {
		
		Component[] components = getComponents();
		
		originalVisibilty = new boolean[components.length];
		
		// Hide all componens on panel
		for (int i = 0; i < components.length; i++) {
			originalVisibilty[i] = components[i].isVisible();
			components[i].setVisible(false);
		}
		
		setHeight(COLLAPSED_HEIGHT);
		
		// Add '+' char to end of border title
		//setBorder(BorderFactory.createTitledBorder(BorderFactory.createEmptyBorder(), borderTitle + " + "));
		setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), " + " + borderTitle));
		
		// Update flag
		isCompnentsVisible = false;
	}
	
	private void showComponents() {
		
		Component[] components = getComponents();
		
		// Set all components in panel to visible
		for (int i = 0; i < components.length; i++) {
			components[i].setVisible(originalVisibilty[i]);
		}
		
		setHeight(originalHeight);
		
		// Add '-' char to end of border title
		setBorder(BorderFactory.createTitledBorder(" - " + borderTitle));
		
		// Update flag
		isCompnentsVisible = true;
	}
	
	private void setHeight(int height) {
		setPreferredSize(new Dimension(getPreferredSize().width, height));
		setMinimumSize(new Dimension(getMinimumSize().width, height));
		setMaximumSize(new Dimension(getMaximumSize().width, height));
	}
	
	@Override
	public Component add(Component component) { // Need this to get the original height of panel
		
		Component returnComponent = super.add(component);
		
		originalHeight = getPreferredSize().height;
		
		return returnComponent;
	}
	
	@Override
	public void setBorder(Border border) { // Need this to get the border title
		
		if (border instanceof TitledBorder && (borderTitle == "")) {
			borderTitle = ((TitledBorder) border).getTitle();
			
			((TitledBorder) border).setTitle(" - " + borderTitle);
		}
		
		super.setBorder(border);
	}
	
	public class onClickHandler implements MouseListener {
		
		@Override
		public void mouseClicked(MouseEvent e) {
		}
		
		@Override
		public void mousePressed(MouseEvent e) {
			if (e.getPoint().y < COLLAPSED_HEIGHT) { // Only if click is on top of panel
				((CollapsibleJPanel) e.getComponent()).toggleCollapsed();
			}
		}
		
		@Override
		public void mouseEntered(MouseEvent e) {
		}
		
		@Override
		public void mouseExited(MouseEvent e) {
		}
		
		@Override
		public void mouseReleased(MouseEvent e) {
		}
		
	}
}
