/*
	ChibiPaint
    Copyright (c) 2006-2008 Marc Schefer

    This file is part of ChibiPaint.

    ChibiPaint is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    ChibiPaint is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with ChibiPaint. If not, see <http://www.gnu.org/licenses/>.

 */

package chibipaint.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.jnlp.*;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;

import chibipaint.CPController;
import chibipaint.engine.AdobeColorTable;
import chibipaint.util.CPColor;

public class CPSwatchesPalette extends CPPalette implements ActionListener {

	private int initColors[] = { 0xffffff, 0x000000, 0xff0000, 0x00ff00, 0x0000ff, 0xffff00 };

	private boolean modified = false;

	private JPanel swatchPanel;

	private CPIconButton btnSettings, btnAdd;

	public CPSwatchesPalette(CPController controller) {
		super(controller);

		title = "Color Swatches";

		setLayout(new BorderLayout());

		swatchPanel = new JPanel();
		swatchPanel.setLayout(new WrapLayout(FlowLayout.LEFT, 0, 0));
		{
			for (int color : initColors) {
				swatchPanel.add(new CPColorSwatch(color));
			}

			add(new JScrollPane(swatchPanel, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
					JScrollPane.HORIZONTAL_SCROLLBAR_NEVER), BorderLayout.CENTER);
		}

		Image icons = controller.loadImage("smallicons.png");

		JPanel buttonPanel = new JPanel();
		{
			buttonPanel.setPreferredSize(new Dimension(19, 50));

			try {
				ServiceManager.lookup("javax.jnlp.FileSaveService");

				//Only show the load/save button if we have JNLP support enabled
				btnSettings = new CPIconButton(icons, 16, 16, 2, 1);
				{
					btnSettings.addCPActionListener(this);

					buttonPanel.add(btnSettings);
				}

			} catch (UnavailableServiceException e) {
			}

			btnAdd = new CPIconButton(icons, 16, 16, 0, 1);
			{
				btnAdd.addCPActionListener(this);

				buttonPanel.add(btnAdd);
			}

			add(buttonPanel, BorderLayout.EAST);
		}
	}

	@Override
	public Dimension getPreferredSize() {
		return new Dimension(160, 65);
	}

	public class CPColorSwatch extends JComponent implements MouseListener {

		CPColor color = null;

		public CPColorSwatch(int color) {
			addMouseListener(this);
			setColor(color);
			setOpaque(true);
		}

		public void paintComponent(Graphics g) {
			Dimension d = getSize();
			if (color != null) {
				g.setColor(new Color(color.getRgb()));
				g.fillRect(0, 0, d.width - 1, d.height - 1);
			} else {
				g.setColor(Color.lightGray);
				g.fillRect(0, 0, d.width - 1, d.height - 1);

				g.setColor(Color.black);
				g.drawLine(0, 0, d.width - 2, d.height - 2);
				g.drawLine(d.width - 2, 0, 0, d.height - 2);
			}
			g.setColor(Color.black);
			g.drawRect(0, 0, d.width - 2, d.height - 2);
		}

		private void setColor(int color) {
			this.color = new CPColor(color);
			repaint();
		}

		@Override
		public Dimension getPreferredSize() {
			return new Dimension(20, 20);
		}

		@Override
		public Dimension getMaximumSize() {
			return new Dimension(20, 20);
		}

		@Override
		public Dimension getMinimumSize() {
			return new Dimension(20, 20);
		}

		public void mousePressed(MouseEvent e) {
			if ((e.getModifiers() & InputEvent.BUTTON1_MASK) != 0 && color != null) {
				controller.setCurColor(color);
			} else if ((e.getModifiers() & InputEvent.BUTTON3_MASK) != 0) {
				JPopupMenu menu = new JPopupMenu();

				JMenuItem mnuRemove = new JMenuItem("Remove");
				mnuRemove.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent e) {
						swatchPanel.remove(CPColorSwatch.this);

						swatchPanel.revalidate();
						swatchPanel.repaint();
						
						CPSwatchesPalette.this.modified = true;
					}
				});
				menu.add(mnuRemove);

