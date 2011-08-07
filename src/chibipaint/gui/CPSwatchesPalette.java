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

import java.awt.*;
import java.awt.event.*;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import javax.jnlp.DownloadService;
import javax.jnlp.FileContents;
import javax.jnlp.FileSaveService;
import javax.jnlp.ServiceManager;
import javax.jnlp.UnavailableServiceException;
import javax.swing.*;

import chibipaint.*;
import chibipaint.engine.AdobeColorTable;
import chibipaint.util.*;

public class CPSwatchesPalette extends CPPalette implements ActionListener {

	int initColors[] = { 0xffffff, 0x000000, 0xff0000, 0x00ff00, 0x0000ff };

	public boolean modified = false;

	private CPScrollableFlowPanel swatchPanel;

	private JButton btnAdd, btnSettings;

	public CPSwatchesPalette(CPController controller) {
		super(controller);

		title = "Color Swatches";

		setLayout(new BorderLayout());

		swatchPanel = new CPScrollableFlowPanel(0, 0);
		{
			for (int color : initColors) {
				swatchPanel.add(new CPColorSwatch(color));
			}

			add(swatchPanel.wrapInScrollPane(), BorderLayout.CENTER);
		}

		JPanel buttonPanel = new JPanel();
		{
			btnAdd = new JButton("+");
			{
				btnAdd.setMargin(new Insets(0, 2, 0, 2));
				btnAdd.addActionListener(this);

				buttonPanel.add(btnAdd);
			}

			btnSettings = new JButton("O");
			{
				btnSettings.setMargin(new Insets(0, 2, 0, 2));
				btnSettings.addActionListener(this);

				buttonPanel.add(btnSettings);
			}

			add(buttonPanel, BorderLayout.EAST);
		}
	}

	@Override
	public Dimension getPreferredSize() {
		return new Dimension((int) (super.getPreferredSize() != null ? super.getPreferredSize().getWidth() : 200), 90);
	}

	public class CPColorSwatch extends JComponent implements MouseListener {

		CPColor color = null;

		public CPColorSwatch(int color) {
			addMouseListener(this);
			setColor(color);
		}

		public void paint(Graphics g) {
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
		}

		public void mouseEntered(MouseEvent e) {
		}

		public void mouseExited(MouseEvent e) {
		}

		public void mouseClicked(MouseEvent e) {
			if ((e.getModifiers() & InputEvent.BUTTON1_MASK) != 0 && color != null) {
				controller.setCurColor(color);
			} else if ((e.getModifiers() & InputEvent.BUTTON3_MASK) != 0) {
				JPopupMenu menu = new JPopupMenu();

				menu.add(new JMenuItem("Remove"));

				menu.show(this, e.getX(), e.getY());
			}

		}

		public void mouseReleased(MouseEvent e) {
		}
	}

	public void actionPerformed(ActionEvent e) {
		if (e.getSource() == btnAdd) {
			addSwatch(controller.getCurColor().rgb);
			swatchPanel.validate();
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
		}
	}

	public void clearSwatches() {
		swatchPanel.removeAll();
	}

	private void addSwatch(int color) {
		CPColorSwatch swatch = new CPColorSwatch(color);

		swatchPanel.add(swatch);
	}

	public int[] getSwatches() {
		int[] colours = new int[swatchPanel.getComponentCount()];

		for (int i = 0; i < swatchPanel.getComponentCount(); i++) {
			colours[i] = ((CPColorSwatch) swatchPanel.getComponent(i)).color.getRgb();
		}

		return colours;
	}

	public void setSwatches(int[] swatches) {
		swatchPanel.removeAll();
		
		for (int swatch : swatches) {
			addSwatch(swatch);
		}
		swatchPanel.validate();
	}
}
