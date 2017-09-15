package com.jackdahms;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JColorChooser;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JSpinner;
import javax.swing.KeyStroke;
import javax.swing.SpinnerModel;
import javax.swing.SpinnerNumberModel;
import javax.swing.SpringLayout;
import javax.swing.UIManager;
import javax.swing.filechooser.FileNameExtensionFilter;

@SuppressWarnings("serial")
public class Planner extends JPanel {
	
	static int FRAME_WIDTH = 800;
	static int FRAME_HEIGHT = 500;
	
	static JFrame window;
	
	JFileChooser chooser = new JFileChooser();
	
	List<ArrayList<Integer>> rows;
	List<ArrayList<Integer>> clipboard;
	
	String fileExtension = ".csp";
	String filename;
	
	boolean drawMajor = false;
	boolean drawMinor = false;
	
	int major;
	int minor;
	
	int xoffset = 0;
	int yoffset = 0;
	
	boolean selected = false;
	int selectx1 = 0;
	int selecty1 = 0;
	int selectx2 = 0;
	int selecty2 = 0;
	
	int zoomFactor = 2;
	int scale = 1;
	
	int color = 0x000000;
	
	boolean open = false;
	
	enum Tool {
		CURSOR,
		PENCIL,
		ERASER,
		SELECT
	}
	
	Tool tool;
	
	enum Direction {
		RIGHT,
		TOP,
		LEFT,
		BOTTOM
	}
	
	Direction dir;
	
	public Planner() {
		//restrict file chooser to .csp only
		chooser.setFileFilter(new FileNameExtensionFilter("Cross-stitch Plan File", "csp"));
	
		MouseManager mm = new MouseManager();
		this.addMouseListener(mm);
		this.addMouseMotionListener(mm);
		this.addMouseWheelListener(mm);
	}
	
	public void paintComponent(Graphics graphics) {
		Graphics2D g = (Graphics2D) graphics;
		if (open) {
			int width = rows.get(0).size();
			int height = rows.size();
			BufferedImage canvas = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
			for (int y = 0; y < height; y++) {
				for (int x = 0; x < width; x++) {
					Integer color = rows.get(y).get(x);
					if (color == null) {
						canvas.setRGB(x, y, 0xffffff); //white == background color
					} else {
						canvas.setRGB(x, y, color);
					}
				}
			}
			g.drawImage(canvas, xoffset, yoffset, width * scale, height * scale, null);
			
			/*Draw minor gridlines*/
			if (drawMinor && scale >= 8) {
				g.setColor(Color.LIGHT_GRAY);
				//horizontal lines
				int count = height / minor + 1;
				for (int i = 0; i < count; i++) {
					int y = yoffset + minor * i * scale;
					g.drawLine(xoffset, y, xoffset + width * scale, y);
				}
				//vertical lines
				count = width / minor + 1;
				for (int i = 0; i < count; i++) {
					int x = xoffset + minor * i * scale;
					g.drawLine(x, yoffset, x, yoffset + height * scale);
				}
			}
			
			/*Draw major gridlines*/
			if (drawMajor) {
				g.setColor(Color.black);
				if (scale >= 8) {
					g.setStroke(new BasicStroke(2));				
				}
				//horizontal lines
				int count = height / major + 1;
				for (int i = 0; i < count; i++) {
					int y = yoffset + major * i * scale;
					g.drawLine(xoffset, y, xoffset + width * scale, y);
				}
				//vertical lines
				count = width / major + 1;
				for (int i = 0; i < count; i++) {
					int x = xoffset + major * i * scale;
					g.drawLine(x, yoffset, x, yoffset + height * scale);
				}
			}
			
			if (selected) {
				int x1;
				int x2;
				int y1;
				int y2;
				if (selectx1 <= selectx2) {
					x1 = xoffset + selectx1 * scale;
					x2 = xoffset + selectx2 * scale + minor * scale;	
				} else {
					x1 = xoffset + selectx2 * scale;
					x2 = xoffset + selectx1 * scale + minor * scale;	
				}
				if (selecty1 <= selecty2) {
					y1 = yoffset + selecty1 * scale;
					y2 = yoffset + selecty2 * scale + minor * scale;					
				} else {
					y1 = yoffset + selecty2 * scale;
					y2 = yoffset + selecty1 * scale + minor * scale;
				}
				int w = x2 - x1;
				int h = y2 - y1;
				g.setColor(new Color(0, 255, 0, 80));
				g.fillRect(x1, y1, w, h);
				g.setColor(Color.GREEN);
				g.drawRect(x1, y1, w, h);
			}
		}
	}
	