				JMenuItem mnuSetToCurrent = new JMenuItem("Replace with current color");
				mnuSetToCurrent.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent e) {
						CPColorSwatch.this.setColor(controller.getCurColor().getRgb());
						
						CPSwatchesPalette.this.modified = true;
					}
				});
				menu.add(mnuSetToCurrent);

				menu.show(this, e.getX(), e.getY());
			}
		}

		public void mouseEntered(MouseEvent e) {
		}

		public void mouseExited(MouseEvent e) {
		}

		public void mouseClicked(MouseEvent e) {
		}

		public void mouseReleased(MouseEvent e) {
		}
	}

	public void actionPerformed(ActionEvent e) {
		if (e.getSource() == btnAdd) {
			addSwatch(controller.getCurColor().getRgb());
			modified = true;			
		} else if (e.getSource() == btnSettings) {
			JPopupMenu menu = new JPopupMenu();

			JMenuItem mnuSave = new JMenuItem("Save swatches to your computer...");
			mnuSave.addActionListener(this);
			mnuSave.setActionCommand("save");
			menu.add(mnuSave);

			JMenuItem mnuLoad = new JMenuItem("Load swatches from your computer...");
			mnuLoad.addActionListener(this);
			mnuLoad.setActionCommand("load");
			menu.add(mnuLoad);

			menu.show(btnSettings, btnSettings.getWidth(), 0);
		} else if (e.getActionCommand().equals("save")) {
			try {
				ByteArrayOutputStream buffer = new ByteArrayOutputStream();

				AdobeColorTable.write(buffer, getSwatches());

				try {
					FileSaveService saveService = (FileSaveService) ServiceManager.lookup("javax.jnlp.FileSaveService");
					saveService.saveFileDialog(null, new String[] { "aco" },
							new ByteArrayInputStream(buffer.toByteArray()), "oekakiswatches.aco");
				} catch (UnavailableServiceException ex) {
					JFileChooser fileChooser = new JFileChooser();

					if (fileChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
						FileOutputStream outputStream = new FileOutputStream(fileChooser.getSelectedFile());
						try {
							outputStream.write(buffer.toByteArray());
						} finally {
							outputStream.close();
						}
					}
				}
			} catch (IOException ex) {
				JOptionPane.showMessageDialog(this, "The swatches could not be saved.");
			}
		} else if (e.getActionCommand().equals("load")) {

			InputStream in;
			try {
				try {
					FileOpenService openService = (FileOpenService) ServiceManager.lookup("javax.jnlp.FileOpenService");
					FileContents file = openService.openFileDialog(null, new String[] { "aco" });

					if (file != null) {
						in = file.getInputStream();
					} else {
						return;
					}
				} catch (UnavailableServiceException ex) {
					JFileChooser fileChooser = new JFileChooser();
					if (fileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
						in = new FileInputStream(fileChooser.getSelectedFile());
					} else {
						return;
					}
				}
			} catch (IOException exx) {
				JOptionPane.showMessageDialog(this, "The swatches could not be read.");
				return;
			}

			int[] swatches = AdobeColorTable.read(in);

			if (swatches != null && swatches.length > 0) {
				setSwatches(swatches);
			} else
				JOptionPane.showMessageDialog(this, "The swatches could not be read.");

		}
	}

	public void clearSwatches() {
		swatchPanel.removeAll();
		swatchPanel.revalidate();
	}

	private void addSwatch(int color) {
		CPColorSwatch swatch = new CPColorSwatch(color);

		swatchPanel.add(swatch);

		swatch.revalidate();
	}
	
	public boolean getResizable() {
		return true;
	}

	public int[] getSwatches() {
		int[] colours = new int[swatchPanel.getComponentCount()];

		for (int i = 0; i < swatchPanel.getComponentCount(); i++) {
			colours[i] = ((CPColorSwatch) swatchPanel.getComponent(i)).color.getRgb();
		}

		return colours;
	}

	public void setSwatches(int[] swatches) {
		clearSwatches();

		for (int swatch : swatches) {
			addSwatch(swatch);
		}
		swatchPanel.revalidate();
		
		modified = true;
	}
	
	public boolean isModified() {
		return modified;
	}
}
