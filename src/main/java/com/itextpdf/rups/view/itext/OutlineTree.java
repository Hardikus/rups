/*
 *
 * This file is part of the iText (R) project.
 * Copyright (c) 2007-2015 iText Group NV
 * Authors: Bruno Lowagie et al.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License version 3
 * as published by the Free Software Foundation with the addition of the
 * following permission added to Section 15 as permitted in Section 7(a):
 * FOR ANY PART OF THE COVERED WORK IN WHICH THE COPYRIGHT IS OWNED BY
 * ITEXT GROUP. ITEXT GROUP DISCLAIMS THE WARRANTY OF NON INFRINGEMENT
 * OF THIRD PARTY RIGHTS
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Affero General Public License for more details.
 * You should have received a copy of the GNU Affero General Public License
 * along with this program; if not, see http://www.gnu.org/licenses or write to
 * the Free Software Foundation, Inc., 51 Franklin Street, Fifth Floor,
 * Boston, MA, 02110-1301 USA, or download the license from the following URL:
 * http://itextpdf.com/terms-of-use/
 *
 * The interactive user interfaces in modified source and object code versions
 * of this program must display Appropriate Legal Notices, as required under
 * Section 5 of the GNU Affero General Public License.
 *
 * In accordance with Section 7(b) of the GNU Affero General Public License,
 * a covered work must retain the producer line in every PDF that is created
 * or manipulated using iText.
 *
 * You can be released from the requirements of the license by purchasing
 * a commercial license. Buying such a license is mandatory as soon as you
 * develop commercial activities involving the iText software without
 * disclosing the source code of your own applications.
 * These activities include: offering paid services to customers as an ASP,
 * serving PDFs on the fly in a web application, shipping iText with a closed
 * source product.
 *
 * For more information, please contact iText Software Corp. at this
 * address: sales@itextpdf.com
 */
package com.itextpdf.rups.view.itext;

import java.util.Observable;
import java.util.Observer;

import javax.swing.JTree;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultTreeModel;

import com.itextpdf.rups.controller.PdfReaderController;
import com.itextpdf.rups.model.ObjectLoader;
import com.itextpdf.rups.model.TreeNodeFactory;
import com.itextpdf.rups.view.icons.IconTreeCellRenderer;
import com.itextpdf.rups.view.itext.treenodes.OutlineTreeNode;
import com.itextpdf.rups.view.itext.treenodes.PdfObjectTreeNode;
import com.itextpdf.rups.view.itext.treenodes.PdfTrailerTreeNode;
import com.itextpdf.text.pdf.PdfName;

/**
 * A JTree visualizing information about the outlines (aka bookmarks) of
 * the PDF file (if any).
 */
public class OutlineTree extends JTree implements TreeSelectionListener, Observer {

	/** Nodes in the FormTree correspond with nodes in the main PdfTree. */
	protected PdfReaderController controller;
	
	/** Creates a new outline tree. */
	public OutlineTree(PdfReaderController controller) {
		super();
		this.controller = controller;
		setCellRenderer(new IconTreeCellRenderer());
		setModel(new DefaultTreeModel(new OutlineTreeNode()));
		addTreeSelectionListener(this);
	}

	/**
	 * @see java.util.Observer#update(java.util.Observable, java.lang.Object)
	 */
	public void update(Observable observable, Object obj) {
		if (obj == null) {
			setModel(new DefaultTreeModel(new OutlineTreeNode()));
			repaint();
			return;
		}
		if (obj instanceof ObjectLoader) {
			ObjectLoader loader = (ObjectLoader)obj;
			TreeNodeFactory factory = loader.getNodes();
			PdfTrailerTreeNode trailer = controller.getPdfTree().getRoot();
			PdfObjectTreeNode catalog = factory.getChildNode(trailer, PdfName.ROOT);
			PdfObjectTreeNode outline = factory.getChildNode(catalog, PdfName.OUTLINES);
			if (outline == null) {
				return;
			}
			OutlineTreeNode root = new OutlineTreeNode();
			PdfObjectTreeNode first = factory.getChildNode(outline, PdfName.FIRST);
			if (first != null)
			    loadOutline(factory, root, first);
			setModel(new DefaultTreeModel(root));
		}
	}
	
	/**
	 * Method that can be used recursively to load the outline hierarchy into the tree.
	 */
	private void loadOutline(TreeNodeFactory factory, OutlineTreeNode parent, PdfObjectTreeNode child) {
		OutlineTreeNode childnode = new OutlineTreeNode(child);
		parent.add(childnode);
		PdfObjectTreeNode first = factory.getChildNode(child, PdfName.FIRST);
		if (first != null) {
			loadOutline(factory, childnode, first);
		}
		PdfObjectTreeNode next = factory.getChildNode(child, PdfName.NEXT);
		if (next != null) {
			loadOutline(factory, parent, next);
		}
	}
	
	/**
	 * @see javax.swing.event.TreeSelectionListener#valueChanged(javax.swing.event.TreeSelectionEvent)
	 */
	public void valueChanged(TreeSelectionEvent evt) {
		if (controller == null)
			return;
		OutlineTreeNode selectednode = (OutlineTreeNode)this.getLastSelectedPathComponent();
		PdfObjectTreeNode node = selectednode.getCorrespondingPdfObjectNode();
		if (node != null)
			controller.selectNode(node);
	}

	/** A serial version uid. */
	private static final long serialVersionUID = 5646572654823301007L;

}
