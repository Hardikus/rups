/*
 * $Id$
 *
 * Copyright 2007 Bruno Lowagie.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
 */

package com.itextpdf.rups.controller;

import com.itextpdf.rups.model.ObjectLoader;
import com.itextpdf.rups.model.PdfFile;
import com.itextpdf.rups.model.TreeNodeFactory;
import com.itextpdf.rups.view.PageSelectionListener;
import com.itextpdf.rups.view.RupsMenuBar;
import com.itextpdf.rups.view.contextmenu.PdfTreeContextMenu;
import com.itextpdf.rups.view.contextmenu.PdfTreeContextMenuMouseListener;
import com.itextpdf.rups.view.itext.FormTree;
import com.itextpdf.rups.view.itext.OutlineTree;
import com.itextpdf.rups.view.itext.PagesTable;
import com.itextpdf.rups.view.itext.PdfObjectPanel;
import com.itextpdf.rups.view.itext.PdfTree;
import com.itextpdf.rups.view.itext.StructureTree;
import com.itextpdf.rups.view.itext.SyntaxHighlightedStreamPane;
import com.itextpdf.rups.view.itext.XRefTable;
import com.itextpdf.rups.view.itext.treenodes.PdfObjectTreeNode;
import com.itextpdf.rups.view.itext.treenodes.PdfTrailerTreeNode;
import com.itextpdf.text.pdf.PRStream;
import com.itextpdf.text.pdf.PdfObject;

import javax.swing.*;
import javax.swing.event.TreeSelectionListener;
import java.util.Observable;
import java.util.Observer;

/**
 * Controls the GUI components that get their content from iText's PdfReader.
 */
public class PdfReaderController extends Observable implements Observer {

	/** Treeview of the PDF file. */
	protected PdfTree pdfTree;
	/** Tabbed Pane containing other components. */
	protected JTabbedPane navigationTabs;
	/** JTable with all the pages and their labels. */
	protected PagesTable pages;
	/** Treeview of the outlines. */
	protected OutlineTree outlines;
	/** Treeview of the structure. */
	protected StructureTree structure;
	/** Treeview of the form. */
	protected FormTree form;
	/** JTable corresponding with the CrossReference table. */
	protected XRefTable xref;
	/** A panel that will show PdfObjects. */
	protected PdfObjectPanel objectPanel;
	/** Tabbed Pane containing other components. */
	protected JTabbedPane editorTabs;
	/** A panel that will show a stream. */
	protected SyntaxHighlightedStreamPane streamPane;

	/** The factory producing tree nodes. */
	protected TreeNodeFactory nodes;

	/**
	 * Constructs the PdfReaderController.
	 * This is an Observable object to which all iText related GUI components
	 * are added as Observers.
	 * @param treeSelectionListener	when somebody selects a tree node, this listener listens to the event
	 * @param pageSelectionListener	when somebody changes a page, this listener changes accordingly
	 */
	public PdfReaderController(TreeSelectionListener treeSelectionListener,
			PageSelectionListener pageSelectionListener) {
		pdfTree = new PdfTree();

		pdfTree.addTreeSelectionListener(treeSelectionListener);
        JPopupMenu menu = PdfTreeContextMenu.getPopupMenu(pdfTree);
        pdfTree.add(menu);
        pdfTree.addMouseListener(new PdfTreeContextMenuMouseListener(menu, pdfTree));
		addObserver(pdfTree);

		pages = new PagesTable(this, pageSelectionListener);
		addObserver(pages);
		outlines = new OutlineTree(this);
		addObserver(outlines);
		structure = new StructureTree(this);
		addObserver(structure);
		form = new FormTree(this);
		addObserver(form);
		xref = new XRefTable(this);
		addObserver(xref);
        
		navigationTabs = new JTabbedPane();
		navigationTabs.addTab("Pages", null, new JScrollPane(pages), "Pages");
		navigationTabs.addTab("Outlines", null, new JScrollPane(outlines), "Outlines (Bookmarks)");
		navigationTabs.addTab("Structure", null, new JScrollPane(structure), "Structure tree");
		navigationTabs.addTab("Form", null, new JScrollPane(form), "Interactive Form");
		navigationTabs.addTab("XFA", null, new JScrollPane(form.getXfaTree()), "Tree view of the XFA form");
		navigationTabs.addTab("XRef", null, new JScrollPane(xref), "Cross-reference table");
        
		objectPanel = new PdfObjectPanel();
		addObserver(objectPanel);
		streamPane = new SyntaxHighlightedStreamPane();
		addObserver(streamPane);
		editorTabs = new JTabbedPane();
		editorTabs.addTab("Stream", null, streamPane, "Stream");
		editorTabs.addTab("XFA", null, form.getXfaTextArea(), "XFA Form XML file");
	}