	public void addCells(Direction dir) {
		if (open) {
			JFrame f = new JFrame("Add Cells");
			f.setResizable(false);
			f.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
			
			Container content = f.getContentPane();
			SpringLayout layout = new SpringLayout();
			content.setLayout(layout);
			
			JLabel dirLabel;
			if (dir == Direction.TOP) {
				dirLabel = new JLabel("Add rows to top:");
			} else if (dir == Direction.RIGHT) {
				dirLabel = new JLabel("Add columns to right:");
			} else if (dir == Direction.LEFT) {
				dirLabel = new JLabel("Add columns to left:");
			} else { //dir == Direciton.BOTTOM
				dirLabel = new JLabel("Add rows to bottom:");
			}
			content.add(dirLabel);

			SpinnerModel model = new SpinnerNumberModel(1, 1, Integer.MAX_VALUE, 1); //init, min, max, step
			JSpinner cellSpinner = new JSpinner(model);
			cellSpinner.setPreferredSize(new Dimension(100, 26));
			content.add(cellSpinner);
			
			JButton add = new JButton("Add");
			add.addActionListener(e -> {
				for (int i = 0; i < (int) cellSpinner.getValue(); i++) {
					if (dir == Direction.TOP) {
						ArrayList<Integer> row = new ArrayList<Integer>();
						for (int k = 0; k < rows.get(0).size(); k++) {
							row.add(null);
						}
						rows.add(0, row);
					} else if (dir == Direction.RIGHT) {
						for (int k = 0; k < rows.size(); k++) {
							rows.get(k).add(null);
						}
					} else if (dir == Direction.LEFT) {
						for (int k = 0; k < rows.size(); k++) {
							rows.get(k).add(0, null);
						}
					} else { //dir == Direciton.BOTTOM
						ArrayList<Integer> row = new ArrayList<Integer>();
						for (int k = 0; k < rows.get(0).size(); k++) {
							row.add(null);
						}
						rows.add(rows.size() - 1, row);
					}
				}
				f.dispose();
			});
			JButton cancel = new JButton("Cancel");
			cancel.addActionListener(e -> f.dispose());
			content.add(add);
			content.add(cancel);
			
			layout.putConstraint(SpringLayout.NORTH, dirLabel, 8, SpringLayout.NORTH, content);
			layout.putConstraint(SpringLayout.NORTH, cellSpinner, 5, SpringLayout.NORTH, content);
			layout.putConstraint(SpringLayout.NORTH, add, 0, SpringLayout.SOUTH, cellSpinner);
			layout.putConstraint(SpringLayout.NORTH, cancel, 0, SpringLayout.SOUTH, cellSpinner);
			layout.putConstraint(SpringLayout.SOUTH, content, 2, SpringLayout.SOUTH, add);
			
			layout.putConstraint(SpringLayout.WEST, dirLabel, 5, SpringLayout.WEST, content);
			layout.putConstraint(SpringLayout.WEST, cellSpinner, 5, SpringLayout.EAST, dirLabel);
			layout.putConstraint(SpringLayout.EAST, content, 5, SpringLayout.EAST, cellSpinner);
			
			layout.putConstraint(SpringLayout.EAST, add, -2, SpringLayout.HORIZONTAL_CENTER, content);
			layout.putConstraint(SpringLayout.WEST, cancel, 2, SpringLayout.HORIZONTAL_CENTER, content);
			
			f.pack();
			f.setLocationRelativeTo(null);
			f.setVisible(true);
		}
	}
	
