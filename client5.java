import java.net.*;
import java.io.*;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import javax.swing.table.*;
import javax.swing.event.*;
class ClientModel extends AbstractTableModel
{
private ArrayList<File> files; 
ClientModel()
{
this.files=new ArrayList<>();
}
public ArrayList<File> getFiles()
{
return this.files;
}
public Object getValueAt(int rowIndex,int columnIndex)
{
if(columnIndex==0) return (rowIndex+1);
return this.files.get(rowIndex).getAbsolutePath();
}
public Class getColumnClass(int columnIndex)
{
if(columnIndex==0) return Integer.class;
else return String.class;
}
public boolean add(File file)
{
if(this.files.contains(file)) return true;
else
{
this.files.add(file);
fireTableDataChanged();
return false;
}
}
public void clean(File file)
{
this.files.remove(file);
fireTableDataChanged();
}
public String getColumnName(int columnIndex)
{
if(columnIndex==0) return "S.NO";
return "Files Name";
}
public int getRowCount()
{
return this.files.size();
}
public int getColumnCount()
{
return 2;
}
public boolean isCellEditTabe(int rowIndex,int columnIndex)
{
return false;
}
}
//********************************************************************************************
class FileUploadEvent
{
private String uploaderId;
private long numberOfBytesUploaded;
private File file;
public FileUploadEvent()
{
this.uploaderId=null;
this.file=null;
this.numberOfBytesUploaded=0;
}
public void setUploaderId(String uploaderId)
{
this.uploaderId=uploaderId;
}
public String getUploaderId()
{
return this.uploaderId;
}
public void setNumberOfBytesUploaded(long numberOfBytesUploaded)
{
this.numberOfBytesUploaded=numberOfBytesUploaded;
}
public long getNumberOfBytesUploaded()
{
return this.numberOfBytesUploaded;
}
public void setFile(File file)
{
this.file=file;
}
public File getFile()
{
return this.file;
}
}
//********************************************************************************************
interface FileUploadListener
{
public void fileUploadStatusChanged(FileUploadEvent fileUploadEvent);
}
class FTClientFrame extends JFrame
{
private String host;
private int portNumber;
private Container container;
private FileSelectionViewPanel fileSelectionViewPanel;
private FileUploadsViewPanel fileUploadsViewPanel;
FTClientFrame(String host,int portNumber)
{
this.host=host;
this.portNumber=portNumber;
container=getContentPane();
container.setLayout(new GridLayout(1,2));
fileSelectionViewPanel=new FileSelectionViewPanel();
fileUploadsViewPanel=new FileUploadsViewPanel();
container.add(fileSelectionViewPanel);
container.add(fileUploadsViewPanel);
setVisible(true);
setLocation(300,200);
setSize(1200,600);
setDefaultCloseOperation(EXIT_ON_CLOSE);
}
/* This needes to be removed
public void setBytesUploaded(String id,int bytesUploaded)
{
//do something here to update progress bar
fileUploadsViewPanel.totalBytesUploaded(id,bytesUploaded);
}
public void fileUploaded(String id) //We may not need this method
{
}*/
class FileSelectionViewPanel extends JPanel
{
private JPanel entryPanel;
private JLabel portNumberLabel;
private JTextField portNumberTextField;
private JLabel hostLabel;
private JTextField hostTextField;
private JLabel titleLabel;
private JTable fileTable;
private ClientModel clientModel;
private JButton selectFileButton;
private JScrollPane jsp;
FileSelectionViewPanel()
{
setLayout(new BorderLayout());
titleLabel=new JLabel("Selected Files");
entryPanel=new JPanel();
entryPanel.setLayout(new GridLayout(2,6));
hostLabel=new JLabel("host");
hostTextField=new JTextField(10);
hostTextField.requestFocus();
portNumberLabel=new JLabel("Port Number");
portNumberTextField=new JTextField(10);
entryPanel.add(hostLabel);
entryPanel.add(hostTextField);
entryPanel.add(new JLabel("              "));
entryPanel.add(portNumberLabel);
entryPanel.add(portNumberTextField);
entryPanel.add(new JLabel("           "));
entryPanel.add(titleLabel);
entryPanel.add(new JLabel("     "));
entryPanel.add(new JLabel("     "));
entryPanel.add(new JLabel("     "));
entryPanel.add(new JLabel("     "));
entryPanel.add(new JLabel("     "));
clientModel=new ClientModel();
fileTable=new JTable(clientModel);
jsp=new JScrollPane(fileTable,ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS,ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS);
selectFileButton=new JButton("Add Files"); 
selectFileButton.addActionListener(new ActionListener(){
public void actionPerformed(ActionEvent ev)
{
JFileChooser selectJFileChooser;
JFileChooser jfc=new JFileChooser();
//jfc.setAcceptAllFileFileterUsed(flase);
jfc.setCurrentDirectory(new File("."));
//jfc.addChoosableFileFilterUsed(new javax.swing.fileChooser.FileFilter(){});
int selectedOption=jfc.showOpenDialog(FileSelectionViewPanel.this);
boolean isFileAlreadySelected=false;
if(selectedOption==jfc.APPROVE_OPTION)
{
File selectedFile=jfc.getSelectedFile();
isFileAlreadySelected=FileSelectionViewPanel.this.clientModel.add(selectedFile);
if(isFileAlreadySelected)
{
JOptionPane.showMessageDialog(FileSelectionViewPanel.this,"This file already selected, Select another File.");
return;
}
}
else
{
}
}
});
//FTClientFrame.this.host=hostTextField.getText(); no need
//FTClientFrame.this.portNumber=Integer.parseInt(portNumberTextField.getText());
add(entryPanel,BorderLayout.NORTH);
add(jsp,BorderLayout.CENTER);
add(selectFileButton,BorderLayout.SOUTH);
}
public void clean(File file)
{
this.clientModel.clean(file);
}
public ArrayList<File> getFiles()
{
return this.clientModel.getFiles();
}
}//innter class ends here
class FileUploadsViewPanel extends JPanel implements ActionListener,FileUploadListener
{
private JButton uploadButton;
private JPanel progressBarPanelsContainer;
private ArrayList<ProgressPanel> progressBarPanels;
private ArrayList<File> files;
private ArrayList<FTClientUploadThread> fileUploaders;
private JScrollPane jsp;
//private Map<String,ProgressPanel> progressCollection;
private ProgressPanel progressPanel;
private long lengthOfFile=0;
private int selector=0;
FileUploadsViewPanel()
{
uploadButton=new JButton("Upload Files");
setLayout(new BorderLayout());
add(uploadButton,BorderLayout.NORTH);
uploadButton.addActionListener(this);
}
public void actionPerformed(ActionEvent ev)
{
files=fileSelectionViewPanel.getFiles();
lengthOfFile=files.size();
if(files.size()==0) JOptionPane.showMessageDialog(FTClientFrame.this,"No Files selected to upload");
progressBarPanelsContainer=new JPanel();
progressBarPanelsContainer.setLayout(new GridLayout(files.size(),1));
progressBarPanels=new ArrayList<>();
FTClientUploadThread clientUpload=null;
host=fileSelectionViewPanel.hostTextField.getText();  
portNumber=Integer.parseInt(fileSelectionViewPanel.portNumberTextField.getText());
String uploaderId;
//this.progressCollection=new HashMap<>();
this.fileUploaders=new ArrayList<>();
for(File file : files)
{
uploaderId=UUID.randomUUID().toString();
progressPanel=new ProgressPanel(uploaderId,file);
progressBarPanelsContainer.add(progressPanel);
progressBarPanels.add(progressPanel);
//progressCollection.put(id,progressPanel);
clientUpload=new FTClientUploadThread(FileUploadsViewPanel.this,file,uploaderId,host,portNumber); 
this.fileUploaders.add(clientUpload);
}
jsp=new JScrollPane(progressBarPanelsContainer,ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS,ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS);
add(jsp,BorderLayout.CENTER);
this.revalidate();
this.repaint();
for(FTClientUploadThread clientUploadStarter: this.fileUploaders)
{
clientUploadStarter.start();
}
}
public void fileUploadStatusChanged(FileUploadEvent fue) 
{
String uploaderId=fue.getUploaderId();
long numberOfBytesUploaded=fue.getNumberOfBytesUploaded();
File file=fue.getFile();
for(ProgressPanel progressPanel : progressBarPanels)
{
if(progressPanel.getId().equals(uploaderId))
{
progressPanel.updateProgressBar(numberOfBytesUploaded);
break;
}
}
}
/*public void totalBytesUploaded(String id,int bytesUploaded)
{
//totalBytesUpload+=bytesUploaded;
progressPanel=progressCollection.get(id);
progressPanel.updateProgressBar(totalBytesUpload);
}//done*/

public class ProgressPanel extends JPanel 
{
private JLabel fileNameLabel; 
private JProgressBar progressBar;
private File file;
private long fileLength;
private String id;
ProgressPanel(String id,File file)
{
this.id=id;
this.file=file;
this.fileLength=file.length();
fileNameLabel=new JLabel("Uploading : "+file.getAbsolutePath());
setLayout(new GridLayout(2,1));
progressBar=new JProgressBar(1,100);
add(fileNameLabel);
add(progressBar);
}
public String getId()
{
return this.id;
}
//an function comes over here
public void updateProgressBar(long bytesUploaded)
{
int percentage;
if(bytesUploaded==fileLength) percentage=100;
else percentage=(int)((bytesUploaded*100)/fileLength);
progressBar.setValue(percentage);
if(percentage==100) 
{
fileNameLabel.setText("Uploaded : "+this.file.getAbsolutePath());
int selectedOption=JOptionPane.showConfirmDialog(this,"Do You want to clean","CleanUp",JOptionPane.YES_NO_CANCEL_OPTION,JOptionPane.QUESTION_MESSAGE);
if(selectedOption==JOptionPane.YES_OPTION)
{
System.out.println("Yes clicked");
fileSelectionViewPanel.clean(file);  
this.setVisible(false);
}
}
//ths function is gona called form the setBytesUploaded function
}
}//progressPanel class ends here
}//FileUploadsViewPanel class ends here

public static void main(String gg[])
{
new FTClientFrame("localhost",5500);
}
}