	/**
	 * Getter for the PDF Tree.
	 * @return the PdfTree object
	 */
	public PdfTree getPdfTree() {
		return pdfTree;
	}

	/**
	 * Getter for the tabs that allow you to navigate through
	 * the PdfTree quickly (pages, form, outlines, xref table).
	 * @return	a JTabbedPane
	 */
	public JTabbedPane getNavigationTabs() {
		return navigationTabs;
	}

	/**
	 * Getter for the panel that will show the contents
	 * of a PDF Object (except for PdfStreams: only the
	 * Stream Dictionary will be shown; the content stream
	 * is shown in a StreamTextArea object).
	 * @return	the PdfObjectPanel
	 */
	public PdfObjectPanel getObjectPanel() {
		return objectPanel;
	}

	/**
	 * Getter for the tabs with the editor windows
	 * (to which the Console window will be added).
	 */
	public JTabbedPane getEditorTabs() {
		return editorTabs;
	}

	/**
	 * Getter for the object that holds the TextPane
	 * with the content stream of a PdfStream object.
	 * @return	a StreamTextArea
	 */
	public SyntaxHighlightedStreamPane getStreamPane() { 
		return streamPane;
	}

	/**
	 * Starts loading the PDF Objects in background.
	 * @param file	the wrapper object that holds the PdfReader as member variable
	 */
	public void startObjectLoader(PdfFile file) {
		setChanged();
		notifyObservers();
		setChanged();
		new ObjectLoader(this, file.getPdfReader(), file.getFilename());
	}

	/**
	 * The GUI components that show the internals of a PDF file,
	 * can only be shown if all objects are loaded into the
	 * IndirectObjectFactory using the ObjectLoader.
	 * As soon as this is done, the GUI components are notified.
	 * @param	obj	in this case the Object should be an ObjectLoader
	 * @see java.util.Observable#notifyObservers(java.lang.Object)
	 */
	@Override
	public void notifyObservers(Object obj) {
		if (obj instanceof ObjectLoader) {
			ObjectLoader loader = (ObjectLoader)obj;
			nodes = loader.getNodes();
			PdfTrailerTreeNode root = pdfTree.getRoot();
			root.setTrailer(loader.getReader().getTrailer());
			root.setUserObject("PDF Object Tree (" + loader.getLoaderName() + ")");
			nodes.expandNode(root);
		}
		super.notifyObservers(obj);
	}

	/**
	 * Selects a node in the PdfTree.
	 * @param node a node in the PdfTree
	 */
	public void selectNode(PdfObjectTreeNode node) {
		pdfTree.selectNode(node);
	}

	/**
	 * Selects a node in the PdfTree.
	 * @param objectNumber a number of a node in the PdfTree
	 */
	public void selectNode(int objectNumber) {
		selectNode(nodes.getNode(objectNumber));
	}

	/**
	 * Renders the syntax of a PdfObject in the objectPanel.
	 * If the object is a PDF Stream, then the stream is shown
	 * in the streamArea too.
	 */
	public void render(PdfObject object) {
		objectPanel.render(object);
		streamPane.render(object);
		if (object instanceof PRStream) {
			editorTabs.setSelectedComponent(streamPane);
		}
		else {
			editorTabs.setSelectedIndex(editorTabs.getComponentCount() - 1);
		}
	}

	/**
	 * Selects the row in the pageTable that corresponds with
	 * a certain page number.
	 * @param pageNumber the page number that needs to be selected
	 */
	public void gotoPage(int pageNumber) {
		pageNumber--;
		if (pages == null || pages.getSelectedRow() == pageNumber)
			return;
		if (pageNumber < pages.getRowCount())
			pages.setRowSelectionInterval(pageNumber, pageNumber);
	}

	/**
	 * Forwards updates from the RupsController to the Observers of this class.
	 * @param	observable	this should be the RupsController
	 * @param	obj	the object that has to be forwarded to the observers of PdfReaderController
	 * @see java.util.Observer#update(java.util.Observable, java.lang.Object)
	 */
	public void update(Observable observable, Object obj) {
		if (RupsMenuBar.CLOSE.equals(obj)) {
			setChanged();
			notifyObservers(null);
			nodes = null;
		}
		if (obj instanceof PdfObjectTreeNode) {
			PdfObjectTreeNode node = (PdfObjectTreeNode)obj;
			nodes.expandNode(node);
			if (node.isRecursive()) {
				pdfTree.selectNode(node.getAncestor());
				return;
			}/*
			if (node.isIndirect()) {
				xref.selectRowByReference(node.getNumber());
				return;
			} */
			render(node.getPdfObject());
		}
	}
}
