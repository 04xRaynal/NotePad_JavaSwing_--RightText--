/*
 * ## RightText ##
 * A traditional NotePad using Java Swing
 * Version - 0.1
 * 
 * Contains a JTextArea to write text, 
 * a MenuBar at the top which displays all the features present, 
 * and a status bar at the bottom displaying the line and column number.
 * 
 * Contains features such as Print File with Page Setup, Find and Replace words,
 * Choose a custom font, select a custom color,
 * count the words and characters, Goto Line and Wrap Text
 * 
 * Can Open and Save files, open a new file and save the file with a different name/location using Save As
 * Create a new File
 * All similar NotePad Keyboard shortcuts are added as well, such as Undo, Redo, etc.
 * 
 * @author - 04xRaynal
 */
package raynal.rightText;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.print.PageFormat;
import java.awt.print.Printable;
import java.awt.print.PrinterException;
import java.awt.print.PrinterJob;
import java.util.Date;

import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JColorChooser;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.event.CaretEvent;
import javax.swing.event.CaretListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.MenuEvent;
import javax.swing.event.MenuListener;
import javax.swing.text.BadLocationException;
import javax.swing.undo.UndoManager;

import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.paint.Color;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;


public class RightText extends JFrame implements ActionListener{
	private static final long serialVersionUID = 1L;
	
