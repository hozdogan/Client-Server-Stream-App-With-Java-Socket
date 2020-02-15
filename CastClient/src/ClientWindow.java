

import java.awt.Graphics;
import java.awt.Image;
import java.awt.List;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import javax.imageio.ImageIO;
import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.ImageOutputStream;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.SourceDataLine;
import javax.swing.ImageIcon;
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


public class ClientWindow extends javax.swing.JFrame implements Runnable{

   private static String name,adress;
    private static int port;
    private InetAddress ip;
    private Client client;
    private Thread run,receivesound,streamsound,streamvideo,receivevideo;
    private boolean running=false;
    private boolean runstream = false;
    private boolean runsound=false;
    private boolean runreceivevideo=false;
    private boolean runstreamvideo=false;
    
    
    public int len;
    public int off=0;
    public int jit=0;
    public int offvid=0; 
    public int sumlen=0;
     ByteArrayOutputStream out = new ByteArrayOutputStream();  
     public ArrayList<BufferedImage> framebuffer = new ArrayList<BufferedImage>();
     Object kilit = new Object();
     Object kilit2 = new Object();
    public ClientWindow(String name,String adress,int port) {
        initComponents();
        Image img1 = new ImageIcon(this.getClass().getResource("/img/play_icon.png")).getImage();
        jButton3.setIcon(new ImageIcon(img1));
        Image img2 = new ImageIcon(this.getClass().getResource("/img/stop-icon.png")).getImage();
        jButton4.setIcon(new ImageIcon(img2));
        Graphics g =jPanel1.getGraphics();
        this.name=name;
        this.port=port;
        this.adress=adress;
        client=new Client(name,adress,port);
        boolean stateconnection=client.openConnection(adress);//server porta bağlıyken yeni soket açınca hata veriyor
        if(!stateconnection)
        {
            JOptionPane.showMessageDialog(null,"Connection Failed");
        }
        else{
            JOptionPane.showMessageDialog(null,"Connection Successfully Welcome Chat "+name.toUpperCase()+"\n"+"Adress:"+adress+" "+"port:"+port+"\n");//append yapıp newline koyarsan alta gelir
        }
         
        String ConnectionString = name.toUpperCase();//adres ve portu datagram packette verdiğimiz icin string işleme ile bu istemciyi server listesine eklemek için adres ve portu kaldırıyoruz
        client.sendData(("/clientname"+ConnectionString).getBytes());//gönderilen byte in ilk şeyi bunlar başına işaret koyar yollarım mesaj mı client kaydımı ayırt etsin diye ex:/clientname   
        client.sendinfoData(("/pi/"+"Bu görüntü ve ses port bilgisidir.").getBytes());
        jTextField1.requestFocus();
        running = true;
        run= new Thread(this,"Running");
        run.start();

    }
    public void run()
    {
        listenservermessage();
    }
    public void send()
    {
        if(jTextField1.getText().equals(""))
        {
            jTextField1.requestFocus();
            return;
        }
        //console(client.getName().toUpperCase()+":"+jTextField1.getText());
        client.sendData(("/n/"+client.getName().toUpperCase()+":"+jTextField1.getText()).getBytes());
        jTextField1.setText(null);
        jTextField1.requestFocus();
       
    }
    public void listenservermessage()
    {
        Thread listenserver = new Thread()
        {
          public void run()
          {
              
             try
             {
                  while(running)
              {
                  byte [] msgarray = client.receivemsgdata();
                  String message=new String(msgarray);
                 
                  if(message.startsWith("/n/"))
                  {
                      console(message.substring(3,message.length()));
                  }
                  else if(message.startsWith("/sn/"))
                  {
                      console("ServerMessage: "+message.substring(4,message.length()));
                  }
                  else if(message.startsWith("/sk/"))
                  {
                      console(message.substring(4,message.length()));
                  }
                  
              }
             }catch(Exception e){e.printStackTrace();}
              
              
          }
             
        };
        listenserver.start();
        
    }
     
    public void console(String message)
    {
        jTextArea1.append(message+"\n");
    }
    private void clear(ByteArrayOutputStream obj)
    {
        obj.reset();
    }
   
