
import java.awt.Graphics;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.channels.DatagramChannel;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import javax.imageio.ImageIO;
import javax.imageio.stream.ImageOutputStream;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.TargetDataLine;
import javax.swing.JOptionPane;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.highgui.HighGui;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.opencv.videoio.VideoCapture;
import org.opencv.videoio.Videoio;


/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

class ProducerId
{
    private Integer UniqueId;
    
    private List<Integer> idlist = new ArrayList<Integer>();
    
    public Integer Idfunc()
    {
        Random r = new Random();
        Integer id = r.nextInt(255);
        for(int i=0;i<7;i++)
        {
           if(i%2==0)
            {
                id+=i;
            }
            if(i%2==1)
            {
                id*=i;
            }
        }
        UniqueId=id;
        for(int i=0;i<idlist.size();i++)
        {
            if(idlist.contains(UniqueId))
            {
                Idfunc();
            }
            else
            {
                idlist.add(UniqueId);
            }
        }
        return UniqueId;
    }

}
public class CServer extends javax.swing.JFrame implements Runnable{

    
    public List<ServerClients> clients = new ArrayList<ServerClients>();
    private static int[] ports;
    public int[] clientports = new int[2];
    private DatagramSocket datasocket;
    private DatagramSocket videosocket;
    private DatagramSocket messagesocket;
    private Thread run,send,receive,stream,streamvideo,sendvideo,sendmessage;
    private boolean running = false;
    private boolean runstream = false;
    public String name;
    
     public boolean openConnection(int [] portnumbers)
    {
        try
        {
            datasocket = new DatagramSocket(portnumbers[0]);
            videosocket = new DatagramSocket(portnumbers[1]);
            messagesocket = new DatagramSocket(8194);
        }
        catch(Exception e)
        {
            e.printStackTrace();
            return false;
        }
        return true;
         
    }
    
    public CServer(int[] ports) {
        initComponents();
        this.ports=ports;
       
        boolean stateconnection=openConnection(ports);
        if(!stateconnection)
        {
            JOptionPane.showMessageDialog(null, "Connection Failed");
        }
        else
        {
            JOptionPane.showMessageDialog(null,"Server Started on port: "+ports[0]+"and "+ports[1]+"and "+8194);
        }
           
        jTextField1.requestFocus();
        run = new Thread(this,"");
        run.start();
       
   
    }
       public void run()
    {
        running=true;
        receive();
        //sendToAll("Bu bir Sunucu Mesajıdır");hatalı olabilir ilerde kaldırılabilir
    }
       private void send(final byte[] data,final InetAddress adress,final int port)
     {
         send = new Thread("send")
         {
           public void run()
           {
               DatagramPacket packet = new DatagramPacket(data,data.length, adress, port);
               
               try
               {
                   datasocket.send(packet);
               }
               catch(Exception e)
               {
                   e.printStackTrace();
               }
           }
           
         };
         send.start();
     }
        private void sendvideo(final byte[] data,final InetAddress adress,final int port)
     {
         sendvideo = new Thread("send")
         {
           public void run()
           {
               DatagramPacket packet = new DatagramPacket(data,data.length, adress, port);
               
               try
               {
                   videosocket.send(packet);
               }
               catch(Exception e)
               {
                   e.printStackTrace();
               }
           }
           
         };
         sendvideo.start();
     }
         private void sendToAll(String message)
     {
         for(int i=0;i<clients.size();i++)
         {
            ServerClients istemci =clients.get(i);
            sendsms(message.getBytes(),istemci.adres,istemci.ports[0]);
         }
     }
      private void sendsms(final byte[] data,final InetAddress adress,final int port)
     {
         sendmessage = new Thread("send")
         {
           public void run()
           {
               DatagramPacket packet = new DatagramPacket(data,data.length, adress, port);
               
               try
               {
                   messagesocket.send(packet);
               }
               catch(Exception e)
               {
                   e.printStackTrace();
               }
           }
           
         };
         sendmessage.start();
     }
    
    
    private void ProcessData(DatagramPacket dpack)
    {
        String text = new String(dpack.getData()); 
        Integer id = new ProducerId().Idfunc();
        int port = dpack.getPort();
        
        InetAddress adress = dpack.getAddress();
       
        if(text.startsWith("/clientname"))
         {
             clientports[0]=port;
             name=text.substring(11,text.length());
         }
        else if(text.startsWith("/d/"))
        {
            for(int i=0;i<clients.size();i++)
            {
                if(clients.get(i).ports[0]==port)
                {
                    
                    jTextArea2.append(adress.toString()+" "+dpack.getPort()+" "+text.substring(3,text.length())+" Disconnected"+" id = "+clients.get(i).getId()+"\n");
                    clients.remove(i);
                    break;
                }
            }
        }
        else if(text.startsWith("/n/"))
        {
             
            sendToAll("/sk/"+text.substring(3,text.length())+" "+"\n");
             for(int i=0;i<clients.size();i++)
             {
                
                 if(clients.get(i).ports[0]==dpack.getPort()&&clients.get(i).adres.equals(adress));
                 {
                     jTextArea1.append(" "+clients.get(i).getId()+"/"+text.substring(3,text.length())+" "+"\n");
                     break;
                 }
             }
            
        }
        else if(text.startsWith("/pi/"))
        {
           
            clientports[1]=port;
            clients.add(new ServerClients(name,adress,clientports,id.toString()));
           
           for(int i=0;i<clients.size();i++)
             {
                 if(clients.get(i).ports[1]==dpack.getPort())
                 {
                     jTextArea2.append(adress.toString()+" "+clients.get(i).ports[0]+" "+clients.get(i).name+" Joined"+" id = "+clients.get(i).getId()+"\n");
             
                 }
             }
        }
        
    }
    private void receive()
    {
        receive = new Thread("Receive"){
            public void run()
            {
                while(running)
                {
                    byte [] data = new byte[1024];
                    DatagramPacket dpacket = new DatagramPacket(data, data.length);
                    try
                    {
                        messagesocket.receive(dpacket);
                    }
                    catch(Exception e)
                    {
                        e.printStackTrace();
                    }
                    
                    ProcessData(dpacket);
                    
                }
            }
        };
        receive.start();
    }
    