	public void removeCells(Direction dir) {
		if (open) {
			JFrame f = new JFrame("Remove Cells");
			f.setResizable(false);
			f.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
			
			Container content = f.getContentPane();
			SpringLayout layout = new SpringLayout();
			content.setLayout(layout);
			
			JLabel dirLabel;
			if (dir == Direction.TOP) {
				dirLabel = new JLabel("Remove rows from top:");
			} else if (dir == Direction.RIGHT) {
				dirLabel = new JLabel("Remove columns from right:");
			} else if (dir == Direction.LEFT) {
				dirLabel = new JLabel("Remove columns from left:");
			} else { //dir == Direciton.BOTTOM
				dirLabel = new JLabel("Remove rows from bottom:");
			}
			content.add(dirLabel);

			SpinnerModel model;
			if (dir == Direction.TOP || dir == Direction.BOTTOM) {
				model = new SpinnerNumberModel(1, 1, rows.size() - 1, 1);
			} else { //dir == Direction.RIGHT || dir == Direction.LEFT
				model = new SpinnerNumberModel(1, 1, rows.get(0).size() - 1, 1);
			}
			JSpinner cellSpinner = new JSpinner(model);
			cellSpinner.setPreferredSize(new Dimension(100, 26));
			content.add(cellSpinner);
			
			JButton add = new JButton("Remove");
			add.addActionListener(e -> {
				for (int i = 0; i < (int) cellSpinner.getValue(); i++) {
					if (dir == Direction.TOP) {
						rows.remove(0);
					} else if (dir == Direction.RIGHT) {
						for (int k = 0; k < rows.size(); k++) {
							rows.get(k).remove(rows.get(k).size() - 1);
						}
					} else if (dir == Direction.LEFT) {
						for (int k = 0; k < rows.size(); k++) {
							rows.get(k).remove(0);
						}
					} else { //dir == Direciton.BOTTOM
						rows.remove(rows.size() - 1);
					}
				}
				f.dispose();
			});
			
			JButton cancel = new JButton("Cancel");
			cancel.addActionListener(e -> f.dispose());
			content.add(add);
			content.add(cancel);
			
			layout.putConstraint(SpringLayout.NORTH, dirLabel, 8, SpringLayout.NORTH, content);
			layout.putConstraint(SpringLayout.NORTH, cellSpinner, 5, SpringLayout.NORTH, content);
			layout.putConstraint(SpringLayout.NORTH, add, 0, SpringLayout.SOUTH, cellSpinner);
			layout.putConstraint(SpringLayout.NORTH, cancel, 0, SpringLayout.SOUTH, cellSpinner);
			layout.putConstraint(SpringLayout.SOUTH, content, 2, SpringLayout.SOUTH, add);
			
			layout.putConstraint(SpringLayout.WEST, dirLabel, 5, SpringLayout.WEST, content);
			layout.putConstraint(SpringLayout.WEST, cellSpinner, 5, SpringLayout.EAST, dirLabel);
			layout.putConstraint(SpringLayout.EAST, content, 5, SpringLayout.EAST, cellSpinner);
			
			layout.putConstraint(SpringLayout.EAST, add, -2, SpringLayout.HORIZONTAL_CENTER, content);
			layout.putConstraint(SpringLayout.WEST, cancel, 2, SpringLayout.HORIZONTAL_CENTER, content);
			
			f.pack();
			f.setLocationRelativeTo(null);
			f.setVisible(true);
		}
	}
	
	/**
	 * Removes cells from bottom and right sides of canvas
	 * until width and height are evenly divisible by the major gridlines.
	 */
	public void trimMajor() {
		/*Trim rows from bottom*/
		int remainder = rows.size() % major;
		for (int i = 0; i < remainder; i++) {
			rows.remove(rows.size() - 1);
		}
		
		/*Trim columns from right*/
		remainder = rows.get(0).size() % major;
		for (int i = 0; i < remainder; i++) {
			for (int k = 0; k < rows.size(); k++) {
				rows.get(k).remove(rows.get(k).size() - 1);
			}
		}
	}
	
	public void resetView() {
		scale = 2;
		xoffset = this.getWidth() / 2 - (rows.size() / 2) * scale;
		yoffset = this.getHeight() / 2 - (rows.get(0).size() / 2) * scale;
	}
	
