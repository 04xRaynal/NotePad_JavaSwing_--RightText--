/*
 * Class enables File operations
 */
package raynal.rightText;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.filechooser.FileNameExtensionFilter;

public class FileOperation {
	RightText rt;
	private boolean saved;
	private boolean newFileFlag;			//flag indicates that the file is a new File, not already present in the system (Hence saved via Save As) or a read only file
	private String fileName, applicationTitleString = " - Right Text";
	
	File file;
	JFileChooser fileChooser;
	
	//getters and setters
	boolean isSave() {
		return saved;
	}
	
	void setSave(boolean saved) {
		this.saved = saved;
	}
	
	String getFileName() {
		return fileName;
	}
	
	void setFileName(String fileName) {
		this.fileName = fileName;
	}
	
	
	public FileOperation(RightText rt) {
		this.rt = rt;
		saved = true;
		newFileFlag = true;
		fileName = new String("Untitled");
		file = new File(fileName);
		this.rt.setTitle(fileName + applicationTitleString);
		
		fileChooser = new JFileChooser();
//		fileChooser.removeChoosableFileFilter(fileChooser.getAcceptAllFileFilter());
		fileChooser.addChoosableFileFilter(new FileNameExtensionFilter("Text Documents (*.txt)", "txt"));
		fileChooser.addChoosableFileFilter(new FileNameExtensionFilter("Java Source Code (*.java)", "java"));
//		fileChooser.addChoosableFileFilter(new FileNameExtensionFilter("C++ Source Code (*.cpp)", "cpp"));
//		fileChooser.addChoosableFileFilter(new FileNameExtensionFilter("Python Source Code (*.py)", "python"));
//		fileChooser.addChoosableFileFilter(new FileNameExtensionFilter("JavaScript Source Code (*.js)", "js"));
//		fileChooser.addChoosableFileFilter(new FileNameExtensionFilter("HTML File (*.html)", "html"));
//		fileChooser.addChoosableFileFilter(new FileNameExtensionFilter("CSS File (*.css)", "css"));
		
		fileChooser.setCurrentDirectory(new File("."));
	}
	
	
	boolean saveFile(File temp) {				//Writes content present in TextArea onto the File
		try(BufferedWriter writer = new BufferedWriter(new PrintWriter(temp))){
			String content = this.rt.textArea.getText();
			content = content.replaceAll("(?!\\r)\\n", "\r\n");			//Some Windows Applications (like Windows 7 NotePad) don't recognize new line via only \n, hence \r\n is put instead
			writer.write(content);
			
		}
		catch(IOException ex) {
			updateStatus(temp, false);
			return false;
		}
		
		updateStatus(temp, true);
		return true;
	}
	
	
	boolean save() {
		if(! newFileFlag)				//If file is already present in the System, Save automatically overwrites it.
			return saveFile(file);
		
		return saveAs();				//Otherwise File is saved with Save As
	}
	
	
	boolean saveAs() {
		File temp;
		fileChooser.setDialogTitle("Save As...");
		fileChooser.setApproveButtonToolTipText("Click here to Save File");
		
		do {
			if(fileChooser.showSaveDialog(this.rt) != JFileChooser.APPROVE_OPTION) 
				return false;
			
			temp = fileChooser.getSelectedFile();
			if(! temp.exists())
				break;
			
			if(JOptionPane.showConfirmDialog(this.rt, temp.getPath() + " already exists.\nDO you want to replace it?", "Save As", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION)
				break;
		} while(true);
		
		return saveFile(temp);
	}
	
	
	boolean openFile(File temp) {				//Reads from the selected File
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(temp)))) {
			String str = "";
			while((str = reader.readLine()) != null) {
				this.rt.textArea.append(str + "\n");
			}
		}
		catch (IOException ex) {
			updateStatus(temp, false);
			return false;
		}
		
		updateStatus(temp, true);
		this.rt.textArea.setCaretPosition(0);
		return true;
	}
	
	
	void openFile() {
		if(!confirmSave())					//checks if the previous file was saved before opening the Open Dialog
			return;
		fileChooser.setDialogTitle("Open File");        
		fileChooser.setApproveButtonToolTipText("Click here to Open File");
		
		File temp;
		do {
			if(fileChooser.showOpenDialog(this.rt) != JFileChooser.APPROVE_OPTION)
				return;
			
			temp = fileChooser.getSelectedFile();
			if(temp.exists())
				break;
			
			JOptionPane.showMessageDialog(this.rt, temp.getPath() + " File Not Found.\nPlease verify the name and location.", "Open", JOptionPane.ERROR_MESSAGE);
		} while(true);
		
		this.rt.textArea.setText("");
		
		if(!openFile(temp)) {					//Reads content if the file is not empty
			fileName = "Untitled";
			saved = true;
			this.rt.setTitle(fileName + applicationTitleString);
		}
		
		if(! temp.canWrite())					//Checks if File is a Read-Only File
			newFileFlag = true;
	}
	
	
	void updateStatus(File temp, boolean saved) {				//Attributes of the File are set
		if(saved) {
			this.saved = true;
			fileName = temp.getName();
			if(! temp.canWrite()) {
				fileName += "(Read Only)";
				newFileFlag = true;
			}
			
			file = temp;
			this.rt.setTitle(fileName + applicationTitleString);
			newFileFlag = false;
		}
		else {
			JOptionPane.showMessageDialog(this.rt, "Failed to Save/Open: " + temp.getPath(), "File Error", JOptionPane.ERROR_MESSAGE);
		}
	}
	
	
	boolean confirmSave() {					//Prompts to Save File
		if(!saved) {
			int reply = JOptionPane.showConfirmDialog(this.rt, "The text in " + file.getPath() + " has been changed.\nDo you want to save the changes?", "Save File", JOptionPane.YES_NO_CANCEL_OPTION);
			
			if(reply == JOptionPane.CANCEL_OPTION)
				return false;
			
			if(reply == JOptionPane.YES_OPTION && !saveAs())
				return false;
		}
		
		return true;
	}
	
	
	int exitDialog() {					//Prompts to Save File before Exit
		return JOptionPane.showConfirmDialog(this.rt, "Do you want to save changes to " + fileName + "?", "Save " + fileName + "?", JOptionPane.YES_NO_CANCEL_OPTION);
	}
	
	
	void newFile() {
		if(! confirmSave())				//Checks if the previous File was saved before invoking New File
			return;
		
		//TextArea is emptied and Attributes are reset
		this.rt.textArea.setText("");
		fileName = "Untitled";
		file = new File(fileName);
		saved = true;
		newFileFlag = true;
		this.rt.setTitle(fileName + applicationTitleString);
	}
}
