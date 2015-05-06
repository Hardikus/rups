/*
 * $Id$
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
package com.itextpdf.rups.model;

import java.util.Observable;

import com.itextpdf.text.pdf.PdfReader;

/**
 * Loads the necessary iText PDF objects in Background.
 */
public class ObjectLoader extends BackgroundTask {
	/** This is the object that will forward the updates to the observers. */
	protected Observable observable;
	/** iText's PdfReader object. */
	protected PdfReader reader;
	/** The factory that can provide PDF objects. */
	protected IndirectObjectFactory objects;
	/** The factory that can provide tree nodes. */
	protected TreeNodeFactory nodes;
	/** a human readable name for this loaded */
	private String loaderName;
	
	/**
	 * Creates a new ObjectLoader.
	 * @param	observable	the object that will forward the changes.
	 * @param	reader		the PdfReader from which the objects will be read.
	 */
	public ObjectLoader(Observable observable, PdfReader reader, String loaderName) {
		this.observable = observable;
		this.reader = reader;
		this.loaderName = loaderName;
		start();
	}
	
	/**
	 * Getter for the PdfReader object.
	 * @return	a reader object
	 */
	public PdfReader getReader() {
		return reader;
	}

	/**
	 * Getter for the object factory.
	 * @return	an indirect object factory
	 */
	public IndirectObjectFactory getObjects() {
		return objects;
	}

	/**
	 * Getter for the tree node factory.
	 * @return	a tree node factory
	 */
	public TreeNodeFactory getNodes() {
		return nodes;
	}
	
	/**
	 * getter for a human readable name representing this loader
	 * @return the human readable name
	 * @since 5.0.3
	 */
	public String getLoaderName(){
	    return loaderName;
	}
	
	/**
	 * @see com.itextpdf.rups.model.BackgroundTask#doTask()
	 */
	@Override
	public void doTask() {
		ProgressDialog progress = new ProgressDialog(null, "Reading PDF file");
		objects = new IndirectObjectFactory(reader);
		int n = objects.getXRefMaximum();
		progress.setMessage("Reading the Cross-Reference table");
		progress.setTotal(n);
		while (objects.storeNextObject()) {
			progress.setValue(objects.getCurrent());
		}
		progress.setTotal(0);
		nodes = new TreeNodeFactory(objects);
		progress.setMessage("Updating GUI");
		observable.notifyObservers(this);
		progress.dispose();
	}
}