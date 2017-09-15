package com.jackdahms;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

import javax.swing.ImageIcon;
import javax.swing.JRadioButtonMenuItem;

@SuppressWarnings("serial")
public class ColorMenuItem extends JRadioButtonMenuItem {
	
	int color;
	int iconSize = 20;
	
	ImageIcon icon;
	
	public ColorMenuItem(String name) {
		super(name);
		setColor(0x000000); //default color black
	}
	
	public void setColor(int color) {
		this.color = color;
		BufferedImage b = new BufferedImage(iconSize, iconSize, BufferedImage.TYPE_INT_RGB);
		Graphics2D g = (Graphics2D) b.getGraphics();
		g.setColor(new Color(color));
		g.fillRect(0, 0, iconSize, iconSize);
		g.dispose();
		icon = new ImageIcon(b);
		this.setIcon(icon);
	}
	
	public int getColor() {
		return color;
	}

}
