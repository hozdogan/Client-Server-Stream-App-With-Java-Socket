
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */


public class Client {

    private static String name,adress;
    private static int port;
    private DatagramSocket datagramsocket;
    private DatagramSocket datagrammsgsocket;
    private InetAddress ip;
    private Thread sendproc;//gönderme thread le yapılacakki çakışma olmasın mesaj kaybolmasın
    
   
    
    public Client(String name,String adress,int port)
    {
        this.adress=adress;
        this.name=name;
        this.port=port;
        
    }
    public String getAdress(){
        return adress;
    }
    public String getName()
    {
        return name;
    }
    public int getPort()
    {
        return port;
    }
    
    public boolean openConnection(String adress)
    {
        try
        {
            datagramsocket = new DatagramSocket();
            datagrammsgsocket = new DatagramSocket();
            ip=InetAddress.getByName(adress);//ismi ip ye dönusturuyor sonradan bind yapacak socketi direk açtı
        }
        catch(Exception e)
        {
            e.printStackTrace();
            return false;
        }
        return true;
      
    }
    public void close()
    {
        new Thread()
        {
            public void run()
            {
                synchronized(datagramsocket){
                    datagramsocket.close();
                    datagrammsgsocket.close();
        }
            }
        }.start();
 
    }
    
    
    public void sendData(final byte[]data)
    {
        sendproc = new Thread()//süreci olustur kos
        {
          public void run()
          {
              DatagramPacket datapack = new DatagramPacket(data, data.length,ip,port);//sunucunun listen yaptıgı port o veri gonderirken bu adresten bu porta veri geldi çekerkende packet.getport packet gelen port ok 
              try
              {
                  datagrammsgsocket.send(datapack);

              }
              catch(Exception e)
              {
                  e.printStackTrace();
              }
          }
        };
        sendproc.start();
        
    }
    public byte[] receivedata()throws Exception 
    {
        byte []data = new byte[65536];
        DatagramPacket datapacket = new DatagramPacket(data,data.length);//veri ve uzunluğu ağlar kitabındaki gibi kaç byte okunacak 1 kb gelecek
        datagramsocket.receive(datapacket);
        
        return data;
    }
    public byte[] receivemsgdata()throws Exception 
    {
        byte []data = new byte[1024];
        DatagramPacket datapacket = new DatagramPacket(data,data.length);//veri ve uzunluğu ağlar kitabındaki gibi kaç byte okunacak 1 kb gelecek
        datagrammsgsocket.receive(datapacket);
        
        return data;
    }
    public void sendinfoData(final byte[]data)
    {
         DatagramPacket datapack = new DatagramPacket(data, data.length,ip,port);//sunucunun listen yaptıgı port o veri gonderirken bu adresten bu porta veri geldi çekerkende packet.getport packet gelen port ok 
              try
              {
                  datagramsocket.send(datapack);

              }
              catch(Exception e)
              {
                  e.printStackTrace();
              }
        
    }
}
    