	public void setColor(ColorMenuItem c) {
		JFrame f = new JFrame("Set Color");
		f.setResizable(false);
		f.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		
		Container content = f.getContentPane();
		SpringLayout layout = new SpringLayout();
		content.setLayout(layout);
		
		JColorChooser jcc = new JColorChooser();
		jcc.setColor(c.getColor());
		content.add(jcc);
		
		JButton add = new JButton("Set");
		add.addActionListener(e -> {
			c.setColor(jcc.getColor().getRGB());
			f.dispose();
		});
		JButton cancel = new JButton("Cancel");
		cancel.addActionListener(e -> f.dispose());
		content.add(add);
		content.add(cancel);
		
		layout.putConstraint(SpringLayout.NORTH, jcc, 5, SpringLayout.NORTH, content);
		layout.putConstraint(SpringLayout.NORTH, add, 5, SpringLayout.SOUTH, jcc);
		layout.putConstraint(SpringLayout.NORTH, cancel, 5, SpringLayout.SOUTH, jcc);
		layout.putConstraint(SpringLayout.SOUTH, content, 2, SpringLayout.SOUTH, add);
		
		layout.putConstraint(SpringLayout.WEST, jcc, 5, SpringLayout.WEST, content);
		layout.putConstraint(SpringLayout.EAST, content, 5, SpringLayout.EAST, jcc);
		
		layout.putConstraint(SpringLayout.EAST, add, -2, SpringLayout.HORIZONTAL_CENTER, content);
		layout.putConstraint(SpringLayout.WEST, cancel, 2, SpringLayout.HORIZONTAL_CENTER, content);
		
		f.pack();
		f.setLocationRelativeTo(null);
		f.setVisible(true);
	}
	
	/**
	 * Zoom in around (x, y)
	 * @param x
	 * @param y
	 */
	public void zoomIn(int x, int y) {
		int dx = x - xoffset;
		int dy = y - yoffset;
		xoffset += dx;
		yoffset += dy;
		dx *= zoomFactor;
		dy *= zoomFactor;
		xoffset -= dx;
		yoffset -= dy;  
		scale *= zoomFactor;
	}
	
	/**
	 * Zoom out around (x, y)
	 * @param x
	 * @param y
	 */
	public void zoomOut(int x, int y) {
		int dx = x - xoffset;
		int dy = y - yoffset;
		xoffset += dx;
		yoffset += dy;
		dx /= zoomFactor;
		dy /= zoomFactor;
		xoffset -= dx;
		yoffset -= dy;  
		scale /= zoomFactor;
	}
	
	public void cut() {
		int x1, x2;
		int y1, y2;
		if (selectx1 <= selectx2) {
			x1 = selectx1;
			x2 = selectx2;
		} else {
			x1 = selectx2;
			x2 = selectx1;
		}
		if (selecty1 <= selecty2) {
			y1 = selecty1;
			y2 = selecty2;
		} else {
			y1 = selecty2;
			y2 = selecty1;
		}
		clipboard = new ArrayList<ArrayList<Integer>>();
		for (int y = y1; y <= y2; y++) {
			ArrayList<Integer> row = new ArrayList<Integer>();
			for (int x = x1; x <= x2; x++) {
				row.add(rows.get(y).get(x));
				rows.get(y).set(x, null);
			}
			clipboard.add(row);
		}
	}
	
	public void copy() {
		int x1, x2;
		int y1, y2;
		if (selectx1 <= selectx2) {
			x1 = selectx1;
			x2 = selectx2;
		} else {
			x1 = selectx2;
			x2 = selectx1;
		}
		if (selecty1 <= selecty2) {
			y1 = selecty1;
			y2 = selecty2;
		} else {
			y1 = selecty2;
			y2 = selecty1;
		}
		clipboard = new ArrayList<ArrayList<Integer>>();
		for (int y = y1; y <= y2; y++) {
			ArrayList<Integer> row = new ArrayList<Integer>();
			for (int x = x1; x <= x2; x++) {
				row.add(rows.get(y).get(x));
			}
			clipboard.add(row);
		}
	}
	
	public void paste() {
		int x1 = selectx1 <= selectx2 ? selectx1 : selectx2;
		int y1 = selecty1 <= selecty2 ? selecty1 : selecty2;
		for (int y = 0; y < clipboard.size(); y++) {
			for (int x = 0; x < clipboard.get(y).size(); x++) {
				Integer value = clipboard.get(y).get(x);
				rows.get(y1 + y).set(x1 + x, value);
			}
		}
	}
	