class FTClientUploadThread extends Thread
{
private FileUploadListener fileUploadListener;
private File file;
private String id;
private String host;
private int portNumber;
FTClientUploadThread(FileUploadListener fileUploadListener,File file,String id,String host,int portNumber)
{
this.fileUploadListener=fileUploadListener;
this.file=file;
this.id=id;
this.host=host;
this.portNumber=portNumber;
}
public void run()
{
try
{
String FILE_NAME=file.getAbsolutePath();
File file=new File(FILE_NAME);
if(!file.exists())
{
System.out.println("File : "+FILE_NAME+" not exists.");
return;
}
if(file.isDirectory())
{
System.out.println(FILE_NAME+" is a directory not a file");
return;
}
long fileLength=file.length();
String file_name=file.getName();
Socket socket=new Socket(host,portNumber);
int chunkSize=1024;
byte header[]=new byte[1024];
long x=fileLength;
int i=0;
while(x>0)
{
header[i]=(byte)(x%10);
x=x/10;
i++;
}
header[i]=(byte)',';
i++;
long bytesToRead=file_name.length();
int j=0;
while(j<bytesToRead)
{
header[i]=(byte)file_name.charAt(j);
i++;
j++;
}
while(i<1024)
{
header[i]=(byte)32; //put spaces at the remaning indexes of header
i++;
}
OutputStream os=socket.getOutputStream();
os.write(header,0,1024);
os.flush();
System.out.println("Header Sent of length : "+fileLength+", and file name : "+file_name);
byte ack[]=new byte[1];
InputStream is=socket.getInputStream();
int bytesReadCount;
while(true)
{
bytesReadCount=is.read(ack);
if(bytesReadCount==-1) continue;
break;
}
System.out.println("Acknowledgement received");
chunkSize=4096;
byte bytes[]=new byte[chunkSize];
long bytesToSend=fileLength;
j=0;
FileInputStream fis=new FileInputStream(file); //load file in fis
while(j<bytesToSend)
{
bytesReadCount=fis.read(bytes); //reading 4096 bytes at each cycle from the given file and send it to server
if(bytesReadCount==-1) continue;
os.write(bytes,0,bytesReadCount); //fis.read(bytes) alwas write in byte array from 0 index
os.flush();
j=j+bytesReadCount;
int brc=j;
SwingUtilities.invokeLater(()->{
FileUploadEvent fileUploadEvent=new FileUploadEvent();
fileUploadEvent.setUploaderId(this.id);
fileUploadEvent.setFile(file);
fileUploadEvent.setNumberOfBytesUploaded(brc);
FTClientUploadThread.this.fileUploadListener.fileUploadStatusChanged(fileUploadEvent);
});
}
fis.close();
while(true)
{
bytesReadCount=is.read();
if(bytesReadCount==-1) continue;
break;
}
System.out.println("Acknowledgement received");
System.out.println("File : "+FILE_NAME+" is uploded to server");
socket.close();
}catch(Exception e)
{
System.out.println(e);
}
}
}