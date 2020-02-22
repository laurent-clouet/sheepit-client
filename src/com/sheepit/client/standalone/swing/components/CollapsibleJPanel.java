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


import java.awt.Color;
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
	
	private boolean isComponentsVisible = true;
	private int originalHeight;
	private String borderTitle = "";
	private int COLLAPSED_HEIGHT = 22;
	private boolean[] originalVisibilty;

	private Color themeForegroundColor;
	
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
		if (isComponentsVisible) {
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
		// If background is black, then create the etched border in white colour (only required with black background)
		// Light mode works fine without specifying the highlight colour
		if (this.themeForegroundColor == Color.white) {
			// Set the shadow transparent by setting the alpha of black colour (new Color(r, g, b, alpha)
			setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(this.themeForegroundColor, new Color(0,0,0,0)), " + " + borderTitle), this.themeForegroundColor);
		} else {
			setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), " + " + borderTitle), this.themeForegroundColor);
		}

		// Update flag
		isComponentsVisible = false;
	}
	
	private void showComponents() {
		Component[] components = getComponents();
		
		// Set all components in panel to visible
		for (int i = 0; i < components.length; i++) {
			components[i].setVisible(originalVisibilty[i]);
		}
		
		setHeight(originalHeight);
		
		// Add '-' char to end of border title
		setBorder(BorderFactory.createTitledBorder(" - " + borderTitle), this.themeForegroundColor);

		// Update flag
		isComponentsVisible = true;
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
	
	public void setBorder(Border border, Color themeForegroundColor) { // Need this to get the border title
		// Update the class value to use it when showing/hiding components
		this.themeForegroundColor = themeForegroundColor;

		if (border instanceof TitledBorder) {
			if (borderTitle.equals("")) {
				borderTitle = ((TitledBorder) border).getTitle();

				((TitledBorder) border).setTitle(" - " + borderTitle);
			}

			((TitledBorder) border).setTitleColor(themeForegroundColor);
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