    private void SoundStreamStart() //throws LineUnavailableException,InterruptedException
    {
       
       stream = new Thread("stream")
       {
          
           public void run()
           {
               try
                {
                AudioFormat aformat = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, 44100, 16, 2, 4, 44100, false);
                DataLine.Info info= new DataLine.Info(TargetDataLine.class, aformat);
                 final TargetDataLine tdl = (TargetDataLine)AudioSystem.getLine(info);
                 
                 tdl.open();
                 tdl.start();
                 
                 byte[]header = "/s/".getBytes();
                 while(runstream)
                 {
                   
                        byte[] soundata=new byte[(tdl.getBufferSize()/5)+3];
                        for(int i=0;i<3;i++)
                        {
                            soundata[i]=header[i];
                        }
                            tdl.read(soundata, 3, soundata.length-3);
                            //out.write(soundata,0,readbytes);
                            
                            for(int i=0;i<clients.size();i++)
                            {
                                ServerClients sc = clients.get(i);
                                send(soundata,sc.adres,sc.ports[1]);
                            }
            
                  }
                 tdl.close();
                
                
                }catch(Exception e){e.printStackTrace();}

              

           } 
       };
       stream.start();
    }
     public void console(String message)
    {
        jTextArea1.append(message+"\n");
    }
    private void StreamVideo()
    {
       System.load("C:\\Users\\asus\\Desktop\\opencv_modules\\opencv\\build\\java\\x64\\opencv_java343.dll");
       VideoCapture cam = new VideoCapture(0);
       cam.set(Videoio.CV_CAP_PROP_FRAME_WIDTH,480);
       cam.set(Videoio.CV_CAP_PROP_FRAME_HEIGHT,320);
       
       
       //String header = "/v/";
       //byte []heads = header.getBytes();
       
       streamvideo = new Thread()
       {
           public void run()
           {
               try{
               while(runstream)
               {
                   Mat frame1 = new Mat();
                   MatOfByte mob = new MatOfByte();
                   if(cam.isOpened())
                   {
                       cam.read(frame1);
                   }
                   Imgcodecs.imencode(".jpg", frame1, mob);
                   byte [] imagedata=mob.toArray();
                   //System.out.println(imagedata.length);
                   for(int i=0;i<clients.size();i++)
                   {
                        ServerClients sc = clients.get(i);
                        sendvideo(imagedata,sc.adres,sc.ports[1]);//ports[1] de yapılabilir
                   }
                   
                   InputStream  is=new ByteArrayInputStream(imagedata);
                   
                   //is.read(array2,0,array2.length); //akışı değiştirmek için miş saçmalık
                   
                   
                   BufferedImage frame=ImageIO.read(is);
                   Graphics g=jPanel1.getGraphics();
                   g.drawImage(frame,0, 0, null);
               }
               cam.release();
               }catch(Exception e){e.printStackTrace();}
           }
           
       };
       streamvideo.start();       
   
    }
   
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jButton1 = new javax.swing.JButton();
        jButton2 = new javax.swing.JButton();
        jLabel1 = new javax.swing.JLabel();
        jScrollPane2 = new javax.swing.JScrollPane();
        jTextArea2 = new javax.swing.JTextArea();
        jPanel1 = new javax.swing.JPanel();
        jScrollPane1 = new javax.swing.JScrollPane();
        jTextArea1 = new javax.swing.JTextArea();
        jTextField1 = new javax.swing.JTextField();
        jButton3 = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);

        jButton1.setFont(new java.awt.Font("Times New Roman", 1, 24)); // NOI18N
        jButton1.setText("START");
        jButton1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton1ActionPerformed(evt);
            }
        });

        jButton2.setFont(new java.awt.Font("Times New Roman", 1, 24)); // NOI18N
        jButton2.setText("STOP");
        jButton2.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton2ActionPerformed(evt);
            }
        });

        jLabel1.setFont(new java.awt.Font("Times New Roman", 1, 24)); // NOI18N
        jLabel1.setText("     ONLİNE AUDİENCES");

        jTextArea2.setEditable(false);
        jTextArea2.setColumns(20);
        jTextArea2.setFont(new java.awt.Font("Monospaced", 3, 12)); // NOI18N
        jTextArea2.setRows(5);
        jScrollPane2.setViewportView(jTextArea2);

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 595, Short.MAX_VALUE)
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 409, Short.MAX_VALUE)
        );

        jTextArea1.setBackground(new java.awt.Color(51, 255, 51));
        jTextArea1.setColumns(20);
        jTextArea1.setRows(5);
        jScrollPane1.setViewportView(jTextArea1);

        jTextField1.setFont(new java.awt.Font("Times New Roman", 0, 18)); // NOI18N
        jTextField1.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyPressed(java.awt.event.KeyEvent evt) {
                jTextField1KeyPressed(evt);
            }
        });

        jButton3.setBackground(new java.awt.Color(0, 204, 255));
        jButton3.setFont(new java.awt.Font("Times New Roman", 0, 24)); // NOI18N
        jButton3.setForeground(new java.awt.Color(255, 0, 51));
        jButton3.setText("Send");
        jButton3.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton3ActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addGroup(layout.createSequentialGroup()
                        .addContainerGap()
                        .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(18, 18, 18))
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                        .addGap(98, 98, 98)
                        .addComponent(jButton1)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(jButton2)
                        .addGap(157, 157, 157)))
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jScrollPane1)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(jTextField1, javax.swing.GroupLayout.PREFERRED_SIZE, 409, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(18, 18, 18)
                        .addComponent(jButton3, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel1, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, 306, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jScrollPane2, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, 279, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 480, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addGap(18, 18, 18)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                .addComponent(jButton1)
                                .addComponent(jButton2))
                            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                                .addComponent(jTextField1)
                                .addComponent(jButton3, javax.swing.GroupLayout.DEFAULT_SIZE, 77, Short.MAX_VALUE)))
                        .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(jLabel1, javax.swing.GroupLayout.PREFERRED_SIZE, 39, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(jScrollPane2, javax.swing.GroupLayout.PREFERRED_SIZE, 492, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addContainerGap(57, Short.MAX_VALUE))))
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void jButton1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton1ActionPerformed
        runstream=true;
        SoundStreamStart();
        StreamVideo();
    }//GEN-LAST:event_jButton1ActionPerformed

    private void jButton2ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton2ActionPerformed
        runstream=false;
    }//GEN-LAST:event_jButton2ActionPerformed

    private void jButton3ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton3ActionPerformed
            
            if(jTextField1.getText().equals(""))
        {
            jTextField1.requestFocus();
            return;
        }
        sendToAll("/sn/"+jTextField1.getText());
        console("ServerMessage: "+jTextField1.getText());
        jTextField1.setText(null);
        jTextField1.requestFocus();
    }//GEN-LAST:event_jButton3ActionPerformed

    private void jTextField1KeyPressed(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_jTextField1KeyPressed
        if(evt.getKeyCode()==KeyEvent.VK_ENTER)
        {
            if(jTextField1.getText().equals(""))
        {
            jTextField1.requestFocus();
            return;
        }
        sendToAll("/sn/"+jTextField1.getText());
        console("ServerMessage: "+jTextField1.getText());
        
        jTextField1.setText(null);
        jTextField1.requestFocus();
            
            
           
        }
    }//GEN-LAST:event_jTextField1KeyPressed

    
    public static void main(String args[]) {
        /* Set the Nimbus look and feel */
        //<editor-fold defaultstate="collapsed" desc=" Look and feel setting code (optional) ">
        /* If Nimbus (introduced in Java SE 6) is not available, stay with the default look and feel.
         * For details see http://download.oracle.com/javase/tutorial/uiswing/lookandfeel/plaf.html 
         */
        try {
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (ClassNotFoundException ex) {
            java.util.logging.Logger.getLogger(CServer.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            java.util.logging.Logger.getLogger(CServer.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            java.util.logging.Logger.getLogger(CServer.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(CServer.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>

        /* Create and display the form */
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                new CServer(ports).setVisible(true);
            }
        });
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton jButton1;
    private javax.swing.JButton jButton2;
    private javax.swing.JButton jButton3;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JTextArea jTextArea1;
    private javax.swing.JTextArea jTextArea2;
    private javax.swing.JTextField jTextField1;
    // End of variables declaration//GEN-END:variables
}