	Container c;
	JTextArea textArea;
	JLabel statusBar;
	JMenuBar menuBar;
	JMenu file, edit, format, view, wordCount, help;
	String fileName = "Untitled";
	String applicationName = "RightText";
	JCheckBoxMenuItem wordWrap, status;
	JPanel bottomPanel;
	JColorChooser backColorChooser, foreColorChooser;
	JDialog backgroundDialog, foregroundDialog;
	FindDialog findDialog;
	ReplaceDialog replaceDialog;
	CharacterCountDialog charCountDialog;
	WordCountDialog wordCountDialog;
	int caretPosition = 0;
	JRadioButton up, down;
	int startUp, startDown, endUp, endDown;
	JButton find;
	FileOperation fileOp;
	UndoManager manager;
	JMenuItem undo, redo;
	PrinterJob printJob;
	PageFormat pageFormat;
	
	
	public RightText() {
		c = getContentPane();
		
		try {
            String cn = UIManager.getSystemLookAndFeelClassName();
            UIManager.setLookAndFeel(cn);       // Use the native L&F
        } 
		catch (Exception cnf) {}				//For the Look and Feel of the UI
		
		textArea = new JTextArea(18, 72);
		JScrollPane scrollPane = new JScrollPane(textArea);
		
		statusBar = new JLabel("||     Ln 1, Col 1", JLabel.RIGHT);
		bottomPanel = new JPanel(new FlowLayout(SwingConstants.RIGHT));
		JSeparator sep = new JSeparator(SwingConstants.VERTICAL);
		bottomPanel.add(sep);
		bottomPanel.add(statusBar);
		bottomPanel.add(new JLabel("           "));
		
		menuBar = new JMenuBar();
		file = new JMenu("File");
		edit = new JMenu("Edit");
		format = new JMenu("Format");
		view = new JMenu("View");
		help = new JMenu("Help");
		undo = new JMenuItem();
		redo = new JMenuItem();
		addMenuItems();
		menuBar.add(file);  menuBar.add(edit);  menuBar.add(format);
		menuBar.add(view);  menuBar.add(help);
		
		textArea.addCaretListener(new CaretListener() {				//whenever the text on the TextArea is edited, caret Listener Updates the Caret Position
			@Override
			public void caretUpdate(CaretEvent e) {
				int  lineNumber = 0, column = 0;
				
				try {
					caretPosition = textArea.getCaretPosition();
					lineNumber = textArea.getLineOfOffset(caretPosition);
					column = caretPosition - textArea.getLineStartOffset(lineNumber);
				}
				catch(BadLocationException ex) {
					ex.printStackTrace();
				}
				
				if(findDialog != null || replaceDialog != null) {			//the start and end points are updated in the Find/Replace Dialog boxes as well
					if(up.isSelected()) {
						startUp = 0;
						endUp = caretPosition;
					}
					else if(down.isSelected()) {
						startDown = caretPosition;
						endDown = textArea.getText().length();
					}
				}
				
				statusBar.setText(" ||     Ln " + (lineNumber + 1) + ", Col " + (column + 1));
			}
		});
		
		
		Image titleIcon = Toolkit.getDefaultToolkit().getImage("src\\resources\\write-text-icon.png").getScaledInstance(80, 80, Image.SCALE_SMOOTH);
		setTitle(fileName + " - "+ applicationName);
		setIconImage(titleIcon);
		setJMenuBar(menuBar);
		setLayout(new BorderLayout());
		add(new JLabel(" "), BorderLayout.EAST);
		add(scrollPane, BorderLayout.CENTER);
		add(new JLabel(" "), BorderLayout.WEST);
		add(bottomPanel, BorderLayout.SOUTH);
		pack();
		setVisible(true);
		setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
		
		fileOp = new FileOperation(this);
		
		textArea.getDocument().addDocumentListener(new DocumentListener() {			//Document Listener updates the flag set in FileOperation whenever the textArea is edited
			
			@Override
			public void removeUpdate(DocumentEvent e) {
				fileOp.setSave(false);
			}
			
			@Override
			public void insertUpdate(DocumentEvent e) {
				fileOp.setSave(false);
			}
			
			@Override
			public void changedUpdate(DocumentEvent e) {
				fileOp.setSave(false);
			}
		});
		
		manager = new UndoManager();				//UndoManager Class helps with the Undo/Redo Feature
		textArea.getDocument().addUndoableEditListener(manager);
		
		//creates the Page Setup and Print Setup Dialogs, taken from Oracle Java Docs
		printJob = PrinterJob.getPrinterJob();
		printJob.setPrintable(new Printable() {

	    public int print(Graphics g, PageFormat pf, int page) throws PrinterException {

	        if (page > 0) { /* We have only one page, and 'page' is zero-based */
	            return NO_SUCH_PAGE;
	        }

	        /* User (0,0) is typically outside the imageable area, so we must
	         * translate by the X and Y values in the PageFormat to avoid clipping
	         */
	        Graphics2D g2d = (Graphics2D)g;
	        g2d.translate(pf.getImageableX(), pf.getImageableY());

	        /* Now we perform our rendering */
//		    g.drawString("Test the print dialog!", 100, 100);
	        textArea.paint(g2d);

	        /* tell the caller that this page is part of the printed document */
	        return PAGE_EXISTS;
	    	}
	    });
		
		
		//If The file in TextArea is edited but not saved, WindowListener prompts a message with the Save Dialog
		addWindowListener(new WindowAdapter() {					
			public void windowClosing(WindowEvent e) {
				if(! fileOp.isSave()) {
					int reply = fileOp.exitDialog();
					
					if(reply == JOptionPane.YES_OPTION) {
						fileOp.saveAs();
						System.exit(0);
					}
					else if(reply == JOptionPane.NO_OPTION)
						System.exit(0);
				}
				else {
					System.exit(0);
				}
			}
		});
	}
	
	
	public static void main(String[] args) {
		SwingUtilities.invokeLater(new Runnable() {
			
			@Override
			public void run() {
				new RightText();
			}
		});
	}
	
	
	void addMenuItems() {				//Adds MenuItems to the Menus
		JMenuItem newFile = new JMenuItem("New");
		newFile.addActionListener(this);
		newFile.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_N, KeyEvent.CTRL_DOWN_MASK));
		JMenuItem open = new JMenuItem("Open...");
		open.addActionListener(this);
		open.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O, KeyEvent.CTRL_DOWN_MASK));
		JMenuItem save = new JMenuItem("Save");
		save.addActionListener(this);
		save.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, KeyEvent.CTRL_DOWN_MASK));
		JMenuItem saveAs = new JMenuItem("Save As...");
		saveAs.addActionListener(this);
		JMenuItem pageSetup = new JMenuItem("Page Setup...");
		pageSetup.addActionListener(this);
		JMenuItem print = new JMenuItem("Print...");
		print.addActionListener(this);
		print.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_P, KeyEvent.CTRL_DOWN_MASK));
		JMenuItem exit = new JMenuItem("Exit");
		exit.addActionListener(this);
		
		file.add(newFile);  file.add(open);
		file.add(save);  file.add(saveAs);
		file.addSeparator();
		file.add(pageSetup);  file.add(print);
		file.addSeparator();
		file.add(exit);
		
		
		undo.setText("Undo");
		undo.setEnabled(false);
		undo.addActionListener(this);
		undo.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Z, KeyEvent.CTRL_DOWN_MASK));
		redo.setText("Redo");
		redo.setEnabled(false);
		redo.addActionListener(this);
		redo.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Y, KeyEvent.CTRL_DOWN_MASK));
		JMenuItem cut = new JMenuItem("Cut");
		cut.addActionListener(this);
		cut.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_X, KeyEvent.CTRL_DOWN_MASK));
		JMenuItem copy = new JMenuItem("Copy");
		copy.addActionListener(this);
		copy.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_C, KeyEvent.CTRL_DOWN_MASK));
		JMenuItem paste = new JMenuItem("Paste");
		paste.addActionListener(this);
		paste.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_V, KeyEvent.CTRL_DOWN_MASK));
		JMenuItem delete = new JMenuItem("Delete");
		delete.addActionListener(this);
		delete.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0));
		JMenuItem find = new JMenuItem("Find...");
		find.addActionListener(this);
		find.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F, KeyEvent.CTRL_DOWN_MASK));
		JMenuItem findNext = new JMenuItem("Find Next");
		findNext.addActionListener(this);
		findNext.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F3, 0));
		JMenuItem replace = new JMenuItem("Replace...");
		replace.addActionListener(this);
		replace.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_R, KeyEvent.CTRL_DOWN_MASK));
		JMenuItem goTo = new JMenuItem("Go To...");
		goTo.addActionListener(this);
		goTo.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_G, KeyEvent.CTRL_DOWN_MASK));
		JMenuItem selectAll = new JMenuItem("Select All");
		selectAll.addActionListener(this);
		selectAll.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_A, KeyEvent.CTRL_DOWN_MASK));
		JMenuItem timeDate = new JMenuItem("Time/Date");
		timeDate.addActionListener(this);
		timeDate.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F5, 0));
		
		edit.add(undo);  edit.add(redo);  
		edit.addSeparator();
		edit.add(cut);  edit.add(copy);
		edit.add(paste);  edit.add(delete);
		edit.addSeparator();
		edit.add(find);  edit.add(findNext);
		edit.add(replace);  edit.add(goTo);
		edit.addSeparator();
		edit.add(selectAll); edit.add(timeDate);
		
		
		wordWrap = new JCheckBoxMenuItem("Word Wrap");
		wordWrap.addActionListener(this);
		JMenuItem font = new JMenuItem("Font...");
		font.addActionListener(this);
		
		JMenuItem textColor = new JMenuItem("Set Text Color");
		textColor.addActionListener(this);
		JMenuItem padColor = new JMenuItem("Set Pad Color");
		padColor.addActionListener(this);
		
		format.add(wordWrap);  format.add(font);
		format.addSeparator();
		format.add(textColor);  format.add(padColor);
		
		
		status = new JCheckBoxMenuItem("Status Bar");
		status.setSelected(true);
		status.addActionListener(this);
		
		wordCount = new JMenu("Word Count");
		JMenuItem countCharacters = new JMenuItem("Count Characters");
		countCharacters.addActionListener(this);
		JMenuItem countWords = new JMenuItem("Count Words");
		countWords.addActionListener(this);
		JMenuItem countCharacter = new JMenuItem("Count By Character");
		countCharacter.addActionListener(this);
		JMenuItem countWord = new JMenuItem("Count By Word");
		countWord.addActionListener(this);
		
		wordCount.add(countCharacters);  wordCount.add(countWords);
		wordCount.add(countCharacter);  wordCount.add(countWord);
		
		view.add(status);
		view.addSeparator();
		view.add(wordCount);
		
		
		JMenuItem viewHelp = new JMenuItem("View Help");
		viewHelp.addActionListener(this);
		JMenuItem about = new JMenuItem("About RightText");
		about.addActionListener(this);
		
		help.add(viewHelp);
		help.addSeparator();
		help.add(about);
		
		
		edit.addMenuListener(new MenuListener() {			//Some MenuItems are disabled at start when there's no text in the TextArea
			@Override
			public void menuSelected(MenuEvent e) {
				if(textArea.getText().length() == 0) {
					find.setEnabled(false);
					findNext.setEnabled(false);
					replace.setEnabled(false);
					selectAll.setEnabled(false);
					goTo.setEnabled(false);
				}
				else {
					undo.setEnabled(true);
					find.setEnabled(true);
					findNext.setEnabled(true);
					replace.setEnabled(true);
					selectAll.setEnabled(true);
					goTo.setEnabled(true);
				}
				
				if(textArea.getSelectionStart() == textArea.getSelectionEnd()) {
					cut.setEnabled(false);
					copy.setEnabled(false);
					delete.setEnabled(false);
				}
				else {
					cut.setEnabled(true);
					copy.setEnabled(true);
					delete.setEnabled(true);
				}
			}
			
			@Override
			public void menuDeselected(MenuEvent e) {}
			
			@Override
			public void menuCanceled(MenuEvent e) {}
		});
	}
	
	
	@Override
	public void actionPerformed(ActionEvent e) {
		if(e.getActionCommand().equals("New")) {
			fileOp.newFile();
		}
		
		
		if(e.getActionCommand().equals("Open...")) {
			fileOp.openFile();
		}
		
		
		if(e.getActionCommand().equals("Save")) {
			fileOp.save();
		}
		
		
		//If The file in TextArea is edited but not saved, Exit prompts a message with the Save Dialog
		if(e.getActionCommand().equals("Exit")) {
			if(! fileOp.isSave()) {
				int reply = fileOp.exitDialog();
				
				if(reply == JOptionPane.YES_OPTION) {
					fileOp.saveAs();
					System.exit(0);
				}
				else if(reply == JOptionPane.NO_OPTION)
					System.exit(0);
			}
			else {
				System.exit(0);
			}
		}
		
		
		if(e.getActionCommand().equals("Save As...")) {
			fileOp.saveAs();
		}
		
		
		if(e.getActionCommand().equals("Page Setup...")) {
			pageFormat = printJob.pageDialog(printJob.defaultPage());
		}
		
		
		if(e.getActionCommand().equals("Print...")) {
			if(printJob.printDialog()) {
				try {
					printJob.print();
				}
				catch(PrinterException ex) {
					ex.printStackTrace();
				}
			}
		}
		
		
		if(e.getActionCommand().equals("Undo")) {
			manager.undo();
			if(!manager.canUndo()) {				//If it was the Last Undo, Undo Button is disabled
				undo.setEnabled(false);
			}
			redo.setEnabled(true);
		}
		
		
		if(e.getActionCommand().equals("Redo")) {
			manager.redo();
			if(! manager.canRedo()) {				//If it was the Last Redo, Redo Button is disabled
				redo.setEnabled(false);
			}
			undo.setEnabled(true);
		}
		
		
		if(e.getActionCommand().equals("Cut")) {
			textArea.cut();
		}
		
		
		if(e.getActionCommand().equals("Copy")) {
			textArea.copy();
		}
		
		
		if(e.getActionCommand().equals("Paste")) {
			textArea.paste();
		}
		
		
		if(e.getActionCommand().equals("Delete")) {
			textArea.replaceSelection("");
		}
		
		
		if(e.getActionCommand().equals("Go To...")) {			//GoTo the entered Line Number
			int lineNumber = 0;
			try {
				String goToInput = JOptionPane.showInputDialog(this, "Enter Line Number", "Go To...", JOptionPane.INFORMATION_MESSAGE);
				if(goToInput == null)
					return;
				lineNumber = Integer.parseInt(goToInput);
				textArea.setCaretPosition(textArea.getLineStartOffset(lineNumber - 1));
			}
			catch(BadLocationException ex) {
				JOptionPane.showMessageDialog(this, "Line " + lineNumber +" doesn't exist!", "Input Error", JOptionPane.ERROR_MESSAGE);
			}
		}
		
		
		if(e.getActionCommand().equals("Select All")) {
			textArea.selectAll();
		}
		
		
		if(e.getActionCommand().equals("Time/Date")) {			//Inputs the Current DateTime on the TextArea on the current Caret Position
			textArea.replaceSelection(new Date().toString());
		}
		
		
		if(e.getSource() == wordWrap) {					//Wraps the Text in TextArea
			if(wordWrap.isSelected()) {
				textArea.setLineWrap(wordWrap.isSelected());
				c.setVisible(false);
				remove(bottomPanel);
				add(new JLabel(" "), BorderLayout.SOUTH);
				status.setSelected(false);
				status.setEnabled(false);
				c.setVisible(true);
			}
			else {
				c.setVisible(false);
				textArea.setLineWrap(false);
				status.setEnabled(true);
				status.setSelected(true);
				add(bottomPanel, BorderLayout.SOUTH);
				c.setVisible(true);
			}
		}
		
		
		if(e.getActionCommand().equals("Font...")) {				//Opens the FontChooser Dialog
			JFontChooser fontChooser = new JFontChooser();
			   int result = fontChooser.showDialog(this);
			   if (result == JFontChooser.OK_OPTION)
			   {
			      Font font = fontChooser.getSelectedFont(); 
			      textArea.setFont(font);
			   }
		}
		
		
		if(e.getActionCommand().equals("Set Text Color")) {				//Opens a Dialog to set the Foreground(Text) Color
			if(foreColorChooser == null)
				foreColorChooser = new JColorChooser();
			
			if(foregroundDialog == null) {
				foregroundDialog = JColorChooser.createDialog(this, "Set Text Color", false, 
												foreColorChooser, new ActionListener() {
					
					@Override
					public void actionPerformed(ActionEvent e) {
						textArea.setForeground(foreColorChooser.getColor());
					}
				}, null);
			}
			
			foregroundDialog.setVisible(true);
		}
		
		
		if(e.getActionCommand().equals("Set Pad Color")) {				//Opens a Dialog to set the Background Color
			if(backColorChooser == null) {
				backColorChooser = new JColorChooser();
			}
			if(backgroundDialog == null) {
				backgroundDialog = JColorChooser.createDialog(this, "Set Pad Color", false, 
												backColorChooser, new ActionListener() {
					
					@Override
					public void actionPerformed(ActionEvent e) {
						textArea.setBackground(backColorChooser.getColor());
					}
				}, null);
			}
			
			backgroundDialog.setVisible(true);
		}
		
		
		if(e.getSource() == status) {				//Disables and Enables the Status Bar when clicked
			if(status.isSelected()) { 
				c.setVisible(false);
				add(bottomPanel, BorderLayout.SOUTH);
				c.setVisible(true);
			}
			else {
				c.setVisible(false);
				remove(bottomPanel);
				add(new JLabel(" "), BorderLayout.SOUTH);
				c.setVisible(true);
			}
		}
		
		
		if(e.getActionCommand().equals("Find...")) {			//Find Dialog is invoked, if not invoked earlier
			if(findDialog == null) {
				findDialog = new FindDialog();
			}
			else {
				findDialog.visibility(true);
			}
		}
		
		
		//If Find Dialog wasn't invoked earlier, invokes a new Find Dialog, else finds the next character/word as specified earlier using the Font Dialog
		if(e.getActionCommand().equals("Find Next")) {			
			if(findDialog == null) {
				findDialog = new FindDialog();
			}
			else {
				ActionEvent event = new ActionEvent(find, ActionEvent.ACTION_PERFORMED, "Time's Up", 1, 0);

				for (ActionListener listener : find.getActionListeners()) {
				    listener.actionPerformed(event);
				}
			}
		}
		
		
		if(e.getActionCommand().equals("Replace...")) {				//invokes the Replace Dialog, if not invoked earlier
			if(replaceDialog == null) {
				replaceDialog = new ReplaceDialog();
			}
			else {
				replaceDialog.visibility(true);
			}
		}
		
		
		if(e.getActionCommand().equals("Count Characters")) {			//Counts the Total Number of Characters
			JOptionPane.showMessageDialog(c, "Character Count: " + textArea.getText().length(), "Count Characters", JOptionPane.INFORMATION_MESSAGE);
		}
		
		
		if(e.getActionCommand().equals("Count Words")) {				//Counts the Total Number of Words
			String words[] = textArea.getText().split("\\s+");
			JOptionPane.showMessageDialog(c, "Word Count: " + words.length, "Count Words", JOptionPane.INFORMATION_MESSAGE);
		}
		
		
		if(e.getActionCommand().equals("Count By Character")) {			//Counts the specified Character
			if(charCountDialog == null) {
				charCountDialog = new CharacterCountDialog();
			}
			else {
				charCountDialog.visibility(true);
			}
		}
		
		
		if(e.getActionCommand().equals("Count By Word")) {				//Counts the specified Word
			if(wordCountDialog == null) {
				wordCountDialog = new WordCountDialog();
			}
			else {
				wordCountDialog.visibility(true);
			}
		}
		
		
		if(e.getActionCommand().equals("View Help")) {
			new HelpDialog();
		}
		
		
		if(e.getActionCommand().equals("About RightText")) {
			JDialog aboutDialog = new JDialog(this, true);
			JLabel aboutName = new JLabel("RightText");
			aboutName.setFont(new Font("Arial", Font.ITALIC, 24));
			
			JLabel aboutBody = new JLabel("<html>Version: 0.1<br/>No Copyright!<br/></html>");
			
			aboutDialog.add(aboutName);
			aboutDialog.add(new JSeparator());
			aboutDialog.add(aboutBody);
			aboutDialog.setLayout(new BoxLayout(aboutDialog.getContentPane(), BoxLayout.Y_AXIS));
			aboutDialog.setTitle("About RightText");
			aboutDialog.setSize(200, 120);
			aboutDialog.setVisible(true);
			aboutDialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
		}
	}
	
	
	//This class creates the Replace Dialog to find and replace text
	class ReplaceDialog extends JDialog implements ActionListener{
		private static final long serialVersionUID = 1L;
		int index = 0, inputLength = 0;
		
		JTextField inputTextField, replaceTextField;
		int ind = 0;
		JLabel findLabel, replaceLabel, directionLabel;
		JButton replace, replaceAll;
		JButton cancel;
		JCheckBox matchCase;
		String input, replaceInput;
	
		
		public ReplaceDialog() {
			super(RightText.this);
			findLabel = new JLabel("Find what: ");
			findLabel.setBounds(5, 10, 60, 20);
			inputTextField = new JTextField();
			inputTextField.setBounds(75, 10, 180, 20);
			replaceLabel = new JLabel("Replace with: ");
			replaceLabel.setBounds(5, 40, 80, 20);
			replaceTextField = new JTextField();
			replaceTextField.setBounds(75, 40, 180, 20);
			find = new JButton("Find Next");
			find.setBounds(260, 10, 90, 25);
			replace = new JButton("Replace");
			replace.setBounds(260, 40, 90, 25);
			replaceAll = new JButton("Replace All");
			replaceAll.setBounds(260, 70, 90, 25);
			cancel = new JButton("Cancel");
			cancel.setBounds(260, 100, 90, 25);
			directionLabel = new JLabel("Direction: ");
			directionLabel.setBounds(160, 75, 100, 20);
			up = new JRadioButton("Up");
			up.setBounds(160, 95, 40, 20);
			down = new JRadioButton("Down");
			down.setBounds(160, 115, 60, 20);
			down.setSelected(true);
			ButtonGroup buttonGroup = new ButtonGroup();
			buttonGroup.add(up);  buttonGroup.add(down);
			matchCase = new JCheckBox("Match Case");
			matchCase.setSelected(true);
			matchCase.setBounds(5, 110, 150, 20);
			startDown = caretPosition;
			up.addActionListener(this);
			down.addActionListener(this);
			find.addActionListener(this);
			cancel.addActionListener(this);
			replace.addActionListener(this);
			replaceAll.addActionListener(this);
			
			setLayout(null);
			add(findLabel);  add(inputTextField);  
			add(replaceLabel);  add(replaceTextField);
			add(find);  add(replace);  
			add(replaceAll);  add(cancel);
			add(directionLabel);  add(up); add(down);
			add(matchCase);
			setTitle("Replace");
			setSize(370, 180);
			setVisible(true);
			setDefaultCloseOperation(JDialog.HIDE_ON_CLOSE);
		}
		
		
		public void visibility(boolean flag) {
			setVisible(flag);
		}
		
		
		@Override
		public void actionPerformed(ActionEvent e) {
			if(matchCase.isSelected()) {
				input = inputTextField.getText();
				replaceInput = replaceTextField.getText();
			}
			else {
				input = inputTextField.getText().toLowerCase();
				replaceInput = replaceTextField.getText().toLowerCase();
			}
			
			
			if(e.getSource() == find || e.getSource() == replace) {
				inputLength = inputTextField.getText().length();

				if(up.isSelected()) {
					if(e.getSource() == replace) {
						if(textArea.getSelectedText() != null) {
							if(textArea.getSelectedText().equals(input)) {
								textArea.replaceRange(replaceInput, endUp, endUp + inputLength);
								textArea.setSelectionStart(endUp - inputLength);
								textArea.setSelectionEnd(endUp + inputLength);
								return;
							}
						}
						
					}
					if(endUp > startUp) {
						index = textArea.getText().substring(startUp, endUp).lastIndexOf(input);
						if(index != -1) {
							try {
								if(textArea.getText(startUp + index, inputLength).equals(input)) {
									textArea.setSelectionStart(startUp + index);
									textArea.setSelectionEnd(startUp + index + inputLength);
									
									if(e.getSource() == replace) {
										textArea.replaceRange(replaceInput, index, index + inputLength);
										textArea.setSelectionStart(index);
										textArea.setSelectionEnd(index + replaceInput.length());
									}
									endUp = index;
									ind = index;
								}
							}
							catch(BadLocationException ex) {
								ex.printStackTrace();
							}
						}
					}
				}
				else if(down.isSelected()) {
					if(e.getSource() == replace) {
						if(textArea.getSelectedText() != null) {
							if(textArea.getSelectedText().equals(input)) {
								textArea.replaceRange(replaceInput, startDown - inputLength, startDown);
								textArea.setSelectionStart(startDown - replaceInput.length());
								textArea.setSelectionEnd(startDown);
								return;
							}
						}
					}
					if(startDown < endDown) {
						index = textArea.getText().substring(startDown, endDown).indexOf(input);
						if(index != -1) {
							try {
								if(textArea.getText(startDown + index, inputLength).equals(input)) {
									textArea.setSelectionStart(startDown + index);
									textArea.setSelectionEnd(startDown + inputLength);
									
									if(e.getSource() == replace) {
										textArea.replaceRange(replaceInput, startDown - inputLength, startDown);
										textArea.setSelectionStart(startDown - replaceInput.length());
										textArea.setSelectionEnd(startDown);
									}
									endDown = textArea.getText().length();
								}
							}
							catch(BadLocationException ex) {
								ex.printStackTrace();
							}
						}
					}
				}
			}
			
			
			if(e.getSource() == replaceAll) {
				inputLength = inputTextField.getText().length();
				
				if(up.isSelected()) {
						if(textArea.getSelectedText() != null) {
							if(textArea.getSelectedText().equals(input)) {
								textArea.replaceRange(replaceInput, endUp, endUp + inputLength);
								textArea.setSelectionStart(endUp - inputLength);
								textArea.setSelectionEnd(endUp + inputLength);
							}
					}
					while(endUp > startUp) {
						index = textArea.getText().substring(startUp, endUp).lastIndexOf(input);
						if(index != -1) {
							try {
								if(textArea.getText(startUp + index, inputLength).equals(input)) {
									textArea.setSelectionStart(startUp + index);
									textArea.setSelectionEnd(startUp + index + inputLength);
									textArea.replaceRange(replaceInput, index, index + inputLength);
									textArea.setSelectionStart(index );
									textArea.setSelectionEnd(index + replaceInput.length());
									endUp = index;
									ind = index;
								}
							}
							catch(BadLocationException ex) {
								ex.printStackTrace();
							}
						}
						else {
							break;
						}
					}
				}
				else if(down.isSelected()) {
					if(textArea.getSelectedText() != null) {
						if(textArea.getSelectedText().equals(input)) {
							textArea.replaceRange(replaceInput, startDown - inputLength, startDown);
							textArea.setSelectionStart(startDown - replaceInput.length());
							textArea.setSelectionEnd(startDown);
						}
					}
					while(startDown < endDown) {
						index = textArea.getText().substring(startDown, endDown).indexOf(input);
						if(index != -1) {
							try {
								if(textArea.getText(startDown + index, inputLength).equals(input)) {
									textArea.setSelectionStart(startDown + index);
									textArea.setSelectionEnd(startDown + inputLength);
									textArea.replaceRange(replaceInput, startDown - inputLength, startDown);
									textArea.setSelectionStart(startDown - replaceInput.length());
									textArea.setSelectionEnd(startDown);
									endDown = textArea.getText().length();
								}
							}
							catch(BadLocationException ex) {
								ex.printStackTrace();
							}
						}
						else {
							break;
						}
					}
				}
			}
			
			
			if(e.getSource() == up) {
				startUp = 0;
				endUp = startDown - inputLength;
			}
			
			if(e.getSource() == down) {
				startDown = ind + inputLength;
				endDown = textArea.getText().length();
			}
			
			if(e.getSource() == cancel) {
				setVisible(false);
			}
		}
	}
	
	
	//This class creates the Find Dialog, to find the input text
	class FindDialog extends JDialog implements ActionListener{
		private static final long serialVersionUID = 1L;
		int index = 0, inputLength = 0;
		
		JTextField inputTextField;
		int ind = 0;
		JLabel findLabel, directionLabel;
		JButton cancel;
		JCheckBox matchCase;
		String input;

		
		public FindDialog() {
			super(RightText.this);
			findLabel = new JLabel("Find what: ");
			findLabel.setBounds(5, 10, 60, 20);
			inputTextField = new JTextField();
			inputTextField.setBounds(60, 10, 180, 20);
			find = new JButton("Find Next");
			find.setBounds(245, 10, 80, 25);
			cancel = new JButton("Cancel");
			cancel.setBounds(245, 40, 80, 25);
			directionLabel = new JLabel("Direction:");
			directionLabel.setBounds(160, 50, 100, 20);
			up = new JRadioButton("Up");
			up.setBounds(160, 70, 40, 20);
			down = new JRadioButton("Down");
			down.setBounds(160, 90, 70, 20);
			down.setSelected(true);
			ButtonGroup buttonGroup = new ButtonGroup();
			buttonGroup.add(up);  buttonGroup.add(down);
			matchCase = new JCheckBox("Match Case");
			matchCase.setSelected(true);
			matchCase.setBounds(5, 85, 150, 20);
			startDown = caretPosition;
			up.addActionListener(this);
			down.addActionListener(this);
			cancel.addActionListener(this);
			find.addActionListener(new ActionListener() {
				
				@Override
				public void actionPerformed(ActionEvent e) {
					if(matchCase.isSelected())
						input = inputTextField.getText();
					else
						input = inputTextField.getText().toLowerCase();
					
					inputLength = inputTextField.getText().length();
					if(up.isSelected()) {
						if(endUp > startUp) {
							index = textArea.getText().substring(startUp, endUp).lastIndexOf(input);
							if(index != -1) {
								try {
									if(textArea.getText(startUp + index, inputLength).equals(input)) {
										textArea.setSelectionStart(startUp + index);
										textArea.setSelectionEnd(startUp + index + inputLength);
										endUp = index;
										ind = index;
									}
								}
								catch(BadLocationException ex) {
									ex.printStackTrace();
								}
							}
						}
					}
					else if(down.isSelected()) {
						if(startDown < endDown) {
							index = textArea.getText().substring(startDown, endDown).indexOf(input);
							if(index != -1) {
								try {
									if(textArea.getText(startDown + index, inputLength).equals(input)) {
										textArea.setSelectionStart(startDown + index);
										textArea.setSelectionEnd(startDown + inputLength);
										endDown = textArea.getText().length();
									}
								}
								catch(BadLocationException ex) {
									ex.printStackTrace();
								}
							}
						}
					}
				}
			});
			
			setLayout(null);
			add(findLabel);  add(inputTextField);  
			add(find);  add(cancel);
			add(directionLabel);
			add(up); add(down);
			add(matchCase);
			setTitle("Find");
			setSize(345, 155);
			setVisible(true);
			setDefaultCloseOperation(JDialog.HIDE_ON_CLOSE);
		}
		
		
		public void visibility(boolean flag) {
			setVisible(flag);
		}
		
		
		@Override
		public void actionPerformed(ActionEvent e) {
			if(e.getSource() == up) {
				startUp = 0;
				endUp = startDown - inputLength;
			}
			
			if(e.getSource() == down) {
				startDown = ind + inputLength;
				endDown = textArea.getText().length();
			}
			
			if(e.getSource() == cancel) {
				setVisible(false);
			}
		}
	}
	
	
	//This class creates the Character Count Dialog to count a specific character
	class CharacterCountDialog extends JDialog implements ActionListener{
		private static final long serialVersionUID = 1L;
		int index = 0, inputLength = 0;
		JLabel characterLabel;
		JTextField inputTextField;
		JButton countButton, cancel;
		JCheckBox matchCase;
		char[] input;
		int start, end, count;
		 
		public CharacterCountDialog() {
			super(RightText.this);
			
			input = new char[1];
			characterLabel = new JLabel("Character: ");
			characterLabel.setBounds(5, 10, 100, 20);
			inputTextField = new JTextField(1);
			inputTextField.setBounds(60, 10, 40, 20);
			countButton = new JButton("Count");
			countButton.setBounds(110, 10, 70, 25);
			countButton.addActionListener(this);
			cancel = new JButton("Cancel");
			cancel.setBounds(110, 40, 70, 25);
			cancel.addActionListener(this);
			matchCase = new JCheckBox("Match Case");
			matchCase.setSelected(true);
			matchCase.setBounds(5, 70, 100, 20);
			
			add(characterLabel);  add(inputTextField);  
			add(countButton);  add(cancel);  
			add(matchCase);
			setLayout(null);
			setTitle("Count By Character");
			setSize(200, 130);
			setVisible(true);
			setDefaultCloseOperation(JDialog.HIDE_ON_CLOSE);
		}
		
		public void actionPerformed(ActionEvent e) {
			start = 0;
			end = textArea.getText().length();
			
			if(matchCase.isSelected()) {
				input = inputTextField.getText().toCharArray();
			}
			else {
				input = inputTextField.getText().toLowerCase().toCharArray();
			}
			
			if(e.getSource() == countButton) {
				while(end > start) {
					index = textArea.getText().substring(start, end).indexOf((int) input[0]);
					if(index != -1) {
						count++;
						start = start + index + input.length;
					}
					else {
						break;
					}
				}
				
				JOptionPane.showMessageDialog(this, "Count of Character '" + input[0] + "' is: " + count, "Character Count", JOptionPane.INFORMATION_MESSAGE);
				count = 0;
			}
			
			if(e.getSource() == cancel) {
				setVisible(false);
			}
		}
		
		public void visibility(boolean status) {
			setVisible(status);
		}
	}
	
	
	//This class creates the Word Count Dialog to count the specified word
	class WordCountDialog extends JDialog implements ActionListener{
		private static final long serialVersionUID = 1L;
		int index = 0, inputLength = 0;
		JLabel wordLabel;
		JTextField inputTextField;
		JButton countButton, cancel;
		JCheckBox matchCase;
		String input;
		int start, end, count;
		 
		public WordCountDialog() {
			super(RightText.this);
			input = new String();
			wordLabel = new JLabel("Word: ");
			wordLabel.setBounds(5, 10, 60, 20);
			inputTextField = new JTextField();
			inputTextField.setBounds(40, 10, 90, 20);
			countButton = new JButton("Count");
			countButton.setBounds(135, 10, 70, 25);
			countButton.addActionListener(this);
			cancel = new JButton("Cancel");
			cancel.setBounds(135, 40, 70, 25);
			cancel.addActionListener(this);
			matchCase = new JCheckBox("Match Case");
			matchCase.setSelected(true);
			matchCase.setBounds(5, 70, 100, 20);
			
			add(wordLabel);  add(inputTextField);  
			add(countButton);  add(cancel);  
			add(matchCase);
			setTitle("Count By Word");
			setLayout(null);
			setSize(225, 130);
			setVisible(true);
			setDefaultCloseOperation(JDialog.HIDE_ON_CLOSE);
		}
		
		public void actionPerformed(ActionEvent e) {
			start = 0;
			end = textArea.getText().length();
			
			if(matchCase.isSelected()) {
				input = inputTextField.getText();
			}
			else {
				input = inputTextField.getText().toLowerCase();
			}
			
			if(e.getSource() == countButton) {
				while(end > start) {
					index = textArea.getText().substring(start, end).indexOf(input);
					if(index != -1) {
						count++;
						start = start + index + input.length();
					}
					else {
						break;
					}
				}
				
				JOptionPane.showMessageDialog(this, "Count of Word \"" + input + "\" is: " + count, "Word Count", JOptionPane.INFORMATION_MESSAGE);
				count = 0;
			}
			
			if(e.getSource() == cancel) {
				setVisible(false);
			}
		}
		
		public void visibility(boolean status) {
			setVisible(status);
		}
	}
	
	
	//Creates a Dialog which displays the WikiHow Webpage
	public class HelpDialog extends JDialog {
		private static final long serialVersionUID = 1L;
		JFXPanel fxPanel;
		
		public HelpDialog() {
			super(RightText.this);
			fxPanel = new JFXPanel();
			
			add(fxPanel);
			setSize(500, 500);
			setTitle("RightText Help");
			setVisible(true);
			
			Platform.runLater(new Runnable() {
				
				@Override
				public void run() {
					initFx(fxPanel);
				}
			});
		}
		
		void initFx(JFXPanel fxPanel) {
			Scene scene = createScene();
			fxPanel.setScene(scene);
		}
		
		Scene createScene() {
			 Group  root  =  new  Group();
		     Scene  scene  =  new  Scene(root, Color.ALICEBLUE);
		     
		     final WebView browser = new WebView();
		     final WebEngine webEngine = browser.getEngine();
		     webEngine.load("https://www.wikihow.com/Use-Notepad");
		     browser.setPrefSize(486, 460);
		     root.getChildren().add(browser);
		     return scene;
		}
	}
}
