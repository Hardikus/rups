/*
 * $Id$
 *
 * Copyright 2012 Raf Hens.
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

package com.itextpdf.rups.view.itext;

import com.itextpdf.rups.view.contextmenu.ContextMenuMouseListener;
import com.itextpdf.rups.view.contextmenu.StreamPanelContextMenu;
import com.itextpdf.text.exceptions.InvalidPdfException;
import com.itextpdf.text.io.RandomAccessSourceFactory;
import com.itextpdf.text.pdf.*;
import com.itextpdf.text.pdf.parser.PdfImageObject;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.text.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.*;

public class SyntaxHighlightedStreamPane extends JScrollPane implements Observer {

	/** The text pane with the content stream. */
	protected ColorTextPane text;

	/** Syntax highlight attributes for operators */
	protected static Map<String, Map<Object, Object>> attributemap = null;

	/** Highlight operands according to their operator */
	protected static boolean matchingOperands = false;

	/** Factory that allows you to create RandomAccessSource files */
	protected static final RandomAccessSourceFactory RASF = new RandomAccessSourceFactory();

	/**
	 * Constructs a SyntaxHighlightedStreamPane.
	 */
	public SyntaxHighlightedStreamPane() {
		super();
		initAttributes();
		text = new ColorTextPane();
		setViewportView(text);

        JPopupMenu popupMenu = StreamPanelContextMenu.getContextMenu(text);
        text.add(popupMenu);
        text.addMouseListener(new ContextMenuMouseListener(popupMenu, text));
	}

	/**
	 * @see java.util.Observer#update(java.util.Observable, java.lang.Object)
	 */
	public void update(Observable observable, Object obj) {
		text.setText(null);
	}
	/**
	 * Renders the content stream of a PdfObject or empties the text area.
	 * @param object	the object of which the content stream needs to be rendered
	 */
	public void render(PdfObject object) {
        if (object instanceof PRStream) {
            PRStream stream = (PRStream)object;
            if( stream.get(PdfName.LENGTH1) == null ){
                String newline = "\n";
                byte[] bb = null;
                try {
                    text.setText("");
                    bb = PdfReader.getStreamBytes(stream);

                    PRTokeniser tokeniser = new PRTokeniser(new RandomAccessFileOrArray(RASF.createSource(bb)));

                    PdfContentParser ps = new PdfContentParser(tokeniser);
                    ArrayList<PdfObject> tokens = new ArrayList<PdfObject>();
                    while (ps.parse(tokens).size() > 0){
                        // operator is at the end
                        //System.out.println((tokens.get(tokens.size()-1)).toString());
                        String operator = (tokens.get(tokens.size()-1)).toString();
                        // operands are in front of their operator
                        StringBuilder operandssb = new StringBuilder();
                        for (int i = 0; i < tokens.size()-1; i++) {
                            append(operandssb, tokens.get(i));
                        }
                        String operands = operandssb.toString();

                        Map<Object, Object> attributes = attributemap.get(operator);
                        Map<Object, Object> attributesOperands = null;
                        if (matchingOperands)
                            attributesOperands = attributes;


                        text.append(operands, attributesOperands);
                        text.append(operator + newline, attributes);
                    }
                }
                catch (InvalidPdfException e) {
                    if ( bb != null ) {
                        text.setText(new String(bb));
                    } else {
                        text.setText(e.getMessage());
                    }
                }
                catch (IOException e) {
                    //throw new ExceptionConverter(e);
                }

                text.setCaretPosition(0); // set the caret at the start so the panel will show the first line
            }
        }
        if(object.type() == 7){
            //Check if stream is image
            PRStream stream = (PRStream)object;
            if(stream.get(PdfName.SUBTYPE) == PdfName.IMAGE){
                try {
                    //Convert byte array back to Image
                    if(!stream.get(PdfName.WIDTH).isNumber() && !stream.get(PdfName.HEIGHT).isNumber())return;
                    PdfImageObject pimg = new PdfImageObject(stream);
                    BufferedImage img = pimg.getBufferedImage();
                    if ( img != null ) {
                        //Show image in textpane
                        StyledDocument doc = (StyledDocument) text.getDocument();
                        Style style = doc.addStyle("Image", null);
                        StyleConstants.setIcon(style, new ImageIcon(img));

                        try {
                            doc.insertString(doc.getLength(), "ignored text", style);
                            JButton saveImage = new JButton("Save Image");
                            final BufferedImage saveImg = img;
                            saveImage.addActionListener(new ActionListener() {

                                public void actionPerformed(ActionEvent event) {
                                    try {
                                        FileDialog fileDialog = new FileDialog(new Frame(), "Save", FileDialog.SAVE);
                                        fileDialog.setFilenameFilter(new FilenameFilter() {

                                            public boolean accept(File dir, String name) {
                                                return name.endsWith(".jpg");
                                            }
                                        });
                                        fileDialog.setFile("Untitled.jpg");
                                        fileDialog.setVisible(true);
                                        ImageIO.write(saveImg, "jpg", new File(fileDialog.getDirectory() + fileDialog.getFile()));
                                    } catch (Exception e) {
                                        e.printStackTrace();

                                    }
                                }
                            });
                            text.append("\n", null);
                            text.insertComponent(saveImage);
                        } catch (BadLocationException e) {
                            e.printStackTrace();
                        }
                    } else {
                        text.setText("Image can't be loaded.");
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }

            } else if ( stream.get(PdfName.LENGTH1) != null ) {
                try {
                    byte[] bytes = PdfReader.getStreamBytesRaw(stream);
                    text.setText(new String(bytes));
                    text.setCaretPosition(0);
                } catch (IOException e) {
                    text.setText("");
                }
            }

        }
		else {
			update(null, null);
			return;
		}
		text.repaint();
		repaint();
	}
    protected void append(StringBuilder sb, PdfObject obj) {
        switch(obj.type()) {
            case PdfObject.STRING:
                PdfString str = (PdfString) obj;
                if (str.isHexWriting()) {
                    sb.append("<");
                    byte b[] = str.getBytes();
                    int len = b.length;
                    String hex;
                    for (int k = 0; k < len; ++k) {
                    	hex = Integer.toHexString(b[k]);
                    	if (hex.length() % 2 == 1)
                    		sb.append("0");
                    	sb.append(hex);
                    }
                    sb.append("> ");
                }
                else {
                    sb.append("(");
                    sb.append(obj);
                    sb.append(") ");
                }
                break;
            case PdfObject.DICTIONARY:
            	PdfDictionary dict = (PdfDictionary)obj;
            	sb.append("<<");
            	for (PdfName key : dict.getKeys()) {
            		sb.append(key);
            		sb.append(" ");
            		append(sb, dict.get(key));
            	}
            	sb.append(">> ");
            	break;
            default:
                sb.append(obj);
                sb.append(" ");
        }
    }

	/**
	 * Initialize the syntax highlighting attributes.
	 * This could be read from a configuration file, but is hard coded for now
	 */
	protected void initAttributes() {
		attributemap = new HashMap<String, Map<Object, Object>>();
		
		Map<Object, Object> opConstructionPainting = new HashMap<Object, Object>();
		Color darkorange = new Color(255, 140, 0);
		opConstructionPainting.put(StyleConstants.Foreground, darkorange);
		opConstructionPainting.put(StyleConstants.Background, Color.WHITE);
		attributemap.put("m", opConstructionPainting);
		attributemap.put("l", opConstructionPainting);
		attributemap.put("c", opConstructionPainting);
		attributemap.put("v", opConstructionPainting);
		attributemap.put("y", opConstructionPainting);
		attributemap.put("h", opConstructionPainting);
		attributemap.put("re", opConstructionPainting);
		attributemap.put("S", opConstructionPainting);
		attributemap.put("s", opConstructionPainting);
		attributemap.put("f", opConstructionPainting);
		attributemap.put("F", opConstructionPainting);
		attributemap.put("f*", opConstructionPainting);
		attributemap.put("B", opConstructionPainting);
		attributemap.put("B*", opConstructionPainting);
		attributemap.put("b", opConstructionPainting);
		attributemap.put("b*", opConstructionPainting);
		attributemap.put("n", opConstructionPainting);
		attributemap.put("W", opConstructionPainting);
		attributemap.put("W*", opConstructionPainting);
		
		Map<Object, Object> graphicsdelim = new HashMap<Object, Object>();
		graphicsdelim.put(StyleConstants.Foreground, Color.WHITE);
		graphicsdelim.put(StyleConstants.Background, Color.RED);
		graphicsdelim.put(StyleConstants.Bold, true);
		attributemap.put("q", graphicsdelim);
		attributemap.put("Q", graphicsdelim);

		Map<Object, Object> graphics = new HashMap<Object, Object>();
		graphics.put(StyleConstants.Foreground, Color.RED);
		graphics.put(StyleConstants.Background, Color.WHITE);
		attributemap.put("w", graphics);
		attributemap.put("J", graphics);
		attributemap.put("j", graphics);
		attributemap.put("M", graphics);
		attributemap.put("d", graphics);
		attributemap.put("ri", graphics);
		attributemap.put("i", graphics);
		attributemap.put("gs", graphics);
		attributemap.put("cm", graphics);
		attributemap.put("g", graphics);
		attributemap.put("G", graphics);
		attributemap.put("rg", graphics);
		attributemap.put("RG", graphics);
		attributemap.put("k", graphics);
		attributemap.put("K", graphics);
		attributemap.put("cs", graphics);
		attributemap.put("CS", graphics);
		attributemap.put("sc", graphics);
		attributemap.put("SC", graphics);
		attributemap.put("scn", graphics);
		attributemap.put("SCN", graphics);
		attributemap.put("sh", graphics);
		
		Map<Object, Object> xObject = new HashMap<Object, Object>();
		xObject.put(StyleConstants.Foreground, Color.BLACK);
		xObject.put(StyleConstants.Background, Color.YELLOW);
		attributemap.put("Do", xObject);
		
		Map<Object, Object> inlineImage = new HashMap<Object, Object>();
		inlineImage.put(StyleConstants.Foreground, Color.BLACK);
		inlineImage.put(StyleConstants.Background, Color.YELLOW);
		inlineImage.put(StyleConstants.Italic, true);
		attributemap.put("BI", inlineImage);
		attributemap.put("EI", inlineImage);
		
		Map<Object, Object> textdelim = new HashMap<Object, Object>();
		textdelim.put(StyleConstants.Foreground, Color.WHITE);
		textdelim.put(StyleConstants.Background, Color.BLUE);
		textdelim.put(StyleConstants.Bold, true);
		attributemap.put("BT", textdelim);
		attributemap.put("ET", textdelim);
		
		Map<Object, Object> text = new HashMap<Object, Object>();
		text.put(StyleConstants.Foreground, Color.BLUE);
		text.put(StyleConstants.Background, Color.WHITE);
		attributemap.put("ID", text);
		attributemap.put("Tc", text);
		attributemap.put("Tw", text);
		attributemap.put("Tz", text);
		attributemap.put("TL", text);
		attributemap.put("Tf", text);
		attributemap.put("Tr", text);
		attributemap.put("Ts", text);
		attributemap.put("Td", text);
		attributemap.put("TD", text);
		attributemap.put("Tm", text);
		attributemap.put("T*", text);
		attributemap.put("Tj", text);
		attributemap.put("'", text);
		attributemap.put("\"", text);
		attributemap.put("TJ", text);
		
		Map<Object, Object> markedContent = new HashMap<Object, Object>();
		markedContent.put(StyleConstants.Foreground, Color.MAGENTA);
		markedContent.put(StyleConstants.Background, Color.WHITE);
		attributemap.put("BMC", markedContent);
		attributemap.put("BDC", markedContent);
		attributemap.put("EMC", markedContent);
	}
	
	/** a serial version id. */
	private static final long serialVersionUID = -3699893393067753664L;

}

class ColorTextPane extends JTextPane {
	
	/**
	 * Appends a string to the JTextPane, with style attributes applied.
	 * @param s       the String to be appended
	 * @param attr    a Map of attributes used to style the string
	 */
	public void append(String s, Map<Object, Object> attr) {
		StyleContext sc = StyleContext.getDefaultStyleContext();
		AttributeSet aset = SimpleAttributeSet.EMPTY;
		// some default attributes
		if (attr == null) {
			attr = new HashMap<Object,Object>();
			attr.put(StyleConstants.Foreground, Color.BLACK);
			attr.put(StyleConstants.Background, Color.WHITE);
		}
		// add attributes
		for (Object key : attr.keySet()) {
			aset = sc.addAttribute(aset, key, attr.get(key));
		}
		int len = getDocument().getLength();
		setCaretPosition(len);
		setCharacterAttributes(aset, true);
		replaceSelection(s);
	}
	
	private static final long serialVersionUID = 1302283071087762495L;
}