	private void newFile() {
		JFrame f = new JFrame("New Cross-stitch Plan");
		f.setResizable(false);
		f.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		
		Container content = f.getContentPane();
		SpringLayout layout = new SpringLayout();
		content.setLayout(layout);
		
		JLabel rowLabel = new JLabel("Rows:");
		JLabel colLabel = new JLabel("Columns:");
		JLabel majorLabel = new JLabel("Major Gridline Increment:");
		JLabel minorLabel = new JLabel("Minor Gridline Increment:");
		
		int spinnerWidth = 100;
		int spinnerHeight = 26;
		SpinnerModel model = new SpinnerNumberModel(1, 1, Integer.MAX_VALUE, 1); //init, min, max, step
		JSpinner rowSpinner = new JSpinner(model);
		rowSpinner.setPreferredSize(new Dimension(spinnerWidth, spinnerHeight));
		model = new SpinnerNumberModel(1, 1, Integer.MAX_VALUE, 1);
		JSpinner colSpinner = new JSpinner(model);
		colSpinner.setPreferredSize(new Dimension(spinnerWidth, spinnerHeight));
		model = new SpinnerNumberModel(14, 0, Integer.MAX_VALUE, 1);
		JSpinner majorSpinner = new JSpinner(model);
		majorSpinner.setPreferredSize(new Dimension(spinnerWidth, spinnerHeight));
		model = new SpinnerNumberModel(1, 0, Integer.MAX_VALUE, 1);
		JSpinner minorSpinner = new JSpinner(model);
		minorSpinner.setPreferredSize(new Dimension(spinnerWidth, spinnerHeight));
		
		JButton create = new JButton("Save As");
		create.addActionListener(e -> {
			int r = (int) rowSpinner.getModel().getValue();
			int c = (int) colSpinner.getModel().getValue();
			major = (int) majorSpinner.getModel().getValue();
			minor = (int) minorSpinner.getModel().getValue();
			
			rows = new ArrayList<ArrayList<Integer>>();
			for (int i = 0; i < r; i++) {
				rows.add(new ArrayList<Integer>());
				for (int j = 0; j < c; j++) {
					rows.get(i).add(null);
				}
			}
			
			resetView();
			saveAs();
			open = true;
			
			f.dispose();
		});
		
		JButton cancel = new JButton("Cancel");
		cancel.addActionListener(e -> f.dispose());
		
		content.add(rowLabel);
		content.add(colLabel);
		content.add(majorLabel);
		content.add(minorLabel);
		
		content.add(rowSpinner);
		content.add(colSpinner);
		content.add(majorSpinner);
		content.add(minorSpinner);
		
		content.add(create);
		content.add(cancel);
		
		//horizontal constraints for labels
		int leftPad = 5;
		layout.putConstraint(SpringLayout.WEST, rowLabel, leftPad, SpringLayout.WEST, content);
		layout.putConstraint(SpringLayout.WEST, colLabel, leftPad, SpringLayout.WEST, content);
		layout.putConstraint(SpringLayout.WEST, majorLabel, leftPad, SpringLayout.WEST, content);
		layout.putConstraint(SpringLayout.WEST, minorLabel, leftPad, SpringLayout.WEST, content);
		
		//horizontal constraints for spinners
		int rightPad = 5;
		int horizontalSpacing = 5;
		layout.putConstraint(SpringLayout.WEST, rowSpinner, horizontalSpacing, SpringLayout.EAST, minorLabel);
		layout.putConstraint(SpringLayout.WEST, colSpinner, horizontalSpacing, SpringLayout.EAST, minorLabel);
		layout.putConstraint(SpringLayout.WEST, majorSpinner, horizontalSpacing, SpringLayout.EAST, minorLabel);
		layout.putConstraint(SpringLayout.WEST, minorSpinner, horizontalSpacing, SpringLayout.EAST, minorLabel);
		layout.putConstraint(SpringLayout.EAST, content, rightPad, SpringLayout.EAST, minorSpinner);
		
		//vertical constraints for labels
		int labelTopPad = 8;
		int labelSpacing = 11;
		layout.putConstraint(SpringLayout.NORTH, rowLabel, labelTopPad, SpringLayout.NORTH, content);
		layout.putConstraint(SpringLayout.NORTH, colLabel, labelSpacing, SpringLayout.SOUTH, rowLabel);
		layout.putConstraint(SpringLayout.NORTH, majorLabel, labelSpacing, SpringLayout.SOUTH, colLabel);
		layout.putConstraint(SpringLayout.NORTH, minorLabel, labelSpacing, SpringLayout.SOUTH, majorLabel);
		
		//vertical constraints for spinners
		int spinnerTopPad = 5;
		int spinnerSpacing = 0;
		layout.putConstraint(SpringLayout.NORTH, rowSpinner, spinnerTopPad, SpringLayout.NORTH, content);
		layout.putConstraint(SpringLayout.NORTH, colSpinner, spinnerSpacing, SpringLayout.SOUTH, rowSpinner);
		layout.putConstraint(SpringLayout.NORTH, majorSpinner, spinnerSpacing, SpringLayout.SOUTH, colSpinner);
		layout.putConstraint(SpringLayout.NORTH, minorSpinner, spinnerSpacing, SpringLayout.SOUTH, majorSpinner);	
		
		//button constraints
		layout.putConstraint(SpringLayout.NORTH, create, 6, SpringLayout.SOUTH, minorLabel);
		layout.putConstraint(SpringLayout.NORTH, cancel, 6, SpringLayout.SOUTH, minorLabel);
		layout.putConstraint(SpringLayout.SOUTH, content, 2, SpringLayout.SOUTH, create);
		layout.putConstraint(SpringLayout.EAST, create, -2, SpringLayout.HORIZONTAL_CENTER, content);
		layout.putConstraint(SpringLayout.WEST, cancel, 2, SpringLayout.HORIZONTAL_CENTER, content);
		
		f.pack();
		f.setLocationRelativeTo(null);
		f.setVisible(true);
	}
	
