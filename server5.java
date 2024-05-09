import java.awt.*;
import java.awt.event.*;
import java.util.*;
import javax.swing.*;
import javax.swing.event.*;
import java.io.*;
import java.net.*;
class FileTransferUI extends JFrame
{
private JLabel portNumberLabel;
private JTextField portNumberTextField;
private JTextArea serverInformationTextArea;
private JButton startButton;
private Container container;
private int portNumber;
private FTServer fTServer;
private JScrollPane jsp;
private boolean serverState=false;
FileTransferUI()
{
initComponents();
setAppearance();
addListeners();
}
public void updateLog(String appendString)
{
serverInformationTextArea.append(appendString+'\n');
}
private void initComponents()
{
portNumberLabel=new JLabel("Port Number");
portNumberTextField=new JTextField(10);
serverInformationTextArea=new JTextArea();
jsp=new JScrollPane(serverInformationTextArea,ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS,ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
startButton=new JButton("Start");
container=getContentPane();
}
private void setAppearance()
{
int lm,tm;
lm=0;
tm=0;
setLayout(null);
portNumberLabel.setBounds(lm+80,tm+30,80,40);
portNumberTextField.setBounds(lm+80+80+5,tm+30,80,40);
startButton.setBounds(lm+80+80+80+5+15,tm+30,100,40);
jsp.setBounds(lm+10,tm+30+40+10,560,450);
container.add(portNumberLabel);
container.add(portNumberTextField);
container.add(startButton);
container.add(jsp);
setSize(600,600);
setLocation(300,200);
setVisible(true);
setDefaultCloseOperation(EXIT_ON_CLOSE);
}
private void addListeners()
{
/*portNumberTextField.getDocument().addDocumentListener(new DocumentListener(){
public void insertUpdate(DocumentEvent de)
{
String portNumberString=portNumberTextField.getText();
if(portNumberString.length()==4)
{
FileTransferUI.this.portNumber=Integer.parseInt(portNumberString);
System.out.println(portNumber);
}
}
public void changedUpdate(DocumentEvent de)
{
//startServer();
}
public void removeUpdate(DocumentEvent de)
{
String portNumberString=portNumberTextField.getText();
if(portNumberString.length()==4)
{
FileTransferUI.this.portNumber=Integer.parseInt(portNumberString);
System.out.println(portNumber);
}
}
});*/
startButton.addActionListener(new ActionListener(){
public void actionPerformed(ActionEvent ev)
{
if(serverState==false)
{
startServer();
serverState=true;
}
else
{
fTServer.shutDown();
serverState=false;
startButton.setText("Start");
serverInformationTextArea.append("Server Stoped\n");
}
}
private void startServer()
{
serverInformationTextArea.append("Server Started\n");
portNumber=Integer.parseInt(portNumberTextField.getText());
fTServer=new FTServer(portNumber,FileTransferUI.this);
fTServer.start();
startButton.setText("Stop");
}
});
}
public static void main(String gg[])
{
FileTransferUI fileTransferUI=new FileTransferUI();
}
} 
//********************************************************************************************
class RequestProcessor extends Thread
{
private Socket socket;
private FileTransferUI fileTransferUI;
String id;
RequestProcessor(Socket socket,String id,FileTransferUI fileTransferUI)
{
this.fileTransferUI=fileTransferUI;
this.id=id;
this.socket=socket;
start();
}
public void run()
{
try
{
SwingUtilities.invokeLater(new Runnable(){
public void run()
{
fileTransferUI.updateLog("Client connected and id alloted is : "+id);
}
});
InputStream is=socket.getInputStream();
OutputStream os=socket.getOutputStream();
byte tmp[]=new byte[1024];
byte header[]=new byte[1024];
int bytesToReceive=1024;
int j=0;
int bytesReadCount;
int k,i;
i=0;
while(j<bytesToReceive)
{
bytesReadCount=is.read(tmp);
if(bytesReadCount==-1) continue;
for(k=0;k<bytesReadCount;k++)
{
header[i]=tmp[k];
i++;
}
j=j+bytesReadCount;
}
i=0;
int fileLength=0;
j=1;
while(header[i]!=',')
{
fileLength=fileLength+(header[i]*j);
j=j*10;
i++;
}
i++;
String file_name;
StringBuffer sb=new StringBuffer();
while(i<1024)
{
sb.append((char)header[i]);
i++;
}
file_name=sb.toString().trim();
int fl=fileLength;
System.out.println("Header received of length : "+fl+", and file name : "+file_name);
SwingUtilities.invokeLater(new Thread(){
public void run()
{
fileTransferUI.updateLog("Header received of length : "+fl+", and file name : "+file_name);
}
});
byte ack[]=new byte[1];
ack[0]=1;
os.write(ack,0,1);
os.flush();
System.out.println("Acknowledgement sent");
SwingUtilities.invokeLater(new Thread(){
public void run()
{
fileTransferUI.updateLog("Acknowledgement sent");
}
});
File file=new File("uploads"+File.separator+file_name);
if(file.exists()) file.delete();
FileOutputStream ofs=new FileOutputStream(file);
bytesToReceive=fileLength;
j=0;
int chunkSize=4096;
byte bytes[]=new byte[chunkSize];
while(j<bytesToReceive)
{
bytesReadCount=is.read(bytes);
if(bytesReadCount==-1) continue;
ofs.write(bytes,0,bytesReadCount); //OutputFileStream wirte the given bytes from bytes array on upload folder of server
ofs.flush();
j=j+bytesReadCount;
}
ofs.close();
ack[0]=1;
os.write(ack,0,1);
os.flush();
System.out.println("Acknowledgement sent");
SwingUtilities.invokeLater(new Thread(){
public void run()
{
fileTransferUI.updateLog("Acknowledgement sent");
}
});
System.out.println("server is reciving File : "+file_name+", plese for a minute.");
SwingUtilities.invokeLater(new Thread(){
public void run()
{
fileTransferUI.updateLog("server is reciving File : "+file_name+", plese for a minute.");
}
});
socket.close();
System.out.println("File saved as : "+file.getAbsolutePath());
SwingUtilities.invokeLater(new Thread(){
public void run()
{
fileTransferUI.updateLog("File saved as : "+file.getAbsolutePath());
fileTransferUI.updateLog("Connection with client whose id is : "+id+" closed.");
}
});
}catch(Exception e)
{
System.out.println(e);
}
}
}// class end
class FTServer extends Thread
{
private ServerSocket serverSocket;
private FileTransferUI fileTransferUI;
private int portNumber;
FTServer(int portNumber,FileTransferUI fileTransferUI)
{
this.portNumber=portNumber;
this.fileTransferUI=fileTransferUI;
}
public void run()
{
try
{
serverSocket=new ServerSocket(portNumber);
startListening();
}catch(Exception e)
{
System.out.println(e);
}
}
public void shutDown()
{
try
{
serverSocket.close();
}catch(Exception e)
{
System.out.println(e); //remove after testing
}
}
private void startListening()
{
try
{
Socket socket;
RequestProcessor requestProcessor;
while(true)
{
System.out.println("Krish's server is ready to accept your request on port number : "+portNumber+".\nHow can I help you");
SwingUtilities.invokeLater(()->{
fileTransferUI.updateLog("Krish's server is ready to accept your request on port number : "+portNumber+".\nHow can I help you");
});
socket=serverSocket.accept(); // server goes in wait mode to accept request and when request arrives then it is diverted to another socket and address of that socket is return to us so that we can use the feature of multithreading
requestProcessor=new RequestProcessor(socket,UUID.randomUUID().toString(),this.fileTransferUI);
}
}catch(Exception e)
{
System.out.println("Server stop listening");
}
}
}