    private void StreamReceive()
    {
       byte [] header1 ="/s/".getBytes();
       
        
        receivesound = new Thread()
      {
           public void run()
           {  
               try
                {
                 int count=0;
                //synchronized(out){    
                    while(runstream){
                    
                       // for(int jitter=0;jitter<200;jitter++){
                              
                            byte [] data = client.receivedata();
                            
                             for(int i=0;i<3;i++)
                             {
                                 if(data[i]==header1[i])
                                 {
                                     count++;
                                 }
                                 
                             }
                             if(count==3)
                            {
                                out.write(data,3,17640);
                                count=0;
                            }
                             else
                            { 
                             
                               InputStream in=new ByteArrayInputStream(data);
                               BufferedImage frame=ImageIO.read(in);
                               framebuffer.add(frame);
                               
                               Graphics g=jPanel1.getGraphics();
                               g.drawImage(frame,100, 60, null);
                           
                            }
                        
                        
                               
                       // }    
                          
                               
             
                }   
                
                }catch(Exception e){e.printStackTrace();}
         }
      };
       
 
        streamsound = new Thread()
        {
           public void run()
           {  
               try
                {
                    AudioFormat aformat = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, 44100, 16, 2, 4, 44100, false);//runsound da koyarak sesi birden kesmeyip kaldıgı yerden devam edebiliriz
                    DataLine.Info info = new DataLine.Info(SourceDataLine.class, aformat);
                    final  SourceDataLine sdl = (SourceDataLine)AudioSystem.getLine(info);//aynı hat üzerinden
                    sdl.open();
                    sdl.start();
                   
                   //synchronized(out){
                   while(runstream)
                    {
                        sdl.write(out.toByteArray(), off, 3528);//arrayindex aşımı buffer dolmadan okumasından dolayı bekleyecez 17640
                        off+=3528;

                    }
                   //}
                }catch(Exception e){e.printStackTrace();}
         }
         };
        
             
         
        
        receivesound.start();
        try
        {
            Thread.sleep(1000);
        }catch(Exception e){e.printStackTrace();}
        streamsound.start();
        
       
     
    }
        
   
   
    
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jButton3 = new javax.swing.JButton();
        jButton4 = new javax.swing.JButton();
        jPanel1 = new javax.swing.JPanel();
        jScrollPane1 = new javax.swing.JScrollPane();
        jTextArea1 = new javax.swing.JTextArea();
        jTextField1 = new javax.swing.JTextField();
        jButton1 = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setResizable(false);
        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosed(java.awt.event.WindowEvent evt) {
                formWindowClosed(evt);
            }
            public void windowClosing(java.awt.event.WindowEvent evt) {
                formWindowClosing(evt);
            }
        });

        jButton3.setFont(new java.awt.Font("Times New Roman", 1, 18)); // NOI18N
        jButton3.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton3ActionPerformed(evt);
            }
        });

        jButton4.setFont(new java.awt.Font("Times New Roman", 1, 18)); // NOI18N
        jButton4.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton4ActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 617, Short.MAX_VALUE)
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 432, Short.MAX_VALUE)
        );

        jTextArea1.setEditable(false);
        jTextArea1.setBackground(new java.awt.Color(255, 255, 51));
        jTextArea1.setColumns(20);
        jTextArea1.setRows(5);
        jScrollPane1.setViewportView(jTextArea1);

        jTextField1.setFont(new java.awt.Font("Times New Roman", 0, 18)); // NOI18N
        jTextField1.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyPressed(java.awt.event.KeyEvent evt) {
                jTextField1KeyPressed(evt);
            }
        });

        jButton1.setBackground(new java.awt.Color(0, 204, 255));
        jButton1.setFont(new java.awt.Font("Times New Roman", 0, 24)); // NOI18N
        jButton1.setForeground(new java.awt.Color(255, 0, 0));
        jButton1.setText("Send");
        jButton1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton1ActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addGap(89, 89, 89)
                        .addComponent(jButton3, javax.swing.GroupLayout.PREFERRED_SIZE, 90, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(208, 208, 208)
                        .addComponent(jButton4, javax.swing.GroupLayout.PREFERRED_SIZE, 90, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(layout.createSequentialGroup()
                        .addContainerGap()
                        .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addGap(24, 24, 24)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(jTextField1, javax.swing.GroupLayout.PREFERRED_SIZE, 526, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(18, 18, 18)
                        .addComponent(jButton1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                    .addComponent(jScrollPane1))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 447, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 24, Short.MAX_VALUE)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jTextField1, javax.swing.GroupLayout.PREFERRED_SIZE, 66, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jButton1, javax.swing.GroupLayout.PREFERRED_SIZE, 66, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addGap(0, 17, Short.MAX_VALUE))
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                            .addComponent(jButton4, javax.swing.GroupLayout.DEFAULT_SIZE, 86, Short.MAX_VALUE)
                            .addComponent(jButton3, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                        .addGap(0, 0, Short.MAX_VALUE))))
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void jButton3ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton3ActionPerformed
       runstream=true;
       StreamReceive();
       
    }//GEN-LAST:event_jButton3ActionPerformed

    private void jButton4ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton4ActionPerformed
        runstream=false;
        clear(out);
    }//GEN-LAST:event_jButton4ActionPerformed

    private void formWindowClosing(java.awt.event.WindowEvent evt) {//GEN-FIRST:event_formWindowClosing
           String disconnect=name.toUpperCase();//giden mesaj doğru
           client.sendData(("/d/"+disconnect).getBytes());
    }//GEN-LAST:event_formWindowClosing

    private void formWindowClosed(java.awt.event.WindowEvent evt) {//GEN-FIRST:event_formWindowClosed
       client.close();
    }//GEN-LAST:event_formWindowClosed

    private void jButton1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton1ActionPerformed
        send();
    }//GEN-LAST:event_jButton1ActionPerformed

    private void jTextField1KeyPressed(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_jTextField1KeyPressed
       if(evt.getKeyCode()==KeyEvent.VK_ENTER)
       {
           send();
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
            java.util.logging.Logger.getLogger(ClientWindow.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            java.util.logging.Logger.getLogger(ClientWindow.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            java.util.logging.Logger.getLogger(ClientWindow.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(ClientWindow.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>

        /* Create and display the form */
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                new ClientWindow(name,adress,port).setVisible(true);
            }
        });
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton jButton1;
    private javax.swing.JButton jButton3;
    private javax.swing.JButton jButton4;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JTextArea jTextArea1;
    private javax.swing.JTextField jTextField1;
    // End of variables declaration//GEN-END:variables
}