	private void open() {
		if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
			filename = chooser.getSelectedFile().getAbsolutePath();
			try (Scanner in = new Scanner(new File(filename))) {
				major = 14;
				minor = 1;
				
				rows = new ArrayList<ArrayList<Integer>>();
				int row = 0;
				while (in.hasNext()) {
					rows.add(new ArrayList<Integer>());
					String[] colors = in.next().split(",");
					for (String s : colors) {
						Integer i = null;
						if (!s.equals("null")) {
							i = Integer.parseInt(s);
						}
						rows.get(row).add(i);
					}
					row++;
				}
				
				resetView();
				open = true;
			} catch (Exception e) {
				System.err.println("Failed to open!");
				e.printStackTrace();
			}
		}
	}
	
	private void save() {
		try (FileWriter out = new FileWriter(filename)){
			for (ArrayList<Integer> r : rows) {
				for (Integer c : r) {
					if (c == null) {
						out.write("null,");
					} else {
						out.write(c + ",");
					}
				}
				out.write("\n");
			}
		} catch (Exception e) {
			System.err.println("Failed to save file!");
		}
	}
	
	private void saveAs() {
		if (chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
			filename = chooser.getSelectedFile().getAbsolutePath();
			boolean endsWithExtension = false;
			if (filename.length() > fileExtension.length()) {
				int beginIndex = filename.length() - fileExtension.length();
				if (filename.substring(beginIndex).equals(fileExtension)) {
					endsWithExtension = true;
				}
			}
			if (!endsWithExtension) {
				filename += fileExtension;
			}
			save();
		} 
	}
	
	/**
	 * TODO
	 * ability to set background color
	 * ability to set major/minor gridline increments
	 * save colors with file
	 * save major/minor increments with file
	 * accelerators for cut/copy/paste
	 */
	
	public static void main(String[] arg) {

		/*Attempt to set system look and feel*/
		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch (Exception e) {
			//will use default L&F in this case
			e.printStackTrace();
		} 
		
		Planner planner = new Planner();
		
		/*Create window*/
		JFrame frame = new JFrame("Cross-stitch Planner");
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setSize(FRAME_WIDTH, FRAME_HEIGHT);
		frame.setLocationRelativeTo(null);
		
		/*Create menu bar*/
		JMenuBar menuBar = new JMenuBar();
		
		/*File menu*/
		JMenu file = new JMenu("File");
		menuBar.add(file);
		JMenuItem newFile = new JMenuItem("New");
		newFile.addActionListener(e -> planner.newFile());
		file.add(newFile);
		JMenuItem open = new JMenuItem("Open");
		open.addActionListener(e -> planner.open());
		file.add(open);
		JMenuItem save = new JMenuItem("Save");
		save.addActionListener(e -> planner.save());
		file.add(save);
		JMenuItem saveAs = new JMenuItem("Save As");
		saveAs.addActionListener(e -> planner.saveAs());
		file.add(saveAs);
		
		/*Edit menu*/
		JMenu edit = new JMenu("Edit");
		JMenuItem trimMajor = new JMenuItem("Trim to Major Gridline");
		trimMajor.addActionListener(e -> planner.trimMajor());
		edit.add(trimMajor);
		edit.addSeparator();
		JMenuItem cut = new JMenuItem("Cut");
		cut.addActionListener(e -> planner.cut());
		edit.add(cut);
		JMenuItem copy = new JMenuItem("Copy");
		copy.addActionListener(e -> planner.copy());
		edit.add(copy);
		JMenuItem paste = new JMenuItem("Paste");
		paste.addActionListener(e -> planner.paste());
		edit.add(paste);
		edit.addSeparator();
		JMenuItem addRowsTop = new JMenuItem("Add Rows to Top");
		addRowsTop.addActionListener(e -> planner.addCells(Direction.TOP));
		edit.add(addRowsTop);
		JMenuItem addRowsBottom = new JMenuItem("Add Rows to Bottom");
		addRowsBottom.addActionListener(e -> planner.addCells(Direction.BOTTOM));
		edit.add(addRowsBottom);
		JMenuItem addColsRight = new JMenuItem("Add Columns to Right");
		addColsRight.addActionListener(e -> planner.addCells(Direction.RIGHT));
		edit.add(addColsRight);
		JMenuItem addColsLeft = new JMenuItem("Add Columns to Left");
		addColsLeft.addActionListener(e -> planner.addCells(Direction.LEFT));
		edit.add(addColsLeft);
		edit.addSeparator();
		JMenuItem removeRowsTop = new JMenuItem("Remove Rows from Top");
		removeRowsTop.addActionListener(e -> planner.removeCells(Direction.TOP));
		edit.add(removeRowsTop);
		JMenuItem removeRowsBottom = new JMenuItem("Remove Rows from Bottom");
		removeRowsBottom.addActionListener(e -> planner.removeCells(Direction.BOTTOM));
		edit.add(removeRowsBottom);
		JMenuItem removeColsRight = new JMenuItem("Remove Columns from Right");
		removeColsRight.addActionListener(e -> planner.removeCells(Direction.RIGHT));
		edit.add(removeColsRight);
		JMenuItem removeColsLeft = new JMenuItem("Remove Columns from Left");
		removeColsLeft.addActionListener(e -> planner.removeCells(Direction.LEFT));
		edit.add(removeColsLeft);
		menuBar.add(edit);
		
		/*View menu*/
		JMenu view = new JMenu("View");
		JMenuItem reset = new JMenuItem("Center and Reset Scale");
		reset.addActionListener(e -> planner.resetView());
		view.add(reset);
		view.addSeparator();
		JMenuItem zoomIn = new JMenuItem("Zoom In");
		zoomIn.addActionListener(e -> planner.zoomIn(planner.getWidth() / 2, planner.getHeight() / 2));
		view.add(zoomIn);
		JMenuItem zoomOut = new JMenuItem("Zoom Out");
		zoomOut.addActionListener(e -> planner.zoomOut(planner.getWidth() / 2, planner.getHeight() / 2));
		view.add(zoomOut);
		view.addSeparator();
		JCheckBoxMenuItem enableMajor = new JCheckBoxMenuItem("Show Major Gridlines");
		enableMajor.addChangeListener(e -> planner.drawMajor = !planner.drawMajor);
		enableMajor.setSelected(true);
		view.add(enableMajor);
		JCheckBoxMenuItem enableMinor = new JCheckBoxMenuItem("Show Minor Gridlines");
		enableMinor.addChangeListener(e -> planner.drawMinor = !planner.drawMinor);
		enableMinor.setSelected(true);
		view.add(enableMinor);
		menuBar.add(view);
		
		/*Tool menu*/
		JMenu tool = new JMenu("Tool");
		ButtonGroup toolButtons = new ButtonGroup();
		JRadioButtonMenuItem cursor = new JRadioButtonMenuItem("Cursor");
		cursor.addActionListener(e -> planner.tool = Tool.CURSOR);
		cursor.setAccelerator(KeyStroke.getKeyStroke('q'));
		toolButtons.add(cursor);
		tool.add(cursor);
		JRadioButtonMenuItem pencil = new JRadioButtonMenuItem("Pencil");
		pencil.addActionListener(e -> planner.tool = Tool.PENCIL);
		pencil.setAccelerator(KeyStroke.getKeyStroke('w'));
		toolButtons.add(pencil);
		tool.add(pencil);
		JRadioButtonMenuItem eraser = new JRadioButtonMenuItem("Eraser");
		eraser.addActionListener(e -> planner.tool = Tool.ERASER);
		eraser.setAccelerator(KeyStroke.getKeyStroke('e'));
		toolButtons.add(eraser);
		tool.add(eraser);
		JRadioButtonMenuItem select = new JRadioButtonMenuItem("Select");
		select.addActionListener(e -> planner.tool = Tool.SELECT);
		select.setAccelerator(KeyStroke.getKeyStroke('r'));
		toolButtons.add(select);
		tool.add(select);
		tool.addSeparator();
		int colorCount = 5;
		ButtonGroup colorGroup = new ButtonGroup();
		ColorMenuItem[] colors = new ColorMenuItem[colorCount];
		for (int i = 0; i < colorCount; i++) {
			ColorMenuItem c = new ColorMenuItem("Color " + (i + 1));
			c.addActionListener(e -> planner.color = c.getColor());
			c.setAccelerator(KeyStroke.getKeyStroke(Character.forDigit(i + 1, 10)));
			colorGroup.add(c);
			tool.add(c);
			colors[i] = c;
		}
		colors[0].setSelected(true);
		//Set color submenu
		JMenu setColorSubmenu = new JMenu("Set Colors");
		for (int i = 0; i < colorCount; i++) {
			JMenuItem setColor = new JMenuItem("Set Color " + (i + 1));
			ColorMenuItem c = colors[i];
			setColor.addActionListener(e -> planner.setColor(c));
			setColorSubmenu.add(setColor);
		}
		tool.add(setColorSubmenu);
		menuBar.add(tool);
		
		//make cursor default tool
		cursor.setSelected(true);
		cursor.doClick();
		
		Container content = frame.getContentPane();
		SpringLayout layout = new SpringLayout();
		content.setLayout(layout);
		
		content.setLayout(new BorderLayout());
		content.add(menuBar, BorderLayout.NORTH);
		content.add(planner, BorderLayout.CENTER);
		
		new Thread(() -> {
			while (true) {
				planner.repaint();
			}
		}).start();
		
		frame.setVisible(true);
	}
	
	private class MouseManager extends MouseAdapter {
		
		int lastx;
		int lasty;

		public void mark(MouseEvent e) {
			int x = (e.getX() - xoffset) / scale;
			int y = (e.getY() - yoffset) / scale;
			if (x < rows.get(0).size() && x > -1 && y < rows.size() && y > -1) {
				if (tool == Tool.PENCIL) {
					rows.get(y).set(x, color);
				} else if (tool == Tool.ERASER) {
					rows.get(y).set(x, null);
				}
			}
			
		}
		
		public void mousePressed(MouseEvent e) {
			if (tool == Tool.CURSOR) {
				lastx = e.getX();
				lasty = e.getY();
			} else if (tool == Tool.SELECT) {
				selectx1 = (e.getX() - xoffset) / scale;
				selecty1 = (e.getY() - yoffset) / scale;
				if (selectx1 < 0) selectx1 = 0;
				if (selecty1 < 0) selecty1 = 0;
				if (selectx1 >= rows.get(0).size()) selectx1 = rows.get(0).size() - 1;
				if (selecty1 >= rows.size()) selecty1 = rows.size() - 1;
				selectx2 = selectx1;
				selecty2 = selecty1;
				selected = true;
			} else {
				mark(e);
			}
		}
		
		public void mouseClicked(MouseEvent e) {
			if (tool == Tool.SELECT) {
				selected = false;
			}
		}
		
		public void mouseDragged(MouseEvent e) {
			if (tool == Tool.CURSOR) {
				int x = e.getX();
				int y = e.getY();
				xoffset += x - lastx;
				yoffset += y - lasty;
				lastx = x;
				lasty = y;
			} else if (tool == Tool.SELECT) {
				selectx2 = (e.getX() - xoffset) / scale;
				selecty2 = (e.getY() - yoffset) / scale;
				if (selectx2 < 0) selectx2 = 0;
				if (selecty2 < 0) selecty2 = 0;
				if (selectx2 >= rows.get(0).size()) selectx2 = rows.get(0).size() - 1;
				if (selecty2 >= rows.size()) selecty2 = rows.size() - 1;
			} else {
				mark(e);
			}
		}
		
		public void mouseWheelMoved(MouseWheelEvent e) {
			if (tool == Tool.CURSOR) { //prevents accidental scrolling while drawing
				int scroll = e.getWheelRotation();
				if (scroll > 0 && scale < 16) {
					zoomIn(e.getX(), e.getY());
				} else if (scroll < 0 && scale > 1){
					zoomOut(e.getX(), e.getY());
				}
			}
		}
		
	}
	
}
